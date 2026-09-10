import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { initializeApp, cert, getApps } from "npm:firebase-admin/app";
import { getMessaging } from "npm:firebase-admin/messaging";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const firebaseProjectId = Deno.env.get("FIREBASE_PROJECT_ID")!;
const firebaseClientEmail = Deno.env.get("FIREBASE_CLIENT_EMAIL")!;
const firebasePrivateKey = (Deno.env.get("FIREBASE_PRIVATE_KEY") || "").replace(/\\n/g, "\n");

if (!getApps().length) {
  initializeApp({
    credential: cert({
      projectId: firebaseProjectId,
      clientEmail: firebaseClientEmail,
      privateKey: firebasePrivateKey,
    }),
  });
}

const admin = createClient(supabaseUrl, serviceRoleKey, {
  auth: { autoRefreshToken: false, persistSession: false },
});

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const authHeader = req.headers.get("Authorization") || "";
    if (!authHeader.startsWith("Bearer ")) {
      return json({ error: "Unauthorized" }, 401);
    }

    const jwt = authHeader.replace("Bearer ", "").trim();
    const { data: userData, error: userError } = await admin.auth.getUser(jwt);
    if (userError || !userData.user) {
      return json({ error: "Invalid session" }, 401);
    }

    const payload = await req.json().catch(() => ({}));
    const patientId = String(payload.patient_id || "").trim();
    const bookingId = payload.booking_id ? String(payload.booking_id) : null;
    const city = String(payload.city || "الأنبار");

    if (!patientId || patientId !== userData.user.id) {
      return json({ error: "Invalid patient_id" }, 400);
    }

    // إرسال الإشعار إلى الممرضين المعتمدين والمتاحين والمشتركين فعلياً.
    const { data: nurses, error: nurseError } = await admin
      .from("nurses")
      .select("user_id, full_name")
      .eq("is_verified", true)
      .eq("is_available", true)
      .eq("subscription_status", "ACTIVE")
      .gt("subscription_end", new Date().toISOString());

    if (nurseError) throw nurseError;

    const nurseUserIds = (nurses || [])
      .map((n) => n.user_id)
      .filter(Boolean);

    if (!nurseUserIds.length) {
      return json({ sent: 0, message: "لا يوجد ممرضون متاحون حالياً" });
    }

    const { data: tokens, error: tokenError } = await admin
      .from("notification_tokens")
      .select("token, user_id")
      .in("user_id", nurseUserIds)
      .eq("role", "nurse");

    if (tokenError) throw tokenError;

    const uniqueTokens = [...new Set((tokens || []).map((x) => x.token).filter(Boolean))];
    if (!uniqueTokens.length) {
      return json({ sent: 0, message: "لا توجد أجهزة ممرضين مسجلة للإشعارات" });
    }

    const messaging = getMessaging();
    let sent = 0;
    let failed = 0;

    for (let i = 0; i < uniqueTokens.length; i += 500) {
      const batch = uniqueTokens.slice(i, i + 500);
      const response = await messaging.sendEachForMulticast({
        tokens: batch,
        notification: {
          title: "طلب تمريض منزلي جديد 🔔",
          body: `يوجد طلب جديد في ${city}. افتح طلبات المرضى لمراجعته.`,
        },
        data: {
          target: "nurse_requests",
          booking_id: bookingId || "",
          type: "new_booking",
        },
        android: {
          priority: "high",
          notification: {
            channelId: "tamreed_fcm",
            sound: "default",
          },
        },
      });
      sent += response.successCount;
      failed += response.failureCount;
    }

    return json({ sent, failed });
  } catch (error) {
    console.error(error);
    return json({ error: error instanceof Error ? error.message : String(error) }, 500);
  }
});

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
        }

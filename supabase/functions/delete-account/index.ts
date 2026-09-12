import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const json = (body: Record<string, unknown>, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return json({ error: "Method not allowed" }, 405);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
  const authorization = req.headers.get("Authorization");

  if (!supabaseUrl || !anonKey || !serviceRoleKey) {
    return json({ error: "Supabase server configuration is incomplete." }, 500);
  }

  if (!authorization?.startsWith("Bearer ")) {
    return json({ error: "Authorization header is required." }, 401);
  }

  try {
    // Validate the caller's JWT with the user's own session context.
    const userClient = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: authorization } },
      auth: { autoRefreshToken: false, persistSession: false },
    });

    const {
      data: { user },
      error: userError,
    } = await userClient.auth.getUser();

    if (userError || !user) {
      return json({ error: "جلسة الدخول غير صالحة أو منتهية." }, 401);
    }

    const admin = createClient(supabaseUrl, serviceRoleKey, {
      auth: { autoRefreshToken: false, persistSession: false },
    });

    const userId = user.id;

    // Delete chat messages first so booking/user records can be removed safely.
    const { error: chatSenderError } = await admin
      .from("chat_messages")
      .delete()
      .eq("sender_id", userId);
    if (chatSenderError) throw chatSenderError;

    const { error: chatReceiverError } = await admin
      .from("chat_messages")
      .delete()
      .eq("receiver_id", userId);
    if (chatReceiverError) throw chatReceiverError;

    // Remove push tokens belonging to the account.
    const { error: tokenError } = await admin
      .from("notification_tokens")
      .delete()
      .eq("user_id", userId);
    if (tokenError) throw tokenError;

    // Remove bookings that belong to the account as patient or nurse.
    const { error: patientBookingsError } = await admin
      .from("bookings")
      .delete()
      .eq("patient_id", userId);
    if (patientBookingsError) throw patientBookingsError;

    const { error: nurseBookingsError } = await admin
      .from("bookings")
      .delete()
      .eq("nurse_id", userId);
    if (nurseBookingsError) throw nurseBookingsError;

    // Remove nurse subscription requests and admin membership, if present.
    const { error: subscriptionError } = await admin
      .from("nurse_subscription_requests")
      .delete()
      .eq("nurse_id", userId);
    if (subscriptionError) throw subscriptionError;

    const { error: adminError } = await admin
      .from("admin_users")
      .delete()
      .eq("user_id", userId);
    if (adminError) throw adminError;

    // Remove the user's public avatar files before deleting auth.users.
    const avatarBucket = admin.storage.from("avatars");
    for (const role of ["patient", "nurse"]) {
      const prefix = `${role}/${userId}`;
      const { data: objects, error: listError } = await avatarBucket.list(prefix, {
        limit: 1000,
        offset: 0,
      });
      if (listError) throw listError;

      if (objects && objects.length > 0) {
        const paths = objects
          .filter((object) => object.name)
          .map((object) => `${prefix}/${object.name}`);
        if (paths.length > 0) {
          const { error: removeError } = await avatarBucket.remove(paths);
          if (removeError) throw removeError;
        }
      }
    }

    // Remove application profile rows.
    const { error: patientError } = await admin
      .from("patients")
      .delete()
      .eq("user_id", userId);
    if (patientError) throw patientError;

    const { error: nurseError } = await admin
      .from("nurses")
      .delete()
      .eq("user_id", userId);
    if (nurseError) throw nurseError;

    // Finally remove the Auth account. The service-role key never leaves this server function.
    const { error: deleteAuthError } = await admin.auth.admin.deleteUser(userId, false);
    if (deleteAuthError) throw deleteAuthError;

    return json({ success: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    console.error("delete-account failed:", message);
    return json({ error: message }, 500);
  }
});

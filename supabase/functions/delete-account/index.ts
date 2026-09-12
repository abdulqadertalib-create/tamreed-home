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
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);

  const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
  const authorization = req.headers.get("Authorization");

  // Support the current Supabase secret-key format, with legacy fallback.
  let secretKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
  try {
    const secretKeys = JSON.parse(Deno.env.get("SUPABASE_SECRET_KEYS") ?? "{}");
    secretKey = secretKeys.default ?? secretKey;
  } catch (_) {}

  // Support the current publishable-key format, with legacy fallback.
  let publishableKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
  try {
    const publishableKeys = JSON.parse(Deno.env.get("SUPABASE_PUBLISHABLE_KEYS") ?? "{}");
    publishableKey = publishableKeys.default ?? publishableKey;
  } catch (_) {}

  if (!supabaseUrl || !publishableKey || !secretKey) {
    console.error("delete-account failed: Supabase server configuration is incomplete");
    return json({ error: "Supabase server configuration is incomplete." }, 500);
  }

  if (!authorization?.startsWith("Bearer ")) {
    return json({ error: "Authorization header is required." }, 401);
  }

  let stage = "start";

  try {
    stage = "validate-user";
    const userClient = createClient(supabaseUrl, publishableKey, {
      global: { headers: { Authorization: authorization } },
      auth: { autoRefreshToken: false, persistSession: false },
    });

    const {
      data: { user },
      error: userError,
    } = await userClient.auth.getUser();

    if (userError || !user) {
      console.error("delete-account failed at validate-user:", userError?.message ?? "No user");
      return json({ error: "جلسة الدخول غير صالحة أو منتهية." }, 401);
    }

    const admin = createClient(supabaseUrl, secretKey, {
      auth: { autoRefreshToken: false, persistSession: false },
    });

    const userId = user.id;

    stage = "chat-sender";
    const { error: chatSenderError } = await admin.from("chat_messages").delete().eq("sender_id", userId);
    if (chatSenderError) throw new Error(`chat_sender: ${chatSenderError.message}`);

    stage = "chat-receiver";
    const { error: chatReceiverError } = await admin.from("chat_messages").delete().eq("receiver_id", userId);
    if (chatReceiverError) throw new Error(`chat_receiver: ${chatReceiverError.message}`);

    stage = "notification-tokens";
    const { error: tokenError } = await admin.from("notification_tokens").delete().eq("user_id", userId);
    if (tokenError) throw new Error(`notification_tokens: ${tokenError.message}`);

    stage = "patient-bookings";
    const { error: patientBookingsError } = await admin.from("bookings").delete().eq("patient_id", userId);
    if (patientBookingsError) throw new Error(`patient_bookings: ${patientBookingsError.message}`);

    // In the current schema bookings.nurse_id points to nurses.id,
    // while nurses.user_id points to the Auth user. Find the internal nurse id
    // first, then remove bookings using both ids for compatibility with older rows.
    stage = "find-nurse-profile";
    const { data: nurseRows, error: nurseLookupError } = await admin
      .from("nurses")
      .select("id")
      .eq("user_id", userId);
    if (nurseLookupError) throw new Error(`nurse_lookup: ${nurseLookupError.message}`);

    const nurseIds = [
      userId,
      ...(nurseRows ?? []).map((row) => row.id).filter((id) => typeof id === "string" && id.length > 0),
    ];

    stage = "nurse-bookings";
    const { error: nurseBookingsError } = await admin
      .from("bookings")
      .delete()
      .in("nurse_id", nurseIds);
    if (nurseBookingsError) throw new Error(`nurse_bookings: ${nurseBookingsError.message}`);

    stage = "subscription-requests";
    const { error: subscriptionError } = await admin
      .from("nurse_subscription_requests")
      .delete()
      .eq("nurse_id", userId);
    if (subscriptionError) throw new Error(`subscription_requests: ${subscriptionError.message}`);

    stage = "admin-users";
    const { error: adminError } = await admin.from("admin_users").delete().eq("user_id", userId);
    if (adminError) throw new Error(`admin_users: ${adminError.message}`);

    stage = "avatar-patient";
    const avatarBucket = admin.storage.from("avatars");
    const patientPrefix = `patient/${userId}`;
    const { data: patientObjects, error: patientListError } = await avatarBucket.list(patientPrefix, {
      limit: 1000,
      offset: 0,
    });
    if (patientListError) throw new Error(`avatar_patient_list: ${patientListError.message}`);
    if (patientObjects?.length) {
      const paths = patientObjects.filter((o) => o.name).map((o) => `${patientPrefix}/${o.name}`);
      if (paths.length) {
        const { error } = await avatarBucket.remove(paths);
        if (error) throw new Error(`avatar_patient_remove: ${error.message}`);
      }
    }

    stage = "avatar-nurse";
    const nursePrefix = `nurse/${userId}`;
    const { data: nurseObjects, error: nurseListError } = await avatarBucket.list(nursePrefix, {
      limit: 1000,
      offset: 0,
    });
    if (nurseListError) throw new Error(`avatar_nurse_list: ${nurseListError.message}`);
    if (nurseObjects?.length) {
      const paths = nurseObjects.filter((o) => o.name).map((o) => `${nursePrefix}/${o.name}`);
      if (paths.length) {
        const { error } = await avatarBucket.remove(paths);
        if (error) throw new Error(`avatar_nurse_remove: ${error.message}`);
      }
    }

    stage = "patient-profile";
    const { error: patientError } = await admin.from("patients").delete().eq("user_id", userId);
    if (patientError) throw new Error(`patient_profile: ${patientError.message}`);

    stage = "nurse-profile";
    const { error: nurseError } = await admin.from("nurses").delete().eq("user_id", userId);
    if (nurseError) throw new Error(`nurse_profile: ${nurseError.message}`);

    stage = "auth-user";
    const { error: deleteAuthError } = await admin.auth.admin.deleteUser(userId, false);
    if (deleteAuthError) throw new Error(`auth_user: ${deleteAuthError.message}`);

    console.log("delete-account success");
    return json({ success: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    console.error(`delete-account failed at ${stage}: ${message}`);
    return json({ error: `Delete failed at ${stage}: ${message}` }, 500);
  }
});

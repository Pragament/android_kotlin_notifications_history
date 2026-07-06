import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

serve(async (req: Request) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    if (req.method !== "POST") {
      return new Response(
        JSON.stringify({ error: "Method not allowed" }),
        { status: 405, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    const { otp_code, device_id, device_name } = await req.json();

    if (!otp_code || !device_id || !device_name) {
      return new Response(
        JSON.stringify({ error: "otp_code, device_id, and device_name are required" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const supabaseServiceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
    const supabase = createClient(supabaseUrl, supabaseServiceRoleKey);

    // Look up the OTP
    const { data: otpRecord, error: otpLookupError } = await supabase
      .from("otps")
      .select("*")
      .eq("code", otp_code)
      .maybeSingle();

    if (otpLookupError) {
      throw otpLookupError;
    }

    if (!otpRecord) {
      return new Response(
        JSON.stringify({ error: "Invalid OTP code" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    // Check if the OTP has expired
    if (new Date(otpRecord.expires_at) < new Date()) {
      // Clean up the expired OTP
      await supabase.from("otps").delete().eq("code", otp_code);

      return new Response(
        JSON.stringify({ error: "OTP has expired" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    const groupId: string = otpRecord.group_id;

    // Upsert the new device into the group
    const { error: upsertError } = await supabase.from("devices").upsert(
      {
        device_id,
        device_name,
        group_id: groupId,
      },
      { onConflict: "device_id" },
    );

    if (upsertError) {
      throw upsertError;
    }

    // Delete the used OTP
    const { error: deleteError } = await supabase
      .from("otps")
      .delete()
      .eq("code", otp_code);

    if (deleteError) {
      throw deleteError;
    }

    return new Response(
      JSON.stringify({ group_id: groupId, message: "Device linked successfully" }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  } catch (error) {
    const message = error instanceof Error ? error.message : "Internal server error";
    return new Response(
      JSON.stringify({ error: message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  }
});

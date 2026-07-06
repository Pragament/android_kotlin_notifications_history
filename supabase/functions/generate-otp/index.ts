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

    const { device_id, device_name } = await req.json();

    if (!device_id || !device_name) {
      return new Response(
        JSON.stringify({ error: "device_id and device_name are required" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const supabaseServiceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
    const supabase = createClient(supabaseUrl, supabaseServiceRoleKey);

    // Check if device already exists
    const { data: existingDevice, error: deviceLookupError } = await supabase
      .from("devices")
      .select("device_id, group_id")
      .eq("device_id", device_id)
      .maybeSingle();

    if (deviceLookupError) {
      throw deviceLookupError;
    }

    let groupId: string;

    if (existingDevice) {
      // Device exists – reuse its group_id
      groupId = existingDevice.group_id;
    } else {
      // New device – generate a fresh group_id and insert
      groupId = crypto.randomUUID();
      const { error: insertError } = await supabase.from("devices").insert({
        device_id,
        device_name,
        group_id: groupId,
      });
      if (insertError) {
        throw insertError;
      }
    }

    // Generate a random 6-digit zero-padded OTP
    const otpCode = String(Math.floor(Math.random() * 1_000_000)).padStart(6, "0");

    // Remove any existing OTPs for this device
    const { error: deleteError } = await supabase
      .from("otps")
      .delete()
      .eq("device_id", device_id);

    if (deleteError) {
      throw deleteError;
    }

    // Insert the new OTP (expires in 5 minutes)
    const expiresAt = new Date(Date.now() + 5 * 60 * 1000).toISOString();

    const { error: otpInsertError } = await supabase.from("otps").insert({
      code: otpCode,
      device_id,
      group_id: groupId,
      expires_at: expiresAt,
    });

    if (otpInsertError) {
      throw otpInsertError;
    }

    return new Response(
      JSON.stringify({ otp: otpCode, group_id: groupId }),
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

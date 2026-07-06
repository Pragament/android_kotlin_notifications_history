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

    const {
      group_id,
      sender_device_id,
      sender_device_name,
      package_name,
      app_name,
      title,
      content,
      timestamp,
    } = await req.json();

    // Validate required fields
    if (
      !group_id ||
      !sender_device_id ||
      !sender_device_name ||
      !package_name ||
      !app_name ||
      timestamp === undefined ||
      timestamp === null
    ) {
      return new Response(
        JSON.stringify({
          error:
            "group_id, sender_device_id, sender_device_name, package_name, app_name, and timestamp are required",
        }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const supabaseServiceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
    const supabase = createClient(supabaseUrl, supabaseServiceRoleKey);

    // Insert the forwarded notification
    const { data, error: insertError } = await supabase
      .from("forwarded_notifications")
      .insert({
        group_id,
        sender_device_id,
        sender_device_name,
        package_name,
        app_name,
        title: title ?? null,
        content: content ?? null,
        timestamp,
      })
      .select("id")
      .single();

    if (insertError) {
      throw insertError;
    }

    return new Response(
      JSON.stringify({ success: true, id: data.id }),
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

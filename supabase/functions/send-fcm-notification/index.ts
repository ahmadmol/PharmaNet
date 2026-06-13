import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

type AppNotificationRecord = {
  id: string;
  user_id: string | null;
  title: string | null;
  body: string | null;
  destination?: string | null;
  destination_id?: string | null;
};

type UserFcmTokenRow = {
  token: string;
};

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (request.method !== "POST") {
    return jsonResponse({ error: "Method not allowed" }, 405);
  }

  const webhookSecret = Deno.env.get("FCM_WEBHOOK_SECRET");
  const authorization = request.headers.get("authorization") ?? "";
  if (webhookSecret && authorization !== `Bearer ${webhookSecret}`) {
    return jsonResponse({ error: "Unauthorized" }, 401);
  }

  try {
    const payload = await request.json();
    const notification = extractNotificationRecord(payload);

    if (!notification.id || !notification.user_id) {
      console.log("Skipping FCM send: notification has no id or user_id", {
        notification_id: notification.id,
        user_id: notification.user_id,
      });
      return jsonResponse({ sent: 0, skipped: true });
    }

    const supabaseUrl = requireEnv("SUPABASE_URL");
    const serviceRoleKey = requireEnv("SUPABASE_SERVICE_ROLE_KEY");
    const supabase = createClient(supabaseUrl, serviceRoleKey, {
      auth: { persistSession: false },
    });

    const { data: tokenRows, error: tokenError } = await supabase
      .from("user_fcm_tokens")
      .select("token")
      .eq("user_id", notification.user_id)
      .eq("is_active", true)
      .eq("platform", "android");

    if (tokenError) {
      console.error("Failed to load active FCM tokens", {
        notification_id: notification.id,
        user_id: notification.user_id,
        error: tokenError.message,
      });
      return jsonResponse({ sent: 0, error: "token_lookup_failed" }, 200);
    }

    const tokens = (tokenRows as UserFcmTokenRow[] | null)?.map((row) => row.token).filter(Boolean) ?? [];
    if (tokens.length === 0) {
      console.log("Skipping FCM send: no active tokens", {
        notification_id: notification.id,
        user_id: notification.user_id,
      });
      return jsonResponse({ sent: 0, skipped: true });
    }

    const accessToken = await getGoogleAccessToken();
    const projectId = getFirebaseProjectId();
    const results = await Promise.allSettled(
      tokens.map((token) => sendFcmMessage(projectId, accessToken, token, notification)),
    );

    const sent = results.filter((result) => result.status === "fulfilled").length;
    const failed = results.length - sent;

    results.forEach((result, index) => {
      if (result.status === "rejected") {
        console.error("FCM send failed", {
          notification_id: notification.id,
          token_prefix: tokens[index]?.slice(0, 20),
          error: result.reason instanceof Error ? result.reason.message : String(result.reason),
        });
      }
    });

    return jsonResponse({ sent, failed });
  } catch (error) {
    console.error("send-fcm-notification failed", {
      error: error instanceof Error ? error.message : String(error),
    });
    return jsonResponse({ sent: 0, error: "function_failed" }, 200);
  }
});

function extractNotificationRecord(payload: unknown): AppNotificationRecord {
  const value = payload as { record?: AppNotificationRecord };
  return value.record ?? (payload as AppNotificationRecord);
}

async function sendFcmMessage(
  projectId: string,
  accessToken: string,
  token: string,
  notification: AppNotificationRecord,
) {
  const title = notification.title ?? "PharmaNet";
  const body = notification.body ?? "";
  const destination = notification.destination ?? "";
  const destinationId = notification.destination_id ?? "";

  const response = await fetch(
    `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
    {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        message: {
          token,
          android: {
            priority: "HIGH",
          },
          data: {
            title,
            body,
            notification_id: notification.id,
            destination,
            destination_id: destinationId,
          },
        },
      }),
    },
  );

  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(`FCM HTTP ${response.status}: ${errorBody}`);
  }
}

async function getGoogleAccessToken(): Promise<string> {
  const serviceAccount = JSON.parse(requireEnv("FIREBASE_SERVICE_ACCOUNT_JSON")) as {
    client_email: string;
    private_key: string;
  };

  const now = Math.floor(Date.now() / 1000);
  const jwt = await createJwt(
    {
      alg: "RS256",
      typ: "JWT",
    },
    {
      iss: serviceAccount.client_email,
      scope: "https://www.googleapis.com/auth/firebase.messaging",
      aud: "https://oauth2.googleapis.com/token",
      iat: now,
      exp: now + 3600,
    },
    serviceAccount.private_key,
  );

  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  });

  if (!response.ok) {
    throw new Error(`Google OAuth HTTP ${response.status}: ${await response.text()}`);
  }

  const body = await response.json() as { access_token?: string };
  if (!body.access_token) {
    throw new Error("Google OAuth response did not include access_token");
  }

  return body.access_token;
}

async function createJwt(
  header: Record<string, unknown>,
  payload: Record<string, unknown>,
  privateKeyPem: string,
): Promise<string> {
  const encodedHeader = base64UrlEncode(JSON.stringify(header));
  const encodedPayload = base64UrlEncode(JSON.stringify(payload));
  const signingInput = `${encodedHeader}.${encodedPayload}`;

  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToArrayBuffer(privateKeyPem),
    {
      name: "RSASSA-PKCS1-v1_5",
      hash: "SHA-256",
    },
    false,
    ["sign"],
  );

  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(signingInput),
  );

  return `${signingInput}.${base64UrlEncode(signature)}`;
}

function pemToArrayBuffer(pem: string): ArrayBuffer {
  const base64 = pem
    .replace(/-----BEGIN PRIVATE KEY-----/g, "")
    .replace(/-----END PRIVATE KEY-----/g, "")
    .replace(/\s/g, "");
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index);
  }
  return bytes.buffer;
}

function base64UrlEncode(value: string | ArrayBuffer): string {
  const bytes = typeof value === "string"
    ? new TextEncoder().encode(value)
    : new Uint8Array(value);
  let binary = "";
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
}

function getFirebaseProjectId(): string {
  const explicitProjectId = Deno.env.get("FIREBASE_PROJECT_ID");
  if (explicitProjectId) return explicitProjectId;

  const serviceAccount = JSON.parse(requireEnv("FIREBASE_SERVICE_ACCOUNT_JSON")) as {
    project_id?: string;
  };
  if (!serviceAccount.project_id) {
    throw new Error("FIREBASE_PROJECT_ID or service_account.project_id is required");
  }
  return serviceAccount.project_id;
}

function requireEnv(name: string): string {
  const value = Deno.env.get(name);
  if (!value) {
    throw new Error(`${name} is required`);
  }
  return value;
}

function jsonResponse(body: Record<string, unknown>, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      ...corsHeaders,
      "Content-Type": "application/json",
    },
  });
}

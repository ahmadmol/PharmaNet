# send-fcm-notification

Supabase Edge Function that sends Android FCM pushes when `app_notifications` rows are inserted.

Required secrets:

- `SUPABASE_URL`
- `SUPABASE_SERVICE_ROLE_KEY`
- `FIREBASE_SERVICE_ACCOUNT_JSON`
- `FCM_WEBHOOK_SECRET`
- `FIREBASE_PROJECT_ID` optional if the service account JSON includes `project_id`

Deploy:

```bash
supabase functions deploy send-fcm-notification --no-verify-jwt
```

The database trigger authenticates with `Authorization: Bearer <FCM_WEBHOOK_SECRET>`.

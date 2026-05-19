-- Batch C safe fixes:
-- - Align client notification inserts with existing app_notifications body/destination_id contract.
-- - Allow only ownership-checked pharmacy/warehouse request notifications.
-- - Allow WAREHOUSE users to delete medicine images under their own warehouse path.

ALTER TABLE public.app_notifications ENABLE ROW LEVEL SECURITY;

GRANT INSERT ON public.app_notifications TO authenticated;

DROP POLICY IF EXISTS notifications_insert_pharmacy_to_warehouse_request ON public.app_notifications;
CREATE POLICY notifications_insert_pharmacy_to_warehouse_request
ON public.app_notifications
FOR INSERT
TO authenticated
WITH CHECK (
    destination = 'REQUEST'
    AND destination_id IS NOT NULL
    AND type = 'ORDER_UPDATE'
    AND category = 'REQUESTS'
    AND read = false
    AND EXISTS (
        SELECT 1
        FROM public.profiles sender
        JOIN public.requests r
          ON r.id = app_notifications.destination_id::uuid
        JOIN public.profiles recipient
          ON recipient.id = app_notifications.user_id
        WHERE sender.id = auth.uid()
          AND sender.account_type = 'PHARMACY'
          AND sender.is_active = true
          AND sender.pharmacy_id IS NOT NULL
          AND app_notifications.pharmacy_id = sender.pharmacy_id
          AND r.pharmacy_id = sender.pharmacy_id
          AND recipient.account_type = 'WAREHOUSE'
          AND recipient.is_active = true
          AND recipient.warehouse_id IS NOT NULL
          AND recipient.warehouse_id = r.warehouse_id
    )
);

DROP POLICY IF EXISTS notifications_insert_warehouse_to_pharmacy_request ON public.app_notifications;
CREATE POLICY notifications_insert_warehouse_to_pharmacy_request
ON public.app_notifications
FOR INSERT
TO authenticated
WITH CHECK (
    destination = 'REQUEST'
    AND destination_id IS NOT NULL
    AND type = 'ORDER_UPDATE'
    AND category IN ('ORDERS', 'REQUESTS')
    AND read = false
    AND EXISTS (
        SELECT 1
        FROM public.profiles sender
        JOIN public.requests r
          ON r.id = app_notifications.destination_id::uuid
        JOIN public.profiles recipient
          ON recipient.id = app_notifications.user_id
        WHERE sender.id = auth.uid()
          AND sender.account_type = 'WAREHOUSE'
          AND sender.is_active = true
          AND sender.warehouse_id IS NOT NULL
          AND r.warehouse_id = sender.warehouse_id
          AND recipient.account_type = 'PHARMACY'
          AND recipient.is_active = true
          AND recipient.pharmacy_id IS NOT NULL
          AND recipient.pharmacy_id = r.pharmacy_id
          AND app_notifications.pharmacy_id = recipient.pharmacy_id
    )
);

DROP POLICY IF EXISTS medicines_warehouse_delete_own_images ON storage.objects;
CREATE POLICY medicines_warehouse_delete_own_images
ON storage.objects
FOR DELETE
TO authenticated
USING (
    bucket_id = 'medicines'
    AND position('..' in name) = 0
    AND EXISTS (
        SELECT 1
        FROM public.profiles p
        WHERE p.id = auth.uid()
          AND p.account_type = 'WAREHOUSE'
          AND p.is_active = true
          AND p.warehouse_id IS NOT NULL
          AND name LIKE 'warehouse/' || p.warehouse_id || '/%'
    )
);

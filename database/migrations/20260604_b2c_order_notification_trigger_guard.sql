-- Batch 1: Keep one guarded B2C order notification trigger.
-- Scope: orders trigger/function only. B2B RPC lifecycle and Kotlin are untouched.

CREATE OR REPLACE FUNCTION public.handle_new_b2c_order_notification()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NEW.order_type IS DISTINCT FROM 'CUSTOMER_PHARMACY'
     OR NEW.request_id IS NOT NULL
     OR NEW.pharmacy_id IS NULL THEN
    RETURN NEW;
  END IF;

  INSERT INTO public.app_notifications (
    user_id,
    pharmacy_id,
    title,
    body,
    type,
    category,
    read,
    destination,
    destination_id,
    created_at
  )
  SELECT
    p.id,
    NEW.pharmacy_id,
    U&'\0637\0644\0628 \0639\0645\064A\0644 \062C\062F\064A\062F',
    U&'\0648\0635\0644 \0637\0644\0628 \062C\062F\064A\062F \0645\0646 \0639\0645\064A\0644 \0628\0627\0646\062A\0638\0627\0631 \0645\0631\0627\062C\0639\062A\0643.',
    'ORDER_UPDATE',
    'ORDERS',
    false,
    'PHARMACY_CUSTOMER_ORDER',
    NEW.id::text,
    now()
  FROM public.profiles p
  WHERE p.account_type = 'PHARMACY'
    AND p.is_active = true
    AND p.pharmacy_id = NEW.pharmacy_id;

  RETURN NEW;
END;
$$;

DO $$
DECLARE
  trigger_record record;
BEGIN
  FOR trigger_record IN
    SELECT t.tgname
    FROM pg_trigger t
    JOIN pg_class c ON c.oid = t.tgrelid
    JOIN pg_namespace cn ON cn.oid = c.relnamespace
    JOIN pg_proc p ON p.oid = t.tgfoid
    JOIN pg_namespace pn ON pn.oid = p.pronamespace
    WHERE cn.nspname = 'public'
      AND c.relname = 'orders'
      AND pn.nspname = 'public'
      AND p.proname = 'handle_new_b2c_order_notification'
      AND NOT t.tgisinternal
      AND t.tgname <> 'notify_pharmacy_on_new_order'
  LOOP
    EXECUTE format('DROP TRIGGER IF EXISTS %I ON public.orders', trigger_record.tgname);
  END LOOP;
END;
$$;

DROP TRIGGER IF EXISTS notify_pharmacy_on_new_order ON public.orders;
DROP TRIGGER IF EXISTS handle_new_b2c_order_notification ON public.orders;
DROP TRIGGER IF EXISTS new_b2c_order_notification ON public.orders;
DROP TRIGGER IF EXISTS trigger_new_b2c_order_notification ON public.orders;
DROP TRIGGER IF EXISTS notify_new_b2c_order ON public.orders;

CREATE TRIGGER notify_pharmacy_on_new_order
AFTER INSERT ON public.orders
FOR EACH ROW
WHEN (
  NEW.order_type = 'CUSTOMER_PHARMACY'
  AND NEW.request_id IS NULL
  AND NEW.pharmacy_id IS NOT NULL
)
EXECUTE FUNCTION public.handle_new_b2c_order_notification();

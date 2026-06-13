-- Pharmacy Radar realtime/select RLS patch
-- Allow pharmacists to read public customer orders within their scope (pending + own pharmacy)
DROP POLICY IF EXISTS "pharmacy_read_nearby_orders" ON public.orders;
CREATE POLICY "pharmacy_read_nearby_orders" ON public.orders 
FOR SELECT 
TO authenticated 
USING (status = 'PENDING' OR pharmacy_id = auth.uid());

-- Migration: Add prescription_url to orders and total_price to requests
-- Date: 2026-05-09

-- Add prescription_url to orders table (B2C)
ALTER TABLE public.orders 
ADD COLUMN IF NOT EXISTS prescription_url TEXT;

-- Add total_price to requests table (B2B)
ALTER TABLE public.requests 
ADD COLUMN IF NOT EXISTS total_price NUMERIC DEFAULT 0.0;

-- Comment for clarity
COMMENT ON COLUMN public.orders.prescription_url IS 'URL to the prescription image uploaded by the customer';
COMMENT ON COLUMN public.requests.total_price IS 'Total price of the request including any additional fees (e.g., urgent fee)';

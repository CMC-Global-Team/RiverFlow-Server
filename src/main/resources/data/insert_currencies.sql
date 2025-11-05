-- ==============================================================================
-- Insert Default Currencies
-- ==============================================================================
-- Run this script to populate the currencies table with common currencies
-- Usage: Execute this in your MySQL database

USE mindmap_system;

-- Insert supported currencies
INSERT INTO currencies (code, name, symbol, decimal_places, is_active, display_order) VALUES
('USD', 'US Dollar', '$', 2, TRUE, 1),
('EUR', 'Euro', '€', 2, TRUE, 2),
('GBP', 'British Pound', '£', 2, TRUE, 3),
('JPY', 'Japanese Yen', '¥', 0, TRUE, 4),
('VND', 'Vietnamese Dong', '₫', 0, TRUE, 5),
('SGD', 'Singapore Dollar', 'S$', 2, TRUE, 6),
('AUD', 'Australian Dollar', 'A$', 2, TRUE, 7),
('CAD', 'Canadian Dollar', 'C$', 2, TRUE, 8),
('CNY', 'Chinese Yuan', '¥', 2, TRUE, 9),
('INR', 'Indian Rupee', '₹', 2, TRUE, 10),
('THB', 'Thai Baht', '฿', 2, TRUE, 11),
('MYR', 'Malaysian Ringgit', 'RM', 2, TRUE, 12);

-- Verify insertion
SELECT * FROM currencies ORDER BY display_order;


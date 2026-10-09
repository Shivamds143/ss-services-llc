-- Insert test data for Insurance Income
INSERT INTO income_entries (income_date, income_category_id, amount, payment_mode, remarks, created_by)
SELECT CURDATE()-INTERVAL 2 DAY,
    (SELECT id FROM income_categories WHERE category_name='Life Insurance Premium Commission' LIMIT 1),
    5500.00, 'UPI', 'Life Insurance commission - Test', (SELECT id FROM users WHERE username='admin' LIMIT 1);

INSERT INTO income_entries (income_date, income_category_id, amount, payment_mode, remarks, created_by)
SELECT CURDATE()-INTERVAL 1 DAY,
    (SELECT id FROM income_categories WHERE category_name='Health Insurance Commission' LIMIT 1),
    3200.00, 'BANK_TRANSFER', 'Health Insurance commission - Test', (SELECT id FROM users WHERE username='admin' LIMIT 1);

INSERT INTO income_entries (income_date, income_category_id, amount, payment_mode, remarks, created_by)
SELECT CURDATE(),
    (SELECT id FROM income_categories WHERE category_name='Term Insurance Commission' LIMIT 1),
    4100.00, 'UPI', 'Term Insurance commission - Test', (SELECT id FROM users WHERE username='admin' LIMIT 1);

INSERT INTO income_entries (income_date, income_category_id, amount, payment_mode, remarks, created_by)
SELECT CURDATE()-INTERVAL 5 DAY,
    (SELECT id FROM income_categories WHERE category_name='General Insurance Commission' LIMIT 1),
    2800.00, 'CASH', 'General Insurance commission - Test', (SELECT id FROM users WHERE username='admin' LIMIT 1);

-- Insert test data for Mutual Fund Income
INSERT INTO income_entries (income_date, income_category_id, amount, payment_mode, remarks, created_by)
SELECT CURDATE()-INTERVAL 3 DAY,
    (SELECT id FROM income_categories WHERE category_name='Mutual Fund Commission (New SIP)' LIMIT 1),
    2500.00, 'BANK_TRANSFER', 'MF SIP commission - Test', (SELECT id FROM users WHERE username='admin' LIMIT 1);

INSERT INTO income_entries (income_date, income_category_id, amount, payment_mode, remarks, created_by)
SELECT CURDATE()-INTERVAL 2 DAY,
    (SELECT id FROM income_categories WHERE category_name='Mutual Fund Commission (Lumpsum)' LIMIT 1),
    6800.00, 'UPI', 'MF Lumpsum commission - Test', (SELECT id FROM users WHERE username='admin' LIMIT 1);

INSERT INTO income_entries (income_date, income_category_id, amount, payment_mode, remarks, created_by)
SELECT CURDATE()-INTERVAL 4 DAY,
    (SELECT id FROM income_categories WHERE category_name='Mutual Fund Renewal / Trail Fee' LIMIT 1),
    1800.00, 'CASH', 'MF Trail commission - Test', (SELECT id FROM users WHERE username='admin' LIMIT 1);

INSERT INTO income_entries (income_date, income_category_id, amount, payment_mode, remarks, created_by)
SELECT CURDATE()-INTERVAL 6 DAY,
    (SELECT id FROM income_categories WHERE category_name='Fixed Deposit Referral Income' LIMIT 1),
    3500.00, 'BANK_TRANSFER', 'FD Referral income - Test', (SELECT id FROM users WHERE username='admin' LIMIT 1);


CREATE TABLE tenant (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    surname VARCHAR(100),
    title VARCHAR(50),
    email VARCHAR(255),
    cell_phone_number VARCHAR(20),
    alternative_cell_phone_number VARCHAR(20),
    room_number VARCHAR(20),
    number_of_tenants_in_unit INT,
    lease_start_date DATE,
    lease_end_date DATE,
    prepaid_electricity_meter_number VARCHAR(50),
    deposit_paid BOOLEAN,
    rental DECIMAL(10,2),
    payment_date DATE,
    tenant_behaviour VARCHAR(100),
    tenant_status VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

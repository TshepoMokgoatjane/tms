
CREATE TABLE tenant (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    surname VARCHAR(100),
    title VARCHAR(20),
    email VARCHAR(255),
    cell_phone_number VARCHAR(20),
    alternative_cell_phone_number VARCHAR(20),
    room_number VARCHAR(20),
    number_of_tenants_in_unit INT,
    lease_start_date DATE,
    lease_end_date DATE,
    prepaid_electricity_meter_number VARCHAR(50),
    deposit_paid BOOLEAN,
    payment_day VARCHAR(20) NOT NULL,
    tenant_behaviour VARCHAR(20) NOT NULL,
    tenant_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_number INT,
    raised_by VARCHAR(255),
    date_raised DATETIME,
    subject VARCHAR(255),
    description TEXT,
    comments TEXT,
    category VARCHAR(50),
    priority VARCHAR(50),
    status VARCHAR(50)
);

CREATE TABLE ticket_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    comment TEXT NOT NULL,
    author VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (ticket_id) REFERENCES ticket(id)
);

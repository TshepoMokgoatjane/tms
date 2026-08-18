
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

CREATE TABLE house_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version INT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_data LONGBLOB NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    uploaded_by VARCHAR(255),
    uploaded_at DATETIME NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE house_rules_acknowledgement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    house_rules_id BIGINT NOT NULL,
    acknowledged_at DATETIME NOT NULL,
    ip_address VARCHAR(45),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    FOREIGN KEY (house_rules_id) REFERENCES house_rules(id),
    UNIQUE KEY uk_tenant_house_rules (tenant_id, house_rules_id)
);

CREATE TABLE co_occupant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    surname VARCHAR(50) NOT NULL,
    relationship VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    cell_phone_number VARCHAR(10),
    vehicle_registration VARCHAR(20),
    tenant_id INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

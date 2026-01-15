-- Migration V5: Recreate attachments table with UUID and new structure
-- User manually deleted the table, so we recreate it entirely with the new schema.

DROP TABLE IF EXISTS attachments CASCADE;

CREATE TABLE attachments (
    uuid VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    
    order_number INTEGER NOT NULL DEFAULT 0,
    is_main BOOLEAN DEFAULT FALSE,
    
    file_path VARCHAR(500) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255),
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    file_extension VARCHAR(50),
    
    -- Timestamp for unique filename (milliseconds)
    file_timestamp BIGINT,
    
    -- Base64 backup for fallback when disk file is missing
    file_data_base64 TEXT,
    
    -- Active status for soft delete or visibility control
    is_active BOOLEAN DEFAULT TRUE,
    
    product_id BIGINT,
    
    CONSTRAINT fk_attachment_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL
);

CREATE INDEX idx_attachment_product_id ON attachments(product_id);
CREATE INDEX idx_attachment_filename ON attachments(filename);

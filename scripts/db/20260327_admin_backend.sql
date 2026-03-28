ALTER TABLE tb_post
    ADD COLUMN audit_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED' COMMENT '审核状态：PENDING/APPROVED/REJECTED' AFTER enabled,
    ADD INDEX idx_audit_status (audit_status);

ALTER TABLE tb_activity
    ADD COLUMN audit_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED' COMMENT '审核状态：PENDING/APPROVED/REJECTED' AFTER status,
    ADD INDEX idx_audit_status (audit_status);

UPDATE tb_post
SET audit_status = 'APPROVED'
WHERE audit_status IS NULL OR audit_status = '';

UPDATE tb_activity
SET audit_status = 'APPROVED'
WHERE audit_status IS NULL OR audit_status = '';

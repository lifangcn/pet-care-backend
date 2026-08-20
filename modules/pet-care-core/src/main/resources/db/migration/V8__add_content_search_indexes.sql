CREATE INDEX idx_tb_post_content_search_trgm
    ON petcare.tb_post
    USING gin (lower(coalesce(title, '') || ' ' || coalesce(content, '')) petcare.gin_trgm_ops)
    WHERE enabled = 1
      AND is_deleted = false
      AND audit_status = 'APPROVED'
      AND post_type IN ('PRODUCT', 'SERVICE', 'LOCATION', 'DAILY');

CREATE INDEX idx_tb_activity_content_search_trgm
    ON petcare.tb_activity
    USING gin (lower(coalesce(title, '') || ' ' || coalesce(description, '') || ' ' || coalesce(address, '')) petcare.gin_trgm_ops)
    WHERE is_deleted = false
      AND audit_status = 'APPROVED'
      AND status IN ('RECRUITING', 'ONGOING');

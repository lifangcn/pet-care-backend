ALTER TABLE petcare.tb_user
    ADD CONSTRAINT uk_tb_user_phone UNIQUE (phone);

ALTER TABLE petcare.tb_post_label
    ADD CONSTRAINT uk_tb_post_label UNIQUE (post_id, label_id);

ALTER TABLE petcare.tb_interaction
    ADD CONSTRAINT uk_tb_interaction_user_post_type UNIQUE (user_id, post_id, interaction_type);

ALTER TABLE petcare.tb_points_account
    ADD CONSTRAINT uk_tb_points_account_user_id UNIQUE (user_id);

ALTER TABLE petcare.tb_user_role
    ADD CONSTRAINT uk_tb_user_role UNIQUE (user_id, role_id);

CREATE INDEX idx_tb_user_deleted ON petcare.tb_user (is_deleted);
CREATE UNIQUE INDEX uk_tb_user_username_ci ON petcare.tb_user (lower(username));
CREATE INDEX idx_tb_pet_user_id ON petcare.tb_pet (user_id);
CREATE INDEX idx_tb_pet_deleted ON petcare.tb_pet (is_deleted);
CREATE INDEX idx_tb_health_record_pet_id ON petcare.tb_health_record (pet_id);
CREATE INDEX idx_tb_health_record_user_id ON petcare.tb_health_record (user_id);
CREATE INDEX idx_tb_health_record_deleted ON petcare.tb_health_record (is_deleted);
CREATE INDEX idx_tb_reminder_pet_id ON petcare.tb_reminder (pet_id);
CREATE INDEX idx_tb_reminder_user_id ON petcare.tb_reminder (user_id);
CREATE INDEX idx_tb_reminder_schedule_time ON petcare.tb_reminder (schedule_time);
CREATE INDEX idx_tb_reminder_deleted ON petcare.tb_reminder (is_deleted);
CREATE INDEX idx_tb_reminder_execution_reminder_id ON petcare.tb_reminder_execution (reminder_id);
CREATE INDEX idx_tb_reminder_execution_user_id ON petcare.tb_reminder_execution (user_id);
CREATE INDEX idx_tb_reminder_execution_schedule_time ON petcare.tb_reminder_execution (schedule_time);
CREATE INDEX idx_tb_reminder_execution_deleted ON petcare.tb_reminder_execution (is_deleted);
CREATE INDEX idx_tb_post_user_id ON petcare.tb_post (user_id);
CREATE INDEX idx_tb_post_activity_id ON petcare.tb_post (activity_id);
CREATE INDEX idx_tb_post_deleted ON petcare.tb_post (is_deleted);
CREATE INDEX idx_tb_post_audit_status ON petcare.tb_post (audit_status);
CREATE INDEX idx_tb_label_type ON petcare.tb_label (type);
CREATE INDEX idx_tb_label_deleted ON petcare.tb_label (is_deleted);
CREATE UNIQUE INDEX uk_tb_label_name_ci ON petcare.tb_label (lower(name));
CREATE INDEX idx_tb_post_label_post_id ON petcare.tb_post_label (post_id);
CREATE INDEX idx_tb_post_label_label_id ON petcare.tb_post_label (label_id);
CREATE INDEX idx_tb_interaction_post_id ON petcare.tb_interaction (post_id);
CREATE INDEX idx_tb_interaction_user_id ON petcare.tb_interaction (user_id);
CREATE INDEX idx_tb_activity_user_id ON petcare.tb_activity (user_id);
CREATE INDEX idx_tb_activity_time_status ON petcare.tb_activity (activity_time, status);
CREATE INDEX idx_tb_activity_deleted ON petcare.tb_activity (is_deleted);
CREATE INDEX idx_tb_activity_audit_status ON petcare.tb_activity (audit_status);
CREATE INDEX idx_tb_knowledge_document_deleted ON petcare.tb_knowledge_document (is_deleted);
CREATE UNIQUE INDEX uk_tb_role_role_code_ci ON petcare.tb_role (lower(role_code));
CREATE INDEX idx_tb_user_role_user_id ON petcare.tb_user_role (user_id);
CREATE INDEX idx_tb_user_role_role_id ON petcare.tb_user_role (role_id);

ALTER TABLE petcare.tb_health_record
    ADD CONSTRAINT ck_tb_health_record_type CHECK (record_type IN ('WEIGHT', 'TEMPERATURE', 'MEDICAL'));

ALTER TABLE petcare.tb_reminder
    ADD CONSTRAINT ck_tb_reminder_source_type CHECK (source_type IN ('MANUAL', 'HEALTH_RECORD', 'SYSTEM')),
    ADD CONSTRAINT ck_tb_reminder_repeat_type CHECK (repeat_type IN ('NONE', 'DAILY', 'WEEKLY', 'MONTHLY', 'CUSTOM')),
    ADD CONSTRAINT ck_tb_reminder_occurrences CHECK (total_occurrences >= 0 AND completed_count >= 0 AND (total_occurrences = 0 OR completed_count <= total_occurrences));

ALTER TABLE petcare.tb_reminder_execution
    ADD CONSTRAINT ck_tb_reminder_execution_status CHECK (status IN ('PENDING', 'COMPLETED', 'OVERDUE'));

ALTER TABLE petcare.tb_post
    ADD CONSTRAINT ck_tb_post_type CHECK (post_type IN ('PRODUCT', 'SERVICE', 'LOCATION', 'DAILY', 'ACTIVITY_CHECK', 'ACTIVITY_JOIN')),
    ADD CONSTRAINT ck_tb_post_audit_status CHECK (audit_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    ADD CONSTRAINT ck_tb_post_counts CHECK (like_count >= 0 AND rating_count >= 0 AND rating_total >= 0 AND view_count >= 0);

ALTER TABLE petcare.tb_label
    ADD CONSTRAINT ck_tb_label_type CHECK (type IN ('GENERAL', 'BREED', 'CONTENT')),
    ADD CONSTRAINT ck_tb_label_use_count CHECK (use_count >= 0);

ALTER TABLE petcare.tb_interaction
    ADD CONSTRAINT ck_tb_interaction_type CHECK (interaction_type IN ('LIKE', 'RATING')),
    ADD CONSTRAINT ck_tb_interaction_rating_value CHECK (rating_value IS NULL OR rating_value BETWEEN 1 AND 5);

ALTER TABLE petcare.tb_activity
    ADD CONSTRAINT ck_tb_activity_type CHECK (activity_type IN ('ONLINE', 'OFFLINE')),
    ADD CONSTRAINT ck_tb_activity_status CHECK (status IN ('RECRUITING', 'ONGOING', 'ENDED')),
    ADD CONSTRAINT ck_tb_activity_audit_status CHECK (audit_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    ADD CONSTRAINT ck_tb_activity_counts CHECK (max_participants >= 0 AND current_participants >= 0 AND check_in_count >= 0);

ALTER TABLE petcare.tb_knowledge_document
    ADD CONSTRAINT ck_tb_knowledge_document_processing_status CHECK (processing_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    ADD CONSTRAINT ck_tb_knowledge_document_counts CHECK (file_size >= 0 AND version >= 1 AND chunk_count >= 0);

ALTER TABLE petcare.tb_points_account
    ADD CONSTRAINT ck_tb_points_account_available_points CHECK (available_points >= 0),
    ADD CONSTRAINT ck_tb_points_account_total_points CHECK (total_points >= 0);

ALTER TABLE petcare.tb_points_record
    ADD CONSTRAINT ck_tb_points_record_action_type CHECK (action_type IN ('REGISTER', 'CHECK_IN', 'PUBLISH', 'COMMENT', 'LIKE', 'LIKED', 'COMMENTED', 'AI_CONSULT', 'AI_CONSULT_AGENT', 'COUPON_REDEEM')),
    ADD CONSTRAINT ck_tb_points_record_balances CHECK (points_before >= 0 AND points_after >= 0 AND (coupon_deduct IS NULL OR coupon_deduct >= 0));

ALTER TABLE petcare.tb_points_coupon_template
    ADD CONSTRAINT ck_tb_points_coupon_template_source_type CHECK (source_type IN ('SYSTEM', 'ACTIVITY', 'NEWCOMER')),
    ADD CONSTRAINT ck_tb_points_coupon_template_values CHECK (face_value >= 0 AND valid_days >= 0 AND total_count >= 0 AND issued_count >= 0 AND per_user_limit >= 0);

ALTER TABLE petcare.tb_points_coupon
    ADD CONSTRAINT ck_tb_points_coupon_status CHECK (status IN ('UNUSED', 'USED', 'EXPIRED')),
    ADD CONSTRAINT ck_tb_points_coupon_face_value CHECK (face_value >= 0);

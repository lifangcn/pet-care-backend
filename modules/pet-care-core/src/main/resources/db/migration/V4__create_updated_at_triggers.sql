CREATE OR REPLACE FUNCTION petcare.touch_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = statement_timestamp();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_tb_user_touch_updated_at BEFORE UPDATE ON petcare.tb_user FOR EACH ROW EXECUTE FUNCTION petcare.touch_updated_at();
CREATE TRIGGER trg_tb_pet_touch_updated_at BEFORE UPDATE ON petcare.tb_pet FOR EACH ROW EXECUTE FUNCTION petcare.touch_updated_at();
CREATE TRIGGER trg_tb_health_record_touch_updated_at BEFORE UPDATE ON petcare.tb_health_record FOR EACH ROW EXECUTE FUNCTION petcare.touch_updated_at();
CREATE TRIGGER trg_tb_reminder_touch_updated_at BEFORE UPDATE ON petcare.tb_reminder FOR EACH ROW EXECUTE FUNCTION petcare.touch_updated_at();
CREATE TRIGGER trg_tb_reminder_execution_touch_updated_at BEFORE UPDATE ON petcare.tb_reminder_execution FOR EACH ROW EXECUTE FUNCTION petcare.touch_updated_at();
CREATE TRIGGER trg_tb_post_touch_updated_at BEFORE UPDATE ON petcare.tb_post FOR EACH ROW EXECUTE FUNCTION petcare.touch_updated_at();
CREATE TRIGGER trg_tb_label_touch_updated_at BEFORE UPDATE ON petcare.tb_label FOR EACH ROW EXECUTE FUNCTION petcare.touch_updated_at();
CREATE TRIGGER trg_tb_interaction_touch_updated_at BEFORE UPDATE ON petcare.tb_interaction FOR EACH ROW EXECUTE FUNCTION petcare.touch_updated_at();
CREATE TRIGGER trg_tb_activity_touch_updated_at BEFORE UPDATE ON petcare.tb_activity FOR EACH ROW EXECUTE FUNCTION petcare.touch_updated_at();
CREATE TRIGGER trg_tb_knowledge_document_touch_updated_at BEFORE UPDATE ON petcare.tb_knowledge_document FOR EACH ROW EXECUTE FUNCTION petcare.touch_updated_at();
CREATE TRIGGER trg_tb_points_account_touch_updated_at BEFORE UPDATE ON petcare.tb_points_account FOR EACH ROW EXECUTE FUNCTION petcare.touch_updated_at();
CREATE TRIGGER trg_tb_points_coupon_template_touch_updated_at BEFORE UPDATE ON petcare.tb_points_coupon_template FOR EACH ROW EXECUTE FUNCTION petcare.touch_updated_at();
CREATE TRIGGER trg_tb_role_touch_updated_at BEFORE UPDATE ON petcare.tb_role FOR EACH ROW EXECUTE FUNCTION petcare.touch_updated_at();

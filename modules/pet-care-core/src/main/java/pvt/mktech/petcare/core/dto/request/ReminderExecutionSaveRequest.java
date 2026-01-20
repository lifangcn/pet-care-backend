package pvt.mktech.petcare.core.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "提醒执行项保存请求")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReminderExecutionSaveRequest {

    @Schema(description = "完成备注")
    private String completionNotes;

}
package pvt.mktech.petcare.core.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "提醒状态修改请求，标记完成、已读")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteReminderRequest {

    @Schema(description = "完成备注")
    private String completionNotes;

}
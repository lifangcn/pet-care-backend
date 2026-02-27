package pvt.mktech.petcare.chat.tool;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pvt.mktech.petcare.chat.dto.RepeatTypeOfReminder;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReminderSaveRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 523556646273809362L;

    private Long id;
    private String petName;
    private Long userId;
    private Long sourceId;
    private String title;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduleTime;
    private Integer remindBeforeMinutes;
    private RepeatTypeOfReminder repeatType;
    private String repeatConfig;
    private Boolean isActive = Boolean.TRUE;
    private Integer totalOccurrences;
    private Integer completedCount;
    private LocalDateTime completedTime;
}
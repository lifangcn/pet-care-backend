package pvt.mktech.petcare.api.service;

import pvt.mktech.petcare.api.dto.ReminderSaveRequest;

import java.io.Serializable;

public interface ReminderDubboService extends Serializable {
    boolean saveReminder(ReminderSaveRequest request);
}


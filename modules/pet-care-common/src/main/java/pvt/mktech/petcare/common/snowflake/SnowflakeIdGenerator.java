package pvt.mktech.petcare.common.snowflake;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.exception.SystemException;

@Data
@ConfigurationProperties(prefix = "snowflake")
public class SnowflakeIdGenerator {

    private static final long BEGIN_TIMESTAMP = 1735689600L;
    private static final long SEQUENCE_BIT = 12;
    private static final long MACHINE_BIT = 5;
    private static final long DATA_CENTER_BIT = 5;
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BIT);
    private static final long MACHINE_LEFT = SEQUENCE_BIT;
    private static final long DATA_CENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    private static final long TIMESTAMP_LEFT = DATA_CENTER_LEFT + DATA_CENTER_BIT;
    private long sequence = 0L;
    private long lastStamp = -1L;

    private Long dataCenterId = 1L;
    private Long machineId = 1L;

    public synchronized long nextId() {
        long currStamp = getNewStamp();
        if (currStamp < lastStamp) {
            throw new SystemException(ErrorCode.ID_GENERATOR_ERROR, "时钟回拨，拒绝生成ID");
        }

        if (currStamp == lastStamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0L) {
                currStamp = getNextMill();
            }
        } else {
            sequence = 0L;
        }

        lastStamp = currStamp;

        return (currStamp - BEGIN_TIMESTAMP) << TIMESTAMP_LEFT
                | dataCenterId << DATA_CENTER_LEFT
                | machineId << MACHINE_LEFT
                | sequence;
    }

    private long getNextMill() {
        long mill = getNewStamp();
        while (mill <= lastStamp) {
            mill = getNewStamp();
        }
        return mill;
    }

    private long getNewStamp() {
        return System.currentTimeMillis();
    }
}
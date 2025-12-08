package pvt.mktech.petcare.common.redis;

import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;

/**
 * {@code @description}:
 * {@code @date}: 2025/11/28 14:35
 *
 * @author Michael
 */
//@Slf4j
//@Service
//@RequiredArgsConstructor
public class IdGeneratorService {

//    private final RedissonClient redissonClient;
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    
}

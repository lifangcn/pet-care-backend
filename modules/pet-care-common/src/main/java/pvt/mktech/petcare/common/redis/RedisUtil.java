package pvt.mktech.petcare.common.redis;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBitSet;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

/**
 * Redis缓存工具类
 */
@RequiredArgsConstructor
public class RedisUtil {

    private final RedissonClient redissonClient;

    /**
     * 设置缓存
     */
    public <T> void set(String key, T value) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value);
    }

    /**
     * 设置缓存（带过期时间）
     */
    public <T> void set(String key, T value, Duration duration) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value, duration);
    }

    /**
     * 获取缓存
     */
    public <T> T get(String key) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    /**
     * 删除缓存
     */
    public boolean delete(String key) {
        return redissonClient.getBucket(key).delete();
    }

    /**
     * 判断缓存是否存在
     */
    public boolean exists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    /**
     * 设置Hash缓存
     */
    public <K, V> void putHash(String key, Map<K, V> fieldValueMap) {
        RMap<K, V> map = redissonClient.getMap(key);
        map.putAll(fieldValueMap);
    }

    /**
     * 设置Hash缓存
     */
    public <K, V> void putHash(String key, K field, V value) {
        RMap<K, V> map = redissonClient.getMap(key);
        map.put(field, value);
    }

    /**
     * 获取Hash缓存
     */
    public <K, V> V getHash(String key, K field) {
        RMap<K, V> map = redissonClient.getMap(key);
        return map.get(field);
    }

    /**
     * 递增（原子操作）
     */
    public long increment(String key, long delta) {
        return redissonClient.getAtomicLong(key).addAndGet(delta);
    }

    /**
     * 递减（原子操作）
     */
    public long decrement(String key, long delta) {
        return redissonClient.getAtomicLong(key).addAndGet(-delta);
    }

    /**
     * 设置缓存有效期
     */
    public boolean expire(String key, Duration duration) {
        return redissonClient.getBucket(key).expire(duration);
    }

    /* 扩展功能 ZSet */
    public boolean addToZSet(String key, Object value, double score) {
        return redissonClient.getScoredSortedSet(key).add(score, value);
    }

    public boolean removeFromZSet(String key, Object value) {
        return redissonClient.getScoredSortedSet(key).remove(value);
    }


    /* 扩展功能 BitMap */
    /**
     * 设置 BitMap 位
     *
     * @param key    缓存键
     * @param offset 位置
     * @param value  值
     * @return 设置结果
     */
    public boolean setBit(String key, long offset, boolean value) {
        return redissonClient.getBitSet(key).set(offset, value);
    }

    /**
     * 获取 BitMap 位
     *
     * @param key    缓存键
     * @return BitMap
     *
     */
    public RBitSet getBitSet(String key) {
        return redissonClient.getBitSet(key);
    }

    /**
     * 获取 BitMap 中位true 位数
     *
     * @param key 缓存键
     * @return 统计位数
     */
    public long getBitCount(String key) {
        return redissonClient.getBitSet(key).cardinality();
    }

    public Collection<ScoredEntry<Object>> rangeByScoreWithScores(String key, double min, double max) {
        return redissonClient.getScoredSortedSet(key).entryRange(min, true, max, true);
    }
}
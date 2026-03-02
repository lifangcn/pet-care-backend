package pvt.mktech.petcare.common.redis;

import org.redisson.api.*;
import org.redisson.api.options.KeysScanOptions;
import org.redisson.client.protocol.ScoredEntry;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@code @description}: Redis缓存工具类
 * {@code @date}: 2026/1/29 15:00
 *
 * @author Michael Li
 */
public record RedisUtil(RedissonClient redissonClient) {

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
     * 设置原子长整型值（用于配合 increment/decrement 操作）
     */
    public long getAtomicLong(String key) {
        return redissonClient.getAtomicLong(key).get();
    }


    /**
     * 设置原子长整型值（用于配合 increment/decrement 操作）
     */
    public void setAtomicLong(String key, long value) {
        redissonClient.getAtomicLong(key).set(value);
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
     * @param offset 位置
     * @return 位值，key不存在返回false
     */
    public boolean getBit(String key, long offset) {
        return redissonClient.getBitSet(key).get(offset);
    }

    /**
     * 获取 BitMap 位
     *
     * @param key 缓存键
     * @return BitMap，key不存在返回null
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

    /* 扩展功能 Set */

    /**
     * 向 Set 集合添加元素
     *
     * @param key   缓存键
     * @param value 元素值
     * @param <T>   元素类型
     * @return 是否添加成功（元素不存在时返回 true）
     */
    public <T> boolean setAdd(String key, T value) {
        RSet<T> set = redissonClient.getSet(key);
        return set.add(value);
    }

    /**
     * 判断 Set 集合是否包含元素
     *
     * @param key   缓存键
     * @param value 元素值
     * @param <T>   元素类型
     * @return 是否包含
     */
    public <T> boolean setIsMember(String key, T value) {
        RSet<T> set = redissonClient.getSet(key);
        return set.contains(value);
    }

    /**
     * 获取 Set 集合所有元素
     *
     * @param key 缓存键
     * @param <T> 元素类型
     * @return 所有元素
     */
    public <T> Collection<T> setMembers(String key) {
        RSet<T> set = redissonClient.getSet(key);
        return set.readAll();
    }

    /**
     * 删除 Set 集合中的元素
     *
     * @param key   缓存键
     * @param value 元素值
     * @param <T>   元素类型
     * @return 是否删除成功
     */
    public <T> boolean setRemove(String key, T value) {
        RSet<T> set = redissonClient.getSet(key);
        return set.remove(value);
    }

    /**
     * 获取 Set 集合大小
     *
     * @param key 缓存键
     * @return 集合大小
     */
    public long setSize(String key) {
        RSet<Object> set = redissonClient.getSet(key);
        return set.size();
    }

    /* 扩展功能 Keys/Scan */

    /**
     * 渐进式扫描 Key（生产环境推荐）
     *
     * @param pattern 匹配模式（如 core:post:view_count:*）
     * @param count   每次扫描返回的数量
     * @return 匹配的 Key 集合
     */
    public Collection<String> scan(String pattern, int count) {
        KeysScanOptions options = KeysScanOptions.defaults().pattern( pattern).limit(count);
        return redissonClient.getKeys().getKeysStream(options).collect(Collectors.toSet());

    }
}
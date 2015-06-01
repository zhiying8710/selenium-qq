package me.binge.selenium.qq.cache.impl;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import me.binge.redis.codec.RedisCodec;
import me.binge.redis.codec.impl.JacksonJsonRedisCodec;
import me.binge.redis.exception.RedisExecExecption;
import me.binge.redis.exec.RedisExecutor;
import me.binge.redis.exec.RedisExecutors;
import me.binge.selenium.qq.cache.Cacher;
import me.binge.selenium.qq.common.LoginResult;

import org.apache.log4j.Logger;

public class RedisCacher implements Cacher {

    private static final Logger logger = Logger.getLogger(RedisCacher.class);

    private RedisExecutor<?> executor;
    private RedisCodec redisCodec = new JacksonJsonRedisCodec();

    private volatile boolean isClosed = false;

    public RedisCacher(Properties props) {
        this.executor = RedisExecutors.get(props);
    }

    public RedisCacher(Properties props, RedisCodec redisCodec) {
        this(props);
        if (redisCodec != null) {
            this.redisCodec = redisCodec;
        }
    }

    public RedisCacher(RedisExecutor<?> executor) {
        this.executor = executor;
    }

    public RedisCacher(RedisExecutor<?> executor, RedisCodec redisCodec) {
        if (executor == null) {
            throw new NullPointerException("RedisExecutor can not be null.");
        }
        this.executor = executor;
        if (redisCodec != null) {
            this.redisCodec = redisCodec;
        }
    }

    @Override
    public void cacheLoginResult(String cacheKey, LoginResult value) {
        this.executor.hset(cacheKey, Cacher.LOGIN_RES_KEY,
                this.redisCodec.encodeToStr(value.toMap()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCacheValue(String cacheKey, String key) {
        String value = this.executor.hget(cacheKey, key);
        if (value == null) {
            return null;
        }
        try {
            return this.redisCodec.decodeFromStr(value);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("get cacheValue for " + key + " from " + cacheKey + " error.", e);
            return (T) value;
        }
    }

    @Override
    public void delCacheValue(String cacheKey, String... key) {
        this.executor.hdel(cacheKey, key);
    }

    @Override
    public void addCacheKey(String queueKey, String... cacheKey) {
        this.executor.sadd(queueKey, cacheKey);
    }

    @Override
    public Set<String> getQueueCacheKeys(String queueKey) {
        try {
            return this.executor.smembers(queueKey);
        } catch (Exception e) {
            logger.error("get cahceKeys from " + queueKey + " error.", e);
            return null;
        }
    }

    @Override
    public void delQueue(String queueKey) {
        this.executor.srem(Cacher.All_QUEUES_KEY, new String[]{queueKey});
        this.executor.del(queueKey);
        this.executor.del(Cacher.RUNNING_CACHE_KEYS_PREFIX + queueKey);
    }

    @Override
    public void registQueueKey(String queueKey) {
        try {
            this.executor.sadd(Cacher.All_QUEUES_KEY, queueKey);
        } catch (Exception e) {
            throw new RedisExecExecption("regist queueKey " + queueKey + " failed", e);
        }
    }

    @Override
    public <T> T getLoginInfo(String cacheKey) {
        return getCacheValue(cacheKey, Cacher.LOGIN_INFO_KEY);
    }

    @Override
    public void delCacheKey(String queueKey, String... cacheKey) {
        this.executor.srem(queueKey, cacheKey);
    }

    @Override
    public Set<String> getAllQueues() {
        return this.executor.smembers(Cacher.All_QUEUES_KEY);
    }

    @Override
    public void addCacheValue(String cacheKey, String key, Object value) {
        this.executor.hset(cacheKey, key, this.redisCodec.encodeToStr(value));
    }

    @Override
    public Map<String, String> getCacheValues(String cacheKey) {
        return this.executor.hgetAll(cacheKey);
    }

    @Override
    public void close() {
        try {
            this.executor.close(null);
        } catch (Exception e) {
        }
        this.isClosed = true;
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    public RedisExecutor<?> getExecutor() {
        return executor;
    }

//    @Override
//    public void delRunningCacheKey(String queueKey, String cacheKey) {
//        this.executor.srem(Cacher.RUNNING_CACHE_KEYS_PREFIX + queueKey, new String[]{cacheKey});
//    }
//
//    @Override
//    public boolean addRunningCacheKey(String queueKey, String cacheKey) {
//        synchronized (cacheKey) {
//            String key = Cacher.RUNNING_CACHE_KEYS_PREFIX + queueKey;
//            if (!this.executor.sismember(key, cacheKey)) {
//                this.executor.sadd(key, cacheKey);
//                return true;
//            }
//        }
//        return false;
//    }
//
//    @Override
//    public Set<String> getAllRunningCacheKeys(String queueKey) {
//        try {
//            return this.executor.smembers(Cacher.RUNNING_CACHE_KEYS_PREFIX + queueKey);
//        } catch (Exception e) {
//            logger.error("get all running cahceKeys from " + queueKey + " error.", e);
//            return null;
//        }
//    }
//
//    @Override
//    public boolean runningCacheKey(String queueKey, String cacheKey) {
//        return this.executor.sismember(Cacher.RUNNING_CACHE_KEYS_PREFIX + queueKey, cacheKey);
//    }
//

//
//    @Override
//    public Long getAllRunningCacheKeysCnt(String queueKey) {
//        try {
//            return this.executor.scard(Cacher.RUNNING_CACHE_KEYS_PREFIX + queueKey);
//        } catch (Exception e) {
//            logger.error("get all running cahceKeys count from " + queueKey + " error.", e);
//            return null;
//        }
//    }

}

package me.binge.selenium.qq.cache;

import java.util.Map;
import java.util.Set;

import me.binge.selenium.qq.common.LoginResult;


public interface Cacher {

    public static final String LOGIN_RES_KEY = "login_result"; // 值是个Map
    public static final String VERIFY_CODE_KEY = "verify_code";
    public static final String EMAIL_KEY = "email";
    public static final String PWD_KEY = "pwd";
    public static final String INDEPENT_PWD_KEY = "indepent_pwd";
    public static final String QQ_LOGIN_QUEUE_PREFIX = "__qq_login_queue_";
    public static final String All_QUEUES_KEY = "__qq_login_queues";
    public static final String LOGIN_INFO_KEY = "login_info"; // 值是个Map
    public static final String RUNNING_CACHE_KEYS_PREFIX = "__running_cache_keys_";

    void cacheLoginResult(String cacheKey, LoginResult value);

    void addCacheValue(String cacheKey, String key, Object value);

    <T> T getCacheValue(String cacheKey, String key);

    void delCacheValue(String cacheKey, String... key);

    void delCacheKey(String queueKey, String... cacheKey);

    void addCacheKey(String queueKey, String... cacheKey);

    Set<String> getQueueCacheKeys(String queueKey);

    void delQueue(String queueKey);

    void registQueueKey(String queueKey);

    <T> T getLoginInfo(String cacheKey);

    Set<String> getAllQueues();

    Map<String, String> getCacheValues(String cacheKey);

    void close();

    boolean isClosed();

//    void delRunningCacheKey(String queueKey, String cacheKey);
//
//    boolean addRunningCacheKey(String queueKey, String cacheKey);
//
//    Set<String> getAllRunningCacheKeys(String queueKey);
//
//    boolean runningCacheKey(String queueKey, String cacheKey);
//
//
//    Long getAllRunningCacheKeysCnt(String queueKey);

}

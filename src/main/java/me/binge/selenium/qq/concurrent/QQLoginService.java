package me.binge.selenium.qq.concurrent;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import me.binge.selenium.qq.Launcher;
import me.binge.selenium.qq.cache.Cacher;
import me.binge.selenium.qq.cache.impl.RedisCacher;
import me.binge.selenium.qq.common.LoginResult;
import me.binge.selenium.qq.login.QQLogin;
import me.binge.selenium.qq.utils.ThrealLocalUtils;

import org.apache.log4j.Logger;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.SessionNotFoundException;
import org.openqa.selenium.remote.UnreachableBrowserException;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractScheduledService;

public class QQLoginService extends AbstractExecutionThreadService {

    private static DesiredCapabilities caps = null;

    static {

        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<String, Object>();
        prefs.put("profile.default_content_settings.images", 2); // 隐藏图片
        prefs.put("profile.default_content_setting_values.images", 2); // 隐藏图片
        options.setExperimentalOption("prefs", prefs);
        caps = DesiredCapabilities.chrome();
        caps.setCapability(ChromeOptions.CAPABILITY, options);

    }

    private static final Logger logger = Logger.getLogger(QQLoginService.class);

    private static final Map<String, AbstractScheduledService> listeners = new ConcurrentHashMap<String, AbstractScheduledService>();

    private static final Map<String, Boolean> runningCacheKeys = new ConcurrentHashMap<String, Boolean>();

    private static final AtomicInteger CURR_CONCURRENT = new AtomicInteger(0);

    static class CacheKeyInfo {
        private String cacheKey;
        private long time;
        private Map<String, String> loginInfo;

        public CacheKeyInfo(String cacheKey, long time, Map<String, String> loginInfo) {
            this.cacheKey = cacheKey;
            this.time = time;
            this.loginInfo = loginInfo;
        }

        public String getCacheKey() {
            return cacheKey;
        }

        public long getTime() {
            return time;
        }

        public Map<String, String> getLoginInfo() {
            return loginInfo;
        }
    }

    private static volatile boolean running = false;

    private static QQLogin qqLogin = null;
    private static long loginTimeOutMills;
    private static Cacher cacher;
    private static String queueKey;
    private static boolean envTest;
    private static long testWaitPerLoginSecs;
    private static int driverConcurrentCnt;

    private String email;
    private Map<String, String> loginInfo;
    private String cacheKey;
    private long start;
    private long enqueueTime;
    private ChromeDriver webDriver;

    public QQLoginService(CacheKeyInfo cacheKeyInfo) {
        this.cacheKey = cacheKeyInfo.getCacheKey();
        this.loginInfo = cacheKeyInfo.getLoginInfo();
        this.email = loginInfo.get(Cacher.EMAIL_KEY);
        this.enqueueTime = cacheKeyInfo.getTime();
    }

    protected static QQLoginService getInstance(CacheKeyInfo cacheKeyInfo) {
        return new QQLoginService(cacheKeyInfo);
    }

    public static void init() {
        String serverId = null;
        try {
            serverId = geneServerId();
            if (serverId == null) {
                throw new NullPointerException("serverId is null");
            }
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }

        Properties props = getProps();

        cacher = new RedisCacher(props);
        queueKey = Cacher.QQ_LOGIN_QUEUE_PREFIX + serverId;
        logger.info("get current sys queueKey is " + queueKey + ", clean it.");
        cacher.delQueue(queueKey);

        cacher.registQueueKey(queueKey);
        logger.info("regist queueKey");

        long loginTimeOutMills = Long.valueOf(props.getProperty("login.timeout.mills", (5 * 60 * 1000) + ""));
        QQLoginService.loginTimeOutMills = loginTimeOutMills;
        logger.info("login timeout mills is " + loginTimeOutMills);

        driverConcurrentCnt = Integer.valueOf(props.getProperty("driver.concurrent.cnt", (3) + ""));

        envTest = Boolean.valueOf(props.getProperty("env.test", "false"));
        if (envTest) {
            testWaitPerLoginSecs = Long.valueOf(props.getProperty("test.wait.sec.per.login", (5 * 60) + ""));
        }

        qqLogin = new QQLogin(cacher, props);
        running = true;
    }

    public static void boot() {

        QQLoginService.init();

        logger.info("init QQLoginService, and add shutdown hook.");
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    QQLoginService.stop();
                    logger.info("sys shutdown.");
                } catch (Exception e) {
                    logger.error("sys shutdown failed.", e);
                }
                System.exit(0);
            }
        }));

        while (running) {
            try {
                Set<String> cacheKeys = cacher.getQueueCacheKeys(queueKey);
                if (cacheKeys == null) { // 缓存异常
                    logger.warn("get cache key from " + queueKey + " is null, pls check cache is function or not.");
                    continue;
                }
                if (cacheKeys.isEmpty()) { // 缓存正常但是无可处理数据
                    continue;
                }

                for (String cacheKey : cacheKeys) {
                    if (!isRunning(cacheKey)) {
                        Map<String, String> loginInfo = null;
                        try {
                            loginInfo = cacher.getLoginInfo(cacheKey);
                            if (loginInfo == null) {
                                logger.warn("get login info from " + cacheKey + " is null.");
                                continue;
                            }
                        } catch (Exception e) {
                            logger.error("get login info from " + cacheKey + " error.", e);
                            continue;
                        }
                        CacheKeyInfo cacheKeyInfo = new CacheKeyInfo(cacheKey, System.currentTimeMillis(), loginInfo);
                        if (running) {
                            getInstance(cacheKeyInfo).startAsync();
                        }
                    }
                }
            } finally {
                sleep(1000);
            }
        }
    }

    private static boolean isRunning(String speicCacheKey) {
        return runningCacheKeys.containsKey(speicCacheKey);
    }

    @Override
    protected void run() throws Exception {

        if (!addRunningCacheKey(cacheKey) || !running) { // 校验cacheKey,
            // 如果有相同的cacheKey正在运行,
            // 则为false, 不进行登录操作
            return;
        }

        synchronized (CURR_CONCURRENT) {
            while (CURR_CONCURRENT.get() >= driverConcurrentCnt && running) { // 超过并发数
                sleep(1000);
            }

            if (!running) {
                return;
            }

            CURR_CONCURRENT.incrementAndGet();
        }

        if (envTest) {
            long miss = (testWaitPerLoginSecs * 1000) - (System.currentTimeMillis() - this.enqueueTime);
            logger.warn("for [" + this.email + "] in test env, every login must sleep " + (testWaitPerLoginSecs * 1000) + " mills, counting previous sleep, now must sleep " + miss + " mills.");
            if (miss > 0) {
                sleep(miss);
            }
        }
        if (!running) {
            return;
        }

        this.webDriver = new ChromeDriver(caps);
        ThrealLocalUtils.WEBDRIVER_LOCAL.set(this.webDriver);
        ThrealLocalUtils.CAHCEKKEY_LOCAL.set(this.cacheKey);

        this.start = System.currentTimeMillis();
        logger.info("login for " + email + " start, regist a listener for it.");
        this.addListener();
        try {
            LoginResult loginResult = qqLogin.login(loginInfo, cacheKey);
            logger.info("[ Z ] login for [" + email + "] complete, result: " + loginResult + ".");
            qqLogin.cacheLoginResult(cacheKey, loginResult);
        } catch (Exception e) {
            if (e instanceof SessionNotFoundException || e instanceof UnreachableBrowserException || e instanceof NoSuchWindowException) { // 浏览器被正常关闭了
                logger.error("login for " + email + " failed, exception occurred, webDriver has been closed: " + e.getMessage());
            } else if (e.getClass().getName().startsWith("org.openqa.selenium")) { // org.openqa.selenium包下的其他错误
                logger.error("login for " + email + " failed, exception occurred, webDriver error, take a screeshot: " + qqLogin.screenshotErr(), e);
            } else {
                logger.error("login for " + email + " failed, exception occurred.", e);
            }
            qqLogin.cacheFailedLoginResult(cacheKey);
        }
        try {
            quitWebDriver();

            AbstractScheduledService listener = listeners.remove(cacheKey);
            if (listener != null) {
                listener.stopAsync();
            }
            release();
        } catch (Exception e) {
            logger.error("release cacheKey " + cacheKey + " error.", e);
        }
        CURR_CONCURRENT.addAndGet(-1);
    }

    /**
     * 不用 {@link ChromeDriver#quit()} 是因为quit会去关闭所有window(虽然这里只会有一个window), 并且去clean和close一些残留的东西, 花的时间更长并且会发生警告:</br>
     * Command failed to close cleanly. Destroying forcefully (v2). org.openqa.selenium.os.UnixProcess$SeleniumWatchDog@fff2a2
     */
    public void quitWebDriver() {
        if (this.webDriver != null) {
            try {
//                this.webDriver.quit();
                this.webDriver.close();
            } catch (Exception e) {
                logger.error("quit chrome web driver for [" + email + "] error.", e);
            }
            this.webDriver = null;
        }
    }

    private boolean addRunningCacheKey(String cacheKey) {
        return runningCacheKeys.put(cacheKey, Boolean.TRUE) == null;
    }

    public long getStart() {
        return start;
    }

    private void release() {
        ThrealLocalUtils.CAHCEKKEY_LOCAL.remove();
        ThrealLocalUtils.WEBDRIVER_LOCAL.remove();
        release(cacheKey, new CountDownLatch(1));
    }

    private static void release(final String speicCacheKey, final CountDownLatch latch) {
        runningCacheKeys.remove(speicCacheKey);
        int cnt = 0;
        while (true) {
            if (cnt > 3) {
                break;
            }
            try {
                if (!cacher.isClosed()) {
                    cacher.delCacheKey(queueKey, speicCacheKey);
                }
                latch.countDown();
                break;
            } catch (Exception e) {
                logger.error("QQLogin release cacheKey : " + speicCacheKey + " from queue : " + queueKey + " failed.", e);
                sleep(3 * 1000);
            }
            cnt ++;
        }
    }

    public void addListener() {
        new AbstractScheduledService() {

            @Override
            protected Scheduler scheduler() {
                return Scheduler.newFixedRateSchedule(100, 1000, TimeUnit.MILLISECONDS);
            }

            @Override
            protected void runOneIteration() throws Exception {
                boolean timeout = System.currentTimeMillis() - getStart() >= loginTimeOutMills;
                if (!running || QQLoginService.this.state() == State.TERMINATED || QQLoginService.this.state() == State.FAILED || timeout) { //超时或线程出现异常
                    logger.warn("for [" + email + "] login thread state :" + QQLoginService.this.state() + ", running : " + running + ", timeout: " + timeout + ", callback call.");

                    quitWebDriver();
                    release();
                    AbstractScheduledService listener = listeners.remove(cacheKey);
                    if (listener != null) {
                        listener.stopAsync();
                    }
                }
            }

            protected void startUp() throws Exception {
                listeners.put(cacheKey, this);
            }

        }.startAsync();
    }

    public static void stop() {
        running = false;
        try {
            cacher.delQueue(queueKey);
        } catch (Exception e) {
            logger.error("delete " + queueKey + " failed, please del it by hand.", e);
        }
        Set<String> allRunningCacheKeys = runningCacheKeys.keySet();
        if (allRunningCacheKeys != null) {
            CountDownLatch latch = new CountDownLatch(allRunningCacheKeys.size());
            for (String cacheKey : allRunningCacheKeys) {
                release(cacheKey, latch);
            }
            while (latch.getCount() != 0) {

            }
        } else {
            logger.error("all running cacheKeys in " + queueKey + " did not be released.");
        }

        Set<String> cacheKeys = QQLoginService.listeners.keySet();
        for (String cacheKey : cacheKeys) {
            try {
                QQLoginService.listeners.get(cacheKey).stopAsync();
            } catch (Exception e) {
            }
        }

        cacher.close();
    }

    private static String geneServerId() throws Exception  {
        InetAddress ia = InetAddress.getLocalHost();

        byte[] mac = NetworkInterface.getByInetAddress(ia).getHardwareAddress();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mac.length; i++) {
            if (i != 0) {
                sb.append("-");
            }
            // mac[i] & 0xFF 是为了把byte转化为正整数
            String s = Integer.toHexString(mac[i] & 0xFF);
            sb.append(s.length() == 1 ? 0 + s : s);
        }
        return sb.toString().toLowerCase().replace("-", "");
    }

    public static String getQueueKey() {
        return queueKey;
    }

    public static Cacher getCacher() {
        return cacher;
    }

    private static Properties getProps() {
        Properties props = new Properties();

        InputStream is = Launcher.class.getClassLoader().getResourceAsStream("conf.properties");
        try {
            props.load(is);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
            }
            is = null;
        }
        return props;
    }

    public static void sleep(long mills) {
        try {
            TimeUnit.MILLISECONDS.sleep(mills);
        } catch (Exception e) {
        }
    }

}

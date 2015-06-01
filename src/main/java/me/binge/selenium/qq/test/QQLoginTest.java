package me.binge.selenium.qq.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import me.binge.redis.codec.impl.JacksonJsonRedisCodec;
import me.binge.redis.exec.impl.SentinelJedisExecutor;
import me.binge.selenium.qq.Launcher;
import me.binge.selenium.qq.cache.Cacher;
import me.binge.selenium.qq.cache.impl.RedisCacher;
import me.binge.selenium.qq.common.LoginResult;
import me.binge.selenium.qq.utils.ThrealLocalUtils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.ScanResult;

public class QQLoginTest {

    private static Properties getProps() {
        Properties props = new Properties();

        InputStream is = Launcher.class.getClassLoader().getResourceAsStream(
                "conf.properties");
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

    public static void main(String[] args) throws Exception {
//        System.getProperties().setProperty("webdriver.chrome.driver", "./src/main/resources/chromedriver.exe");
//        String[] PROXIES = {"119.254.101.165:3128"};
//        ChromeOptions options = new ChromeOptions();
//        Map<String, Object> prefs = new HashMap<String, Object>();
//        prefs.put("profile.default_content_settings.images", 2); // 隐藏图片
//        prefs.put("profile.default_content_setting_values.images", 2); // 隐藏图片
//        options.setExperimentalOption("prefs", prefs);
//        DesiredCapabilities caps = DesiredCapabilities.chrome();
//        caps.setCapability(ChromeOptions.CAPABILITY, options);
//
////        if (PROXIES.length > 0) {
////            Proxy proxy = new Proxy();
////            String ip = PROXIES[RandomUtils.nextInt(0, PROXIES.length)];
////            proxy.setHttpProxy(ip);
////            caps.setCapability("proxy", proxy);
////            ThrealLocalUtils.PROXY_LOCAL.set(ip);
////        }
//
//        ChromeDriver webDriver = new ChromeDriver(caps);
//        webDriver.get("http://ip138.com");
//        System.exit(0);

//        HashSet<String> sentinels = new HashSet<String>();
//        sentinels.add("10.193.1.87:26379");
//        sentinels.add("10.193.1.87:26380");
//        sentinels.add("10.193.1.86:26379");
//        JedisSentinelPool jedisSentinelPool = new JedisSentinelPool(
//                "mymaster", sentinels,
//                new GenericObjectPoolConfig(), 30000);
//        Jedis jedis = jedisSentinelPool.getResource();
//        JacksonJsonRedisCodec codec = new JacksonJsonRedisCodec();
//        Map<Integer, Integer> resCount = new HashMap<Integer, Integer>();
//        while (true) {
//            ScanResult<String> scanResult = jedis.scan("0");
//            List<String> keys = scanResult.getResult();
//            for (String key : keys) {
//                if (!jedis.type(key).equals("hash") || !key.toLowerCase().endsWith("@qq.com") || !key.matches("^[0-9]+$")) {
//                    continue;
//                }
//                String loginRes = jedis.hget(key, Cacher.LOGIN_INFO_KEY);
//                if (loginRes == null) {
//                    continue;
//                }
//                LoginResult loginResult = codec.decodeFromStr(loginRes);
//            }
//            if (scanResult.getStringCursor().equals("0")) {
//                break;
//            }
//        }
//        jedisSentinelPool.close();
//        System.exit(0);


        Cacher cacher = new RedisCacher(getProps());


        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("gd.json"))));
        String sloginInfos = br.readLine();
        br.close();
        JSONArray loginInfos = (JSONArray) JSONArray.parse(sloginInfos);
        for (int i = 0; i < loginInfos.size(); i++) {
            Map<String, String> loginInfo = (Map<String, String>) loginInfos.get(i);
            String e = loginInfo.get("email").toString();
            String cacheKey = DigestUtils.md5Hex(e);
            cacher.addCacheValue(cacheKey, Cacher.LOGIN_INFO_KEY, loginInfo);
            cacher.addCacheKey("__qq_login_queue_3464a91a1301", cacheKey);
        }

//        Set<String> es = mails.keySet();
//        for (String e : es) {
//            String cacheKey = DigestUtils.md5Hex(e);
//            Map<String, String> loginInfo = new HashMap<String, String>();
//            loginInfo.put(Cacher.EMAIL_KEY, e);
//            loginInfo.put(Cacher.PWD_KEY, mails.get(e));
//            cacher.addCacheValue(cacheKey, Cacher.LOGIN_INFO_KEY, loginInfo);
//            cacher.addCacheKey("__qq_login_queue_3464a91a1301", cacheKey);
////            cacher.addCacheKey("__qq_login_queue_525400522194", cacheKey);
//        }

        // for (int i = 0; i < 30; i++) {
        // String cacheKey = "__test1111" + i;
        // Map<String, String> loginInfo = new HashMap<String, String>();
        // loginInfo.put(Cacher.EMAIL_KEY, "28611693" + i + "@qq.com");
        // loginInfo.put(Cacher.PWD_KEY, "zhiying8710@");
        // loginInfo.put(Cacher.INDEPENT_PWD_KEY, "zhiying8710@");
        // cacher.addCacheValue(cacheKey, Cacher.LOGIN_INFO_KEY, loginInfo);
        // cacher.addCacheKey("__qq_login_queue_3464a91a1301", cacheKey);
        // }
        //
        // Map<String, String> cacheValues =
        // cacher.getCacheValues("__test1111");
        // System.out.println(cacheValues);
        // Map<String, Object> loginResult = cacher.getCacheValue("__test1111",
        // "login_result");
        // System.out.println(loginResult);
        // Map<String, Object> data = (Map<String, Object>)
        // loginResult.get("data");
        // String sid = data.get("sid").toString();
        // String r = data.get("r").toString();
        // Collection<String> cookies = (List<String>) data.get("cookies");
        // System.out.println(cookies);
        // List<org.apache.commons.httpclient.Cookie> rCookies = new
        // ArrayList<org.apache.commons.httpclient.Cookie>();
        // for (String cookie : cookies) {
        // String[] s = cookie.split(";");
        // String name = "";
        // String value = "";
        // String domain = "";
        // Date expiryDate = null;
        // String path = "/";
        // boolean secure = false;
        // for (String ss : s) {
        // String[] sss = ss.split("=");
        // String v = sss[0].trim();
        // if (v.toLowerCase().equals("domain")) {
        // if (sss.length > 1) {
        // domain = sss[1];
        // }
        // continue;
        // } else if (v.toLowerCase().equals("path")) {
        // if (sss.length > 1) {
        // path = sss[1];
        // }
        // continue;
        // } else if (v.toLowerCase().equals("expires")) {
        // if (sss.length > 1) {
        // expiryDate = new
        // SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss z").parse(sss[1]);
        // }
        // continue;
        // } else {
        // name = v;
        // if (sss.length > 1) {
        // value = sss[1];
        // }
        // }
        // }
        // org.apache.commons.httpclient.Cookie rCookie = new
        // org.apache.commons.httpclient.Cookie();
        // rCookie.setDomain(domain);
        // rCookie.setExpiryDate(expiryDate);
        // rCookie.setName(name);
        // rCookie.setPath(path);
        // rCookie.setSecure(secure);
        // rCookie.setValue(value);
        // System.out.println(rCookie);
        // rCookies.add(rCookie);
        // }
        //
        // System.out.println(HttpUtils.getQQMailMainPage(sid, r, rCookies));

    }

}

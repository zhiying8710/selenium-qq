package me.binge.selenium.qq.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import me.binge.selenium.qq.Launcher;
import me.binge.selenium.qq.cache.Cacher;
import me.binge.selenium.qq.cache.impl.RedisCacher;

import org.apache.commons.codec.digest.DigestUtils;

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

        Cacher cacher = new RedisCacher(getProps());

        Map<String, String> mails = new HashMap<String, String>();
        mails.put("402095404@qq.com", "jj880928..");
        mails.put("511926723@qq.com", "qq168518");
        mails.put("1003280071@qq.com", "lvcehuan921206");
        mails.put("1761786787@qq.com", "DfShuai520");
        mails.put("boaichaoye@qq.com", "888999aaa");
        mails.put("362451601@qq.com", "long0107.");
        mails.put("397339468@qq.com", "heli19880904$$$$");
        mails.put("690713262@qq.com", "dandan920517");
        mails.put("248249243@qq.com", "dd121024");
        mails.put("153529799@qq.com", "zw752600zw");
        mails.put("109456808@qq.com", "6680638yymyym");
        mails.put("584168122@qq.com", "pangfu8230880");
        mails.put("545280394@qq.com", "zr1307720");
        mails.put("fei378262835@qq.com", "feier020708");
        mails.put("1939257614@qq.com", ".le13233972829le.");
        mails.put("139898875@qq.com", "a1234567");
        mails.put("feier378262835@qq.com", "feier020708");
        mails.put("707304296@qq.com", "www.yanchao.com");
        mails.put("237228365@qq.com", "zxx19900504");
        mails.put("258662182@qq.com", "kkk15887828387k");
        mails.put("panxiuyi0754@qq.com", "pjy121024");
        mails.put("56977449@qq.com", "*110110qq");
        mails.put("150641850@qq.com", "19840125cat");
        mails.put("8682304@qq.com", "hua415172175.");
        mails.put("865609@qq.com", "evankk01143");
        mails.put("1097295794@qq.com", "jiege.19940409");
        mails.put("1967553506@qq.com", "a07013525785");
        mails.put("925136168@qq.com", "925136168zhao@");
        mails.put("460140622@qq.com", "happy..2014");
        mails.put("274688171@qq.com", "CM274688171/");
        mails.put("252878532@qq.com", "miaozhi37");
        mails.put("252877532@qq.com", "miaozhi37");
        mails.put("961294689@qq.com", "sunhuiai@193");
        mails.put("838913963@qq.com", "tutu520");
        mails.put("303807968@qq.com", "19920220");
        mails.put("1187484328@qq.com", "taotao0605");
        mails.put("286634114@qq.com", "wuhao25251325");
        mails.put("271234760@qq.com", "a12121");

        Set<String> es = mails.keySet();
        for (String e : es) {
            String cacheKey = DigestUtils.md5Hex(e);
            Map<String, String> loginInfo = new HashMap<String, String>();
            loginInfo.put(Cacher.EMAIL_KEY, e);
            loginInfo.put(Cacher.PWD_KEY, mails.get(e));
            cacher.addCacheValue(cacheKey, Cacher.LOGIN_INFO_KEY, loginInfo);
            cacher.addCacheKey("__qq_login_queue_3464a91a1301", cacheKey);
//            cacher.addCacheKey("__qq_login_queue_525400522194", cacheKey);
        }

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

package me.binge.selenium.qq.login;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import me.binge.selenium.qq.cache.Cacher;
import me.binge.selenium.qq.common.LoginResult;
import me.binge.selenium.qq.common.VerifyCode;
import me.binge.selenium.qq.concurrent.QQLoginCallback;
import me.binge.selenium.qq.utils.HttpUtils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.internal.Base64Encoder;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.alibaba.fastjson.JSONArray;

public class QQLogin {

    private static final Logger logger = Logger.getLogger(QQLogin.class);

    private static DesiredCapabilities caps = null;

    private static final String R_NAME = "r=";
    private static final String SID_NAME = "sid=";
    private static final String VERIFY_CODE_FILE_PREFIX = "";
    private static final int DAMA_RETRY_MAX_TIMES = 3;
    private static final int VERIFY_CODE_RETRY_MAX_TIMES = 3;

    private static ThreadLocal<ChromeDriver> webDriverLocal = new ThreadLocal<ChromeDriver>();
    private static ThreadLocal<String> cacheKeyLocal = new ThreadLocal<String>();

    private Cacher cacher;
    private long timeout; // 从缓存获取数据的超时时间
    private int verifyRetryTimes; // 验证码重试次数
    private int indepentErrRetryTimes;
    private String errScreenshotDir;
    private int yundamaRetryMaxTimes;
    private int verifyCodeRetryMaxTimes;

    public QQLogin(Cacher cacher, Properties props) {
        this.cacher = cacher;
        this.timeout = TimeUnit.MILLISECONDS.convert(Long.valueOf(props.getProperty("cache.fetch.timeout.secs", "30")), TimeUnit.SECONDS);
        this.verifyRetryTimes = Integer.valueOf(props.getProperty("verify.retry.times", "5"));
        this.indepentErrRetryTimes = Integer.valueOf(props.getProperty("indepent.err.retry.times", "5"));
        this.errScreenshotDir = props.getProperty("err.screenshot.dir", "err_screenshot");
        this.yundamaRetryMaxTimes = Integer.valueOf(props.getProperty("yundama.retry.max.times", DAMA_RETRY_MAX_TIMES + ""));
        this.verifyCodeRetryMaxTimes = Integer.valueOf(props.getProperty("verify.code.retry.max.times", VERIFY_CODE_RETRY_MAX_TIMES + ""));
    }

    static {

        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<String, Object>();
        prefs.put("profile.default_content_settings.images", 2); // 隐藏图片
        prefs.put("profile.default_content_setting_values.images", 2); // 隐藏图片
        options.setExperimentalOption("prefs", prefs);
        caps = DesiredCapabilities.chrome();
        caps.setCapability(ChromeOptions.CAPABILITY, options);

    }

    /**
     * 禁止错误信息自动消失
     */
    private void disableShowErr() {
        webDriverLocal.get()
                .executeScript("pt.plogin.show_err = function(b, a){pt.plogin.hideLoading();"
                        + "$.css.show($('error_tips'));"
                        + "pt.plogin.err_m.innerHTML = b;"
                        + "clearTimeout(pt.plogin.errclock);}");
    }

    /**
     * 隐藏错误消息
     */
    private void hideErr() {
        webDriverLocal.get().executeScript("pt.plogin.hide_err();");
    }

    public LoginResult login(Map<String, String> loginInfo, String cacheKey, QQLoginCallback callback) {

        String email = loginInfo.get(Cacher.EMAIL_KEY);
        String password = loginInfo.get(Cacher.PWD_KEY);
        String indepentPassword = loginInfo.get(Cacher.INDEPENT_PWD_KEY);

        ChromeDriver webDriver = new ChromeDriver(caps);
        callback.setWebDriver(webDriver);

        cacheKeyLocal.set(cacheKey);
        webDriverLocal.set(webDriver);

        return login(email, password, indepentPassword, cacheKey, false);
    }

    public LoginResult login(String email,
            String password, String indepentPassword, String cacheKey, boolean retry) {

        logger.info("login for " + email + (retry ? " retry." : " begin."));

        LoginResult loginResult = doLogin(email, password);

        if (loginResult != null) { // 失败

            int resCode = loginResult.getResCode();
            switch (resCode) {
            case LoginResult.RES_NEED_RETRY:// 过期重试
                return login(email, password, indepentPassword, cacheKey, true);
            default:
                return loginResult;
            }
        }

        ChromeDriver webDriver = webDriverLocal.get();
        String succUrl = webDriver.getCurrentUrl();
        int idx = succUrl.indexOf(SID_NAME);
        String sid = null;
        if (idx == -1) { // 可能是登录出错, 也可能是需要进一步验证

            loginResult = doAfterLogin(email, indepentPassword);

            if (loginResult != null) {
                switch (loginResult.getResCode()) {
                case LoginResult.RES_NEED_RETRY:
                    return login(email, password, indepentPassword, cacheKey, true);
                default:
                    return loginResult;
                }
            }
        }
        succUrl = webDriver.getCurrentUrl();
        idx = succUrl.indexOf(SID_NAME);
        sid = succUrl.substring(idx + SID_NAME.length());
        idx = sid.indexOf("&");
        if (idx != -1) {
            sid = sid.substring(0, idx);
        }
        idx = succUrl.indexOf(R_NAME);
        String r = succUrl.substring(idx + R_NAME.length());
        idx = r.indexOf("&");
        if (idx != -1) {
            r = sid.substring(0, idx);
        }
        loginResult = new LoginResult();
        loginResult.setResCode(LoginResult.RES_LOGIN_SUCC);
        loginResult.add(LoginResult.DATA_SID, sid);
        loginResult.add(LoginResult.DATA_R, r);
        loginResult.add(LoginResult.DATA_COOKIES, getSuccCookies());

        logger.info("[ X ] login for ]" + email + "] suss, res: " + loginResult);

        return loginResult;
    }

    private LoginResult doLogin(String email, String password) {
        ChromeDriver webDriver = webDriverLocal.get();
        webDriver.get("https://mail.qq.com/");

        String frame = webDriver.findElement(By.cssSelector("#login_frame"))
                .getAttribute("src");
        webDriver.navigate().to(frame);

        click(By.cssSelector("#switcher_plogin"));

        sleep(100);

        WebElement u = webDriver.findElement(By.cssSelector("#u"));
        click(By.cssSelector("#u"));
        u.clear();
        sendKeys(u, email);

        WebElement p = webDriver.findElement(By.cssSelector("#p"));
        click(By.cssSelector("#p"));
        p.clear();
        sendKeys(p, password);

        sleep(1000);

        WebElement verifyimgElement = webDriver.findElement(By.cssSelector("#verifyimg"));
        if (verifyimgElement == null) {
            sleep(1000);

            verifyimgElement = webDriver.findElement(By.cssSelector("#verifyimg"));
        }
        if (verifyimgElement != null) { // 可能有验证码
            String verifyimgUrl = verifyimgElement.getAttribute("src");
            if (StringUtils.isNotBlank(verifyimgUrl)) { // 有验证码
                LoginResult loginResult = setVerifyCode(dama(getNewVerifyCode()));
                if (loginResult != null) { // 如果为null则拿到了验证码
                    return loginResult;
                }
            }
        }

        disableShowErr();

        click(By.cssSelector("#login_button"));

        sleep(500);

        int verifyRetryCnt = 0;
        while (!webDriver.getCurrentUrl().startsWith("https://mail.qq.com") && !webDriver.getCurrentUrl().startsWith("http://mail.qq.com")) { // 说明未到登录成功页面,可能发生了错误
            LoginResult loginResult = new LoginResult();
            try {
                WebElement errTips = webDriver.findElement(By.cssSelector("#error_tips"));
                if (errTips != null && errTips.isDisplayed()) {
                    String err = webDriver.findElement(By.cssSelector("#err_m")).getText();

                    if (err.contains("快速登录异常")) {
                        continue;
                    }

                    if (err.contains("页面过期")) { // 过期重试
                        logger.warn("login for [" + email + "], page timeout, retry.");
                        loginResult.setResCode(LoginResult.RES_NEED_RETRY);
                    } else if (err.contains("还没有输入验证码")) {
                        logger.warn("login for [" + email + "], need verify code.");
                        loginResult = setVerifyCode(dama(getNewVerifyCode()));
                        if (loginResult != null) { // 如果为null则拿到了验证码
                            logger.error("login for [" + email + "], can not get verify code, loginResult:" + loginResult);
                            return loginResult;
                        }
                        hideErr(); // 重试前隐藏错误信息
                        click(By.cssSelector("#login_button"));
                        continue;
                    } else if (err.contains("验证码不正确")) {
                        logger.warn("login for [" + email + "], verify code is err.");
                        if (verifyRetryCnt <= this.verifyRetryTimes) { // 如果小于重试次数
                            verifyRetryCnt += 1;
                            logger.warn("login for [" + email + "], verify code is err, retry " + verifyRetryCnt + " times to get verifycode.");
                            loginResult = setVerifyCode(dama(getNewVerifyCode()));
                            if (loginResult != null) {
                                logger.error("login for [" + email + "], verify code is err, failed in retry " + verifyRetryCnt + " times, loginResult:" + loginResult);
                                return loginResult;
                            }
                            hideErr(); // 重试前隐藏错误信息
                            click(By.cssSelector("#login_button"));
                            continue;
                        }
                        loginResult.setResCode(LoginResult.RES_FAILED);
                    } else if (err.contains("帐号或密码不正确")) {
                        logger.warn("login for [" + email + "], account or password is err.");
                        loginResult.setResCode(LoginResult.RES_ACCOUNT_ERR);
                    } else if (err.contains("您的帐号暂时无法登录")) {
                        logger.warn("login for [" + email + "], account is blocked.");
                        loginResult.setResCode(LoginResult.RES_ACCOUNT_BLOCKED);
                    } else if (err.contains("登录失败")
                            || err.contains("您已开启邮箱登录保护，请使用QQ安全中心扫描")) {
                        logger.warn("login for [" + email + "], login failed or account is protected.");
                        loginResult.setResCode(LoginResult.RES_FAILED);
                    } else if (err.contains("网络繁忙")) {
                        logger.warn("login for [" + email + "], network is busy.");
                        loginResult.setResCode(LoginResult.RES_SYS_BUSY);
                    } else if (err.contains("务必对手机收到的登录请求进行确认")) { // "您的帐号千金难求。为确保安全，请务必对手机收到的登录请求进行确认。"
                        logger.warn("login for [" + email + "], login need confirm on phone.");
                        loginResult.setResCode(LoginResult.RES_SAFETY_LIMITED);
                    } else if (err.contains("提交参数错误") || err.contains("输入正确的QQ帐号")) { // 账号不正确
                        logger.warn("login for [" + email + "], account is error.");
                        loginResult.setResCode(LoginResult.RES_ACCOUNT_ERR);
                    } else {
                        logger.warn("login for [" + email + "], failed unknown reason.");
                        loginResult.setResCode(LoginResult.RES_FAILED_UNKNOWN);
                        screenshotErr();
                    }
                    logger.warn("login for [" + email + "], failed message: " + err + ".");
                    loginResult.add(LoginResult.DATA_ERR_MSG, err);
                    return loginResult;
                } else if (webDriver.getCurrentUrl().contains("/cgi-bin/mibao_vry?")) {
                    logger.warn("login for [" + email + "], need sweep safe qrcode.");
                    loginResult.setResCode(LoginResult.RES_SWEEP_IMG);
                    loginResult.add(LoginResult.DATA_ERR_MSG,
                            "需要安全扫一扫,您已开启了网页登录保护，请验证密保.");
                    return loginResult;
                }
            } catch (Exception e) { // 如果发生异常, 说明在取页面元素时页面已经跳转了.
                logger.error("in doLogin for [" + email + "]: " + e.getMessage(), e);
            } finally {
                sleep(100);
            }
        }
        return null;
    }

    /**
     * 避免出现类似  Element is not clickable at point (450, 255). Other element would receive the click 这样的异常
     * @param by
     */
    public void click(By by) {
        while (true) {
            try {
                webDriverLocal.get().findElement(by).click();
                break;
            } catch (WebDriverException e) {
                if (e.getMessage().contains("Element is not clickable")) {
                    continue;
                }
                throw e;
            }
        }
    }

    /**
     * 对错误页面进行截图
     */
    public String screenshotErr() {
        ChromeDriver webDriver = webDriverLocal.get();
        if (webDriver == null) {
            return null;
        }
        File screenshot = webDriver.getScreenshotAs(new OutputType<File>() {
            public File convertFromBase64Png(String base64Png) {
                return save(BYTES.convertFromBase64Png(base64Png));
            }

            public File convertFromPngBytes(byte[] data) {
                return save(data);
            }

            private File save(byte[] data) {
                OutputStream stream = null;

                try {
                    String pathname = errScreenshotDir + File.separator + cacheKeyLocal.get() + ".png";
                    logger.warn("take a screeshoterr file : " + pathname);
                    File screenshot = new File(pathname);
                    if (!screenshot.getParentFile().exists()) {
                        screenshot.getParentFile().mkdirs();
                    }
                    stream = new FileOutputStream(screenshot);
                    stream.write(data);

                    return screenshot;
                } catch (IOException e) {
                    throw new WebDriverException(e);
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            // Nothing sane to do
                        }
                    }
                }
            }
        });
        if (screenshot != null) {
            return screenshot.getAbsolutePath();
        }
        return null;
    }

    private LoginResult doAfterLogin(String email, String indepentPassword) {

        ChromeDriver webDriver = webDriverLocal.get();
        if (webDriver.getPageSource().contains("验证独立密码")) {
            String cacheKey = cacheKeyLocal.get();
            if (StringUtils.isNotBlank(indepentPassword)) {
                WebElement ppEl = webDriver.findElement(By.cssSelector("#pp")); // 独立密码的input
                click(By.cssSelector("#pp"));
                ppEl.clear();
                sendKeys(ppEl, indepentPassword);

                WebElement vfcode = webDriver.findElement(By.cssSelector("#vfcode"));
                if (vfcode != null) {
                    String vfcodeUrl = vfcode.getAttribute("src");
                    if (StringUtils.isNotBlank(vfcodeUrl)) {
                        LoginResult loginResult = setVerifyCode(dama(getNewIndepentVerifyCode()));
                        if (loginResult != null) {
                            return loginResult;
                        }
                    }
                }

                click(By.cssSelector("#btlogin"));
                sleep(500);
                int indepentErrRetryCnt = 0;
                int verifyRetryCnt = 0;
                while (!webDriver.getCurrentUrl().contains(SID_NAME)) {
                    String err = webDriver.findElement(By.cssSelector("#msgContainer")).getText();
                    if (err.contains("您输入的独立密码有误，请重新输入")) {
                        if (indepentErrRetryCnt <= this.indepentErrRetryTimes) {
                            indepentErrRetryCnt += 1;
                            logger.warn("login for [" + email + "], indepent password is error, retry " + indepentErrRetryCnt + " times to get right indepent password.");

                            cacher.delCacheValue(cacheKey, Cacher.INDEPENT_PWD_KEY);
                            cacheLoginResult(cacheKey, new LoginResult(LoginResult.RES_INDEPENT_ERR));

                            String pwd = null;
                            long start = System.currentTimeMillis();
                            while (pwd == null) {
                                sleep(100);
                                pwd = cacher.getCacheValue(cacheKey, Cacher.INDEPENT_PWD_KEY);
                                if (pwd == null && System.currentTimeMillis() - start > timeout) {
                                    logger.warn("login for [" + email + "], indepent password is error, failed in retry " + indepentErrRetryCnt + " times, timeout.");
                                    return new LoginResult(LoginResult.RES_TIME_OUT);
                                }
                            }
                            ppEl = webDriver.findElement(By.cssSelector("#pp")); // 独立密码的input
                            click(By.cssSelector("#pp"));
                            ppEl.clear();
                            sendKeys(ppEl, pwd);
                            vfcode = webDriver.findElement(By.cssSelector("#vfcode"));
                            if (vfcode != null) {
                                String vfcodeUrl = vfcode.getAttribute("src");
                                if (StringUtils.isNotBlank(vfcodeUrl)) {
                                    LoginResult loginResult = setVerifyCode(dama(getNewIndepentVerifyCode()));
                                    if (loginResult != null) {
                                        return loginResult;
                                    }
                                }
                            }
                            webDriver.executeScript("document.getElementById('msgContainer').innerHTML='';"); // 去掉错误信息
                            click(By.cssSelector("#btlogin"));
                            sleep(100);
                            continue;
                        }
                        // =====
                        logger.info("[ Y ] login for [" + email + "] succ, by indepent problem");
                        // =====
                        logger.warn("login for [" + email + "], indepent password is error, failed in retry " + indepentErrRetryCnt + " times, is the max retry times.");
                        return new LoginResult(LoginResult.RES_FAILED);
                    } else if (err.contains("验证码不正确")) {
                        // 重试
                        webDriver.executeScript("document.getElementById('msgContainer').innerHTML='';"); // 去掉错误信息

                        if (verifyRetryCnt <= this.verifyRetryTimes) { // 如果小于重试次数
                            verifyRetryCnt += 1;
                            logger.warn("login for [" + email + "], indepent verify code is error, retry " + indepentErrRetryCnt + " times.");
                            LoginResult loginResult = setVerifyCode(dama(getNewIndepentVerifyCode()));
                            if (loginResult != null) {
                                logger.warn("login for [" + email + "], indepent verify code is error, failed in retry " + indepentErrRetryCnt + " times, loginResult: " + loginResult);
                                return loginResult;
                            }
                            click(By.cssSelector("#btlogin"));
                            sleep(100);
                            continue;
                        }
                        // =====
                        logger.info("[ Y ] login for [" + email + "] succ, by indepent problem");
                        // =====
                        return new LoginResult(LoginResult.RES_FAILED);

                    } else if (err.contains("请输入验证码")) { // 重试
                        return doAfterLogin(email, indepentPassword);
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) {
                    }
                }
                return null;
            } else {
                cacher.delCacheValue(cacheKey, Cacher.INDEPENT_PWD_KEY);

                cacheLoginResult(cacheKey, new LoginResult(LoginResult.RES_NEED_INDEPENT));
                String pwd = null;
                logger.warn("login for [" + email + "], indepent password is null, wait for user send it.");
                long start = System.currentTimeMillis();
                while (pwd == null) {
                    sleep(100);
                    pwd = cacher.getCacheValue(cacheKey, Cacher.INDEPENT_PWD_KEY);
                    if (pwd == null && System.currentTimeMillis() - start > timeout) {
                        // =====
                        logger.info("[ Y ] login for [" + email + "] succ, by indepent problem");
                        // =====
                        logger.warn("login for [" + email + "], indepent password is null, wait for user send it but timeout.");
                        return new LoginResult(LoginResult.RES_TIME_OUT);
                    }
                }
                return doAfterLogin(email, pwd); // 拿到独立密码了, 重试
            }
        } else if (webDriver.getCurrentUrl().contains("mail.qq.com/cgi-bin/loginpage")) { // 回到了登录页面, 重试
            return new LoginResult(LoginResult.RES_NEED_RETRY);
        } else { // 未知错误
            LoginResult loginResult = new LoginResult(LoginResult.RES_FAILED_UNKNOWN);
            loginResult.add(LoginResult.DATA_URL, webDriver.getCurrentUrl());
            logger.warn("login for [" + email + "], failed on unknown reason, url is " + webDriver.getCurrentUrl());
            return loginResult;
        }

    }

    private void sendKeys(By by, String value) {
        sendKeys(webDriverLocal.get().findElement(by), value);
    }

    private void sendKeys(WebElement el, String value) {
        if (StringUtils.isNotBlank(value)) {
            char[] vs = value.toCharArray();
            for (char v : vs) {
                el.sendKeys(v + "");
            }
        }
    }


    private String getSuccCookies() {

        ChromeDriver webDriver = webDriverLocal.get();
        Set<Cookie> cookies = webDriver.manage().getCookies();
        List<Map<String, String>> succCookies = new ArrayList<Map<String,String>>();
        Map<String, List<String>> cookiesPerDomain = new HashMap<String, List<String>>();
        for (Cookie cookie : cookies) {
            String domain = cookie.getDomain();
            if (domain.startsWith(".")) {
                domain = domain.substring(1);
            }
            if (!cookiesPerDomain.containsKey(domain)) {
                cookiesPerDomain.put(domain, new ArrayList<String>());
            }
            List<String> domainCookies = cookiesPerDomain.get(domain);
            domainCookies.add(cookie.getName() + "=" + cookie.getValue());
            cookiesPerDomain.put(domain, domainCookies);
        }
        Set<String> domains = cookiesPerDomain.keySet();
        for (String domain : domains) {
            Map<String, String> domainCookies = new HashMap<String, String>();
            domainCookies.put("domain", domain);
            domainCookies.put("value", StringUtils.join(cookiesPerDomain.get(domain), "; "));
            succCookies.add(domainCookies);
        }
        return JSONArray.toJSONString(succCookies);
    }

    private LoginResult setVerifyCode(VerifyCode verifyCode) {
        LoginResult loginResult = new LoginResult();
        if (verifyCode != null && verifyCode.getCode() != null) {
            click(By.cssSelector("#verifycode"));
            sendKeys(By.cssSelector("#verifycode"), verifyCode.getCode());
            return null;
        } else { // 云打码失败
            if (verifyCode != null) {
                String cacheKey = cacheKeyLocal.get();

                loginResult.setResCode(LoginResult.RES_NEED_VERIFY_CODE);
                loginResult.add(LoginResult.DATA_VERIFY_CODE, new Base64Encoder().encode(verifyCode.getBytes()));
                cacher.delCacheValue(cacheKey, Cacher.VERIFY_CODE_KEY);

                cacheLoginResult(cacheKey, loginResult);
                loginResult.remove(LoginResult.DATA_VERIFY_CODE);

                String code = verifyCode.getCode();
                long start = System.currentTimeMillis();
                while (code == null) {
                    sleep(100);
                    code = cacher.getCacheValue(cacheKey, Cacher.VERIFY_CODE_KEY);
                    if (code == null && System.currentTimeMillis() - start > timeout) { // 超时没拿到数据
                        loginResult.setResCode(LoginResult.RES_TIME_OUT);
                        return loginResult;
                    }
                }
                // 拿到验证码
                click(By.cssSelector("#verifycode"));
                sendKeys(By.cssSelector("#verifycode"), verifyCode.getCode());
                return null;
            } else { // 验证码图片下载出错了
                loginResult.setResCode(LoginResult.RES_FAILED);
                return loginResult;
            }
        }
    }

    /**
     * 缓存登录结果
     */
    public void cacheLoginResult(String cacheKey, LoginResult loginResult) {
        cacher.cacheLoginResult(cacheKey, loginResult);
    }

    private void sleep(int mills) {
        try {
            TimeUnit.MILLISECONDS.sleep(mills);
        } catch (InterruptedException e) {
        }
    }

    private Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36");
//        headers.put("Accept", "image/webp,*/*;q=0.8");
//        headers.put("Accept-Encoding", "gzip, deflate, sdch");
//        headers.put("Accept-Language", "zh,zh-CN;q=0.8,en;q=0.6,zh-TW;q=0.4");
//        headers.put("Connection", "keep-alive");
//        headers.put("DNT", "1");
//        headers.put("Host", "ssl.captcha.qq.com");
        headers.put("Referer", webDriverLocal.get().getCurrentUrl());
        return headers;
    }

    private VerifyCode dama(VerifyCode verifyCode) {
        if (verifyCode == null) {
            return null;
        }
        String filePath = VERIFY_CODE_FILE_PREFIX
                + DigestUtils.md5Hex(verifyCode.getBytes()) + ".jpg";
        File codeFile = new File(filePath);
        try {
            FileOutputStream output = new FileOutputStream(codeFile);
            IOUtils.write(verifyCode.getBytes(), output);
            IOUtils.closeQuietly(output);
        } catch (Exception e) {
            return verifyCode;
        }

        String code = HttpUtils.dama(filePath, 0, this.yundamaRetryMaxTimes);
        verifyCode.setCode(code);
        try {
            return verifyCode;
        } finally {
            try {
                codeFile.delete();
            } catch (Exception e) {
            }
        }

    }

    private VerifyCode getNewIndepentVerifyCode() {
        ChromeDriver webDriver = webDriverLocal.get();
        click(By.cssSelector("#vfcode"));
        WebElement vfcode = webDriver.findElement(By.cssSelector("#vfcode"));
        String vfcodeUrl = vfcode.getAttribute("src");
        if (!vfcodeUrl.startsWith("https://mail.qq.com")) {
            vfcodeUrl = "https://mail.qq.com" + vfcodeUrl;
        }
        Map<String, String> headers = getHeaders();
        VerifyCode verifyCode = HttpUtils.getQQVerifyCode(vfcodeUrl, headers,
                webDriver.manage().getCookies(),
                webDriver.executeScript("return document.cookie;").toString());
        if (verifyCode == null) {
            for (int i = 0; i < this.verifyCodeRetryMaxTimes; i++) {
                vfcodeUrl = "https://mail.qq.com" + vfcode.getAttribute("src");
                verifyCode = HttpUtils.getQQVerifyCode(vfcodeUrl, headers,
                        webDriver.manage().getCookies(), webDriver
                                .executeScript("return document.cookie;")
                                .toString());
                if (verifyCode != null) {
                    break;
                }
            }
        }
        if (verifyCode == null) {
            return null;
        }
        List<Cookie> cookies = verifyCode.getCookies();
        if (cookies != null && !cookies.isEmpty()) {
            for (Cookie cookie : cookies) {
                webDriver.manage().deleteCookieNamed(cookie.getName());
                webDriver.manage().addCookie(
                        new Cookie(cookie.getName(), cookie.getValue(),
                                "mail.qq.com", "/", null, false));
            }
        }
        return verifyCode;
    }

    private VerifyCode getNewVerifyCode() {
        ChromeDriver webDriver = webDriverLocal.get();
        WebElement verifyimgElement = webDriver.findElement(By
                .cssSelector("#verifyimg"));
        String verifyimgUrl = verifyimgElement.getAttribute("src");
        Map<String, String> headers = getHeaders();
        VerifyCode verifyCode = HttpUtils.getQQVerifyCode(verifyimgUrl,
                headers, webDriver.manage().getCookies(), webDriver
                        .executeScript("return document.cookie;").toString());
        if (verifyCode == null) {
            for (int i = 0; i < this.verifyCodeRetryMaxTimes; i++) {
                verifyimgUrl = verifyimgElement.getAttribute("src");
                verifyCode = HttpUtils.getQQVerifyCode(verifyimgUrl, headers,
                        webDriver.manage().getCookies(), webDriver
                                .executeScript("return document.cookie;")
                                .toString());
                if (verifyCode != null) {
                    break;
                }
            }
        }
        if (verifyCode == null) {
            return null;
        }
        List<Cookie> cookies = verifyCode.getCookies();
        if (cookies != null && !cookies.isEmpty()) {
            for (Cookie cookie : cookies) {
                webDriver.manage().deleteCookieNamed(cookie.getName());
                webDriver.manage().addCookie(
                        new Cookie(cookie.getName(), cookie.getValue(),
                                "qq.com", "/", null, false));
            }
        }

        return verifyCode;
    }

    public void release() {
        cacheKeyLocal.remove();
        webDriverLocal.remove();
    }

    public void cacheFailedLoginResult(String cacheKey) {
        try {
            LoginResult failedLoginResult = new LoginResult();
            failedLoginResult.setResCode(LoginResult.RES_FAILED);
            cacheLoginResult(cacheKey, failedLoginResult);
        } catch (Exception e) {
            logger.error("cache failed login result error.", e);
        }
    }

    public Cacher getCacher() {
        return this.cacher;
    }

}

package me.binge.selenium.qq.test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;

import me.binge.selenium.qq.cache.Cacher;
import me.binge.selenium.qq.common.LoginResult;

public class QQLoginClient {

    private Cacher cacher;

    public QQLoginClient(Cacher cacher) {
        this.cacher = cacher;
    }

    public void login(String email, String password, String indepentPassword) {

        String cacheKey = "__login_" + DigestUtils.md5(email + password + indepentPassword);
        Map<String, String> loginInfo = new HashMap<String, String>();
        loginInfo.put(Cacher.EMAIL_KEY, email);
        loginInfo.put(Cacher.PWD_KEY, password);
        loginInfo.put(Cacher.INDEPENT_PWD_KEY, indepentPassword);
        cacher.addCacheValue(cacheKey, Cacher.LOGIN_INFO_KEY, loginInfo);
        this.cacher.addCacheKey("__qq_login_queue_3464a91a1301", cacheKey);

        handleRes(cacheKey);
    }

    private void handleRes(String cacheKey) {
        LoginResult loginResult = null;
        while ((loginResult = cacher.getCacheValue(cacheKey, Cacher.LOGIN_RES_KEY)) == null) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
            }
            continue;
        }
        int resCode = loginResult.getResCode();
        switch (resCode) {
        case 0: // 成功
        case 3: // 账号密码错误
        case 4: // 账号被冻结
        case 5: // 登录失败
        case 8: // 未知错误
        case 10: // 网络繁忙
        case 11: // 需要手机确认登录
        case 12: // 需要安全扫一扫
        case 13: // 登录超时
            System.out.println(loginResult);
        return;
        case 1: // 需要验证码或验证码错误
            cacher.delCacheValue(cacheKey, Cacher.LOGIN_RES_KEY);
            setVerifyCode(cacheKey, loginResult);
            handleRes(cacheKey);
            return;
        case 7: // 独立密码错误
        case 9: // 需要独立密码
            cacher.delCacheValue(cacheKey, Cacher.LOGIN_RES_KEY);
            setIndepentPwd(cacheKey, loginResult);
            handleRes(cacheKey);
            return;
        default:
            break;
        }
    }

    private void setIndepentPwd(String cacheKey, LoginResult loginResult) {
        String indepentPwd = "oooooooo"; // 用户输入的新的独立密码
        cacher.addCacheValue(cacheKey, Cacher.INDEPENT_PWD_KEY, indepentPwd);
    }

    private void setVerifyCode(String cacheKey, LoginResult loginResult) {

        String verifyCodeBS64 = loginResult.get(LoginResult.DATA_VERIFY_CODE).toString();
        // TODO: 将base64字符串转存为文件, 生成链接给用户输入验证码

        String verifyCode = "xxxx"; // 用户输入的验证码
        cacher.addCacheValue(cacheKey, Cacher.VERIFY_CODE_KEY, verifyCode);
    }





}

package me.binge.selenium.qq.common;

import java.util.HashMap;
import java.util.Map;

public class LoginResult {

    public static final int RES_LOGIN_SUCC = 0;
    public static final String DATA_COOKIES = "cookies";
    public static final String DATA_SID = "sid";
    public static final String DATA_R = "r";
    public static final String RES_DATA_KEY = "data";
    public static final String RES_CODE_KEY = "res_code";

    public static final int RES_NEED_VERIFY_CODE = 1;
    public static final String DATA_VERIFY_CODE = "verify_code";

    public static final int RES_VERIFY_CODE_ERR = 2;

    public static final int RES_ACCOUNT_ERR = 3;

    public static final int RES_ACCOUNT_BLOCKED = 4;

    public static final int RES_FAILED = 5;

    public static final int RES_NEED_RETRY = 6;

    public static final int RES_INDEPENT_ERR = 7;

    public static final int RES_FAILED_UNKNOWN = 8;

    public static final int RES_NEED_INDEPENT = 9;

    public static final String DATA_ERR_MSG = "err_msg";

    public static final int RES_SYS_BUSY = 10;

    public static final int RES_SAFETY_LIMITED = 11; // 出现错误:
                                                        // 您的帐号千金难求。为确保安全，请务必对手机收到的登录请求进行确认。"773900100@qq.com",
                                                        // "FFshinima1994..."

    public static final int RES_SWEEP_IMG = 12; // 安全扫一扫

    public static final int RES_TIME_OUT = 13;

    public static final String DATA_URL = "url";

    private int resCode;
    private Map<String, Object> data = new HashMap<String, Object>();

    public LoginResult() {
    }

    public LoginResult(int resCode) {
        this.resCode = resCode;
    }

    public void add(String key, Object value) {
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public Map<String, Object> getData() {
        return data;
    }

    public int getResCode() {
        return resCode;
    }

    public void setResCode(int resCode) {
        this.resCode = resCode;
    }

    @Override
    public String toString() {
        return "LoginResult [resCode=" + resCode + ", data=" + data + "]";
    }

    public void remove(String key) {
        data.remove(key);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> res = new HashMap<String, Object>();
        res.put(RES_CODE_KEY, resCode);
        res.put(RES_DATA_KEY, data);
        return res;
    }

}

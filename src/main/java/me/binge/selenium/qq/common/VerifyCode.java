package me.binge.selenium.qq.common;

import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.Cookie;


public class VerifyCode {

    protected byte[] bytes;
    protected String code;
    protected List<Cookie> cookies;

    public VerifyCode() {
    }

    public byte[] getBytes() {
        return bytes;
    }


    public String getCode() {
        return code;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public void setCookies(List<Cookie> cookies) {
        this.cookies = cookies;
    }

    @Override
    public String toString() {
        return "VerifyCode [bytes=" + Arrays.toString(bytes) + ", code=" + code
                + ", cookies=" + cookies + "]";
    }


}

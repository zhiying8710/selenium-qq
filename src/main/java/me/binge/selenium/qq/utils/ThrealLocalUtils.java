package me.binge.selenium.qq.utils;

import org.openqa.selenium.chrome.ChromeDriver;

public class ThrealLocalUtils {

    public static final ThreadLocal<ChromeDriver> WEBDRIVER_LOCAL = new ThreadLocal<ChromeDriver>();

    public static final ThreadLocal<String> CAHCEKKEY_LOCAL = new ThreadLocal<String>();

    public static final ThreadLocal<String> PROXY_LOCAL = new ThreadLocal<String>();

}

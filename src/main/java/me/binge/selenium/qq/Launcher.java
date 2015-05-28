package me.binge.selenium.qq;

import java.lang.management.ManagementFactory;
import java.net.URL;

import me.binge.selenium.qq.concurrent.QQLoginService;
import me.binge.selenium.qq.login.QQLogin;

import org.apache.log4j.Logger;

public class Launcher {

    private static final Logger logger = Logger.getLogger(Launcher.class);


    static {
        URL driverUrl = QQLogin.class.getClassLoader().getResource("chromedriver.exe");
        logger.info("find chromedriver.exe : " + driverUrl);
        if (driverUrl == null) {
            throw new NullPointerException("chromedriver.exe can not be found.");
        }
        System.getProperties().setProperty("webdriver.chrome.driver", driverUrl.getPath().substring(1));
    }


    public static void main(String[] args) throws Exception {

        logger.info("sys working..., PID: " + ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
        QQLoginService.boot();

    }

}

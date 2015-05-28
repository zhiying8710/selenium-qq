package me.binge.selenium.qq.concurrent;

import java.util.concurrent.Callable;

import org.openqa.selenium.chrome.ChromeDriver;

public class QQLoginCallback implements Callable<Void> {

    private ChromeDriver webDriver;

    @Override
    public Void call() throws Exception {
        if (this.webDriver != null) {
            try {
                this.webDriver.quit();
            } catch (Exception e) {
            }
            this.webDriver = null;
        }
        return null;
    }

    public void setWebDriver(ChromeDriver webDriver) {
        this.webDriver = webDriver;
    }

}

package me.binge.selenium.qq.exception;

public class QQLoginException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public QQLoginException() {
        super();
    }

    public QQLoginException(String message, Throwable cause) {
        super(message, cause);
    }

    public QQLoginException(String message) {
        super(message);
    }

    public QQLoginException(Throwable cause) {
        super(cause);
    }

}

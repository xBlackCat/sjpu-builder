package org.xblackcat.sjpu.builder;

/**
 * 16.10.13 9:49
 *
 * @author xBlackCat
 */
public class GeneratorException extends RuntimeException {
    public GeneratorException() {
        super();
    }

    public GeneratorException(String message) {
        super(message);
    }

    public GeneratorException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeneratorException(Throwable cause) {
        super(cause);
    }

    protected GeneratorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

package com.mikerusoft.redirect.to.stream.utils;

public class Utils {

    private Utils() {}

    public static <T> T rethrowRuntime(Throwable t) {
        if (t instanceof Error)
            throw (Error)t;
        else if (t instanceof RuntimeException)
            throw (RuntimeException)t;

        throw new RuntimeException(t);
    }
}

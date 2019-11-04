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

    public static RuntimeException generateRuntime(Throwable original) {
        if (original instanceof Error)
            throw (Error)original;
        else if (original instanceof RuntimeException)
            return  (RuntimeException)original;

        return new RuntimeException(original);
    }

    public static boolean isEmptyString(String str) {
        return str == null || str.isEmpty();
    }
}

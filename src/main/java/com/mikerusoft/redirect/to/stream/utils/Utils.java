package com.mikerusoft.redirect.to.stream.utils;

import java.util.Map;

public class Utils {

    private Utils() {}

    public static <T> T rethrowRuntime(Throwable t) {
        if (t instanceof Error)
            throw (Error)t;
        else if (t instanceof RuntimeException)
            throw (RuntimeException)t;

        throw new RuntimeException(t);
    }

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static <K, V> boolean isEmpty(Map<K, V> map) {
        return map == null || map.isEmpty();
    }
}

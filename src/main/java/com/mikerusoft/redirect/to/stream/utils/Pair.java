package com.mikerusoft.redirect.to.stream.utils;

import lombok.Data;
import lombok.Value;

@Data
@Value
public class Pair<L, R> {
    private L left;
    private R right;

    public static <L, R> Pair<L, R> of(L left, R right) {
        return new Pair<>(left, right);
    }

    private Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public L getKey() {
        return getLeft();
    }

    public R getValue() {
        return getRight();
    }
}

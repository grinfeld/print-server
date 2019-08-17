package com.mikerusoft.redirect.to.stream.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Iterator;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Filter implements Iterator<Filter> {
    private FilterType type;
    private Object value;
    private FilterOp nextOp;
    private Filter next;

    @Override
    public boolean hasNext() {
        return next == null;
    }

    @Override
    public Filter next() {
        return next;
    }
}

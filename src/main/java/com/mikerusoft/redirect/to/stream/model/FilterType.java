package com.mikerusoft.redirect.to.stream.model;

import com.mikerusoft.redirect.to.stream.utils.Utils;

import java.util.Collection;

public enum FilterType {
    regex {
        public boolean filter(Object value, Object filterValue) {
            return false;
        }
    },
    isnull {
        public boolean filter(Object value, Object filterValue) {
            return value == null;
        }
    },
    isempty {
        public boolean filter(Object value, Object filterValue) {
            if (value == null) return true;
            if (value instanceof String)
                return ((String) value).isEmpty();
            if (value.getClass().isArray()) {
                try {
                    return (int) value.getClass().getMethod("length").invoke(value) <= 0;
                } catch (Exception e) {
                    return Utils.rethrowRuntime(e);
                }
            }
            if (value instanceof Collection)
                return ((Collection) value).isEmpty();

            return false;
        }
    },
    equals {
        public boolean filter(Object value, Object filterValue) {
            return value != null && value.equals(filterValue);
        }
    },
    notequals {
        public boolean filter(Object value, Object filterValue) {
            return !equals.filter(value, filterValue);
        }
    },
    gt {
        public boolean filter(Object value, Object filterValue) {
            if (value == null) return false;
            if (value instanceof String && filterValue instanceof String)
                return ((String) value).compareTo((String)filterValue) > 0;
            if (value instanceof Number && filterValue instanceof Number) {
                return ((Number) value).doubleValue() > ((Number) filterValue).doubleValue();
            }
            return false;
        }
    },
    lt {
        public boolean filter(Object value, Object filterValue) {
            if (value == null) return false;
            if (value instanceof String && filterValue instanceof String)
                return ((String) value).compareTo((String)filterValue) < 0;
            if (value instanceof Number && filterValue instanceof Number) {
                return ((Number) value).doubleValue() < ((Number) filterValue).doubleValue();
            }
            return false;
        }
    };

    public abstract boolean filter(Object value, Object filterValue);
}

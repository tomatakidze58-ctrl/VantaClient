package dev.vanta.client.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class Reflect {
    private Reflect() {}

    public static Object field(Object target, String... names) {
        if (target == null) return null;
        Class<?> c = target.getClass();
        for (String name : names) {
            try {
                Field f = findField(c, name);
                if (f != null) {
                    f.setAccessible(true);
                    return f.get(target);
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static Field findField(Class<?> c, String name) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try { return k.getDeclaredField(name); } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    public static Object call(Object target, String name, Object... args) {
        if (target == null) return null;
        for (Method m : target.getClass().getMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
            try {
                m.setAccessible(true);
                return m.invoke(target, args);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    public static int intCall(Object target, String name, int fallback, Object... args) {
        Object v = call(target, name, args);
        return v instanceof Number n ? n.intValue() : fallback;
    }

    public static boolean boolCall(Object target, String name, boolean fallback, Object... args) {
        Object v = call(target, name, args);
        return v instanceof Boolean b ? b : fallback;
    }

    public static boolean keyDown(Object options, String... fieldNames) {
        Object key = field(options, fieldNames);
        Object v = call(key, "isDown");
        return v instanceof Boolean b && b;
    }

    public static void setOption(Object options, Object value, String... accessors) {
        if (options == null) return;
        for (String accessor : accessors) {
            Object option = call(options, accessor);
            if (option == null) option = field(options, accessor);
            if (option == null) continue;
            Object r = call(option, "set", value);
            if (r != null || call(option, "get") != null) return;
        }
    }

    public static Object getOption(Object options, String... accessors) {
        if (options == null) return null;
        for (String accessor : accessors) {
            Object option = call(options, accessor);
            if (option == null) option = field(options, accessor);
            if (option == null) continue;
            Object v = call(option, "get");
            if (v != null) return v;
        }
        return null;
    }
}

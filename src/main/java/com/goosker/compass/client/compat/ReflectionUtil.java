package com.goosker.compass.client.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class ReflectionUtil {
    private ReflectionUtil() {}

    static Object invoke(Object target, String name, Object... args) throws ReflectiveOperationException {
        Class<?> type = target instanceof Class<?> c ? c : target.getClass();
        Method method = findMethod(type, name, args.length);
        method.setAccessible(true);
        return method.invoke(target instanceof Class<?> ? null : target, args);
    }

    static Method findMethod(Class<?> type, String name, int argCount) throws NoSuchMethodException {
        Class<?> cursor = type;
        while (cursor != null) {
            for (Method method : cursor.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == argCount) {
                    return method;
                }
            }
            cursor = cursor.getSuperclass();
        }
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == argCount) {
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + "#" + name + "/" + argCount);
    }

    static Object staticField(String className, String fieldName) throws ReflectiveOperationException {
        Class<?> type = Class.forName(className);
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(null);
    }

    static boolean hasMethod(Object target, String name, int argCount) {
        try {
            findMethod(target.getClass(), name, argCount);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}

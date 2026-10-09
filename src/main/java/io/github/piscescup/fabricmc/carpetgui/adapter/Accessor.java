/*
 * This file is part of the Carpet GUI project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  Fallen_Breath and contributors
 *
 * Carpet GUI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Carpet GUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Carpet GUI.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.piscescup.fabricmc.carpetgui.adapter;

import carpet.CarpetExtension;
import carpet.CarpetServer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;

/**
 * Reads configured members without constructing addon instances.
 * Instance accessors accept an existing object; static accessors accept its declaring class.
 * Private and inherited members are supported.
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public interface Accessor<T> {
    Object get(Object target);

    boolean isStaticAccess();

    /**
     * Adds an explicit runtime check for the returned value (null remains null).
     */
    default Accessor<T> checked(Class<T> resultType) {
        Objects.requireNonNull(resultType, "resultType");
        return new CheckedAccess<>(this, resultType);
    }

    static RegisteredExtensionAccess ofRegisteredExtension() {
        return new RegisteredExtensionAccess();
    }

    static <T> Accessor<T> ofInstanceField(String name) {
        return fieldAccessor(name, false);
    }

    static <T> Accessor<T> ofStaticField(String name) {
        return fieldAccessor(name, true);
    }

    static <T> Accessor<T> ofInstanceMethod(String name) {
        return ofInstanceMethod(name, new Class<?>[0], new Object[0]);
    }

    static <T> Accessor<T> ofInstanceMethod(String name, Class<?>[] parameterTypes, Object[] args) {
        return methodAccessor(name, parameterTypes, args, false);
    }

    static <T> Accessor<T> ofStaticMethod(String name) {
        return ofStaticMethod(name, new Class<?>[0], new Object[0]);
    }

    static <T> Accessor<T> ofStaticMethod(String name, Class<?>[] parameterTypes, Object[] args) {
        return methodAccessor(name, parameterTypes, args, true);
    }

    private static <T> Accessor<T> fieldAccessor(String name, boolean staticAccess) {
        return new FieldAccess<>(name, staticAccess);
    }

    private static <T> Accessor<T> methodAccessor(
        String name, Class<?>[] parameterTypes, Object[] args, boolean staticAccess
    ) {
        return new MethodAccess<>(name, staticAccess, parameterTypes, args);
    }

    record CheckedAccess<T>(Accessor<T> source, Class<T> resultType) implements Accessor<T> {
        public CheckedAccess {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(resultType, "resultType");
        }

        @Override
        public Object get(Object target) {
            return resultType.cast(source.get(target));
        }

        @Override
        public boolean isStaticAccess() {
            return source.isStaticAccess();
        }
    }

    /**
     * Named implementations retain configuration for JSON serialization.
     */
    record FieldAccess<T>(String name, boolean isStaticAccess) implements Accessor<T> {
        public FieldAccess {
            requireName(name);
        }

        @Override
        public Object get(Object target) {
            Class<?> owner = owner(target, isStaticAccess);
            try {
                Field field = findField(owner, name);
                requireStaticMode(field.getModifiers(), isStaticAccess);
                field.setAccessible(true);
                return field.get(isStaticAccess ? null : target);
            } catch (ReflectiveOperationException | RuntimeException failure) {
                throw accessFailure(owner, name, failure);
            }
        }

        private static Field findField(Class<?> owner, String name) throws NoSuchFieldException {
            for (Class<?> type = owner; type != null; type = type.getSuperclass()) {
                try {
                    return type.getDeclaredField(name);
                } catch (NoSuchFieldException ignored) {
                    // Continue with the superclass.
                }
            }
            return owner.getField(name); // Includes public interface fields.
        }
    }

    record MethodAccess<T>(String name, boolean isStaticAccess, Class<?>[] parameterTypes, Object[] args)
        implements Accessor<T>
    {
        public MethodAccess {
            requireName(name);
            parameterTypes = parameterTypes == null ? new Class<?>[0] : parameterTypes.clone();
            args = args == null ? new Object[0] : args.clone();
            if (parameterTypes.length != args.length) {
                throw new IllegalArgumentException("Method " + name + " expects " + parameterTypes.length
                    + " arguments, got " + args.length);
            }
            for (Class<?> type : parameterTypes) Objects.requireNonNull(type, "parameter type");
        }

        @Override
        public Class<?>[] parameterTypes() {
            return parameterTypes.clone();
        }

        @Override
        public Object[] args() {
            return args.clone();
        }

        @Override
        public Object get(Object target) {
            Class<?> owner = owner(target, isStaticAccess);
            try {
                Method method = findMethod(owner, name, parameterTypes);
                requireStaticMode(method.getModifiers(), isStaticAccess);
                method.setAccessible(true);
                return method.invoke(isStaticAccess ? null : target, args);
            } catch (ReflectiveOperationException | RuntimeException failure) {
                throw accessFailure(owner, name, failure);
            }
        }

        private static Method findMethod(Class<?> owner, String name, Class<?>[] types) throws NoSuchMethodException {
            for (Class<?> type = owner; type != null; type = type.getSuperclass()) {
                try {
                    return type.getDeclaredMethod(name, types);
                } catch (NoSuchMethodException ignored) {
                    // Continue with the superclass.
                }
            }
            return owner.getMethod(name, types); // Includes public interface methods.
        }
    }

    record RegisteredExtensionAccess() implements Accessor<CarpetExtension> {
        @Override
        public CarpetExtension get(Object target) {
            if (!(target instanceof Class<?> type) || !CarpetExtension.class.isAssignableFrom(type)) {
                throw new IllegalArgumentException("Registered extension access requires a CarpetExtension class");
            }
            return List.copyOf(CarpetServer.extensions).stream().filter(type::isInstance).findFirst().orElse(null);
        }

        /** True means that this accessor requires a Class target, not an instance target. */
        @Override
        public boolean isStaticAccess() {
            return true;
        }
    }


    private static Class<?> owner(Object target, boolean staticAccess) {
        Objects.requireNonNull(target, "target");
        if (staticAccess) {
            if (target instanceof Class<?> type) return type;
            throw new IllegalArgumentException("Static access requires a Class<?> target");
        }
        if (target instanceof Class<?>) {
            throw new IllegalArgumentException("Instance access requires an existing object, not a Class<?>");
        }
        return target.getClass();
    }

    private static void requireStaticMode(int modifiers, boolean staticAccess) {
        if (Modifier.isStatic(modifiers) != staticAccess) {
            throw new IllegalArgumentException("Expected " + (staticAccess ? "static" : "instance") + " member");
        }
    }

    private static void requireName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Member name must not be blank");
    }

    private static IllegalStateException accessFailure(Class<?> owner, String name, Exception failure) {
        Throwable cause = failure instanceof InvocationTargetException invocation
            ? invocation.getTargetException() : failure;
        return new IllegalStateException("Cannot access " + owner.getName() + "#" + name, cause);
    }
}

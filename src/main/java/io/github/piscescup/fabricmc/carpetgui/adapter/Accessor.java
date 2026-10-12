/*
 * This file is part of the Carpet GUI project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  PiscesCup and contributors
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
 * Describes a deferred reflective read of a field, method, or registered Carpet extension.
 * Instance accessors receive an existing object, while static accessors receive the declaring
 * {@link Class}. Private and inherited members are supported, and addon instances are never
 * constructed by field or method accessors.
 *
 * <pre>{@code
 * Accessor<String> name = Accessor.<String>ofInstanceMethod("name").checked(String.class);
 * String value = (String) name.get(ruleObject);
 *
 * Accessor<?> settings = Accessor.ofStaticField("SETTINGS");
 * Object instance = settings.get(Settings.class);
 * }</pre>
 *
 * @param <T> the expected result type; use {@link #checked(Class)} to enforce it at runtime
 * @author REN YuanTong
 * @since 1.0.0
 * @see CarpetAddonAdapter
 * @see java.lang.reflect.Field
 * @see java.lang.reflect.Method
 */
public interface Accessor<T> {
    /**
     * Reads the configured value from the supplied target.
     *
     * <pre>{@code
     * Object value = accessor.get(accessor.isStaticAccess() ? Owner.class : ownerInstance);
     * }</pre>
     *
     * @param target a declaring {@link Class} for static access, or an existing instance for
     *               instance access
     * @return the reflected value, which may be {@code null}
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws IllegalArgumentException if the target kind or member's static mode is incompatible
     * @throws IllegalStateException if the configured member cannot be found, accessed, or invoked
     * @throws ClassCastException when this accessor was wrapped by {@link #checked(Class)} and the
     *                            result has the wrong type
     */
    Object get(Object target);

    /**
     * Indicates which target form {@link #get(Object)} requires.
     *
     * <pre>{@code
     * Object target = accessor.isStaticAccess() ? Owner.class : owner;
     * }</pre>
     *
     * @return {@code true} when the target must be a {@link Class}; {@code false} when it must be an instance
     */
    boolean isStaticAccess();

    /**
     * Wraps this accessor with an explicit runtime result check. A {@code null} result remains
     * {@code null} because {@link Class#cast(Object)} accepts it.
     *
     * <pre>{@code
     * Accessor<String> checked = Accessor.ofInstanceField("name").checked(String.class);
     * String name = (String) checked.get(instance);
     * }</pre>
     *
     * @param resultType the class accepted for non-null results
     * @return an accessor that delegates to this accessor and checks its result
     * @throws NullPointerException if {@code resultType} is {@code null}
     */
    default Accessor<T> checked(Class<T> resultType) {
        Objects.requireNonNull(resultType, "resultType");
        return new CheckedAccess<>(this, resultType);
    }

    /**
     * Creates an accessor that finds the first registered extension assignable to the target class.
     *
     * <pre>{@code
     * CarpetExtension extension = Accessor.ofRegisteredExtension().get(MyExtension.class);
     * }</pre>
     *
     * @return a static-mode accessor over {@link CarpetServer#extensions}
     */
    static RegisteredExtensionAccess ofRegisteredExtension() {
        return new RegisteredExtensionAccess();
    }

    /**
     * Creates an accessor for an instance field.
     *
     * <pre>{@code
     * Object value = Accessor.ofInstanceField("settings").get(extension);
     * }</pre>
     *
     * @param name the declared or inherited field name
     * @param <T> the expected field value type
     * @return the configured field accessor
     * @throws IllegalArgumentException if {@code name} is {@code null} or blank
     */
    static <T> Accessor<T> ofInstanceField(String name) {
        return fieldAccessor(name, false);
    }

    /**
     * Creates an accessor for a static field.
     *
     * <pre>{@code
     * Object value = Accessor.ofStaticField("INSTANCE").get(Extension.class);
     * }</pre>
     *
     * @param name the declared or inherited field name
     * @param <T> the expected field value type
     * @return the configured field accessor
     * @throws IllegalArgumentException if {@code name} is {@code null} or blank
     */
    static <T> Accessor<T> ofStaticField(String name) {
        return fieldAccessor(name, true);
    }

    /**
     * Creates an accessor for a no-argument instance method.
     *
     * <pre>{@code
     * Object value = Accessor.ofInstanceMethod("getSettings").get(extension);
     * }</pre>
     *
     * @param name the declared or inherited method name
     * @param <T> the expected return type
     * @return the configured method accessor
     * @throws IllegalArgumentException if {@code name} is {@code null} or blank
     */
    static <T> Accessor<T> ofInstanceMethod(String name) {
        return ofInstanceMethod(name, new Class<?>[0], new Object[0]);
    }

    /**
     * Creates an accessor for an instance method with fixed arguments.
     *
     * <pre>{@code
     * Accessor<?> lookup = Accessor.ofInstanceMethod(
     *     "rule", new Class<?>[] {String.class}, new Object[] {"myRule"});
     * Object rule = lookup.get(manager);
     * }</pre>
     *
     * @param name the declared or inherited method name
     * @param parameterTypes the exact declared parameter types; {@code null} means no parameters
     * @param args the fixed invocation arguments; {@code null} means no arguments
     * @param <T> the expected return type
     * @return the configured method accessor
     * @throws IllegalArgumentException if the name is blank or the array lengths differ
     * @throws NullPointerException if any element of {@code parameterTypes} is {@code null}
     */
    static <T> Accessor<T> ofInstanceMethod(String name, Class<?>[] parameterTypes, Object[] args) {
        return methodAccessor(name, parameterTypes, args, false);
    }

    /**
     * Creates an accessor for a no-argument static method.
     *
     * <pre>{@code
     * Object value = Accessor.ofStaticMethod("getInstance").get(Extension.class);
     * }</pre>
     *
     * @param name the declared or inherited method name
     * @param <T> the expected return type
     * @return the configured method accessor
     * @throws IllegalArgumentException if {@code name} is {@code null} or blank
     */
    static <T> Accessor<T> ofStaticMethod(String name) {
        return ofStaticMethod(name, new Class<?>[0], new Object[0]);
    }

    /**
     * Creates an accessor for a static method with fixed arguments.
     *
     * <pre>{@code
     * Accessor<?> lookup = Accessor.ofStaticMethod(
     *     "find", new Class<?>[] {String.class}, new Object[] {"myRule"});
     * Object value = lookup.get(Registry.class);
     * }</pre>
     *
     * @param name the declared or inherited method name
     * @param parameterTypes the exact declared parameter types; {@code null} means no parameters
     * @param args the fixed invocation arguments; {@code null} means no arguments
     * @param <T> the expected return type
     * @return the configured method accessor
     * @throws IllegalArgumentException if the name is blank or the array lengths differ
     * @throws NullPointerException if any element of {@code parameterTypes} is {@code null}
     */
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

    /**
     * Serializable accessor decorator that validates non-null results with {@link Class#cast(Object)}.
     *
     * @param source the accessor to invoke
     * @param resultType the required runtime result type
     * @param <T> the checked result type
     * @see #checked(Class)
     */
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
     * Serializable description of a field read. Its record components retain the configuration
     * needed by the JSON codec.
     *
     * @param name the field name
     * @param isStaticAccess whether the target must be a declaring class
     * @param <T> the expected field value type
     * @see io.github.piscescup.fabricmc.carpetgui.adapter.codec.AccessorJsonCodec
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

    /**
     * Serializable description of a method invocation with fixed arguments. Parameter and argument
     * arrays are defensively copied on construction and access.
     *
     * @param name the method name
     * @param isStaticAccess whether the target must be a declaring class
     * @param parameterTypes the exact declared parameter types
     * @param args the fixed invocation arguments
     * @param <T> the expected return type
     * @see io.github.piscescup.fabricmc.carpetgui.adapter.codec.AccessorJsonCodec
     */
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

    /**
     * Looks up a live extension instance in {@link CarpetServer#extensions} by its class.
     *
     * @see #ofRegisteredExtension()
     */
    record RegisteredExtensionAccess() implements Accessor<CarpetExtension> {
        @Override
        public CarpetExtension get(Object target) {
            if (!(target instanceof Class<?> type) || !CarpetExtension.class.isAssignableFrom(type)) {
                throw new IllegalArgumentException("Registered extension access requires a CarpetExtension class");
            }
            return List.copyOf(CarpetServer.extensions).stream().filter(type::isInstance).findFirst().orElse(null);
        }

        /**
         * Reports that registered-extension lookup accepts a {@link Class} target.
         *
         * @return always {@code true}
         */
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

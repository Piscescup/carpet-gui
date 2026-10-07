package io.github.piscescup.fabricmc.carpetgui.adapter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/** Discovers rule names from objects held in a settings collection. */
public class RuleClassCarpetAddonAdapter<T> extends CarpetAddonAdapter {
    protected String listSettingsFieldName;
    private final String ruleClassName;
    private final String stringMethodName;
    private final Class<?>[] parameterTypes;
    private final Function<T, String> mapper;

    RuleClassCarpetAddonAdapter(
        String modId,
        String modFancyName,
        String extensionCanonicalName,
        String extensionFieldName,
        String settingsCanonicalName,
        String listSettingsFieldName,
        String ruleClassName,
        String stringMethodName,
        Function<T, String> mapper,
        Class<?>... parameterTypes
    ) {
        super(modId, modFancyName, extensionCanonicalName, extensionFieldName, settingsCanonicalName);
        if (listSettingsFieldName == null || listSettingsFieldName.isBlank()) {
            throw new IllegalArgumentException("Rules collection field must not be blank");
        }
        if (ruleClassName == null || ruleClassName.isBlank()) {
            throw new IllegalArgumentException("Rule class must not be blank");
        }
        if (stringMethodName == null || stringMethodName.isBlank()) {
            throw new IllegalArgumentException("Rule name method must not be blank");
        }
        this.listSettingsFieldName = listSettingsFieldName;
        this.ruleClassName = ruleClassName;
        this.stringMethodName = stringMethodName;
        this.mapper = mapper;
        this.parameterTypes = parameterTypes == null ? new Class<?>[0] : parameterTypes.clone();
    }

    @Override
    protected Collection<String> getRuleNames(Object... args)
        throws InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchFieldException,
               NoSuchMethodException, InstantiationException
    {
        Object[] invocationArguments = args == null ? new Object[0] : args;
        if (invocationArguments.length != parameterTypes.length) {
            throw new IllegalArgumentException(
                "Expected " + parameterTypes.length + " rule-name arguments, got " + invocationArguments.length);
        }

        Field rulesField = listSettingsField();
        rulesField.setAccessible(true);
        Object settingsInstance = getSettingsInstance();
        Object fieldValue = rulesField.get(settingsInstance);
        if (!(fieldValue instanceof Collection<?> settings)) {
            throw new IllegalStateException(
                "Field " + listSettingsFieldName + " must be a Collection, got "
                + (fieldValue == null ? "null" : fieldValue.getClass().getName()));
        }

        Class<?> ruleClass = load(ruleClassName);
        Method nameMethod = ruleClass.getDeclaredMethod(stringMethodName, parameterTypes);
        nameMethod.setAccessible(true);
        boolean staticMethod = Modifier.isStatic(nameMethod.getModifiers());

        List<String> names = new ArrayList<>(settings.size());
        for (Object setting : settings) {
            if (!staticMethod && !ruleClass.isInstance(setting)) {
                throw new IllegalStateException("Rules must be instances of " + ruleClass.getName());
            }
            @SuppressWarnings("unchecked")
            T value = (T) nameMethod.invoke(staticMethod ? null : setting, invocationArguments);
            names.add(mapper.apply(value));
        }
        return names;
    }

    protected Field listSettingsField() throws ClassNotFoundException, NoSuchFieldException {
        return getSettingsClass().getDeclaredField(listSettingsFieldName);
    }

    protected Object getSettingsInstance()
        throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException,
               ClassNotFoundException, NoSuchFieldException {
        Field field = listSettingsField();
        if (Modifier.isStatic(field.getModifiers())) return null;
        Class<?> settingsClass = getSettingsClass();
        Constructor<?> constructor = settingsClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}

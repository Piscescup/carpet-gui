package io.github.piscescup.fabricmc.carpetgui.adapter;


import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Discovers rule names from objects held in a settings collection. */
public class RuleClassCarpetAddonAdapter extends CarpetAddonAdapter {
    private final PackageRef ruleClassRef;
    private final String carpetRuleCanonicalClassName;
    private final Accessor<?> rulesAccessor;
    private final Accessor<?> ruleNameAccessor;

    RuleClassCarpetAddonAdapter(
        RuleBuilder builder
    ) {
        super(builder);
        this.ruleClassRef = builder.ruleClassRef;
        this.carpetRuleCanonicalClassName = builder.ruleClassCanonicalName;
        this.rulesAccessor = builder.rulesAccessor;
        this.ruleNameAccessor = builder.ruleNameAccessor;
    }

    public PackageRef ruleClassRef() { return ruleClassRef; }
    public Accessor<?> rulesAccessor() { return rulesAccessor; }
    public Accessor<?> ruleNameAccessor() { return ruleNameAccessor; }

    @Override
    protected Collection<String> getRuleNames(Object... args) throws ClassNotFoundException {
        if (args != null && args.length != 0) {
            throw new IllegalArgumentException("Rule-name arguments must be configured in the Accessor");
        }
        Object target = rulesAccessor.isStaticAccess() ? getSettingsClass() : getSettingsInstance();
        if (target == null) return List.of();

        Object value = rulesAccessor.get(target);

        if (value == null) return List.of();

        if (!(value instanceof Collection<?> rules)) {
            throw new IllegalArgumentException("Rules accessor for " + modId
                + " must return a Collection, got " + value.getClass().getName());
        }

        Class<?> ruleClass = load(carpetRuleCanonicalClassName);
        var names = new LinkedHashSet<String>();

        for (Object rule : rules) {
            if (rule == null) continue;
            if (!ruleClass.isInstance(rule)) {
                throw new IllegalArgumentException("Rules for " + modId + " must be instances of "
                    + ruleClass.getName() + ", got " + rule.getClass().getName());
            }
            Object name = ruleNameAccessor.get(ruleNameAccessor.isStaticAccess() ? ruleClass : rule);
            if (name == null) continue;
            if (!(name instanceof String text)) {
                throw new IllegalArgumentException("Rule-name accessor for " + modId
                    + " must return a String, got " + name.getClass().getName());
            }
            if (!text.isBlank()) names.add(text);
        }

        return List.copyOf(names);
    }

}

class RuleBuilder
    extends Builder<RuleAdapterBuilder>
    implements RuleAdapterBuilder
{
    PackageRef ruleClassRef;
    String ruleClassCanonicalName;

    Accessor<?> rulesAccessor;

    Accessor<?> ruleNameAccessor;

    protected RuleBuilder(String modId, String packageName) {
        super(modId, packageName);
    }

    @Override
    public RuleAdapterBuilder carpetRuleClassName(PackageRef packageRef) {
        this.ruleClassRef = Objects.requireNonNull(packageRef, "Class reference");
        this.ruleClassCanonicalName =
            packageRef.toCanonicalPackage(this.packageName);
        return this;
    }

    @Override
    public RuleAdapterBuilder rulesAccessor(Accessor<?> accessor) {
        this.rulesAccessor = Objects.requireNonNull(accessor, "Rules accessor");
        return this;
    }

    @Override
    public RuleAdapterBuilder ruleNameAccessor(Accessor<String> accessor) {
        this.ruleNameAccessor = Objects.requireNonNull(accessor, "Rule-name accessor");
        return this;
    }

    @Override
    public RuleClassCarpetAddonAdapter build() {
        requireText(modId, "Mod ID");
        requireText(carpetSettingsClassCanonicalName, "Settings class");
        requireText(ruleClassCanonicalName, "Rule class");
        Objects.requireNonNull(rulesAccessor, "Rules accessor");
        Objects.requireNonNull(ruleNameAccessor, "Rule-name accessor");
        if (!rulesAccessor.isStaticAccess()) {
            Objects.requireNonNull(settingsAccessor, "Settings accessor");
        }
        if (carpetExtensionClassCanonicalName != null) {
            Objects.requireNonNull(extensionAccessor, "Extension accessor");
        }
        return new RuleClassCarpetAddonAdapter(this);
    }
    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
    }

}

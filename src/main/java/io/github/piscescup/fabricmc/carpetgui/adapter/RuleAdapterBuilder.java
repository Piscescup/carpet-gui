package io.github.piscescup.fabricmc.carpetgui.adapter;


/**
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public interface RuleAdapterBuilder extends AdapterBuilder<RuleAdapterBuilder> {
    RuleAdapterBuilder carpetRuleClassName(PackageRef packageRef);

    default RuleAdapterBuilder carpetRuleClassName(String carpetRuleClassName) {
        return carpetRuleClassName(PackageRef.relative(carpetRuleClassName));
    }

    RuleAdapterBuilder rulesAccessor(
        Accessor<?> accessor
    );

    RuleAdapterBuilder ruleNameAccessor(
        Accessor<String> accessor
    );
}

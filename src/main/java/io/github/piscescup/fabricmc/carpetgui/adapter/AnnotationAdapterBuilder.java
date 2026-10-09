package io.github.piscescup.fabricmc.carpetgui.adapter;

import carpet.api.settings.Rule;

/**
 *
 * @author REN YuanTong
 * @since
 */
public interface AnnotationAdapterBuilder extends AdapterBuilder<AnnotationAdapterBuilder> {
    AnnotationAdapterBuilder ruleAnnotationClassName(PackageRef packageRef);

    /** Optional index of annotated settings classes, such as ROFSettings.ruleClasses. */
    AnnotationAdapterBuilder settingsClassesAccessor(Accessor<?> accessor);

    default AnnotationAdapterBuilder ruleAnnotationClassName(String ruleAnnotationClassName) {
        return ruleAnnotationClassName(PackageRef.relative(ruleAnnotationClassName));
    }

    default AnnotationAdapterBuilder onCarpetRuleAnnotation() {
        return ruleAnnotationClassName(
            PackageRef.absolute(Rule.class.getCanonicalName())
        );
    }
}

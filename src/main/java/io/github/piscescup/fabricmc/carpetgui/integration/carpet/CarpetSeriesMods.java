package io.github.piscescup.fabricmc.carpetgui.integration.carpet;

import io.github.piscescup.fabricmc.carpetgui.adapter.CarpetAddonAdapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author REN YuanTong
 * @since
 */
public final class CarpetSeriesMods {
    /** Compatibility adapters for released addons which cannot implement Carpet GUI's APIs themselves. */
    public static final List<CarpetAddonAdapter> CARPET_SERIES = new ArrayList<>(
        List.of(
            CarpetAddonAdapter.fromAnnotation(
                "carpet-pry-addition", "Carpet PRY Addition",
                "CarpetPrimaryuanServer", "CarpetPrimaryuanSettings",
                "settings.Rule"
            ).withPackage("me.primaryuan.carpet"),
            CarpetAddonAdapter.fromMethod(
                "carpet-igny-addition", "Carpet Igny Addition",
                "IGNYServer", "IGNYSettings",
                "rule.RuleContext", "getName", String::valueOf
            ).withPackage("com.liuyue.igny"),
            CarpetAddonAdapter.fromAnnotation(
                "carpet-tis-addition", "Carpet TIS Addition",
                "CarpetTISAdditionServer",
                "CarpetTISAdditionSettings",
                "settings.Rule"
            ).withPackage("carpettisaddition"),
            CarpetAddonAdapter.fromMethod(
                "carpet-org-addition", "Carpet Org Addition",
                "CarpetOrgAdditionExtension", "CarpetOrgAdditionSettings",
                "rule.RuleContext", "getName"
            ).withPackage("boat.carpetorgaddition"),
            CarpetAddonAdapter.fromAnnotation(
                "carpet-ams-addition", "Carpet AMS Addition",
                "CarpetAMSAdditionServer",
                "CarpetAMSAdditionSettings",
                "settings.Rule"
            ).withPackage("carpetamsaddition"),
            CarpetAddonAdapter.fromAnnotationOnCarpetRule(
                "carpet-extra-extras", "Carpet Extra Extras",
                "CarpetExtraExtrasServer", null, "CarpetExtraExtrasSettings"
            ).withPackage("net.thedustbuster.cee.server"),
            CarpetAddonAdapter.fromAnnotationOnCarpetRule(
                "gca", "Carpet Gugle Addition",
                "GcaExtension", null,
                "GcaSetting"
            ).withPackage("dev.dubhe.gugle.carpet"),
            CarpetAddonAdapter.rof().withPackage("com.carpet.rof")
        )
    );

    static {
        CARPET_SERIES
            .sort(Comparator.comparing(CarpetAddonAdapter::carpetModId));
    }

}

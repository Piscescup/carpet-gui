package io.github.piscescup.fabricmc.carpetgui.adapter;

/** Configures access to an addon's dedicated settings manager. */
public interface ManagerAdapterBuilder extends AdapterBuilder<ManagerAdapterBuilder> {
    /** Class declaring the manager member; it does not have to implement CarpetExtension. */
    ManagerAdapterBuilder managerClassName(PackageRef packageRef);

    default ManagerAdapterBuilder managerClassName(String className) {
        return managerClassName(PackageRef.relative(className));
    }

    /** Static access targets the manager class; instance access targets the live extension. */
    ManagerAdapterBuilder managerAccessor(Accessor<?> accessor);
}

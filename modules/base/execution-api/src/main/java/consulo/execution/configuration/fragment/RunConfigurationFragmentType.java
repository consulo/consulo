package consulo.execution.configuration.fragment;

public enum RunConfigurationFragmentType {
    EDITOR,
    /**
     * An on/off option shown as a removable chip while it is on.
     */
    TAG,
    /**
     * A titled section holding the fragments that name it as their {@link RunConfigurationFragment#getParentId() parent}; it has
     * no editor or state of its own, shows while any of its fragments shows, and lists them in a submenu of "Modify options" and in
     * its own "Modify" link. A group without applicable fragments is left out.
     */
    GROUP
}

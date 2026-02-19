/**
 * @author UNV
 * @since 2026-01-15
 */
module consulo.localization.impl {
    requires consulo.container.api;
    requires consulo.logging.api;
    requires consulo.localization.api;
    requires consulo.localize.api;
    requires consulo.util.lang;
    requires consulo.util.io;
    requires consulo.proxy;

    requires org.yaml.snakeyaml;
    requires com.ibm.icu;

    provides consulo.localization.LocalizationManager with consulo.localization.impl.LocalizationManagerImpl;
}
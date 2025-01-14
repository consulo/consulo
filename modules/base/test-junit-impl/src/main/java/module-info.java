/**
 * @author VISTALL
 * @since 2025-01-14
 */
module consulo.test.junit.impl {
    requires consulo.test.impl;
    requires transitive org.junit.jupiter.api;

    requires consulo.language.impl;

    exports consulo.test.junit.impl.extension;
    exports consulo.test.junit.impl.language;
}
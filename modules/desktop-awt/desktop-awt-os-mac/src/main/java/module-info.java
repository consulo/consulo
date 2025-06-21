/**
 * @author VISTALL
 * @since 2025-06-07
 */
module consulo.desktop.awt.os.mac {
    requires consulo.ide.impl;
    requires consulo.desktop.awt.ide.impl;

    requires consulo.desktop.awt.hacking;

    opens consulo.desktop.awt.os.mac.internal.touchBar to com.sun.jna;
}
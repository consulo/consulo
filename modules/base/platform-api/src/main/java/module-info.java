import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-01-13
 */
@NullMarked
module consulo.platform.api {
  requires consulo.ui.api;
  requires consulo.annotation;
  requires consulo.container.api;
  requires consulo.util.lang;
  requires consulo.util.dataholder;

  uses consulo.platform.internal.PlatformInternal;
  
  exports consulo.platform;
  exports consulo.platform.os;
  exports consulo.platform.internal to
    consulo.desktop.awt.ide.impl, consulo.desktop.awt.ui.impl,
    consulo.test.impl,
    consulo.desktop.swt.ide.impl, consulo.desktop.qt.ide.impl, consulo.desktop.qt.editor.impl, consulo.desktop.qt.ui.impl,
    consulo.it,
    consulo.web.ide, consulo.web.ui.impl, consulo.web.editor.impl;
}
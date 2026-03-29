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
    consulo.desktop.awt.ide.impl,
    consulo.test.impl,
    consulo.desktop.swt.ide.impl;
}
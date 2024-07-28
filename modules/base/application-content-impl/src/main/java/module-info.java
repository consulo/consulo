/**
 * @author VISTALL
 * @since 09-Apr-22
 */
module consulo.application.content.impl {
  requires transitive consulo.application.content.api;
  requires transitive consulo.virtual.file.system.impl;
  requires static consulo.component.impl;

  exports consulo.application.content.impl.internal to consulo.ide.impl, consulo.module.impl;
  exports consulo.application.content.impl.internal.bundle to consulo.ide.impl;
  exports consulo.application.content.impl.internal.library to consulo.ide.impl, consulo.module.impl;
}
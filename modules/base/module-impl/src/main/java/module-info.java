/**
 * @author VISTALL
 * @since 09-Apr-22
 */
module consulo.module.impl {
  requires transitive consulo.application.content.impl;
  requires transitive consulo.module.content.api;
  requires static consulo.component.impl;
  requires static consulo.application.impl;

  exports consulo.module.impl.internal to consulo.ide.impl, consulo.project.impl;
  exports consulo.module.impl.internal.extension to consulo.ide.impl;
  exports consulo.module.impl.internal.layer to consulo.ide.impl, consulo.util.xml.serializer;
  exports consulo.module.impl.internal.layer.library to consulo.ide.impl;
  exports consulo.module.impl.internal.layer.orderEntry to consulo.ide.impl;
}
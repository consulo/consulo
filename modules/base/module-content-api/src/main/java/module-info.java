/**
 * @author VISTALL
 * @since 21/01/2022
 */
module consulo.module.content.api {
  requires transitive consulo.application.content.api;
  requires transitive consulo.project.content.api;
  requires transitive consulo.module.api;

  requires static consulo.index.io;

  exports consulo.module.content;
  exports consulo.module.content.scope;
  exports consulo.module.content.util;
  exports consulo.module.content.library;
  exports consulo.module.content.library.util;
  exports consulo.module.content.layer;
  exports consulo.module.content.layer.extension;
  exports consulo.module.content.layer.event;
  exports consulo.module.content.layer.orderEntry;

  exports consulo.module.content.internal to
    consulo.ide.impl,
    consulo.module.impl,
    consulo.language.impl,
    consulo.compiler.impl,
    consulo.compiler.artifact.impl,
    consulo.module.ui.api;
}
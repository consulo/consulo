/**
 * @author VISTALL
 * @since 19/01/2022
 */
module consulo.project.api {
  // TODO [VISTALL] obsolete requires
  requires java.desktop;

  requires transitive consulo.application.api;
  requires transitive consulo.virtual.file.system.api;
  requires transitive consulo.datacontext.api;

  exports consulo.project;
  exports consulo.project.macro;
  exports consulo.project.event;
  exports consulo.project.startup;
  exports consulo.project.util;
  exports consulo.project.util.query;
  exports consulo.project.localize;

  exports consulo.project.internal to consulo.ide.impl,
                                      consulo.desktop.awt.ide.impl,
                                      consulo.sand.language.plugin,
                                      consulo.application.impl,
                                      consulo.component.impl,
                                      consulo.version.control.system.api,
                                      consulo.project.impl;
}
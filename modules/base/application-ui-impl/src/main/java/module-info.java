/**
 * @author VISTALL
 * @since 30/12/2022
 */
module consulo.application.ui.impl {
  requires transitive consulo.application.ui.api;
  requires consulo.project.ui.api;

  exports consulo.application.ui.impl.internal to
                                        consulo.ide.impl,
                                        consulo.desktop.awt.ide.impl,
                                        consulo.desktop.swt.ide.impl;

  opens consulo.application.ui.impl.internal to consulo.util.xml.serializer;
}

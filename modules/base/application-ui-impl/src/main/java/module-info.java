/**
 * @author VISTALL
 * @since 30/12/2022
 */
module consulo.application.ui.impl {
  requires transitive consulo.application.ui.api;
  requires consulo.project.ui.api;

  exports consulo.application.ui.impl.internal to
                                        consulo.ide.impl,
                                        consulo.desktop.awt.ide.impl, consulo.desktop.awt.editor.impl, consulo.desktop.awt.ui.impl,
                                        consulo.desktop.swt.ide.impl, consulo.desktop.qt.ide.impl, consulo.desktop.qt.editor.impl, consulo.desktop.qt.ui.impl,
                                        consulo.web.ide, consulo.web.ui.impl, consulo.web.editor.impl;

  opens consulo.application.ui.impl.internal to consulo.util.xml.serializer;
}

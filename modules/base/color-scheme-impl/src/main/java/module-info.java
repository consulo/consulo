/**
 * @author VISTALL
 * @since 19-Mar-22
 */
module consulo.color.scheme.impl {
  requires transitive consulo.color.scheme.api;

  exports consulo.colorScheme.impl to consulo.code.editor.impl,
                                    consulo.ide.impl,
                                    consulo.desktop.awt.ide.impl;
}
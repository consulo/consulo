/**
 * @author VISTALL
 * @since 19-Mar-22
 */
module consulo.code.editor.impl {
  // TODO remove in future this dependency
  requires java.desktop;

  requires transitive consulo.code.editor.api;
  requires transitive consulo.ui.ex.awt.api;
  requires transitive consulo.document.impl;
  requires transitive consulo.color.scheme.impl;
  requires transitive consulo.language.api;
  requires transitive consulo.language.code.style.api;

  requires consulo.desktop.awt.hacking;

  requires consulo.injecting.api;

  exports consulo.codeEditor.impl to
          consulo.ide.impl,
          consulo.language.inject.impl,
          consulo.desktop.ide.impl,
          consulo.injecting.pico.impl,
          consulo.util.xml.serializer,
          consulo.desktop.swt.ide.impl;

  exports consulo.codeEditor.impl.action to consulo.ide.impl, consulo.injecting.pico.impl, consulo.util.xml.serializer;

  exports consulo.codeEditor.impl.util to
          consulo.ide.impl,
          consulo.language.inject.impl,
          consulo.desktop.ide.impl, consulo.desktop.swt.ide.impl;

  exports consulo.codeEditor.impl.softwrap.mapping to consulo.ide.impl, consulo.desktop.swt.ide.impl;
  exports consulo.codeEditor.impl.softwrap to consulo.ide.impl, consulo.desktop.swt.ide.impl;
}
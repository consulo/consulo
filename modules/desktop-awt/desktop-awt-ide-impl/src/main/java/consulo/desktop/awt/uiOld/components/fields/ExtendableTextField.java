// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.uiOld.components.fields;

import consulo.application.AllIcons;
import consulo.disposer.Disposable;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.UIBundle;
import consulo.ui.ex.awt.JBTextField;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

/**
 * @author Sergey Malenkov
 */
public class ExtendableTextField extends JBTextField implements ExtendableTextComponent {
  private List<Extension> extensions = emptyList();

  public ExtendableTextField() {
    this(null);
  }

  public ExtendableTextField(int columns) {
    this(null, columns);
  }

  public ExtendableTextField(String text) {
    this(text, 20);
  }

  public ExtendableTextField(String text, int columns) {
    super(text, columns);
  }

  @Override
  public List<Extension> getExtensions() {
    return extensions;
  }

  @Override
  public void setExtensions(Extension... extensions) {
    setExtensions(asList(extensions));
  }

  @Override
  public void setExtensions(Collection<? extends Extension> extensions) {
    setExtensions(new ArrayList<>(extensions));
  }

  private void setExtensions(List<? extends Extension> extensions) {
    putClientProperty("JTextField.variant", null);
    this.extensions = unmodifiableList(extensions);
    putClientProperty("JTextField.variant", ExtendableTextComponent.VARIANT);
  }

  @Override
  public void addExtension(@Nonnull Extension extension) {
    if (!getExtensions().contains(extension)) {
      List<Extension> extensions = new ArrayList<>(getExtensions());
      extensions.add(extension);
      setExtensions(extensions);
    }
  }

  @Override
  public void removeExtension(@Nonnull Extension extension) {
    ArrayList<Extension> extensions = new ArrayList<>(getExtensions());
    if (extensions.remove(extension)) setExtensions(extensions);
  }

  /**
   * Temporary solution to support icons in the text component for different L&F.
   * This method replaces non-supported UI with Darcula UI.
   *
   * @param ui an object to paint this text component
   */
  //@Override
  //@Deprecated
  //public void setUI(TextUI ui) {
  //  TextUI suggested = ui;
  //  try {
  //    if (ui == null || !Class.forName("consulo.ide.impl.idea.ide.ui.laf.darcula.ui.TextFieldWithPopupHandlerUI_New").isAssignableFrom(ui.getClass())) {
  //      ui = (TextUI)Class.forName("consulo.ide.impl.idea.ide.ui.laf.darcula.ui.DarculaTextFieldUI_New").getDeclaredMethod("createUI", JComponent.class).invoke(null, this);
  //    }
  //  }
  //  catch (Exception ignore) {
  //  }
  //
  //  super.setUI(ui);
  //  if (ui != suggested) {
  //    try {
  //      setBorder((Border)Class.forName("consulo.ide.impl.idea.ide.ui.laf.darcula.ui.DarculaTextBorder_New").newInstance());
  //    }
  //    catch (Exception ignore) {
  //    }
  //  }
  //}

  public ExtendableTextField addBrowseExtension(@Nonnull Runnable action, @Nullable Disposable parentDisposable) {
    KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK);
    String tooltip = UIBundle.message("component.with.browse.button.browse.button.tooltip.text") + " (" + KeymapUtil.getKeystrokeText(keyStroke) + ")";

    ExtendableTextComponent.Extension browseExtension = ExtendableTextComponent.Extension.create(AllIcons.Nodes.TreeOpen, AllIcons.Nodes.TreeOpen, tooltip, action);

    new DumbAwareAction() {
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        action.run();
      }
    }.registerCustomShortcutSet(new CustomShortcutSet(keyStroke), this, parentDisposable);
    addExtension(browseExtension);

    return this;
  }
}

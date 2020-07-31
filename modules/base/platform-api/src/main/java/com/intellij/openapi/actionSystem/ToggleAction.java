// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.actionSystem;

import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.ui.migration.SwingImageRef;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * An action which has a selected state, and which toggles its selected state when performed.
 * Can be used to represent a menu item with a checkbox, or a toolbar button which keeps its pressed state.
 */
@SuppressWarnings("StaticInheritance")
public abstract class ToggleAction extends AnAction implements Toggleable {
  public ToggleAction() {
  }

  public ToggleAction(@Nullable @Nls(capitalization = Nls.Capitalization.Title) final String text) {
    super(text);
  }

  public ToggleAction(@Nullable @Nls(capitalization = Nls.Capitalization.Title) final String text,
                      @Nullable @Nls(capitalization = Nls.Capitalization.Sentence) final String description,
                      @Nullable final Icon icon) {
    super(text, description, icon);
  }

  @Deprecated
  public ToggleAction(@Nullable @Nls(capitalization = Nls.Capitalization.Title) final String text,
                      @Nullable @Nls(capitalization = Nls.Capitalization.Sentence) final String description,
                      @Nullable final SwingImageRef icon) {
    super(text, description, icon);
  }

  public ToggleAction(@Nullable @Nls(capitalization = Nls.Capitalization.Title) final String text,
                      @Nullable @Nls(capitalization = Nls.Capitalization.Sentence) final String description,
                      @Nullable final Image icon) {
    super(text, description, icon);
  }

  public ToggleAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable final Image icon) {
    super(text, description, icon);
  }

  @Override
  public final void actionPerformed(@Nonnull final AnActionEvent e) {
    final boolean state = !isSelected(e);
    setSelected(e, state);
    final Presentation presentation = e.getPresentation();
    Toggleable.setSelected(presentation, state);
  }

  /**
   * Returns the selected (checked, pressed) state of the action.
   *
   * @param e the action event representing the place and context in which the selected state is queried.
   * @return true if the action is selected, false otherwise
   */
  public abstract boolean isSelected(@Nonnull AnActionEvent e);

  /**
   * Sets the selected state of the action to the specified value.
   *
   * @param e     the action event which caused the state change.
   * @param state the new selected state of the action.
   */
  public abstract void setSelected(@Nonnull AnActionEvent e, boolean state);

  @Override
  public void update(@Nonnull final AnActionEvent e) {
    boolean selected = isSelected(e);
    final Presentation presentation = e.getPresentation();
    Toggleable.setSelected(presentation, selected);
    if (e.isFromContextMenu()) {
      //force to show check marks instead of toggled icons in context menu
      presentation.setIcon(null);
    }
  }
}

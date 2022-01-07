// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.editorHeaderActions;

import com.intellij.find.SearchSession;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CheckboxAction;
import com.intellij.openapi.project.DumbAware;
import consulo.ui.image.Image;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;

public abstract class EditorHeaderToggleAction extends CheckboxAction implements DumbAware {
  protected EditorHeaderToggleAction(@Nonnull String text) {
    this(text, null, null, null);
  }

  protected EditorHeaderToggleAction(@Nonnull String text, @Nullable Image icon, @Nullable Image hoveredIcon, @Nullable Image selectedIcon) {
    super(text);
    getTemplatePresentation().setIcon(icon);
    getTemplatePresentation().setHoveredIcon(hoveredIcon);
    getTemplatePresentation().setSelectedIcon(selectedIcon);
  }

  @Override
  public boolean displayTextInToolbar() {
    return true;
  }

  @Nonnull
  @Override
  public JComponent createCustomComponent(@Nonnull Presentation presentation, @Nonnull String place) {
    JComponent customComponent = super.createCustomComponent(presentation, place);
    customComponent.setFocusable(false);
    customComponent.setOpaque(false);
    return customComponent;
  }

  @Override
  public boolean isSelected(@Nonnull AnActionEvent e) {
    SearchSession search = e.getData(SearchSession.KEY);
    return search != null && isSelected(search);
  }

  @Override
  public void setSelected(@Nonnull AnActionEvent e, boolean selected) {
    SearchSession search = e.getData(SearchSession.KEY);
    if (search != null) {
      setSelected(search, selected);
    }
  }

  protected abstract boolean isSelected(@Nonnull SearchSession session);

  protected abstract void setSelected(@Nonnull SearchSession session, boolean selected);
}

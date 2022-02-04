package com.intellij.application.options;

import consulo.application.AllIcons;
import consulo.module.Module;
import com.intellij.ui.ColoredListCellRenderer;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * @author yole
 */
public class ModuleListCellRenderer extends ColoredListCellRenderer<Module> {
  private final String myEmptySelectionText;

  public ModuleListCellRenderer() {
    this("[none]");
  }

  public ModuleListCellRenderer(@Nonnull String emptySelectionText) {
    myEmptySelectionText = emptySelectionText;
  }

  @Override
  protected void customizeCellRenderer(@Nonnull JList<? extends Module> list, Module module, int index, boolean selected, boolean hasFocus) {
    if (module == null) {
      append(myEmptySelectionText);
    }
    else {
      setIcon(AllIcons.Nodes.Module);
      append(module.getName());
    }
  }
}

package consulo.module.ui.awt;

import consulo.application.AllIcons;
import consulo.module.Module;
import consulo.ui.ex.awt.ColoredListCellRenderer;

import jakarta.annotation.Nonnull;
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

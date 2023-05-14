package consulo.ide.impl.idea.find.editorHeaderActions;

import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.popup.JBPopup;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.application.util.SystemInfo;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.List;

public class Utils {
  private Utils() {
  }

  public static void showCompletionPopup(JComponent toolbarComponent,
                                         final JList list,
                                         String title,
                                         final JTextComponent textField,
                                         String ad) {

    final Runnable callback = new Runnable() {
      @Override
      public void run() {
        String selectedValue = (String)list.getSelectedValue();
        if (selectedValue != null) {
          textField.setText(selectedValue);
        }
      }
    };

    final PopupChooserBuilder builder = new PopupChooserBuilder<>(list);
    if (title != null) {
      builder.setTitle(title);
    }
    final JBPopup popup = builder.setMovable(false).setResizable(false)
            .setRequestFocus(true).setItemChoosenCallback(callback).createPopup();

    if (ad != null) {
      popup.setAdText(ad, SwingConstants.LEFT);
    }

    if (toolbarComponent != null) {
      popup.showUnderneathOf(toolbarComponent);
    }
    else {
      popup.showUnderneathOf(textField);
    }
  }

  public static void setSmallerFont(final JComponent component) {
    if (SystemInfo.isMac) {
      component.setFont(JBUI.Fonts.smallFont());
    }
  }

  public static void setSmallerFontForChildren(JComponent component) {
    for (Component c : component.getComponents()) {
      if (c instanceof JComponent) {
        setSmallerFont((JComponent)c);
      }
    }
  }

  @Nonnull
  public static CustomShortcutSet shortcutSetOf(@Nonnull List<Shortcut> shortcuts) {
    return new CustomShortcutSet(shortcuts.toArray(new Shortcut[shortcuts.size()]));
  }

  @Nonnull
  public static List<Shortcut> shortcutsOf(@Nonnull String actionId) {
    AnAction action = ActionManager.getInstance().getAction(actionId);
    return action == null ? ContainerUtil.<Shortcut>emptyList() : ContainerUtil.immutableList(action.getShortcutSet().getShortcuts());
  }
}

package consulo.fileEditor.impl.internal.search;

import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.SimpleListPopupStepBuilder;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.List;

public class SearchUtils {
    private SearchUtils() {
    }

    public static void showCompletionPopup(Project project,
                                           JComponent toolbarComponent,
                                           String[] elements,
                                           String title,
                                           JTextComponent textField,
                                           String ad) {
        SimpleListPopupStepBuilder<String> stepBuilder = SimpleListPopupStepBuilder.newBuilder(List.of(elements));
        stepBuilder.withFinishAction(selectedValue -> {
            if (selectedValue != null) {
                textField.setText(selectedValue);
            }
        });
        stepBuilder.withTitle(LocalizeValue.ofNullable(title));

        ListPopup popup = JBPopupFactory.getInstance().createListPopup(project, stepBuilder.build());
        popup.setRequestFocus(false);

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

    public static void setSmallerFont(JComponent component) {
        if (Platform.current().os().isMac()) {
            component.setFont(JBUI.Fonts.smallFont());
        }
    }

    public static void setSmallerFontForChildren(JComponent component) {
        for (Component c : component.getComponents()) {
            if (c instanceof JComponent) {
                setSmallerFont((JComponent) c);
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
        return action == null ? List.of() : List.of(action.getShortcutSet().getShortcuts());
    }
}

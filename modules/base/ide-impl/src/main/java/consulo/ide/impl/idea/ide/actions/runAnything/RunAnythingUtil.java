// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything;

import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.ui.SeparatorComponent;
import consulo.ide.internal.RunAnythingCache;
import consulo.ide.runAnything.RunAnythingProvider;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.ex.Gray;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.ScrollingUtil;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Couple;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RunAnythingUtil {
    public static final Logger LOG = Logger.getInstance(RunAnythingUtil.class);
    public static final String SHIFT_SHORTCUT_TEXT = KeymapUtil.getShortcutText(KeyboardShortcut.fromString("SHIFT"));
    public static final String SHIFT_BACK_SPACE = KeymapUtil.getShortcutText(KeyboardShortcut.fromString("shift BACK_SPACE"));
    public static final String PRESSED_ALT = KeymapUtil.getShortcutText(KeyboardShortcut.fromString("pressed ALT"));
    private static final Key<Collection<Couple<String>>> RUN_ANYTHING_WRAPPED_COMMANDS = Key.create("RUN_ANYTHING_WRAPPED_COMMANDS");

    static Font getTitleFont() {
        return UIUtil.getLabelFont().deriveFont(UIUtil.getFontSize(UIUtil.FontSize.SMALL));
    }

    static JComponent createTitle(@Nonnull String titleText, @Nonnull Color background) {
        JLabel titleLabel = new JLabel(StringUtil.capitalizeWords(titleText, true));
        titleLabel.setFont(getTitleFont());
        titleLabel.setForeground(UIUtil.getLabelDisabledForeground());

        SeparatorComponent separatorComponent = new SeparatorComponent(
            titleLabel.getPreferredSize().height / 2,
            new JBColor(Gray._220, Gray._80),
            null
        );

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(titleLabel, BorderLayout.WEST);
        panel.add(separatorComponent, BorderLayout.CENTER);

        panel.setBorder(JBUI.Borders.empty(3));
        titleLabel.setBorder(JBUI.Borders.emptyRight(3));

        panel.setBackground(background);
        return panel;
    }

    static void jumpNextGroup(boolean forward, JBList list) {
        int index = list.getSelectedIndex();
        RunAnythingSearchListModel model = getSearchingModel(list);
        if (model != null && index >= 0) {
            int newIndex = forward ? model.next(index) : model.prev(index);
            list.setSelectedIndex(newIndex);
            int more = model.next(newIndex) - 1;
            if (more < newIndex) {
                more = list.getItemsCount() - 1;
            }
            ScrollingUtil.ensureIndexIsVisible(list, more, forward ? 1 : -1);
            ScrollingUtil.ensureIndexIsVisible(list, newIndex, forward ? 1 : -1);
        }
    }

    @Nonnull
    public static Collection<Couple<String>> getOrCreateWrappedCommands(@Nonnull Project project) {
        Collection<Couple<String>> list = project.getUserData(RUN_ANYTHING_WRAPPED_COMMANDS);
        if (list == null) {
            list = new ArrayList<>();
            project.putUserData(RUN_ANYTHING_WRAPPED_COMMANDS, list);
        }
        return list;
    }

    @Nonnull
    @Deprecated
    public static Project fetchProject(@Nonnull DataContext dataContext) {
        return dataContext.getRequiredData(Project.KEY);
    }

    public static boolean executeMatched(@Nonnull DataContext dataContext, @Nonnull String pattern) {
        List<String> commands = ((RunAnythingCacheImpl) RunAnythingCache.getInstance(fetchProject(dataContext))).getState().getCommands();

        Module module = dataContext.getData(Module.KEY);
        if (module == null) {
            LOG.info("RunAnything: module hasn't been found, command will be executed in context of 'null' module.");
        }

        return Application.get().getExtensionPoint(RunAnythingProvider.class).anyMatchSafe(provider -> {
            Object value = provider.findMatchingValue(dataContext, pattern);
            if (value != null) {
                //noinspection unchecked
                provider.execute(dataContext, value);
                commands.remove(pattern);
                commands.add(pattern);
                return true;
            }
            return false;
        });
    }

    @Nullable
    public static RunAnythingSearchListModel getSearchingModel(@Nonnull JBList list) {
        return list.getModel() instanceof RunAnythingSearchListModel model ? model : null;
    }
}
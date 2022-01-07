// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.LangDataKeys;
import consulo.logging.Logger;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.SeparatorComponent;
import com.intellij.ui.components.JBList;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RunAnythingUtil {
  public static final Logger LOG = Logger.getInstance(RunAnythingUtil.class);
  public static final String SHIFT_SHORTCUT_TEXT = KeymapUtil.getShortcutText(KeyboardShortcut.fromString(("SHIFT")));
  public static final String AD_DEBUG_TEXT = IdeBundle.message("run.anything.ad.run.with.debug", SHIFT_SHORTCUT_TEXT);
  public static final String AD_DELETE_COMMAND_TEXT = IdeBundle.message("run.anything.ad.command.delete", KeymapUtil.getShortcutText(KeyboardShortcut.fromString("shift BACK_SPACE")));
  public static final String AD_CONTEXT_TEXT = IdeBundle.message("run.anything.ad.run.in.context", KeymapUtil.getShortcutText(KeyboardShortcut.fromString("pressed ALT")));
  private static final Key<Collection<Pair<String, String>>> RUN_ANYTHING_WRAPPED_COMMANDS = Key.create("RUN_ANYTHING_WRAPPED_COMMANDS");

  static Font getTitleFont() {
    return UIUtil.getLabelFont().deriveFont(UIUtil.getFontSize(UIUtil.FontSize.SMALL));
  }

  static JComponent createTitle(@Nonnull String titleText, @Nonnull Color background) {
    JLabel titleLabel = new JLabel(StringUtil.capitalizeWords(titleText, true));
    titleLabel.setFont(getTitleFont());
    titleLabel.setForeground(UIUtil.getLabelDisabledForeground());

    SeparatorComponent separatorComponent = new SeparatorComponent(titleLabel.getPreferredSize().height / 2, new JBColor(Gray._220, Gray._80), null);

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(titleLabel, BorderLayout.WEST);
    panel.add(separatorComponent, BorderLayout.CENTER);

    panel.setBorder(JBUI.Borders.empty(3));
    titleLabel.setBorder(JBUI.Borders.emptyRight(3));

    panel.setBackground(background);
    return panel;
  }

  static void jumpNextGroup(boolean forward, JBList list) {
    final int index = list.getSelectedIndex();
    final RunAnythingSearchListModel model = getSearchingModel(list);
    if (model != null && index >= 0) {
      final int newIndex = forward ? model.next(index) : model.prev(index);
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
  public static Collection<Pair<String, String>> getOrCreateWrappedCommands(@Nonnull Project project) {
    Collection<Pair<String, String>> list = project.getUserData(RUN_ANYTHING_WRAPPED_COMMANDS);
    if (list == null) {
      list = new ArrayList<>();
      project.putUserData(RUN_ANYTHING_WRAPPED_COMMANDS, list);
    }
    return list;
  }

  @Nonnull
  public static Project fetchProject(@Nonnull DataContext dataContext) {
    return ObjectUtils.assertNotNull(dataContext.getData(CommonDataKeys.PROJECT));
  }

  public static void executeMatched(@Nonnull DataContext dataContext, @Nonnull String pattern) {
    List<String> commands = RunAnythingCache.getInstance(fetchProject(dataContext)).getState().getCommands();

    Module module = dataContext.getData(LangDataKeys.MODULE);
    if (module == null) {
      LOG.info("RunAnything: module hasn't been found, command will be executed in context of 'null' module.");
    }

    for (RunAnythingProvider provider : RunAnythingProvider.EP_NAME.getExtensions()) {
      Object value = provider.findMatchingValue(dataContext, pattern);
      if (value != null) {
        //noinspection unchecked
        provider.execute(dataContext, value);
        commands.remove(pattern);
        commands.add(pattern);
        break;
      }
    }
  }

  @Nullable
  public static RunAnythingSearchListModel getSearchingModel(@Nonnull JBList list) {
    ListModel model = list.getModel();
    return model instanceof RunAnythingSearchListModel ? (RunAnythingSearchListModel)model : null;
  }
}
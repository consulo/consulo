// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.actionSystem.impl;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.UIUtil;
import consulo.actionSystem.ex.TopApplicationMenuUtil;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.concurrency.CancellablePromise;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public class Utils {
  private static final Logger LOG = Logger.getInstance(Utils.class);
  public static final LocalizeValue NOTHING_HERE = LocalizeValue.localizeTODO("Nothing here");
  public static final AnAction EMPTY_MENU_FILLER = new AnAction(NOTHING_HERE) {

    {
      getTemplatePresentation().setEnabled(false);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
      e.getPresentation().setEnabled(false);
    }
  };

  /**
   * @return actions from the given and nested non-popup groups that are visible after updating
   */
  public static List<AnAction> expandActionGroup(boolean isInModalContext, @Nonnull ActionGroup group, PresentationFactory presentationFactory, @Nonnull DataContext context, String place) {
    return expandActionGroup(isInModalContext, group, presentationFactory, context, place, null);
  }

  public static List<AnAction> expandActionGroup(boolean isInModalContext,
                                                 @Nonnull ActionGroup group,
                                                 PresentationFactory presentationFactory,
                                                 @Nonnull DataContext context,
                                                 String place,
                                                 ActionGroupVisitor visitor) {
    return new ActionUpdater(isInModalContext, presentationFactory, context, place, false, false, visitor).expandActionGroup(group, group instanceof CompactActionGroup);
  }

  public static CancellablePromise<List<AnAction>> expandActionGroupAsync(boolean isInModalContext,
                                                                          @Nonnull ActionGroup group,
                                                                          PresentationFactory presentationFactory,
                                                                          @Nonnull DataContext context,
                                                                          String place,
                                                                          @Nullable Utils.ActionGroupVisitor visitor) {
    if (!(context instanceof AsyncDataContext)) context = DataManager.getInstance().createAsyncDataContext(context);
    return new ActionUpdater(isInModalContext, presentationFactory, context, place, false, false, visitor).expandActionGroupAsync(group, group instanceof CompactActionGroup);
  }

  public static List<AnAction> expandActionGroupWithTimeout(boolean isInModalContext,
                                                            @Nonnull ActionGroup group,
                                                            PresentationFactory presentationFactory,
                                                            @Nonnull DataContext context,
                                                            String place,
                                                            ActionGroupVisitor visitor,
                                                            int timeoutMs) {
    return new ActionUpdater(isInModalContext, presentationFactory, context, place, false, false, visitor).expandActionGroupWithTimeout(group, group instanceof CompactActionGroup, timeoutMs);
  }

  private static final boolean DO_FULL_EXPAND = Boolean.getBoolean("actionSystem.use.full.group.expand"); // for tests and debug

  public static void fillMenu(@Nonnull ActionGroup group,
                       JComponent component,
                       boolean enableMnemonics,
                       PresentationFactory presentationFactory,
                       @Nonnull DataContext context,
                       String place,
                       boolean isWindowMenu,
                       boolean isInModalContext,
                       boolean useDarkIcons) {
    final boolean checked = group instanceof CheckedActionGroup;

    ActionUpdater updater = new ActionUpdater(isInModalContext, presentationFactory, context, place, true, false);
    List<AnAction> list = DO_FULL_EXPAND ? updater.expandActionGroupFull(group, group instanceof CompactActionGroup) : updater.expandActionGroupWithTimeout(group, group instanceof CompactActionGroup);

    final boolean fixMacScreenMenu = TopApplicationMenuUtil.isMacSystemMenu && isWindowMenu && Registry.is("actionSystem.mac.screenMenuNotUpdatedFix");
    final ArrayList<Component> children = new ArrayList<>();

    for (int i = 0, size = list.size(); i < size; i++) {
      final AnAction action = list.get(i);
      Presentation presentation = presentationFactory.getPresentation(action);
      if (!(action instanceof AnSeparator) && presentation.isVisible() && StringUtil.isEmpty(presentation.getText())) {
        String message = "Skipping empty menu item for action " + action + " of " + action.getClass();
        if (action.getTemplatePresentation().getText() == null) {
          message += ". Please specify some default action text in plugin.xml or action constructor";
        }
        LOG.warn(message);
        continue;
      }

      if (action instanceof AnSeparator) {
        final LocalizeValue textValue = ((AnSeparator)action).getTextValue();
        if (textValue != LocalizeValue.empty() || (i > 0 && i < size - 1)) {
          JPopupMenu.Separator separator = new JPopupMenu.Separator() {
            private final JMenuItem myMenu = textValue != LocalizeValue.empty() ? new JMenuItem(textValue.getValue()) : null;

            @Override
            public void doLayout() {
              super.doLayout();
              if (myMenu != null) {
                myMenu.setBounds(getBounds());
              }
            }

            @Override
            protected void paintComponent(Graphics g) {
              if (UIUtil.isUnderBuildInLaF()) {
                g.setColor(component.getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
              }
              if (myMenu != null) {
                myMenu.paint(g);
              }
              else {
                super.paintComponent(g);
              }
            }

            @Override
            public Dimension getPreferredSize() {
              return myMenu != null ? myMenu.getPreferredSize() : super.getPreferredSize();
            }
          };
          component.add(separator);
          children.add(separator);
        }
      }
      else if (action instanceof ActionGroup && !Boolean.TRUE.equals(presentation.getClientProperty("actionGroup.perform.only"))) {
        ActionMenu menu = new ActionMenu(context, place, (ActionGroup)action, presentationFactory, enableMnemonics, useDarkIcons);
        component.add(menu);
        children.add(menu);
      }
      else {
        final ActionMenuItem each = new ActionMenuItem(action, presentation, place, context, enableMnemonics, !fixMacScreenMenu, checked, useDarkIcons);
        component.add(each);
        children.add(each);
      }
    }

    if (list.isEmpty()) {
      final ActionMenuItem each =
              new ActionMenuItem(EMPTY_MENU_FILLER, presentationFactory.getPresentation(EMPTY_MENU_FILLER), place, context, enableMnemonics, !fixMacScreenMenu, checked, useDarkIcons);
      component.add(each);
      children.add(each);
    }

    if (fixMacScreenMenu) {
      //noinspection SSBasedInspection
      SwingUtilities.invokeLater(() -> {
        for (Component each : children) {
          if (each.getParent() != null && each instanceof ActionMenuItem) {
            ((ActionMenuItem)each).prepare();
          }
        }
      });
    }
  }

  public interface ActionGroupVisitor {
    void begin();

    boolean enterNode(@Nonnull ActionGroup groupNode);

    void visitLeaf(@Nonnull AnAction act);

    void leaveNode();

    Component getCustomComponent(@Nonnull AnAction action);

    boolean beginUpdate(@Nonnull AnAction action, AnActionEvent e);

    void endUpdate(@Nonnull AnAction action);
  }
}

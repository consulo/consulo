// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.action;

import consulo.application.util.registry.Registry;
import consulo.dataContext.DataContext;
import consulo.ide.impl.actionSystem.ex.TopApplicationMenuUtil;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionUpdater;
import consulo.ide.impl.idea.ui.popup.NothingHereAction;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.ex.action.*;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

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

    ActionUpdater updater =
      new ActionUpdater(ActionManager.getInstance(), isInModalContext, presentationFactory, context, place, true, false);
    List<AnAction> list =
      DO_FULL_EXPAND ? updater.expandActionGroupFull(group, group instanceof CompactActionGroup) : updater.expandActionGroupWithTimeout(
        group,
        group instanceof CompactActionGroup);

    final boolean fixMacScreenMenu =
      TopApplicationMenuUtil.isMacSystemMenu && isWindowMenu && Registry.is("actionSystem.mac.screenMenuNotUpdatedFix");
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
        final ActionMenuItem each =
          new ActionMenuItem(action, presentation, place, context, enableMnemonics, !fixMacScreenMenu, checked, useDarkIcons);
        component.add(each);
        children.add(each);
      }
    }

    if (list.isEmpty()) {
      final ActionMenuItem each =
        new ActionMenuItem(NothingHereAction.INSTANCE,
                           presentationFactory.getPresentation(NothingHereAction.INSTANCE),
                           place,
                           context,
                           enableMnemonics,
                           !fixMacScreenMenu,
                           checked,
                           useDarkIcons);
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
}

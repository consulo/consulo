/*
 * Copyright 2013-2022 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ui.ex.action.util;

import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.*;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 09-Mar-22
 */
public class ActionUtil {
  public static List<AnAction> getActions(@Nonnull JComponent component) {
    return ObjectUtil.notNull((List<AnAction>)component.getClientProperty(AnAction.ACTIONS_KEY), Collections.emptyList());
  }

  public static void clearActions(@Nonnull JComponent component) {
    component.putClientProperty(AnAction.ACTIONS_KEY, null);
  }

  public static void copyRegisteredShortcuts(@Nonnull JComponent to, @Nonnull JComponent from) {
    for (AnAction anAction : getActions(from)) {
      anAction.registerCustomShortcutSet(anAction.getShortcutSet(), to);
    }
  }

  /**
   * Convenience method for merging not null properties from a registered action
   *
   * @param action   action to merge to
   * @param actionId action id to merge from
   */
  public static AnAction mergeFrom(@Nonnull AnAction action, @Nonnull String actionId) {
    //noinspection UnnecessaryLocalVariable
    AnAction a1 = action;
    AnAction a2 = ActionManager.getInstance().getAction(actionId);
    Presentation p1 = a1.getTemplatePresentation();
    Presentation p2 = a2.getTemplatePresentation();
    p1.setIcon(ObjectUtil.chooseNotNull(p1.getIcon(), p2.getIcon()));
    p1.setDisabledIcon(ObjectUtil.chooseNotNull(p1.getDisabledIcon(), p2.getDisabledIcon()));
    p1.setSelectedIcon(ObjectUtil.chooseNotNull(p1.getSelectedIcon(), p2.getSelectedIcon()));
    p1.setHoveredIcon(ObjectUtil.chooseNotNull(p1.getHoveredIcon(), p2.getHoveredIcon()));
    if (StringUtil.isEmpty(p1.getText())) {
      p1.setTextValue(p2.getTextValue());
    }
    p1.setDescriptionValue(p1.getDescriptionValue() == LocalizeValue.empty() ? p2.getDescriptionValue() : p1.getDescriptionValue());
    ShortcutSet ss1 = a1.getShortcutSet();
    if (ss1 == null || ss1 == CustomShortcutSet.EMPTY) {
      a1.copyShortcutFrom(a2);
    }
    return a1;
  }

  public static void registerForEveryKeyboardShortcut(@Nonnull JComponent component, @Nonnull ActionListener action, @Nonnull ShortcutSet shortcuts) {
    for (Shortcut shortcut : shortcuts.getShortcuts()) {
      if (shortcut instanceof KeyboardShortcut) {
        KeyboardShortcut ks = (KeyboardShortcut)shortcut;
        KeyStroke first = ks.getFirstKeyStroke();
        KeyStroke second = ks.getSecondKeyStroke();
        if (second == null) {
          component.registerKeyboardAction(action, first, JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
      }
    }
  }
}

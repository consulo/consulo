/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.actionSystem.ex;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.util.text.TextWithMnemonic;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ObjectUtils;
import com.intellij.util.PausesStat;
import com.intellij.util.ui.UIUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class ActionUtil {
  private static final Logger LOG = Logger.getInstance(ActionUtil.class);
  @NonNls
  private static final String WAS_ENABLED_BEFORE_DUMB = "WAS_ENABLED_BEFORE_DUMB";
  @NonNls
  public static final String WOULD_BE_ENABLED_IF_NOT_DUMB_MODE = "WOULD_BE_ENABLED_IF_NOT_DUMB_MODE";
  @NonNls
  private static final String WOULD_BE_VISIBLE_IF_NOT_DUMB_MODE = "WOULD_BE_VISIBLE_IF_NOT_DUMB_MODE";

  private ActionUtil() {
  }

  public static boolean recursiveContainsAction(@Nonnull ActionGroup group, @Nonnull AnAction action) {
    return anyActionFromGroupMatches(group, true, Predicate.isEqual(action));
  }

  public static boolean anyActionFromGroupMatches(@Nonnull ActionGroup group, boolean processPopupSubGroups, @Nonnull Predicate<? super AnAction> condition) {
    for (AnAction child : group.getChildren(null)) {
      if (condition.test(child)) return true;
      if (child instanceof ActionGroup) {
        ActionGroup childGroup = (ActionGroup)child;
        if ((processPopupSubGroups || !childGroup.isPopup()) && anyActionFromGroupMatches(childGroup, processPopupSubGroups, condition)) {
          return true;
        }
      }
    }
    return false;
  }

  public static void recursiveRegisterShortcutSet(@Nonnull ActionGroup group, @Nonnull JComponent component, @Nullable Disposable parentDisposable) {
    for (AnAction action : group.getChildren(null)) {
      if (action instanceof ActionGroup) {
        recursiveRegisterShortcutSet((ActionGroup)action, component, parentDisposable);
      }
      action.registerCustomShortcutSet(component, parentDisposable);
    }
  }

  public static void showDumbModeWarning(@Nonnull AnActionEvent... events) {
    Project project = null;
    List<String> actionNames = new ArrayList<>();
    for (final AnActionEvent event : events) {
      final String s = event.getPresentation().getText();
      if (StringUtil.isNotEmpty(s)) {
        actionNames.add(s);
      }

      final Project _project = event.getProject();
      if (_project != null && project == null) {
        project = _project;
      }
    }

    if (project == null) {
      return;
    }

    DumbService.getInstance(project).showDumbModeNotification(getActionUnavailableMessage(actionNames));
  }

  @Nonnull
  private static String getActionUnavailableMessage(@Nonnull List<String> actionNames) {
    String message;
    final String beAvailableUntil = " available while " + ApplicationNamesInfo.getInstance().getProductName() + " is updating indices";
    if (actionNames.isEmpty()) {
      message = "This action is not" + beAvailableUntil;
    }
    else if (actionNames.size() == 1) {
      message = "'" + actionNames.get(0) + "' action is not" + beAvailableUntil;
    }
    else {
      message = "None of the following actions are" + beAvailableUntil + ": " + StringUtil.join(actionNames, ", ");
    }
    return message;
  }

  @Nonnull
  public static String getUnavailableMessage(@Nonnull String action, boolean plural) {
    return action + (plural ? " are" : " is") + " not available while " + ApplicationNamesInfo.getInstance().getProductName() + " is updating indices";
  }

  private static int insidePerformDumbAwareUpdate;

  @Deprecated
  // Use #performDumbAwareUpdate with isModalContext instead
  public static boolean performDumbAwareUpdate(@Nonnull AnAction action, @Nonnull AnActionEvent e, boolean beforeActionPerformed) {
    return performDumbAwareUpdate(false, action, e, beforeActionPerformed);
  }

  /**
   * @param action                action
   * @param e                     action event
   * @param beforeActionPerformed whether to call
   *                              {@link AnAction#beforeActionPerformedUpdate(AnActionEvent)}
   *                              or
   *                              {@link AnAction#update(AnActionEvent)}
   * @return true if update tried to access indices in dumb mode
   */
  public static boolean performDumbAwareUpdate(boolean isInModalContext, @Nonnull AnAction action, @Nonnull AnActionEvent e, boolean beforeActionPerformed) {
    final Presentation presentation = e.getPresentation();
    final Boolean wasEnabledBefore = (Boolean)presentation.getClientProperty(WAS_ENABLED_BEFORE_DUMB);
    final boolean dumbMode = isDumbMode(e.getProject());
    if (wasEnabledBefore != null && !dumbMode) {
      presentation.putClientProperty(WAS_ENABLED_BEFORE_DUMB, null);
      presentation.setEnabled(wasEnabledBefore.booleanValue());
      presentation.setVisible(true);
    }
    final boolean enabledBeforeUpdate = presentation.isEnabled();

    final boolean notAllowed = dumbMode && !action.isDumbAware() || (Registry.is("actionSystem.honor.modal.context") && isInModalContext && !action.isEnabledInModalContext());

    if (insidePerformDumbAwareUpdate++ == 0) {
      ActionPauses.STAT.started();
    }
    try {
      boolean enabled = checkModuleExtensions(action, e);
      //FIXME [VISTALL] hack
      if (enabled && action instanceof ActionGroup) {
        presentation.setEnabledAndVisible(true);
      }
      else if (!enabled) {
        presentation.setEnabledAndVisible(enabled);
      }

      if (beforeActionPerformed) {
        action.beforeActionPerformedUpdate(e);
      }
      else {
        action.update(e);
      }
      presentation.putClientProperty(WOULD_BE_ENABLED_IF_NOT_DUMB_MODE, notAllowed && presentation.isEnabled());
      presentation.putClientProperty(WOULD_BE_VISIBLE_IF_NOT_DUMB_MODE, notAllowed && presentation.isVisible());
    }
    catch (IndexNotReadyException e1) {
      if (notAllowed) {
        return true;
      }
      throw e1;
    }
    finally {
      if (--insidePerformDumbAwareUpdate == 0) {
        ActionPauses.STAT.finished(presentation.getText() + " action update (" + action.getClass() + ")");
      }
      if (notAllowed) {
        if (wasEnabledBefore == null) {
          presentation.putClientProperty(WAS_ENABLED_BEFORE_DUMB, enabledBeforeUpdate);
        }
        presentation.setEnabled(false);
      }
    }

    return false;
  }

  public static class ActionPauses {
    public static final PausesStat STAT = new PausesStat("AnAction.update()");
  }


  @RequiredReadAction
  private static boolean checkModuleExtensions(AnAction action, AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return true;
    }
    String[] moduleExtensionIds = action.getModuleExtensionIds();
    if (moduleExtensionIds.length == 0) {
      return true;
    }
    if (action.isCanUseProjectAsDefault()) {
      for (Module temp : ModuleManager.getInstance(project).getModules()) {
        boolean b = checkModuleForModuleExtensions(temp, moduleExtensionIds);
        if (b) {
          return true;
        }
      }
    }
    else {
      Module module = e.getData(CommonDataKeys.MODULE);
      if (module != null) {
        boolean result = checkModuleForModuleExtensions(module, moduleExtensionIds);
        if (result) {
          return true;
        }
      }

      VirtualFile[] virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
      if (virtualFiles != null) {
        for (VirtualFile virtualFile : virtualFiles) {
          Module moduleForFile = ModuleUtilCore.findModuleForFile(virtualFile, project);
          if (moduleForFile != null) {
            boolean b = checkModuleForModuleExtensions(moduleForFile, moduleExtensionIds);
            if (b) {
              return true;
            }
          }
        }
      }
    }
    return false;

  }

  private static boolean checkModuleForModuleExtensions(@Nullable Module module, @Nonnull String[] array) {
    if (module == null) {
      return false;
    }
    for (String moduleExtensionId : array) {
      if (ModuleUtilCore.getExtension(module, moduleExtensionId) != null) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return whether a dumb mode is in progress for the passed project or, if the argument is null, for any open project.
   * @see DumbService
   */
  public static boolean isDumbMode(@Nullable Project project) {
    if (project != null) {
      return DumbService.getInstance(project).isDumb();
    }
    for (Project proj : ProjectManager.getInstance().getOpenProjects()) {
      if (DumbService.getInstance(proj).isDumb()) {
        return true;
      }
    }
    return false;

  }

  public static boolean lastUpdateAndCheckDumb(AnAction action, AnActionEvent e, boolean visibilityMatters) {
    performDumbAwareUpdate(false, action, e, true);

    final Project project = e.getProject();
    if (project != null && DumbService.getInstance(project).isDumb() && !action.isDumbAware()) {
      if (Boolean.FALSE.equals(e.getPresentation().getClientProperty(WOULD_BE_ENABLED_IF_NOT_DUMB_MODE))) {
        return false;
      }
      if (visibilityMatters && Boolean.FALSE.equals(e.getPresentation().getClientProperty(WOULD_BE_VISIBLE_IF_NOT_DUMB_MODE))) {
        return false;
      }

      showDumbModeWarning(e);
      return false;
    }

    if (!e.getPresentation().isEnabled()) {
      return false;
    }
    if (visibilityMatters && !e.getPresentation().isVisible()) {
      return false;
    }

    return true;
  }

  public static void performActionDumbAwareWithCallbacks(@Nonnull AnAction action, @Nonnull AnActionEvent e, @Nonnull DataContext context) {
    final ActionManagerEx manager = ActionManagerEx.getInstanceEx();
    manager.fireBeforeActionPerformed(action, context, e);
    performActionDumbAware(action, e);
    manager.fireAfterActionPerformed(action, context, e);
  }

  public static void performActionDumbAware(AnAction action, AnActionEvent e) {
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        try {
          action.actionPerformed(e);
        }
        catch (IndexNotReadyException e1) {
          showDumbModeWarning(e);
        }
      }

      @Override
      public String toString() {
        return action + " of " + action.getClass();
      }
    };

    if (action.startInTransaction()) {
      TransactionGuard.getInstance().submitTransactionAndWait(runnable);
    }
    else {
      runnable.run();
    }
  }

  @Nonnull
  public static List<AnAction> getActions(@Nonnull JComponent component) {
    return ObjectUtils.notNull(UIUtil.getClientProperty(component, AnAction.ACTIONS_KEY), Collections.emptyList());
  }

  public static void clearActions(@Nonnull JComponent component) {
    UIUtil.putClientProperty(component, AnAction.ACTIONS_KEY, null);
  }

  public static void copyRegisteredShortcuts(@Nonnull JComponent to, @Nonnull JComponent from) {
    for (AnAction anAction : getActions(from)) {
      anAction.registerCustomShortcutSet(anAction.getShortcutSet(), to);
    }
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

  /**
   * Convenience method for copying properties from a registered action
   *
   * @param actionId action id
   */
  public static AnAction copyFrom(@Nonnull AnAction action, @Nonnull String actionId) {
    action.copyFrom(ActionManager.getInstance().getAction(actionId));
    return action;
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
    p1.setIcon(ObjectUtils.chooseNotNull(p1.getIcon(), p2.getIcon()));
    p1.setDisabledIcon(ObjectUtils.chooseNotNull(p1.getDisabledIcon(), p2.getDisabledIcon()));
    p1.setSelectedIcon(ObjectUtils.chooseNotNull(p1.getSelectedIcon(), p2.getSelectedIcon()));
    p1.setHoveredIcon(ObjectUtils.chooseNotNull(p1.getHoveredIcon(), p2.getHoveredIcon()));
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

  public static void invokeAction(@Nonnull AnAction action, @Nonnull DataContext dataContext, @Nonnull String place, @Nullable InputEvent inputEvent, @Nullable Runnable onDone) {
    Presentation presentation = action.getTemplatePresentation().clone();
    AnActionEvent event = new AnActionEvent(inputEvent, dataContext, place, presentation, ActionManager.getInstance(), 0);
    performDumbAwareUpdate(false, action, event, true);
    final ActionManagerEx manager = ActionManagerEx.getInstanceEx();
    if (event.getPresentation().isEnabled() && event.getPresentation().isVisible()) {
      manager.fireBeforeActionPerformed(action, dataContext, event);
      performActionDumbAware(action, event);
      if (onDone != null) {
        onDone.run();
      }
      manager.fireAfterActionPerformed(action, dataContext, event);
    }
  }

  public static void invokeAction(@Nonnull AnAction action, @Nonnull Component component, @Nonnull String place, @Nullable InputEvent inputEvent, @Nullable Runnable onDone) {
    invokeAction(action, DataManager.getInstance().getDataContext(component), place, inputEvent, onDone);
  }

  @Nonnull
  public static ActionListener createActionListener(@Nonnull String actionId, @Nonnull Component component, @Nonnull String place) {
    return e -> {
      AnAction action = ActionManager.getInstance().getAction(actionId);
      if (action == null) {
        LOG.warn("Can not find action by id " + actionId);
        return;
      }
      invokeAction(action, component, place, null, null);
    };
  }

  @Nonnull
  public static AnActionEvent createEmptyEvent() {
    return AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, null, DataContext.EMPTY_CONTEXT);
  }

  public static void sortAlphabetically(@Nonnull List<? extends AnAction> list) {
    list.sort((o1, o2) -> Comparing.compare(o1.getTemplateText(), o2.getTemplateText()));
  }

  /**
   * Tries to find an 'action' and 'target action' by text and put the 'action' just before of after the 'target action'
   */
  public static void moveActionTo(@Nonnull List<AnAction> list, @Nonnull String actionText, @Nonnull String targetActionText, boolean before) {
    if (Comparing.equal(actionText, targetActionText)) {
      return;
    }

    int actionIndex = -1;
    int targetIndex = -1;
    for (int i = 0; i < list.size(); i++) {
      AnAction action = list.get(i);
      if (actionIndex == -1 && Comparing.equal(actionText, action.getTemplateText())) actionIndex = i;
      if (targetIndex == -1 && Comparing.equal(targetActionText, action.getTemplateText())) targetIndex = i;
      if (actionIndex != -1 && targetIndex != -1) {
        if (actionIndex < targetIndex) targetIndex--;
        AnAction anAction = list.remove(actionIndex);
        list.add(before ? Math.max(0, targetIndex) : targetIndex + 1, anAction);
        return;
      }
    }
  }

  @Nullable
  public static ShortcutSet getMnemonicAsShortcut(@Nonnull AnAction action) {
    int mnemonic = KeyEvent.getExtendedKeyCodeForChar(TextWithMnemonic.parse(action.getTemplatePresentation().getTextWithMnemonic()).getMnemonic());
    if (mnemonic != KeyEvent.VK_UNDEFINED) {
      KeyboardShortcut ctrlAltShortcut = new KeyboardShortcut(KeyStroke.getKeyStroke(mnemonic, InputEvent.ALT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK), null);
      KeyboardShortcut altShortcut = new KeyboardShortcut(KeyStroke.getKeyStroke(mnemonic, InputEvent.ALT_DOWN_MASK), null);
      CustomShortcutSet shortcutSet;
      if (SystemInfo.isMac) {
        if (Registry.is("ide.mac.alt.mnemonic.without.ctrl")) {
          shortcutSet = new CustomShortcutSet(ctrlAltShortcut, altShortcut);
        }
        else {
          shortcutSet = new CustomShortcutSet(ctrlAltShortcut);
        }
      }
      else {
        shortcutSet = new CustomShortcutSet(altShortcut);
      }
      return shortcutSet;
    }
    return null;
  }

  @Nonnull
  public static ActionListener createActionListener(@Nonnull AnAction action, @Nonnull Component component, @Nonnull String place) {
    return e -> invokeAction(action, component, place, null, null);
  }
}

/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.openapi.actionSystem;

import com.intellij.openapi.actionSystem.ex.ActionUtil;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * This class purpose is to reserve action-id in a plugin.xml so the action appears in Keymap.
 * Then Keymap assignments can be used for non-registered actions created on runtime.
 *
 * Another usage is to override (hide) already registered actions by means of plugin.xml, see {@link EmptyActionGroup} as well.
 *
 * @see EmptyActionGroup
 *
 * @author Gregory.Shrago
 * @author Konstantin Bulenkov
 */
public final class EmptyAction extends AnAction {
  private boolean myEnabled;

  public EmptyAction() {
  }

  public EmptyAction(boolean enabled) {
    myEnabled = enabled;
  }

  public EmptyAction(@Nullable String text, @Nullable String description, @Nullable Image icon) {
    super(text, description, icon);
  }

  public EmptyAction(@Nonnull LocalizeValue text) {
    super(text);
  }

  public EmptyAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
    super(text, description);
  }

  public EmptyAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
    super(text, description, icon);
  }

  public static AnAction createEmptyAction(@Nullable String name, @Nullable Image icon, boolean alwaysEnabled) {
    final EmptyAction emptyAction = new EmptyAction(name, null, icon);
    emptyAction.myEnabled = alwaysEnabled;
    return emptyAction;
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
  }

  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(myEnabled);
  }

  public static void setupAction(@Nonnull AnAction action, @Nonnull String id, @Nullable JComponent component) {
    ActionUtil.mergeFrom(action, id).registerCustomShortcutSet(component, null);
  }

  public static void registerActionShortcuts(@Nonnull JComponent component, @Nonnull JComponent fromComponent) {
    ActionUtil.copyRegisteredShortcuts(component, fromComponent);
  }

  /**
   * Registers global action on a component with a custom shortcut set.
   * <p>
   * ActionManager.getInstance().getAction(id).registerCustomShortcutSet(shortcutSet, component) shouldn't be used directly,
   * because it will erase shortcuts, assigned to this action in keymap.
   */
  @Nonnull
  public static AnAction registerWithShortcutSet(@Nonnull String id, @Nonnull ShortcutSet shortcutSet, @Nonnull JComponent component) {
    AnAction newAction = wrap(ActionManager.getInstance().getAction(id));
    newAction.registerCustomShortcutSet(shortcutSet, component);
    return newAction;
  }

  /**
   * Creates proxy action
   * <p>
   * It allows to alter template presentation and shortcut set without affecting original action,
   */
  public static AnAction wrap(final AnAction action) {
    return action instanceof ActionGroup ?
           new MyDelegatingActionGroup(((ActionGroup)action)) :
           new MyDelegatingAction(action);
  }

  public static class MyDelegatingAction extends AnAction {
    @Nonnull
    private final AnAction myDelegate;

    public MyDelegatingAction(@Nonnull AnAction action) {
      myDelegate = action;
      copyFrom(action);
      setEnabledInModalContext(action.isEnabledInModalContext());
    }

    @Override
    public void update(final AnActionEvent e) {
      myDelegate.update(e);
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
      myDelegate.actionPerformed(e);
    }

    @Override
    public boolean isDumbAware() {
      return myDelegate.isDumbAware();
    }

    @Override
    public boolean isTransparentUpdate() {
      return myDelegate.isTransparentUpdate();
    }

    @Override
    public boolean isInInjectedContext() {
      return myDelegate.isInInjectedContext();
    }
  }

  public static class MyDelegatingActionGroup extends ActionGroup {
    @Nonnull
    private final ActionGroup myDelegate;

    public MyDelegatingActionGroup(@Nonnull ActionGroup action) {
      myDelegate = action;
      copyFrom(action);
      setEnabledInModalContext(action.isEnabledInModalContext());
    }

    @Nonnull
    public ActionGroup getDelegate() {
      return myDelegate;
    }

    @Override
    public boolean isPopup() {
      return myDelegate.isPopup();
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable final AnActionEvent e) {
      return myDelegate.getChildren(e);
    }

    @Override
    public void update(final AnActionEvent e) {
      myDelegate.update(e);
    }

    @Override
    public boolean canBePerformed(DataContext context) {
      return myDelegate.canBePerformed(context);
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
      myDelegate.actionPerformed(e);
    }

    @Override
    public boolean isDumbAware() {
      return myDelegate.isDumbAware();
    }

    @Override
    public boolean isTransparentUpdate() {
      return myDelegate.isTransparentUpdate();
    }

    @Override
    public boolean isInInjectedContext() {
      return myDelegate.isInInjectedContext();
    }

    @Override
    public boolean hideIfNoVisibleChildren() {
      return myDelegate.hideIfNoVisibleChildren();
    }

    @Override
    public boolean disableIfNoVisibleChildren() {
      return myDelegate.disableIfNoVisibleChildren();
    }
  }

  public static class DelegatingCompactActionGroup extends MyDelegatingActionGroup implements CompactActionGroup {
    public DelegatingCompactActionGroup(@Nonnull ActionGroup action) {
      super(action);
    }
  }
}

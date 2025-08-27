/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ui.ex.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.container.plugin.PluginId;
import consulo.disposer.Disposable;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.util.concurrent.ActionCallback;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.Objects;

/**
 * A manager for actions. Used to register and unregister actions, also
 * contains utility methods to easily fetch action by id and id by action.
 *
 * @see AnAction
 */
@ServiceAPI(value = ComponentScope.APPLICATION)
public abstract class ActionManager {

  /**
   * Fetches the instance of ActionManager implementation.
   */
  @Nonnull
  public static ActionManager getInstance(){
    return ApplicationManager.getApplication().getComponent(ActionManager.class);
  }

  /**
   * Factory method that creates an <code>ActionPopupMenu</code> from the
   * specified group. The specified place is associated with the created popup.
   *
   * @param place Determines the place that will be set for {@link AnActionEvent} passed
   *  when an action from the group is either performed or updated
   *  See {@link consulo.ide.impl.idea.openapi.actionSystem.ActionPlaces}
   *
   * @param group Group from which the actions for the menu are taken.
   *
   * @return An instance of <code>ActionPopupMenu</code>
   */
  public abstract ActionPopupMenu createActionPopupMenu(String place, @Nonnull ActionGroup group);

  /**
   * @see ActionToolbarFactory
   */
  public abstract ActionToolbar createActionToolbar(String place, ActionGroup group, boolean horizontal);

  /**
   * Returns action associated with the specified actionId.
   *
   * @param actionId Id of the registered action
   *
   * @return Action associated with the specified actionId, <code>null</code> if
   *  there is no actions associated with the speicified actionId
   *
   * @exception java.lang.IllegalArgumentException if <code>actionId</code> is <code>null</code>
   *
   * @see IdeActions
   */
  public abstract AnAction getAction(@Nonnull String actionId);

  @Nonnull
  @SuppressWarnings("unchecked")
  public <T extends AnAction> T getAction(@Nonnull Class<T> actionClass) {
    ActionImpl annotation = actionClass.getAnnotation(ActionImpl.class);
    if (annotation == null) {
      throw new IllegalArgumentException("Action Class is not annotated by @ActionImpl");
    }
    return Objects.requireNonNull((T) getAction(annotation.id()));
  }

  /**
   * Returns actionId associated with the specified action.
   *
   * @return id associated with the specified action, <code>null</code> if action
   *  is not registered
   *
   * @exception java.lang.IllegalArgumentException if <code>action</code> is <code>null</code>
   */
  public abstract String getId(@Nonnull AnAction action);

  /**
   * Registers the specified action with the specified id. Note that IDEA's keymaps
   * processing deals only with registered actions.
   *
   * @param actionId Id to associate with the action
   * @param action Action to register
   */
  public abstract void registerAction(@Nonnull String actionId, @Nonnull AnAction action);

  /**
   * Registers the specified action with the specified id.
   *
   * @param actionId Id to associate with the action
   * @param action   Action to register
   * @param pluginId Identifier of the plugin owning the action. Used to show the actions in the
   *                 correct place under the "Plugins" node in the "Keymap" settings pane and similar dialogs.
   */
  public abstract void registerAction(@Nonnull String actionId, @Nonnull AnAction action, @Nullable PluginId pluginId);

  public abstract String[] getPluginActions(PluginId pluginId);

  /**
   * Unregisters the action with the specified actionId.
   *
   * @param actionId Id of the action to be unregistered
   */
  public abstract void unregisterAction(@Nonnull String actionId);

  /**
   * Returns the list of all registered action IDs with the specified prefix.
   *
   * @return all action <code>id</code>s which have the specified prefix.
   * @since 5.1
   */
  public abstract String[] getActionIds(@Nonnull String idPrefix);

  /**
   * Checks if the specified action ID represents an action group and not an individual action.
   * Calling this method does not cause instantiation of a specific action class corresponding
   * to the action ID.
   *
   * @param actionId the ID to check.
   * @return true if the ID represents an action group, false otherwise.
   * @since 5.1
   */
  public abstract boolean isGroup(@Nonnull String actionId);

  public abstract AnAction getActionOrStub(String id);

  public abstract void addTimerListener(int delay, TimerListener listener);

  public abstract void removeTimerListener(TimerListener listener);

  public abstract void addTransparentTimerListener(int delay, TimerListener listener);

  public abstract void removeTransparentTimerListener(TimerListener listener);

  public abstract ActionCallback tryToExecute(@Nonnull AnAction action, @Nonnull InputEvent inputEvent, @Nullable Component contextComponent,
                                              @Nullable String place, boolean now);

  /**
   * @deprecated Use {@link AnActionListener#getClass()}
   */
  public abstract void addAnActionListener(AnActionListener listener);

  /**
   * @deprecated Use {@link AnActionListener#getClass}
   */
  public abstract void removeAnActionListener(AnActionListener listener);

  /**
   * @deprecated Use {@link AnActionListener#getClass}
   */
  public void addAnActionListener(AnActionListener listener, Disposable parentDisposable) {
    Application.get().getMessageBus().connect(parentDisposable).subscribe(AnActionListener.class, listener);
  }

  @Nullable
  public abstract KeyboardShortcut getKeyboardShortcut(@Nonnull String actionId);
}

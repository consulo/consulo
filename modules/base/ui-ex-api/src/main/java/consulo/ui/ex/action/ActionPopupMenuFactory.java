/*
 * Copyright 2013-2023 consulo.io
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 27/06/2023
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ActionPopupMenuFactory {
  @Nonnull
  static ActionButtonFactory getInstance() {
    return Application.get().getInstance(ActionButtonFactory.class);
  }

  /**
   * Factory method that creates an <code>ActionPopupMenu</code> from the
   * specified group. The specified place is associated with the created popup.
   *
   * @param place Determines the place that will be set for {@link AnActionEvent} passed
   *              when an action from the group is either performed or updated
   *              See {@link consulo.ide.impl.idea.openapi.actionSystem.ActionPlaces}
   * @param group Group from which the actions for the menu are taken.
   * @return An instance of <code>ActionPopupMenu</code>
   */
  ActionPopupMenu createActionPopupMenu(@Nonnull ActionManager actionManager, String place, @Nonnull ActionGroup group);

  ActionPopupMenu createActionPopupMenu(@Nonnull ActionManager actionManager,
                                        @Nonnull String place,
                                        @Nonnull ActionGroup group,
                                        @Nullable PresentationFactory presentationFactory);
}

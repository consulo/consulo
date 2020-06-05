/*
 * Copyright 2013-2020 consulo.io
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
package consulo.actionSystem;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionToolbar;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-05-11
 */
public interface ActionToolbarFactory {
  /**
   * Factory method that creates an <code>ActionToolbar</code> from the
   * specified group. The specified place is associated with the created toolbar.
   *
   * @param place      Determines the place that will be set for {@link AnActionEvent} passed
   *                   when an action from the group is either performed or updated.
   *                   See {@link com.intellij.openapi.actionSystem.ActionPlaces}
   * @param group      Group from which the actions for the toolbar are taken.
   * @param horizontal The orientation of the toolbar (true - horizontal, false - vertical)
   * @return An instance of <code>ActionToolbar</code>
   */
  @Nonnull
  default ActionToolbar createActionToolbar(String place, ActionGroup group, boolean horizontal) {
    return createActionToolbar(place, group, horizontal, false);
  }

  @Nonnull
  ActionToolbar createActionToolbar(String place, ActionGroup group, boolean horizontal, boolean decorateButtons);
}

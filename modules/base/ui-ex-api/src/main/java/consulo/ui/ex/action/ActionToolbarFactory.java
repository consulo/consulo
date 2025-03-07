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
package consulo.ui.ex.action;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-05-11
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ActionToolbarFactory {
    @Nonnull
    static ActionToolbarFactory getInstance() {
        return Application.get().getInstance(ActionToolbarFactory.class);
    }

    /**
     * Factory method that creates an <code>ActionToolbar</code> from the
     * specified group. The specified place is associated with the created toolbar.
     *
     * @param place Determines the place that will be set for {@link AnActionEvent} passed
     *              when an action from the group is either performed or updated.
     *              See {@link ActionPlaces}
     * @param group Group from which the actions for the toolbar are taken.
     * @param style Style of toolbar, see documentation of {@link ActionToolbar.Style}
     * @return An instance of <code>ActionToolbar</code>
     */
    @Nonnull
    ActionToolbar createActionToolbar(String place, ActionGroup group, @Nonnull ActionToolbar.Style style);
}

/*
 * Copyright 2013-2025 consulo.io
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
package consulo.versionControlSystem.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.EmptyAction;
import consulo.ui.ex.popup.ListPopup;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2025-08-30
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface FlatSpeedSearchPopupFactory {
    static FlatSpeedSearchPopupFactory getInstance() {
        return Application.get().getInstance(FlatSpeedSearchPopupFactory.class);
    }

    public interface SpeedsearchAction {
    }

    static boolean isSpeedsearchAction(AnAction action) {
        return action instanceof SpeedsearchAction;
    }

    ListPopup createFlatSpeedSearchPopup(String title,
                                         ActionGroup actionGroup,
                                         DataContext dataContext,
                                         @Nullable Predicate<AnAction> preselectActionCondition,
                                         boolean showDisableActions,
                                         FlatSpeedSearchShouldBeShowingFilter filter);

    ListPopup createBranchPopup(String title,
                                Project project,
                                Predicate<AnAction> preselectActionCondition,
                                ActionGroup actions,
                                @Nullable String dimensionKey);

    
    static AnAction createSpeedSearchWrapper(AnAction child) {
        return new MySpeedSearchAction(child);
    }

    
    static ActionGroup createSpeedSearchActionGroupWrapper(ActionGroup child) {
        return new MySpeedSearchActionGroup(child);
    }

    static class MySpeedSearchAction extends EmptyAction.MyDelegatingAction implements FlatSpeedSearchPopupFactory.SpeedsearchAction, DumbAware {

        MySpeedSearchAction(AnAction action) {
            super(action);
        }
    }

    static class MySpeedSearchActionGroup extends EmptyAction.MyDelegatingActionGroup implements FlatSpeedSearchPopupFactory.SpeedsearchAction, DumbAware {
        MySpeedSearchActionGroup(ActionGroup actionGroup) {
            super(actionGroup);
        }
    }
}

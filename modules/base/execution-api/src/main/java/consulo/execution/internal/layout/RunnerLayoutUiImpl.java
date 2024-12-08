/*
 * Copyright 2013-2024 consulo.io
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
package consulo.execution.internal.layout;

import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.execution.internal.layout.RunnerContentUi;
import consulo.execution.ui.layout.LayoutStateDefaults;
import consulo.execution.ui.layout.LayoutViewOptions;
import consulo.execution.ui.layout.RunnerLayoutUi;
import consulo.ui.ex.action.AnAction;

import java.util.List;

/**
 * @author VISTALL
 * @since 12.05.2024
 */
public interface RunnerLayoutUiImpl extends Disposable.Parent, RunnerLayoutUi, LayoutStateDefaults, LayoutViewOptions, DataProvider {
    void setLeftToolbarVisible(boolean value);

    void setTopLeftActionsBefore(boolean value);

    void setContentToolbarBefore(boolean value);

    void setTopLeftActionsVisible(boolean value);

    List<AnAction> getActions();

    RunnerContentUi getContentUI();
}

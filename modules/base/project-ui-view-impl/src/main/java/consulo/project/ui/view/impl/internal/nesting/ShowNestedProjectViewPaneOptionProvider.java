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
package consulo.project.ui.view.impl.internal.nesting;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.ui.view.ProjectViewPane;
import consulo.project.ui.view.ProjectViewPaneOptionProvider;
import consulo.project.ui.view.internal.ProjectViewSharedSettings;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.util.dataholder.KeyWithDefaultValue;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @author VISTALL
 * @since 2025-05-31
 */
@ExtensionImpl(order = "last")
public class ShowNestedProjectViewPaneOptionProvider extends ProjectViewPaneOptionProvider.BoolValue {
    public static final KeyWithDefaultValue<Boolean> SHOW_NESTED_FILES_KEY = KeyWithDefaultValue.create("show-nested-files", Boolean.TRUE);

    private final Provider<ProjectViewSharedSettings> myProjectViewSharedSettings;

    @Inject
    public ShowNestedProjectViewPaneOptionProvider(Provider<ProjectViewSharedSettings> projectViewSharedSettings) {
        myProjectViewSharedSettings = projectViewSharedSettings;
    }

    @Nonnull
    @Override
    public KeyWithDefaultValue<Boolean> getKey() {
        return SHOW_NESTED_FILES_KEY;
    }

    @Override
    public void addToolbarActions(@Nonnull ProjectViewPane pane, @Nonnull DefaultActionGroup actionGroup) {
        actionGroup.add(new ConfigureFilesNestingAction(myProjectViewSharedSettings));
    }
}

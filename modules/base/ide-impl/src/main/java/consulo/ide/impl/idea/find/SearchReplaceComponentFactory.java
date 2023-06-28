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
package consulo.ide.impl.idea.find;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.dataContext.DataProvider;
import consulo.ide.impl.idea.openapi.util.BooleanGetter;
import consulo.project.Project;
import consulo.ui.ex.action.DefaultActionGroup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 28/06/2023
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface SearchReplaceComponentFactory {
  SearchReplaceComponent create(@Nullable Project project,
                                @Nonnull JComponent targetComponent,
                                @Nonnull DefaultActionGroup searchToolbar1Actions,
                                @Nonnull final BooleanGetter searchToolbar1ModifiedFlagGetter,
                                @Nonnull DefaultActionGroup searchToolbar2Actions,
                                @Nonnull DefaultActionGroup searchFieldActions,
                                @Nonnull DefaultActionGroup replaceToolbar1Actions,
                                @Nonnull DefaultActionGroup replaceToolbar2Actions,
                                @Nonnull DefaultActionGroup replaceFieldActions,
                                @Nullable Runnable replaceAction,
                                @Nullable Runnable closeAction,
                                @Nullable DataProvider dataProvider);
}

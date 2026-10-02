/*
 * Copyright 2013-2026 consulo.io
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
package consulo.fileEditor.impl.internal.search;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.dataContext.UiDataProvider;
import consulo.fileEditor.internal.SearchReplaceComponent;
import consulo.fileEditor.internal.SearchReplaceComponentFactory;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.DefaultActionGroup;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * @author VISTALL
 * @since 2026-10-02
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedSearchReplaceComponentFactoryImpl implements SearchReplaceComponentFactory {
    @Override
    @RequiredUIAccess
    public SearchReplaceComponent create(
        @Nullable Project project,
        Component targetComponent,
        DefaultActionGroup searchToolbar1Actions,
        BooleanSupplier searchToolbar1ModifiedFlagGetter,
        DefaultActionGroup searchToolbar2Actions,
        DefaultActionGroup searchFieldActions,
        DefaultActionGroup replaceToolbar1Actions,
        DefaultActionGroup replaceToolbar2Actions,
        DefaultActionGroup replaceFieldActions,
        @Nullable Runnable replaceAction,
        @Nullable Runnable closeAction,
        @Nullable UiDataProvider dataProvider
    ) {
        return new UnifiedSearchReplaceComponentImpl(
            project,
            targetComponent,
            searchToolbar1Actions,
            searchToolbar2Actions,
            searchFieldActions,
            replaceToolbar1Actions,
            replaceToolbar2Actions,
            replaceFieldActions,
            replaceAction,
            closeAction,
            dataProvider
        );
    }
}

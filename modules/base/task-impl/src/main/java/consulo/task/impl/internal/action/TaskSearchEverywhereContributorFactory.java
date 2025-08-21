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
package consulo.task.impl.internal.action;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.searchEverywhere.SearchEverywhereContributor;
import consulo.searchEverywhere.SearchEverywhereContributorFactory;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2025-08-21
 */
@ExtensionImpl
public class TaskSearchEverywhereContributorFactory implements SearchEverywhereContributorFactory<Object> {
    @Nullable
    @Override
    public SearchEverywhereContributor<Object> createContributor(@Nonnull AnActionEvent initEvent) {
        Project project = initEvent.getData(Project.KEY);
        if (project == null) {
            return null;
        }
        return new TaskSearchEverywhereContributor(project);
    }
}

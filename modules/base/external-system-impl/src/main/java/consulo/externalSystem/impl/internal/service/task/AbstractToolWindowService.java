/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.impl.internal.service.task;

import consulo.externalSystem.impl.internal.util.ExternalSystemUtil;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.service.project.ExternalEntityData;
import consulo.externalSystem.service.project.manage.ProjectDataService;
import consulo.externalSystem.ui.awt.ExternalSystemTasksTreeModel;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.util.Collection;

/**
 * @author Denis Zhdanov
 * @since 5/15/13 1:32 PM
 */
public abstract class AbstractToolWindowService<T extends ExternalEntityData> implements ProjectDataService<T, Void> {

    @Override
    public void importData(@Nonnull final Collection<DataNode<T>> toImport, @Nonnull final Project project, boolean synchronous) {
        if (toImport.isEmpty()) {
            return;
        }
        ExternalSystemApiUtil.executeOnEdt(false, new Runnable() {
            @Override
            public void run() {
                ExternalSystemTasksTreeModel model = ExternalSystemUtil.getToolWindowElement(ExternalSystemTasksTreeModel.class,
                    project,
                    ExternalSystemDataKeys.ALL_TASKS_MODEL,
                    toImport.iterator().next().getData().getOwner());
                processData(toImport, project, model);
            }
        });
    }

    protected abstract void processData(@Nonnull Collection<DataNode<T>> nodes,
                                        @Nonnull Project project,
                                        @jakarta.annotation.Nullable ExternalSystemTasksTreeModel model);

    @Override
    public void removeData(@Nonnull Collection<? extends Void> toRemove, @Nonnull Project project, boolean synchronous) {
    }
}

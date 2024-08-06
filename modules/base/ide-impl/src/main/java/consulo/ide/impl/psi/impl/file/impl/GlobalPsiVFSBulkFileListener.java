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
package consulo.ide.impl.psi.impl.file.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.ide.impl.idea.openapi.roots.impl.PushedFilePropertiesUpdaterImpl;
import consulo.module.content.PushedFilePropertiesUpdater;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author VISTALL
 * @since 2024-08-06
 */
@TopicImpl(ComponentScope.APPLICATION)
public class GlobalPsiVFSBulkFileListener implements BulkFileListener {
    @Override
    public void before(@Nonnull List<? extends VFileEvent> events) {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            project.getInstance(PsiVFSListener.class).before(events);
        }
    }

    @Override
    public void after(@Nonnull List<? extends VFileEvent> events) {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        // let PushedFilePropertiesUpdater process all pending vfs events and update file properties before we issue PSI events
        for (Project project : projects) {
            PushedFilePropertiesUpdater updater = PushedFilePropertiesUpdater.getInstance(project);
            // false in upsource
            if (updater instanceof PushedFilePropertiesUpdaterImpl) {
                ((PushedFilePropertiesUpdaterImpl) updater).processAfterVfsChanges(events);
            }
        }
        for (Project project : projects) {
            project.getInstance(PsiVFSListener.class).after(events);
        }
    }
}

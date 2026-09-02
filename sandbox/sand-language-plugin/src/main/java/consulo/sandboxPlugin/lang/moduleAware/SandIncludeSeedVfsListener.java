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
package consulo.sandboxPlugin.lang.moduleAware;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListenerBackgroundable;
import consulo.virtualFileSystem.event.VFileEvent;

import java.util.List;

/**
 * Any sand file change may move an include site's entry environment — re-derive the seeds.
 */
@TopicImpl(ComponentScope.APPLICATION)
final class SandIncludeSeedVfsListener implements BulkFileListenerBackgroundable {
    public SandIncludeSeedVfsListener() {
    }

    @Override
    public void after(List<? extends VFileEvent> events) {
        boolean sandTouched = false;
        for (VFileEvent event : events) {
            VirtualFile file = event.getFile();
            String name = file != null ? file.getName() : event.getPath();
            if (name != null && name.endsWith(".sand")) {
                sandTouched = true;
                break;
            }
        }
        if (!sandTouched) {
            return;
        }
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            SandSeedEnv.scheduleRecompute(project);
        }
    }
}

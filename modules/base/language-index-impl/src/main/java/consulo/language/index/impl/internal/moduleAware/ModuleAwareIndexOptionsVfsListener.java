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
package consulo.language.index.impl.internal.moduleAware;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListenerBackgroundable;
import consulo.virtualFileSystem.event.VFileEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Any change to a file some option provider claims may move the options of other files, so the providers are asked
 * to analyse the change.
 */
@TopicImpl(ComponentScope.APPLICATION)
final class ModuleAwareIndexOptionsVfsListener implements BulkFileListenerBackgroundable {
    public ModuleAwareIndexOptionsVfsListener() {
    }

    @Override
    public void after(List<? extends VFileEvent> events) {
        List<VirtualFile> claimed = new ArrayList<>();
        for (VFileEvent event : events) {
            VirtualFile file = event.getFile();
            if (file != null && !file.isDirectory() && !ModuleAwareIndexOptionRegistry.getApplicableProviders(file.getFileType()).isEmpty()) {
                claimed.add(file);
            }
        }
        if (claimed.isEmpty()) {
            return;
        }
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            ModuleAwareIndexOptionsAnalyzer.getInstance(project).schedule(claimed);
        }
    }
}

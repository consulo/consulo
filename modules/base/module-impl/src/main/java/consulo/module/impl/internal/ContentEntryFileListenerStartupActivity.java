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
package consulo.module.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2024-11-15
 */
@ExtensionImpl(order = "after FileBasedIndexProjectHandlerActivity")
public class ContentEntryFileListenerStartupActivity implements PostStartupActivity, DumbAware {
    private final ProjectLocator myProjectLocator;
    private final VirtualFileManager myVirtualFileManager;

    @Inject
    public ContentEntryFileListenerStartupActivity(ProjectLocator projectLocator, VirtualFileManager virtualFileManager) {
        myProjectLocator = projectLocator;
        myVirtualFileManager = virtualFileManager;
    }

    @Override
    public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        myVirtualFileManager.addAsyncFileListener(new ContentEntryDeleteFileListener(myProjectLocator, project), project);
    }
}

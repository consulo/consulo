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
package consulo.extensionPreviewRecorder.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.extension.preview.ExtensionPreview;
import consulo.component.extension.preview.ExtensionPreviewRecorder;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.versionControlSystem.VcsFactory;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 22/01/2023
 */
@ExtensionImpl
public class VcsFactoryPreviewRecorder implements ExtensionPreviewRecorder<VcsFactory> {
    private final ProjectManager myProjectManager;

    @Inject
    public VcsFactoryPreviewRecorder(ProjectManager projectManager) {
        myProjectManager = projectManager;
    }

    @Override
    public void analyze(@Nonnull Consumer<ExtensionPreview> recorder) {
        Project project = myProjectManager.getDefaultProject();

        project.getExtensionPoint(VcsFactory.class).forEachExtensionSafe(it -> {
            ExtensionPreview preview = ExtensionPreview.of(VcsFactory.class, it.getId(), it);
            recorder.accept(preview);
        });
    }
}
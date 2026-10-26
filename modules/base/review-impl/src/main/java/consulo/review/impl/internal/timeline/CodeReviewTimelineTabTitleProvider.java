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
package consulo.review.impl.internal.timeline;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.fileEditor.EditorTabTitleProvider;
import consulo.project.Project;
import consulo.review.localize.ReviewLocalize;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@ExtensionImpl
public class CodeReviewTimelineTabTitleProvider implements EditorTabTitleProvider, DumbAware {
    @Override
    public @Nullable String getEditorTabTitle(Project project, VirtualFile file) {
        return file instanceof CodeReviewTimelineVirtualFile timelineFile ? timelineFile.getDisplayName().get() : null;
    }

    @Override
    public @Nullable String getEditorTabTooltipText(Project project, VirtualFile file) {
        if (file instanceof CodeReviewTimelineVirtualFile timelineFile) {
            return ReviewLocalize.reviewTimelineEditorName().get() + ": " + timelineFile.getDescription().get();
        }
        return null;
    }
}

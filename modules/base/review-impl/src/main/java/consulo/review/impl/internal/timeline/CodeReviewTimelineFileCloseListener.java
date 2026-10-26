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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.virtualFileSystem.VirtualFile;

/**
 * Releases what a review timeline tab owns once its last editor is closed.
 */
@TopicImpl(ComponentScope.PROJECT)
public class CodeReviewTimelineFileCloseListener implements FileEditorManagerListener {
    @Override
    public void fileClosed(FileEditorManager source, VirtualFile file) {
        if (file instanceof CodeReviewTimelineVirtualFile timelineFile && source.getAllEditors(file).length == 0) {
            timelineFile.disposeOwner();
        }
    }
}

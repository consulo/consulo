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

import consulo.fileEditor.FileEditorManager;
import consulo.project.Project;
import consulo.review.details.model.CodeReviewDetailsTabViewModel;
import consulo.review.timeline.CodeReviewTimelineViewModel;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.light.LightVirtualFileBase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The editor tab of one review timeline. It lives only while the review provider keeps its view model, so it is never
 * reopened on the next start.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public class CodeReviewTimelineVirtualFile extends LightVirtualFileBase {
    @RequiredUIAccess
    public static void open(Project project, CodeReviewDetailsTabViewModel tabVm, CodeReviewTimelineViewModel timelineVm) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (VirtualFile file : manager.getOpenFiles()) {
            if (file instanceof CodeReviewTimelineVirtualFile timelineFile && timelineFile.getTimelineVm() == timelineVm) {
                manager.openFile(file, true);
                return;
            }
        }
        manager.openFile(new CodeReviewTimelineVirtualFile(tabVm, timelineVm), true);
    }

    private final CodeReviewDetailsTabViewModel myTabVm;
    private final CodeReviewTimelineViewModel myTimelineVm;

    public CodeReviewTimelineVirtualFile(CodeReviewDetailsTabViewModel tabVm, CodeReviewTimelineViewModel timelineVm) {
        super(tabVm.getDisplayName().get(), CodeReviewTimelineFileType.INSTANCE, System.currentTimeMillis());
        myTabVm = tabVm;
        myTimelineVm = timelineVm;
        setWritable(false);
    }

    public CodeReviewDetailsTabViewModel getTabVm() {
        return myTabVm;
    }

    public CodeReviewTimelineViewModel getTimelineVm() {
        return myTimelineVm;
    }

    @Override
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] contentsToByteArray() throws IOException {
        return new byte[0];
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(new byte[0]);
    }
}

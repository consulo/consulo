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

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditorManager;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.review.details.model.CodeReviewDetailsTabViewModel;
import consulo.review.timeline.CodeReviewTimelineViewModel;
import consulo.review.util.ComputedResult;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.light.LightVirtualFileBase;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The editor tab of one review timeline. It lives only while the review provider keeps its view model, so it is never
 * reopened on the next start.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public class CodeReviewTimelineVirtualFile extends LightVirtualFileBase {
    @RequiredUIAccess
    public static void open(Project project,
                            LocalizeValue name,
                            ObservableValue<ComputedResult<CodeReviewDetailsTabViewModel>> tabVm,
                            CodeReviewTimelineViewModel timelineVm,
                            @Nullable Disposable owner) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (VirtualFile file : manager.getOpenFiles()) {
            if (file instanceof CodeReviewTimelineVirtualFile timelineFile && timelineFile.getTimelineVm() == timelineVm) {
                if (owner != null) {
                    Disposer.dispose(owner);
                }
                manager.openFile(file, true);
                return;
            }
        }
        manager.openFile(new CodeReviewTimelineVirtualFile(name, tabVm, timelineVm, owner), true);
    }

    @RequiredUIAccess
    public static void close(Project project, CodeReviewTimelineViewModel timelineVm) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (VirtualFile file : manager.getOpenFiles()) {
            if (file instanceof CodeReviewTimelineVirtualFile timelineFile && timelineFile.getTimelineVm() == timelineVm) {
                manager.closeFile(file);
            }
        }
    }

    @RequiredUIAccess
    public static void updatePresentation(Project project, CodeReviewTimelineViewModel timelineVm) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (VirtualFile file : manager.getOpenFiles()) {
            if (file instanceof CodeReviewTimelineVirtualFile timelineFile && timelineFile.getTimelineVm() == timelineVm) {
                manager.updateFilePresentation(file);
            }
        }
    }

    private final LocalizeValue myName;
    private final ObservableValue<ComputedResult<CodeReviewDetailsTabViewModel>> myTabVmState;
    private final CodeReviewTimelineViewModel myTimelineVm;
    private final AtomicReference<@Nullable Disposable> myOwner;

    public CodeReviewTimelineVirtualFile(LocalizeValue name,
                                         ObservableValue<ComputedResult<CodeReviewDetailsTabViewModel>> tabVm,
                                         CodeReviewTimelineViewModel timelineVm,
                                         @Nullable Disposable owner) {
        super(name.get(), CodeReviewTimelineFileType.INSTANCE, System.currentTimeMillis());
        myName = name;
        myTabVmState = tabVm;
        myTimelineVm = timelineVm;
        myOwner = new AtomicReference<>(owner);
        setWritable(false);
    }

    /**
     * Disposes the owner given to {@link #open}, once the last tab of this file is closed.
     */
    public void disposeOwner() {
        Disposable owner = myOwner.getAndSet(null);
        if (owner != null) {
            Disposer.dispose(owner);
        }
    }

    public ObservableValue<ComputedResult<CodeReviewDetailsTabViewModel>> getTabVmState() {
        return myTabVmState;
    }

    /**
     * @return the tab view model once it is loaded
     */
    public @Nullable CodeReviewDetailsTabViewModel getTabVm() {
        return myTabVmState.get().getOrNull();
    }

    public LocalizeValue getDisplayName() {
        CodeReviewDetailsTabViewModel tabVm = getTabVm();
        return tabVm == null ? myName : tabVm.getDisplayName();
    }

    public LocalizeValue getDescription() {
        CodeReviewDetailsTabViewModel tabVm = getTabVm();
        return tabVm == null ? myName : tabVm.getDescription();
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

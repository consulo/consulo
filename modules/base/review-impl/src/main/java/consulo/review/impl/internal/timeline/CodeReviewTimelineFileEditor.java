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
import consulo.review.localize.ReviewLocalize;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.UserDataHolderBase;
import kava.beans.PropertyChangeListener;
import org.jspecify.annotations.Nullable;

/**
 * Shows the timeline once the review is loaded - the loading or the failure until then.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public class CodeReviewTimelineFileEditor extends UserDataHolderBase implements FileEditor {
    private final Project myProject;
    private final CodeReviewTimelineVirtualFile myFile;
    private final Disposable myUIDisposable = Disposable.newDisposable("CodeReviewTimelineFileEditor");
    private @Nullable Component myComponent;

    public CodeReviewTimelineFileEditor(Project project, CodeReviewTimelineVirtualFile file) {
        myProject = project;
        myFile = file;
    }

    @RequiredUIAccess
    @Override
    public Component getUIComponent() {
        Component component = myComponent;
        if (component == null) {
            component = CodeReviewTimelineLoadingComponent.create(myUIDisposable, myFile.getTabVmState(), myFile::getTimelineVm, () -> {
                if (!myProject.isDisposed()) {
                    FileEditorManager.getInstance(myProject).updateFilePresentation(myFile);
                }
            });
            myComponent = component;
        }
        return component;
    }

    @Override
    public void selectNotify() {
        myFile.getTimelineVm().update();
    }

    @Override
    public String getName() {
        return ReviewLocalize.reviewTimelineEditorName().get();
    }

    @Override
    public CodeReviewTimelineVirtualFile getFile() {
        return myFile;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public void dispose() {
        Disposer.dispose(myUIDisposable);
        myComponent = null;
    }
}

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
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.project.Project;
import consulo.review.details.model.CodeReviewDetailsTabViewModel;
import consulo.review.list.error.ErrorStatusAlignment;
import consulo.review.list.error.ErrorStatusPanelFactory;
import consulo.review.list.error.ErrorStatusPresenter;
import consulo.review.localize.ReviewLocalize;
import consulo.review.util.ComputedResult;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
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
    private @Nullable DockLayout myComponent;

    public CodeReviewTimelineFileEditor(Project project, CodeReviewTimelineVirtualFile file) {
        myProject = project;
        myFile = file;
    }

    @RequiredUIAccess
    @Override
    public Component getUIComponent() {
        DockLayout component = myComponent;
        if (component == null) {
            component = DockLayout.create();
            myComponent = component;
            showState(component);
        }
        return component;
    }

    @RequiredUIAccess
    private void showState(DockLayout panel) {
        Disposable stateDisposable = Disposable.newDisposable("CodeReviewTimelineFileEditor state");
        Disposer.register(myUIDisposable, stateDisposable);
        boolean[] shown = {false};
        Runnable listener = myFile.getTabVmState().addListener(result -> myProject.getUIAccess().give(() -> {
            if (!shown[0] && !Disposer.isDisposed(stateDisposable)) {
                shown[0] = show(panel, result);
                if (shown[0]) {
                    Disposer.dispose(stateDisposable);
                }
            }
        }));
        Disposer.register(stateDisposable, listener::run);
        if (show(panel, myFile.getTabVmState().get())) {
            shown[0] = true;
            Disposer.dispose(stateDisposable);
        }
    }

    /**
     * @return whether the timeline is shown, so the state is not needed any more
     */
    @RequiredUIAccess
    private boolean show(DockLayout panel, ComputedResult<CodeReviewDetailsTabViewModel> result) {
        CodeReviewDetailsTabViewModel tabVm = result.getOrNull();
        Throwable error = result.exceptionOrNull();
        if (tabVm != null) {
            panel.center(CodeReviewTimelineComponent.create(myUIDisposable, tabVm, myFile.getTimelineVm()));
            if (!myProject.isDisposed()) {
                FileEditorManager.getInstance(myProject).updateFilePresentation(myFile);
            }
            return true;
        }

        if (error != null) {
            ErrorStatusPanelFactory factory = myProject.getApplication().getInstance(ErrorStatusPanelFactory.class);
            Component errorPanel = factory.create(error,
                ErrorStatusPresenter.<Throwable>simple(ReviewLocalize.reviewTimelineDetailsCannotLoad()),
                ErrorStatusAlignment.CENTER);
            panel.center(centered(errorPanel));
        }
        else {
            Label loading = Label.create(ReviewLocalize.reviewTimelineLoading());
            loading.setImage(Image.busy());
            loading.setForegroundColor(ComponentColors.DISABLED_TEXT);
            panel.center(centered(loading));
        }
        return false;
    }

    @RequiredUIAccess
    private static Component centered(Component component) {
        VerticalLayout layout = VerticalLayout.create(Space.NONE, HorizontalAlignment.CENTER);
        layout.add(component);
        layout.paddingBuilder().allSet(Space.X_LARGE).apply();
        return layout;
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

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
package consulo.ide.impl.module.importing;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.module.creation.importing.ModuleImportContext;
import consulo.module.creation.importing.ModuleImportProvider;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.action.DialogCancelAction;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.ex.wizard.WizardSession;
import consulo.ui.ex.wizard.WizardStep;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.SwipeLayout;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The wizard drives the two buttons of the dialog rather than adding its own - the right button walks forward and
 * finishes on the last step, the left one walks back and cancels on the first.
 *
 * @author VISTALL
 * @since 2026-08-24
 */
public class ModuleImportDialogDescriptor<C extends ModuleImportContext> extends DialogDescriptor {
    private final C myContext;
    private final WizardSession<C> myWizardSession;

    private @Nullable Disposable myUiDisposable;
    private @Nullable SwipeLayout mySwipeLayout;

    @RequiredUIAccess
    public ModuleImportDialogDescriptor(
        @Nullable Project project,
        VirtualFile targetFile,
        ModuleImportProvider<C> moduleImportProvider
    ) {
        super(LocalizeValue.localizeTODO("Import from " + moduleImportProvider.getName()));

        myContext = moduleImportProvider.createContext(project);

        String pathToImport = moduleImportProvider.getPathToBeImported(targetFile);
        myContext.setPath(pathToImport);
        myContext.setName(new File(pathToImport).getName());
        myContext.setFileToImport(targetFile.getPath());

        List<WizardStep<C>> steps = new ArrayList<>();
        moduleImportProvider.buildSteps(steps::add, myContext);

        myWizardSession = new WizardSession<>(myContext, steps);

        if (!myWizardSession.hasNext()) {
            throw new IllegalArgumentException("no steps for show");
        }
    }

    public C getContext() {
        return myContext;
    }

    public void disposeContext() {
        myContext.dispose();
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofFont(40, 30);
    }

    @Override
    public @Nullable String getDimensionServiceKey() {
        return getClass().getName();
    }

    @Override
    @RequiredUIAccess
    public Component createCenterComponent(Disposable uiDisposable) {
        myUiDisposable = uiDisposable;

        Disposer.register(uiDisposable, myWizardSession::dispose);

        SwipeLayout swipeLayout = SwipeLayout.create();
        mySwipeLayout = swipeLayout;

        WizardStep<C> first = myWizardSession.next();
        swipeLayout.register(stepId(), () -> stepLayout(first));
        swipeLayout.swipeLeftTo(stepId());

        return swipeLayout;
    }

    /**
     * A wizard walks one way, so the button which goes back is always the left one and the button which goes on is
     * always the right one - the order the platform would otherwise put ok and cancel in does not apply.
     */
    @Override
    public AnAction[] createActions(boolean inverseOrder) {
        return new AnAction[]{new BackAction(), new ForwardAction()};
    }

    @RequiredUIAccess
    private void goTo(WizardStep<C> step, boolean forward) {
        String id = stepId();

        SwipeLayout swipeLayout = mySwipeLayout;
        if (swipeLayout == null) {
            return;
        }

        swipeLayout.register(id, () -> stepLayout(step));

        if (forward) {
            swipeLayout.swipeLeftTo(id);
        }
        else {
            swipeLayout.swipeRightTo(id);
        }

        // the button row is only re-read when it is asked to be, so the step which changed what the buttons say is
        // the one which has to ask
        updateOkButtonState();
    }

    @RequiredUIAccess
    private Layout stepLayout(WizardStep<C> step) {
        Component component = step.getComponent(myContext, myUiDisposable);

        // a swipe holds layouts, while a step is free to answer any component - the ones which are not a layout are
        // given one to sit in rather than being refused
        return component instanceof Layout<?> layout ? layout : DockLayout.create().center(component);
    }

    private String stepId() {
        return "step-" + myWizardSession.getCurrentStepIndex();
    }

    private boolean isLastStep() {
        return !myWizardSession.hasNext();
    }

    private boolean isFirstStep() {
        return myWizardSession.getCurrentStepIndex() == 0;
    }

    /**
     * The right button of the dialog. It is the ok action on the last step - the dialog closes with a value - and a
     * plain step forward before that.
     */
    private class ForwardAction extends DialogOkAction {
        private ForwardAction() {
            super(IdeLocalize.buttonCreate());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            if (isLastStep()) {
                myWizardSession.finish();

                super.actionPerformed(e);
                return;
            }

            goTo(myWizardSession.next(), true);
        }

        @Override
        public void update(AnActionEvent e) {
            super.update(e);

            e.getPresentation().setText(isLastStep() ? IdeLocalize.buttonCreate() : CommonLocalize.buttonNext());
        }
    }

    /**
     * The left button of the dialog - a step back while there is one, and the cancel action on the first step.
     */
    private class BackAction extends DialogCancelAction implements AnActionWithSyncUpdate {
        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            if (isFirstStep()) {
                myWizardSession.finish();

                super.actionPerformed(e);
                return;
            }

            goTo(myWizardSession.prev(), false);
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setText(isFirstStep() ? CommonLocalize.buttonCancel() : CommonLocalize.buttonBack());
        }
    }
}

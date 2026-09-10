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
package consulo.ide.impl.module.creation;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.localize.IdeLocalize;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.ui.Component;
import consulo.ui.Size2D;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TitlelessDecorator;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.action.DialogCancelAction;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * The wizard drives the two buttons of the dialog rather than adding its own - the right button walks forward and
 * creates on the last step, the left one walks back and cancels on the first. Until a type is picked in the tree
 * there is no wizard at all, so the right button stays disabled.
 *
 * @author VISTALL
 * @since 2026-09-06
 */
public class NewProjectDialogDescriptor extends DialogDescriptor {
    private final @Nullable VirtualFile myModuleHome;

    private @Nullable UnifiedNewProjectPanel myPanel;

    public NewProjectDialogDescriptor(@Nullable VirtualFile moduleHome) {
        super(moduleHome != null ? IdeLocalize.titleAddModule() : IdeLocalize.titleNewProject());
        myModuleHome = moduleHome;
    }

    public NewProjectWizardData getWizardData() {
        return requirePanel();
    }

    @Override
    @RequiredUIAccess
    public Component createCenterComponent(Disposable uiDisposable) {
        UnifiedNewProjectPanel panel =
            new UnifiedNewProjectPanel(uiDisposable, myModuleHome, TitlelessDecorator.NOTHING, false);
        myPanel = panel;

        Disposer.register(uiDisposable, panel);
        panel.setPresentationListener(this::updateOkButtonState);

        return panel.getLayout();
    }

    @Override
    public boolean doUpdateOkButtonState() {
        UnifiedNewProjectPanel panel = myPanel;
        return panel != null && panel.isTypeSelected();
    }

    @Override
    public AnAction[] createActions(boolean inverseOrder) {
        return new AnAction[]{new BackAction(), new ForwardAction()};
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        Size2D size = WelcomeFrameManager.getDefaultWindowSize();
        return WidthAndHeight.ofPixel(size.width(), size.height());
    }

    @Override
    public @Nullable String getDimensionServiceKey() {
        return getClass().getName();
    }

    @Override
    public boolean hasDefaultContentBorder() {
        return false;
    }

    private UnifiedNewProjectPanel requirePanel() {
        UnifiedNewProjectPanel panel = myPanel;
        if (panel == null) {
            throw new IllegalStateException("Dialog is not built yet");
        }
        return panel;
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
            UnifiedNewProjectPanel panel = requirePanel();

            if (panel.hasNextStep()) {
                panel.goNextStep();
                return;
            }

            panel.finish();

            super.actionPerformed(e);
        }

        @Override
        public void update(AnActionEvent e) {
            super.update(e);

            UnifiedNewProjectPanel panel = myPanel;
            e.getPresentation().setText(
                panel != null && panel.hasNextStep() ? CommonLocalize.buttonNext() : IdeLocalize.buttonCreate()
            );
        }
    }

    /**
     * The left button of the dialog - a step back while there is one, and the cancel action on the first step.
     */
    private class BackAction extends DialogCancelAction implements AnActionWithSyncUpdate {
        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            UnifiedNewProjectPanel panel = requirePanel();

            if (panel.hasPrevStep()) {
                panel.goPrevStep();
                return;
            }

            super.actionPerformed(e);
        }

        @Override
        public void update(AnActionEvent e) {
            UnifiedNewProjectPanel panel = myPanel;
            e.getPresentation().setText(
                panel != null && panel.hasPrevStep() ? CommonLocalize.buttonBack() : CommonLocalize.buttonCancel()
            );
        }
    }
}

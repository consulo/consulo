/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ide.moduleImport;

import consulo.ide.localize.IdeLocalize;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.ui.Size2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBCardLayout;
import consulo.ui.ex.wizard.WizardSession;
import consulo.ui.ex.wizard.WizardStep;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-08-26
 */
public class ModuleImportDialog<C extends ModuleImportContext> extends DialogWrapper {

    private final WizardSession<C> myWizardSession;

    private Runnable myNextRunnable;
    private Runnable myBackRunnable;
    private final C myContext;

    protected ModuleImportDialog(
        @Nullable Project project,
        @Nonnull VirtualFile targetFile,
        @Nonnull ModuleImportProvider<C> moduleImportProvider
    ) {
        super(project);

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

        setTitle("Import from " + moduleImportProvider.getName());
        setOKButtonText(IdeLocalize.buttonCreate());

        Size2D size = WelcomeFrameManager.getDefaultWindowSize();
        setScalableSize(size.width(), size.height());

        init();
    }

    @Nonnull
    public C getContext() {
        return myContext;
    }

    @Nonnull
    @Override
    protected Action[] createActions() {
        return new Action[]{getCancelAction(), getOKAction()};
    }

    @Override
    protected void doOKAction() {
        if (myNextRunnable != null) {
            myNextRunnable.run();
        }
        else {
            myWizardSession.finish();

            super.doOKAction();
        }
    }

    @Override
    public void doCancelAction(AWTEvent source) {
        if (source instanceof WindowEvent) {
            // if it's window event - close it via X
            super.doCancelAction();
            return;
        }
        super.doCancelAction(source);
    }

    @Override
    public void doCancelAction() {
        if (myBackRunnable != null) {
            myBackRunnable.run();
        }
        else {
            myWizardSession.finish();

            super.doCancelAction();
        }
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return getClass().getName();
    }

    @Nullable
    @Override
    @RequiredUIAccess
    @SuppressWarnings("deprecation")
    protected JComponent createCenterPanel() {
        JBCardLayout layout = new JBCardLayout();
        JPanel contentPanel = new JPanel(layout);

        WizardStep<C> first = myWizardSession.next();

        int currentStepIndex = myWizardSession.getCurrentStepIndex();

        String id = "step-" + currentStepIndex;
        contentPanel.add(first.getSwingComponent(myContext, getDisposable()), id);

        layout.show(contentPanel, id);

        updateButtonPresentation(contentPanel);
        return contentPanel;
    }

    @RequiredUIAccess
    @SuppressWarnings("deprecation")
    private void gotoStep(JPanel rightContentPanel, WizardStep<C> step) {
        Component swingComponent = step.getSwingComponent(myContext, getDisposable());

        String id = "step-" + myWizardSession.getCurrentStepIndex();

        JBCardLayout layout = (JBCardLayout) rightContentPanel.getLayout();

        rightContentPanel.add(swingComponent, id);

        layout.swipe(rightContentPanel, id, JBCardLayout.SwipeDirection.FORWARD);

        updateButtonPresentation(rightContentPanel);
    }

    private void updateButtonPresentation(JPanel contentPanel) {
        boolean hasNext = myWizardSession.hasNext();

        if (hasNext) {
            setOKButtonText(CommonLocalize.buttonNext());
            myNextRunnable = () -> gotoStep(contentPanel, myWizardSession.next());
        }
        else {
            setOKButtonText(IdeLocalize.buttonCreate());
            myNextRunnable = null;
        }

        int currentStepIndex = myWizardSession.getCurrentStepIndex();
        if (currentStepIndex != 0) {
            myBackRunnable = () -> gotoStep(contentPanel, myWizardSession.prev());

            setCancelButtonText(CommonLocalize.buttonBack());
        }
        else {
            myBackRunnable = null;
            setCancelButtonText(CommonLocalize.buttonCancel());
        }
    }

    @Override
    protected void dispose() {
        myWizardSession.dispose();

        super.dispose();
    }
}

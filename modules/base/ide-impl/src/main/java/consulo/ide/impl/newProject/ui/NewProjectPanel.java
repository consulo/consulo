/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl.newProject.ui;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.ide.impl.welcomeScreen.BaseWelcomeScreenPanel;
import consulo.ide.localize.IdeLocalize;
import consulo.ide.newModule.*;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.util.ProjectUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.tree.AsyncTreeModel;
import consulo.ui.ex.awt.tree.StructureTreeModel;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.wizard.WizardSession;
import consulo.ui.ex.wizard.WizardStep;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2016-09-14
 */
public abstract class NewProjectPanel extends BaseWelcomeScreenPanel implements Disposable {
    private static final Logger LOG = Logger.getInstance(NewProjectPanel.class);

    private static final String EMPTY_PANEL = "empty-panel";

    // per module builder fields
    private WizardSession<NewModuleWizardContext> myWizardSession;
    private NewModuleWizardContext myWizardContext;
    private NewModuleBuilderProcessor<NewModuleWizardContext> myProcessor;
    @Nullable
    private final VirtualFile myModuleHome;

    private Tree myTree;

    @RequiredUIAccess
    public NewProjectPanel(
        @Nonnull Disposable parentDisposable,
        @Nullable Project project,
        @Nullable VirtualFile moduleHome,
        @Nonnull TitlelessDecorator titlelessDecorator
    ) {
        super(parentDisposable, titlelessDecorator);
        myModuleHome = moduleHome;
        setOKActionText(IdeLocalize.buttonCreate());
        setCancelText(CommonLocalize.buttonCancel());
    }

    @Override
    protected int getLeftComponentWidth() {
        return 300;
    }

    @Nullable
    public NewModuleBuilderProcessor<NewModuleWizardContext> getProcessor() {
        return myProcessor;
    }

    @Nullable
    public NewModuleWizardContext getWizardContext() {
        return myWizardContext;
    }

    public boolean isModuleCreation() {
        return myModuleHome != null;
    }

    @Nonnull
    @RequiredUIAccess
    protected abstract JComponent createSouthPanel();

    public abstract void setOKActionEnabled(boolean enabled);

    public abstract void setOKActionText(@Nonnull LocalizeValue text);

    public abstract void setOKAction(@Nullable Runnable action);

    public abstract void setCancelText(@Nonnull LocalizeValue text);

    public abstract void setCancelAction(@Nullable Runnable action);

    @Nonnull
    @Override
    protected JComponent createLeftComponent(@Nonnull Disposable parentDisposable) {
        NewModuleContext context = new NewModuleContext();

        Application.get().getExtensionPoint(NewModuleBuilder.class).forEach(it -> it.setupContext(context));

        myTree = new Tree(new AsyncTreeModel(
            new StructureTreeModel<>(new NewProjectTreeStructure(context), parentDisposable),
            parentDisposable
        ));
        myTree.setFont(UIUtil.getFont(UIUtil.FontSize.BIGGER, null));
        myTree.setOpaque(false);
        myTree.setBackground(MorphColor.of(UIUtil::getPanelBackground));
        myTree.setRootVisible(false);
        myTree.setRowHeight(JBUI.scale(24));

        TreeUtil.expandAll(myTree);
        return ScrollPaneFactory.createScrollPane(myTree, true);
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    @SuppressWarnings({"unchecked", "RequiredXAction"})
    protected JComponent createRightComponent() {
        JPanel panel = new JPanel(new VerticalFlowLayout());

        JPanel rightPanel = new JPanel(new BorderLayout());

        JBCardLayout rightContentLayout = new JBCardLayout();
        JPanel rightContentPanel = new JPanel(rightContentLayout);
        rightContentPanel.setBorder(JBUI.Borders.empty(5));

        rightPanel.add(rightContentPanel, BorderLayout.CENTER);

        JPanel nullPanel = new JPanel(new BorderLayout());
        JBLabel nodeLabel = new JBLabel(
            myModuleHome == null ? "Please select project type" : "Please select module type",
            UIUtil.ComponentStyle.SMALL,
            UIUtil.FontColor.BRIGHTER
        );
        nodeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        nullPanel.add(nodeLabel, BorderLayout.CENTER);

        rightContentPanel.add(nullPanel, EMPTY_PANEL);
        rightContentLayout.show(rightContentPanel, EMPTY_PANEL);

        myTree.addTreeSelectionListener(e -> {
            TreePath selected = e.getPath();

            Object node = TreeUtil.getLastUserObject(selected);
            Object selectedValue = node;
            if (node instanceof NodeDescriptor nodeDescriptor) {
                selectedValue = nodeDescriptor.getElement();
            }

            rightContentPanel.removeAll();

            if (myWizardSession != null) {
                myWizardSession.finish();
                myWizardSession.dispose();
                myWizardSession = null;
            }

            myProcessor = selectedValue instanceof NewModuleContextItem newModuleContextItem
                ? (NewModuleBuilderProcessor<NewModuleWizardContext>)newModuleContextItem.getProcessor() : null;

            String id = null;
            Component toShow = null;

            if (selectedValue instanceof NewModuleContextItem) {
                if (myProcessor != null) {
                    myWizardContext = myProcessor.createContext(!isModuleCreation());

                    if (myModuleHome != null) {
                        myWizardContext.setName(myModuleHome.getName());
                        myWizardContext.setPath(myModuleHome.getPath());
                    }
                    else {
                        Path baseDir = ProjectUtil.getProjectsDirectory();
                        File suggestedProjectDirectory = FileUtil.findSequentNonexistentFile(baseDir.toFile(), "untitled", "");

                        myWizardContext.setName(suggestedProjectDirectory.getName());
                        myWizardContext.setPath(suggestedProjectDirectory.getPath());
                    }

                    List<WizardStep<NewModuleWizardContext>> steps = new ArrayList<>();
                    myProcessor.buildSteps(steps::add, myWizardContext);

                    myWizardSession = new WizardSession<>(myWizardContext, steps);

                    if (myWizardSession.hasNext()) {
                        WizardStep<NewModuleWizardContext> step = myWizardSession.next();

                        toShow = step.getSwingComponent(myWizardContext, this);
                    }
                    else {
                        LOG.error("There no visible steps for " + selectedValue);
                        toShow = new JPanel();
                    }

                    id = "step-" + myWizardSession.getCurrentStepIndex();
                }
                rightContentPanel.add(panel, BorderLayout.CENTER);
            }


            if (myProcessor == null) {
                rightContentPanel.add(nullPanel, EMPTY_PANEL);

                rightContentLayout.show(rightContentPanel, EMPTY_PANEL);
            }
            else {
                assert toShow != null;

                rightContentPanel.add(toShow, id);

                rightContentLayout.show(rightContentPanel, id);
            }

            updateButtonPresentation(rightContentPanel);
        });

        JPanel root = new JPanel(new BorderLayout());
        root.add(rightPanel, BorderLayout.CENTER);
        JComponent southPanel = createSouthPanel();
        southPanel.setBorder(JBUI.Borders.empty(DialogWrapper.ourDefaultBorderInsets));
        root.add(southPanel, BorderLayout.SOUTH);
        return root;
    }

    @RequiredUIAccess
    private void updateButtonPresentation(JPanel rightContentPanel) {
        if (myProcessor != null) {
            assert myWizardSession != null;

            boolean hasNext = myWizardSession.hasNext();

            if (hasNext) {
                setOKActionText(CommonLocalize.buttonNext());
                setOKAction(() -> gotoStep(rightContentPanel, myWizardSession.next()));
            }
            else {
                setOKActionText(IdeLocalize.buttonCreate());
                setOKAction(null);
            }

            int currentStepIndex = myWizardSession.getCurrentStepIndex();
            if (currentStepIndex != 0) {
                setCancelAction(() -> gotoStep(rightContentPanel, myWizardSession.prev()));
                setCancelText(CommonLocalize.buttonBack());
            }
            else {
                setCancelAction(null);
                setCancelText(CommonLocalize.buttonCancel());
            }

            setOKActionEnabled(true);
        }
        else {
            setOKActionEnabled(false);

            setOKActionText(IdeLocalize.buttonCreate());
            setOKAction(null);
            setCancelAction(null);
        }
    }

    public void finish() {
        if (myWizardSession != null) {
            myWizardSession.finish();
        }
    }

    @RequiredUIAccess
    private void gotoStep(JPanel rightContentPanel, WizardStep<NewModuleWizardContext> step) {
        Component swingComponent = step.getSwingComponent(myWizardContext, this);

        String id = "step-" + myWizardSession.getCurrentStepIndex();

        JBCardLayout layout = (JBCardLayout)rightContentPanel.getLayout();

        rightContentPanel.add(swingComponent, id);

        layout.swipe(rightContentPanel, id, JBCardLayout.SwipeDirection.FORWARD);

        updateButtonPresentation(rightContentPanel);
    }

    @Override
    public void dispose() {
        if (myWizardSession != null) {
            myWizardSession.finish();
            myWizardSession.dispose();
        }
    }
}

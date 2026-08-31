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

import consulo.application.Application;
import consulo.component.extension.preview.ExtensionPreview;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.externalService.pluginAdvertiser.PluginAdvertiserHelper;
import consulo.ide.impl.welcomeScreen.WelcomeSlide;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.creation.NewModuleWizardContext;
import consulo.module.creation.scratch.NewModuleBuilder;
import consulo.module.creation.scratch.NewModuleBuilderProcessor;
import consulo.module.creation.scratch.NewModuleContext;
import consulo.module.creation.scratch.NewModuleContextGroup;
import consulo.module.creation.scratch.NewModuleContextItem;
import consulo.module.creation.scratch.NewModuleContextNode;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.util.ProjectUtil;
import consulo.ui.Length;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.Size2D;
import consulo.ui.TextAttribute;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.TreeStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.ex.TitlelessDecorator;
import consulo.ui.ex.wizard.WizardSession;
import consulo.ui.ex.wizard.WizardStep;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SwipeLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.layout.WrappedLayout;
import consulo.ui.style.ComponentColors;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-08-15
 */
public class UnifiedNewProjectPanel implements NewProjectWizardData, WelcomeSlide {
    private static final Logger LOG = Logger.getInstance(UnifiedNewProjectPanel.class);

    private static final String EMPTY_PANEL = "empty-panel";

    private static class NewProjectTreeModel implements TreeModel<NewModuleContextNode> {
        private final NewModuleContext myContext;

        NewProjectTreeModel(NewModuleContext context) {
            myContext = context;
        }

        @Override
        public void buildChildren(
            Function<NewModuleContextNode, TreeNode<NewModuleContextNode>> nodeFactory,
            @Nullable NewModuleContextNode parentValue
        ) {
            NewModuleContextGroup group;
            if (parentValue == null) {
                group = myContext;
            }
            else if (parentValue instanceof NewModuleContextGroup contextGroup) {
                group = contextGroup;
            }
            else {
                return;
            }

            for (Object child : group.getAll()) {
                NewModuleContextNode value = (NewModuleContextNode) child;

                TreeNode<NewModuleContextNode> node = nodeFactory.apply(value);
                node.setLeaf(!(value instanceof NewModuleContextGroup));
                node.setRenderer((nodeValue, presentation) -> {
                    presentation.withIcon(nodeValue.getImage());
                    presentation.append(
                        nodeValue.getName(),
                        nodeValue instanceof NewModuleContextGroup ? TextAttribute.REGULAR_BOLD : TextAttribute.REGULAR
                    );
                });
            }
        }
    }

    private final Disposable myParentDisposable;
    private final @Nullable VirtualFile myModuleHome;

    private @Nullable WizardSession<NewModuleWizardContext> myWizardSession;
    private @Nullable NewModuleWizardContext myWizardContext;
    private @Nullable NewModuleBuilderProcessor<NewModuleWizardContext> myProcessor;

    private final TitlelessDecorator myTitlelessDecorator;

    private @Nullable DockLayout myRoot;
    private SwipeLayout myStepLayout;
    private Hyperlink myMoreViaPlugins;
    private @Nullable Button myOkButton;
    private @Nullable Button myCancelButton;

    private @Nullable Runnable myOkAction;
    private @Nullable Runnable myCancelAction;

    private @Nullable Runnable myDefaultOkAction;
    private @Nullable Runnable myDefaultCancelAction;

    private int myStepCounter;

    public UnifiedNewProjectPanel(Disposable parentDisposable, @Nullable VirtualFile moduleHome) {
        this(parentDisposable, moduleHome, TitlelessDecorator.NOTHING);
    }

    public UnifiedNewProjectPanel(
        Disposable parentDisposable,
        @Nullable VirtualFile moduleHome,
        TitlelessDecorator titlelessDecorator
    ) {
        myParentDisposable = parentDisposable;
        myModuleHome = moduleHome;
        myTitlelessDecorator = titlelessDecorator;
    }

    public void setDefaultOkAction(@Nullable @RequiredUIAccess Runnable okAction) {
        myDefaultOkAction = okAction;
    }

    @Override
    public void setCloseAction(Runnable closeAction) {
        myDefaultCancelAction = closeAction;
    }

    public boolean isModuleCreation() {
        return myModuleHome != null;
    }

    @Override
    public @Nullable NewModuleBuilderProcessor<NewModuleWizardContext> getProcessor() {
        return myProcessor;
    }

    @Override
    public @Nullable NewModuleWizardContext getWizardContext() {
        return myWizardContext;
    }

    @Override
    @RequiredUIAccess
    public Layout getLayout() {
        DockLayout root = myRoot;
        if (root == null) {
            myRoot = root = buildLayout();
        }
        return root;
    }

    @RequiredUIAccess
    private DockLayout buildLayout() {
        DockLayout root = DockLayout.create();

        Component leftPanel = buildLeftPanel();
        myTitlelessDecorator.makeLeftComponentLower(leftPanel);
        root.left(leftPanel);

        myStepLayout = SwipeLayout.create();
        myStepLayout.register(EMPTY_PANEL, this::buildEmptyPanel);

        DockLayout rightPanel = DockLayout.create();
        rightPanel.center(myStepLayout);

        Component southPanel = buildSouthPanel();
        if (southPanel != null) {
            rightPanel.bottom(southPanel);
        }

        root.center(myTitlelessDecorator.modifyRightComponent(root, rightPanel));
        return root;
    }

    @RequiredUIAccess
    private Component buildLeftPanel() {
        NewModuleContext context = new NewModuleContext();

        Application.get().getExtensionPoint(NewModuleBuilder.class).forEach(it -> it.setupContext(context));

        Tree<NewModuleContextNode> tree = Tree.create(new NewProjectTreeModel(context));
        Disposer.register(myParentDisposable, tree.destroyHook());
        tree.addStyle(TreeStyle.FONT_LARGE);
        tree.addStyle(TreeStyle.TRANSPARENT_BACKGROUND);
        tree.setItemHeightGetter(node -> Length.ofPixel(24));
        tree.expandAll();
        tree.addSelectListener(event -> {
            TreeNode<NewModuleContextNode> node = event.getValue();

            nodeSelected(node == null ? null : node.getValue());
        });

        myMoreViaPlugins = Hyperlink.create(LocalizeValue.localizeTODO("More via plugins..."), event -> {
            PluginAdvertiserHelper.getInstance().showDialogForExtension(ExtensionPreview.of(NewModuleBuilder.class, "*"));
        });

        WrappedLayout southPanel = WrappedLayout.create(myMoreViaPlugins);
        southPanel.addBorders(BorderStyle.EMPTY, null, 8);

        DockLayout leftPanel = DockLayout.create();
        leftPanel.center(ScrollableLayout.create(tree));
        leftPanel.bottom(southPanel);
        leftPanel.addBorder(BorderPosition.RIGHT, BorderStyle.LINE, ComponentColors.BORDER, 1);
        leftPanel.setSize(new Size2D(300, -1));
        return leftPanel;
    }

    @RequiredUIAccess
    private Layout buildEmptyPanel() {
        VerticalLayout layout = VerticalLayout.create(0, HorizontalAlignment.CENTER);
        layout.add(Label.create(myModuleHome == null
            ? LocalizeValue.localizeTODO("Please select project type")
            : LocalizeValue.localizeTODO("Please select module type")));
        return layout;
    }

    @RequiredUIAccess
    protected @Nullable Component buildSouthPanel() {
        HorizontalLayout buttonsPanel = HorizontalLayout.create(5);

        myCancelButton = Button.create(CommonLocalize.buttonCancel());
        myCancelButton.addClickListener(e -> doCancelAction());
        buttonsPanel.add(myCancelButton);

        myOkButton = Button.create(IdeLocalize.buttonCreate());
        myOkButton.addStyle(ButtonStyle.PRIMARY);
        myOkButton.setEnabled(false);
        myOkButton.addClickListener(e -> doOkAction());
        buttonsPanel.add(myOkButton);

        DockLayout south = DockLayout.create();
        south.right(buttonsPanel);
        south.addBorders(BorderStyle.EMPTY, null, 8);
        return south;
    }

    @RequiredUIAccess
    private void nodeSelected(@Nullable NewModuleContextNode value) {
        if (myWizardSession != null) {
            myWizardSession.finish();
            myWizardSession.dispose();
            myWizardSession = null;
        }

        myProcessor = value instanceof NewModuleContextItem item
            ? (NewModuleBuilderProcessor<NewModuleWizardContext>) item.getProcessor()
            : null;

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
                showStep(myWizardSession.next());
            }
            else {
                LOG.error("There no visible steps for " + value);
                myStepLayout.swipeRightTo(EMPTY_PANEL);
            }
        }
        else {
            myStepLayout.swipeRightTo(EMPTY_PANEL);
        }

        updateButtonPresentation();
    }

    @RequiredUIAccess
    private void showStep(WizardStep<NewModuleWizardContext> step) {
        String id = "step-" + (++myStepCounter);

        myStepLayout.register(id, () -> {
            WrappedLayout layout = WrappedLayout.create(step.getComponent(myWizardContext, myParentDisposable));
            layout.addBorders(BorderStyle.EMPTY, null, 5);
            return layout;
        });
        myStepLayout.swipeLeftTo(id);
    }

    @RequiredUIAccess
    private void updateButtonPresentation() {
        myMoreViaPlugins.setVisible(false);

        WizardSession<NewModuleWizardContext> wizardSession = myWizardSession;
        if (myProcessor != null && wizardSession != null) {
            boolean hasNext = wizardSession.hasNext();

            if (hasNext) {
                setOKActionText(CommonLocalize.buttonNext());
                setOKAction(() -> {
                    showStep(wizardSession.next());
                    updateButtonPresentation();
                });
            }
            else {
                setOKActionText(IdeLocalize.buttonCreate());
                setOKAction(null);
            }

            int currentStepIndex = wizardSession.getCurrentStepIndex();
            if (currentStepIndex != 0) {
                setCancelAction(() -> {
                    showStep(wizardSession.prev());
                    updateButtonPresentation();
                });
                setCancelText(CommonLocalize.buttonBack());
            }
            else {
                setCancelAction(null);
                setCancelText(CommonLocalize.buttonCancel());
            }

            setOKActionEnabled(true);
            myMoreViaPlugins.setVisible(currentStepIndex == 0);
        }
        else {
            setOKActionEnabled(false);

            setOKActionText(IdeLocalize.buttonCreate());
            setOKAction(null);
            setCancelAction(null);
            setCancelText(CommonLocalize.buttonCancel());

            myMoreViaPlugins.setVisible(true);
        }
    }

    @RequiredUIAccess
    public void setOKActionEnabled(boolean enabled) {
        if (myOkButton != null) {
            myOkButton.setEnabled(enabled);
        }
    }

    @RequiredUIAccess
    public void setOKActionText(LocalizeValue text) {
        if (myOkButton != null) {
            myOkButton.setText(text);
        }
    }

    @RequiredUIAccess
    public void setCancelText(LocalizeValue text) {
        if (myCancelButton != null) {
            myCancelButton.setText(text);
        }
    }

    public void setOKAction(@Nullable Runnable action) {
        myOkAction = action;
    }

    public void setCancelAction(@Nullable Runnable action) {
        myCancelAction = action;
    }

    @RequiredUIAccess
    public void doOkAction() {
        if (myOkAction != null) {
            myOkAction.run();
        }
        else if (myDefaultOkAction != null) {
            myDefaultOkAction.run();
        }
    }

    @RequiredUIAccess
    public void doCancelAction() {
        if (myCancelAction != null) {
            myCancelAction.run();
        }
        else if (myDefaultCancelAction != null) {
            myDefaultCancelAction.run();
        }
    }

    @Override
    public void finish() {
        if (myWizardSession != null) {
            myWizardSession.finish();
        }
    }

    @Override
    public void dispose() {
        if (myWizardSession != null) {
            myWizardSession.finish();
            myWizardSession.dispose();
            myWizardSession = null;
        }
    }
}

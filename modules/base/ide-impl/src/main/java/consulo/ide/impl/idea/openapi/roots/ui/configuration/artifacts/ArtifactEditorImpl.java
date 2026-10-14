/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.ModifiableArtifact;
import consulo.compiler.artifact.element.ArchivePackagingElement;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.compiler.artifact.element.PackagingElementType;
import consulo.compiler.artifact.ui.LibrarySourceItem;
import consulo.compiler.localize.CompilerLocalize;
import consulo.content.library.Library;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.actions.AddPackagingElementAction;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.actions.ExtractArtifactAction;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.actions.HideContentAction;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.actions.InlineArtifactAction;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.actions.LayoutTreeNavigateAction;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.actions.MovePackagingElementAction;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.actions.RemovePackagingElementAction;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.actions.RenamePackagingElementAction;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.actions.ShowContentAction;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.actions.SortElementsToggleAction;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.actions.SurroundElementWithAction;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.sourceItems.SourceItemsTree;
import consulo.language.editor.internal.action.LanguageEditorActions;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.Tab;
import consulo.ui.TextBox;
import consulo.ui.Tree;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.CommonActionsManager;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.util.FormBuilder;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author nik
 */
public class ArtifactEditorImpl implements ArtifactEditorEx {
    private final Project myProject;
    private final ComplexElementSubstitutionParameters mySubstitutionParameters;
    private final ArtifactEditorContextImpl myContext;
    private final SourceItemsTree mySourceItemsTree;
    private final Artifact myOriginalArtifact;
    private final LayoutTreeComponent myLayoutTreeComponent;
    private final ArtifactValidationManagerImpl myValidationManager;
    private ArtifactPropertiesEditors myPropertiesEditors;
    private boolean myBuildOnMake;
    private String myOutputPath;

    private @Nullable Component myMainComponent;
    private @Nullable Disposable myUIDisposable;
    private @Nullable TabbedLayout myTabbedLayout;
    private @Nullable Tab myOutputLayoutTab;
    private @Nullable Tab mySelectedTab;
    private @Nullable TextBox myNameField;
    private FileChooserTextBoxBuilder.@Nullable Controller myOutputDirectoryField;
    private boolean myDisposed;

    public ArtifactEditorImpl(ArtifactsStructureConfigurableContext context, Artifact originalArtifact, ArtifactEditorSettings settings) {
        myContext = new ArtifactEditorContextImpl(context, this);
        myOriginalArtifact = originalArtifact;
        myProject = context.getProject();
        mySubstitutionParameters = new ComplexElementSubstitutionParameters(myProject);
        mySubstitutionParameters.setTypesToShowContent(settings.getTypesToShowContent());
        mySourceItemsTree = new SourceItemsTree(myContext, this);
        myLayoutTreeComponent = new LayoutTreeComponent(this, mySubstitutionParameters, myContext, originalArtifact, settings.isSortElements());
        myPropertiesEditors = new ArtifactPropertiesEditors(myContext, originalArtifact, originalArtifact);
        myValidationManager = new ArtifactValidationManagerImpl(this);
        Disposer.register(this, mySourceItemsTree);
        Disposer.register(this, myLayoutTreeComponent);

        Artifact artifact = context.getArtifactModel().getArtifactByOriginal(originalArtifact);
        myBuildOnMake = artifact.isBuildOnMake();
        myOutputPath = StringUtil.notNullize(artifact.getOutputPath());
    }

    @RequiredUIAccess
    public Component createMainComponent(Disposable uiDisposable, Runnable nameChanged) {
        Component mainComponent = myMainComponent;
        if (mainComponent != null) {
            return mainComponent;
        }

        myUIDisposable = uiDisposable;

        TextBox nameField = TextBox.create(getArtifact().getName());
        nameField.addValueListener(event -> {
            if (rename(StringUtil.notNullize(event.getValue()))) {
                nameChanged.run();
            }
        });
        myNameField = nameField;

        FileChooserTextBoxBuilder.Controller outputDirectoryField = FileChooserTextBoxBuilder.create(myProject)
            .dialogTitle(CompilerLocalize.dialogTitleOutputDirectoryForArtifact())
            .dialogDescription(CompilerLocalize.chooserDescriptionSelectOutputDirectoryFor0Artifact(getArtifact().getName()))
            .fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor())
            .uiDisposable(uiDisposable)
            .build();
        outputDirectoryField.setValue(FileUtil.toSystemDependentName(myOutputPath), false);
        outputDirectoryField.getComponent()
            .addValueListener(event -> myOutputPath = FileUtil.toSystemIndependentName(StringUtil.notNullize(event.getValue()).trim()));
        myOutputDirectoryField = outputDirectoryField;

        CheckBox buildOnMakeCheckBox = CheckBox.create(ProjectLocalize.checkboxTextIncludeInProjectBuild());
        buildOnMakeCheckBox.setValue(myBuildOnMake, false);
        buildOnMakeCheckBox.addValueListener(event -> myBuildOnMake = Boolean.TRUE.equals(event.getValue()));

        FormBuilder formBuilder = FormBuilder.create();
        formBuilder.addLabeled(CommonLocalize.nameLabelText(), nameField);
        formBuilder.addLabeled(ProjectLocalize.labelTextArtifactType(), createTypeComboBox());
        formBuilder.addLabeled(ProjectLocalize.labelTextOutputDirectory(), outputDirectoryField.getComponent());

        TabbedLayout tabbedLayout = TabbedLayout.create();
        Tab outputLayoutTab = tabbedLayout.createTab();
        outputLayoutTab.setRenderer((tab, presentation) -> presentation.append(ProjectLocalize.artifactOutputLayoutTab()));
        tabbedLayout.addTab(outputLayoutTab, createOutputLayoutComponent(uiDisposable));
        tabbedLayout.addSelectListener(event -> mySelectedTab = event.getTab());
        myPropertiesEditors.addTabs(tabbedLayout, uiDisposable);
        myTabbedLayout = tabbedLayout;
        myOutputLayoutTab = outputLayoutTab;
        mySelectedTab = outputLayoutTab;

        DockLayout formPanel = DockLayout.create();
        formPanel.top(formBuilder.build());
        formPanel.bottom(buildOnMakeCheckBox);

        DockLayout mainPanel = DockLayout.create();
        mainPanel.top(formPanel);
        mainPanel.center(tabbedLayout);
        mainPanel.bottom(myValidationManager.getErrorPanel().createUIComponent());
        myMainComponent = mainPanel;

        queueValidation();
        return mainPanel;
    }

    public @Nullable Component getMainComponent() {
        return myMainComponent;
    }

    @RequiredUIAccess
    private ComboBox<ArtifactType> createTypeComboBox() {
        List<ArtifactType> types = new ArrayList<>();
        myProject.getApplication().getExtensionPoint(ArtifactType.class).forEach(types::add);

        ComboBox<ArtifactType> typeBox = ComboBox.create(types);
        typeBox.setRenderer((presentation, item) -> {
            ArtifactType type = item.getValue();
            if (type != null) {
                presentation.withIcon(type.getIcon());
                presentation.append(type.getPresentableName());
            }
        });
        typeBox.setValue(getArtifact().getArtifactType(), false);
        typeBox.addValueListener(event -> {
            ArtifactType selected = event.getValue();
            if (selected != null && !selected.equals(getArtifact().getArtifactType())) {
                setArtifactType(selected);
            }
        });
        return typeBox;
    }

    @RequiredUIAccess
    private Component createOutputLayoutComponent(Disposable uiDisposable) {
        Component layoutTreeComponent = myLayoutTreeComponent.createUIComponent(uiDisposable);
        Tree<PackagingElement<?>> layoutTree = Objects.requireNonNull(myLayoutTreeComponent.getTree());

        AnAction addAction = new AddPackagingElementAction(this);
        AnAction removeAction = new RemovePackagingElementAction(this);
        AnAction renameAction = new RenamePackagingElementAction(this);
        AnAction surroundAction = new SurroundElementWithAction(this);
        AnAction navigateAction = new LayoutTreeNavigateAction(myLayoutTreeComponent);

        addAction.registerCustomShortcutSet(CommonShortcuts.getNew(), layoutTree, uiDisposable);
        removeAction.registerCustomShortcutSet(CommonShortcuts.getDelete(), layoutTree, uiDisposable);
        renameAction.registerCustomShortcutSet(CommonShortcuts.getRename(), layoutTree, uiDisposable);
        surroundAction.registerCustomShortcutSet(
            new CustomShortcutSet(KeymapManager.getInstance().getActiveKeymap().getShortcuts(LanguageEditorActions.SURROUND_WITH)),
            layoutTree,
            uiDisposable
        );
        navigateAction.registerCustomShortcutSet(CommonShortcuts.getEditSource(), layoutTree, uiDisposable);

        ActionGroup toolbarGroup = ActionGroup.newImmutableBuilder()
            .add(addAction)
            .add(removeAction)
            .addSeparator()
            .add(new SortElementsToggleAction(myLayoutTreeComponent))
            .add(new MovePackagingElementAction(myLayoutTreeComponent, true))
            .add(new MovePackagingElementAction(myLayoutTreeComponent, false))
            .addSeparator()
            .add(new ShowContentAction(this))
            .build();

        ActionToolbar toolbar = ActionToolbarFactory.getInstance()
            .createActionToolbar(ActionPlaces.UNKNOWN, toolbarGroup, ActionToolbar.Style.HORIZONTAL);
        toolbar.setTargetUIComponent(layoutTree);

        TreeExpander treeExpander = TreeExpander.of(layoutTree);
        CommonActionsManager actionsManager = CommonActionsManager.getInstance();
        ActionGroup popupGroup = ActionGroup.newImmutableBuilder()
            .add(addAction)
            .add(removeAction)
            .add(new ExtractArtifactAction(this))
            .add(new InlineArtifactAction(this))
            .add(renameAction)
            .add(surroundAction)
            .addSeparator()
            .add(new HideContentAction(this))
            .add(navigateAction)
            .addSeparator()
            .add(actionsManager.createExpandAllAction(treeExpander))
            .add(actionsManager.createCollapseAllAction(treeExpander))
            .build();
        ArtifactTreeContextMenu.install(layoutTree, popupGroup);

        DockLayout leftPanel = DockLayout.create(Space.NONE);
        leftPanel.top(toolbar.getUIComponent());
        leftPanel.center(layoutTreeComponent);

        Label helpLabel = Label.create();
        helpLabel.setImage(PlatformIconGroup.actionsHelp());
        helpLabel.setToolTipText(ProjectLocalize.artifactSourceItemsTreeTooltip());

        HorizontalLayout headerPanel = HorizontalLayout.create();
        headerPanel.add(Label.create(ProjectLocalize.artifactAvailableElementsLabel()));
        headerPanel.add(helpLabel);

        DockLayout rightPanel = DockLayout.create(Space.NONE);
        rightPanel.top(headerPanel);
        rightPanel.center(ScrollableLayout.create(mySourceItemsTree.createUIComponent(uiDisposable)));

        TwoComponentSplitLayout splitLayout = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        splitLayout.setFirstComponent(leftPanel);
        splitLayout.setSecondComponent(rightPanel);
        splitLayout.setProportion(50);
        return splitLayout;
    }

    @RequiredUIAccess
    private boolean rename(String newName) {
        String name = newName.trim();
        String oldName = getArtifact().getName();
        if (name.isEmpty() || name.equals(oldName)) {
            return false;
        }

        myContext.getOrCreateModifiableArtifactModel().getOrCreateModifiableArtifact(myOriginalArtifact).setName(name);
        updateOutputPath(oldName, name);
        return true;
    }

    @RequiredUIAccess
    public void updateNameField() {
        TextBox nameField = myNameField;
        if (nameField != null) {
            nameField.setValue(getArtifact().getName(), false);
        }
    }

    @RequiredUIAccess
    public void onShowContentSettingsChanged() {
        myContext.getParent().getDefaultSettings().setTypesToShowContent(mySubstitutionParameters.getTypesToSubstitute());
    }

    public ArtifactEditorSettings createSettings() {
        return new ArtifactEditorSettings(myProject, myLayoutTreeComponent.isSortElements(), mySubstitutionParameters.getTypesToSubstitute());
    }

    @RequiredUIAccess
    public void apply() {
        ModifiableArtifact modifiableArtifact = myContext.getOrCreateModifiableArtifactModel().getOrCreateModifiableArtifact(myOriginalArtifact);
        modifiableArtifact.setBuildOnMake(myBuildOnMake);
        modifiableArtifact.setOutputPath(getConfiguredOutputPath());
        myPropertiesEditors.applyProperties();
        myLayoutTreeComponent.saveElementProperties();
    }

    private @Nullable String getConfiguredOutputPath() {
        String outputPath = FileUtil.toSystemIndependentName(myOutputPath.trim());
        return outputPath.isEmpty() ? null : outputPath;
    }

    @RequiredUIAccess
    public boolean isModified() {
        return myBuildOnMake != myOriginalArtifact.isBuildOnMake()
            || !Objects.equals(getConfiguredOutputPath(), myOriginalArtifact.getOutputPath())
            || myPropertiesEditors.isModified()
            || myLayoutTreeComponent.isPropertiesModified();
    }

    public SourceItemsTree getSourceItemsTree() {
        return mySourceItemsTree;
    }

    @Override
    public ArtifactEditorContextImpl getContext() {
        return myContext;
    }

    @Override
    public Artifact getArtifact() {
        return myContext.getArtifactModel().getArtifactByOriginal(myOriginalArtifact);
    }

    @Override
    public CompositePackagingElement<?> getRootElement() {
        return myLayoutTreeComponent.getRootElement();
    }

    @Override
    public ComplexElementSubstitutionParameters getSubstitutionParameters() {
        return mySubstitutionParameters;
    }

    @Override
    public LayoutTreeComponent getLayoutTreeComponent() {
        return myLayoutTreeComponent;
    }

    @Override
    @RequiredUIAccess
    public void rebuildTries() {
        myLayoutTreeComponent.rebuildTree();
        mySourceItemsTree.rebuildTree();
    }

    @Override
    @RequiredUIAccess
    public void queueValidation() {
        if (!myDisposed) {
            myValidationManager.queueValidation();
        }
    }

    @Override
    @RequiredUIAccess
    public void addNewPackagingElement(PackagingElementType<?> type) {
        myLayoutTreeComponent.addNewPackagingElement(type);
    }

    @Override
    @RequiredUIAccess
    public void removeSelectedElements() {
        myLayoutTreeComponent.removeSelectedElements();
    }

    @Override
    @RequiredUIAccess
    public void updateLayoutTree() {
        myLayoutTreeComponent.rebuildTree();
    }

    @Override
    @RequiredUIAccess
    public void putLibraryIntoDefaultLocation(Library library) {
        myLayoutTreeComponent.putIntoDefaultLocations(List.of(new LibrarySourceItem(library)));
    }

    @Override
    public boolean isDisposed() {
        return myDisposed;
    }

    @Override
    @RequiredUIAccess
    public void dispose() {
        myDisposed = true;
        myPropertiesEditors.disposeUIResources();
        myMainComponent = null;
        myUIDisposable = null;
        myTabbedLayout = null;
        myOutputLayoutTab = null;
        mySelectedTab = null;
        myNameField = null;
        myOutputDirectoryField = null;
    }

    @RequiredUIAccess
    public void updateOutputPath(String oldArtifactName, String newArtifactName) {
        String oldDefaultPath = ArtifactUtil.getDefaultArtifactOutputPath(oldArtifactName, myProject);
        if (!Objects.equals(oldDefaultPath, getConfiguredOutputPath())) {
            return;
        }

        String newOutputPath = StringUtil.notNullize(ArtifactUtil.getDefaultArtifactOutputPath(newArtifactName, myProject));
        myOutputPath = newOutputPath;

        FileChooserTextBoxBuilder.Controller outputDirectoryField = myOutputDirectoryField;
        if (outputDirectoryField != null) {
            outputDirectoryField.setValue(FileUtil.toSystemDependentName(newOutputPath), false);
        }

        if (getRootElement() instanceof ArchivePackagingElement archivePackagingElement) {
            String oldFileName = ArtifactUtil.suggestArtifactFileName(oldArtifactName);
            String name = archivePackagingElement.getArchiveFileName();
            String fileName = FileUtil.getNameWithoutExtension(name);
            String extension = FileUtil.getExtension(name);
            if (fileName.equals(oldFileName) && !extension.isEmpty()) {
                myLayoutTreeComponent.editLayout(
                    () -> archivePackagingElement.setArchiveFileName(ArtifactUtil.suggestArtifactFileName(newArtifactName) + "." + extension)
                );
                myLayoutTreeComponent.rebuildTree();
            }
        }
    }

    @RequiredUIAccess
    public void setArtifactType(ArtifactType artifactType) {
        ModifiableArtifact modifiableArtifact = myContext.getOrCreateModifiableArtifactModel().getOrCreateModifiableArtifact(myOriginalArtifact);
        modifiableArtifact.setArtifactType(artifactType);

        TabbedLayout tabbedLayout = myTabbedLayout;
        Disposable uiDisposable = myUIDisposable;
        if (tabbedLayout != null) {
            myPropertiesEditors.removeTabs(tabbedLayout);
        }
        myPropertiesEditors.disposeUIResources();
        myPropertiesEditors = new ArtifactPropertiesEditors(myContext, myOriginalArtifact, getArtifact());
        if (tabbedLayout != null && uiDisposable != null) {
            myPropertiesEditors.addTabs(tabbedLayout, uiDisposable);
        }

        CompositePackagingElement<?> oldRootElement = getRootElement();
        CompositePackagingElement<?> newRootElement =
            artifactType.createRootElement(PackagingElementFactory.getInstance(myProject), getArtifact().getName());
        ArtifactUtil.copyChildren(oldRootElement, newRootElement, myProject);
        myLayoutTreeComponent.setRootElement(newRootElement);
    }

    public ArtifactValidationManagerImpl getValidationManager() {
        return myValidationManager;
    }

    public @Nullable String getHelpTopic() {
        Tab selectedTab = mySelectedTab;
        if (selectedTab == null || selectedTab == myOutputLayoutTab) {
            return "reference.project.structure.artifacts.output";
        }
        String helpId = myPropertiesEditors.getHelpId(selectedTab);
        return helpId != null ? helpId : "reference.settingsdialog.project.structure.artifacts";
    }
}

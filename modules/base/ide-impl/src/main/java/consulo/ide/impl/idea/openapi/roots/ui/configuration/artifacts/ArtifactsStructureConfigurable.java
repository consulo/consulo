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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.WriteAction;
import consulo.compiler.artifact.*;
import consulo.compiler.artifact.element.LibraryElementType;
import consulo.compiler.artifact.element.LibraryPackagingElement;
import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.compiler.artifact.event.ArtifactListener;
import consulo.compiler.artifact.impl.internal.InvalidArtifact;
import consulo.configurable.ConfigurationException;
import consulo.configurable.NonDefaultProjectConfigurable;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.configurable.internal.ConfigurableWeight;
import consulo.configurable.internal.FullContentConfigurable;
import consulo.content.internal.LibraryEx;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.BaseStructureConfigurable;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import consulo.ide.impl.idea.openapi.ui.NamedConfigurable;
import consulo.ide.impl.roots.ui.configuration.ProjectConfigurableWeights;
import consulo.ide.setting.ProjectStructureSettingsUtil;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.MasterDetailsState;
import consulo.ui.ex.awt.MasterDetailsStateService;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author nik
 */
@ExtensionImpl
public class ArtifactsStructureConfigurable extends BaseStructureConfigurable
    implements ConfigurableWeight, ProjectConfigurable, NonDefaultProjectConfigurable, FullContentConfigurable {

    public static final String ID = "project.artifacts";

    @Nonnull
    private final Project myProject;
    @Nonnull
    private final ArtifactManager myArtifactManager;
    @Nonnull
    private final ArtifactPointerManager myArtifactPointerManager;
    @Nonnull
    private final ShowSettingsUtil myShowSettingsUtil;
    @Nullable
    private ArtifactsStructureConfigurableContextImpl myPackagingEditorContext;
    @Nonnull
    private final ArtifactEditorSettings myDefaultSettings;

    @Inject
    public ArtifactsStructureConfigurable(
        @Nonnull Project project,
        @Nonnull ArtifactManager artifactManager,
        @Nonnull ArtifactPointerManager artifactPointerManager,
        @Nonnull ShowSettingsUtil showSettingsUtil,
        @Nonnull Provider<MasterDetailsStateService> masterDetailsStateService
    ) {
        super(masterDetailsStateService, new ArtifactStructureConfigurableState());
        myProject = project;
        myArtifactManager = artifactManager;
        myArtifactPointerManager = artifactPointerManager;
        myShowSettingsUtil = showSettingsUtil;
        myDefaultSettings = new ArtifactEditorSettings(project);
    }

    @Override
    public void setBannerComponent(JComponent bannerComponent) {
        myNorthPanel.add(bannerComponent, BorderLayout.NORTH);
    }

    @Nullable
    @Override
    public String getParentId() {
        return StandardConfigurableIds.PROJECT_GROUP;
    }

    @Override
    protected String getComponentStateKey() {
        return "ArtifactsStructureConfigurable.UI";
    }

    @RequiredUIAccess
    @Override
    public void initialize() {
        super.initialize();

        ProjectStructureSettingsUtil showSettingsUtil = myShowSettingsUtil;

        ModulesConfigurator modulesModel = showSettingsUtil.getModulesModel(myProject);
        LibrariesConfigurator librariesModel = showSettingsUtil.getLibrariesModel(myProject);

        myPackagingEditorContext = new ArtifactsStructureConfigurableContextImpl(
            modulesModel,
            librariesModel,
            myProject,
            myArtifactManager,
            myArtifactPointerManager,
            myDefaultSettings,
            new ArtifactListener() {
                @Override
                public void artifactAdded(@Nonnull Artifact artifact) {
                    MyNode node = addArtifactNode(artifact);
                    selectNodeInTree(node);
                    // todo myContext.getDaemonAnalyzer().queueUpdate(myPackagingEditorContext.getOrCreateArtifactElement(artifact));
                }
            }
        );

        //modulesModel.addAllModuleChangeListener(moduleRootModel -> {
        //  for (ProjectStructureElement element : getProjectStructureElements()) {
        //    // todo myContext.getDaemonAnalyzer().queueUpdate(element);
        //  }
        //});

        ItemsChangeListener listener = deletedItem -> {
            if (deletedItem instanceof Library || deletedItem instanceof Module) {
                onElementDeleted();
            }
        };
        // todo moduleStructureConfigurable.addItemsChangeListener(listener);
        // todo projectLibrariesConfig.addItemsChangeListener(listener);

        librariesModel.addLibraryEditorListener((library, oldName, newName) -> {
            Artifact[] artifacts = myPackagingEditorContext.getArtifactModel().getArtifacts();
            for (Artifact artifact : artifacts) {
                updateLibraryElements(artifact, library, oldName, newName);
            }
        });
    }

    private void updateLibraryElements(Artifact artifact, Library library, String oldName, String newName) {
        if (ArtifactUtil.processPackagingElements(
            myPackagingEditorContext.getRootElement(artifact),
            LibraryElementType.getInstance(),
            new PackagingElementProcessor<>() {
                @Override
                public boolean process(@Nonnull LibraryPackagingElement element, @Nonnull PackagingElementPath path) {
                    return !isResolvedToLibrary(element, library, oldName);
                }
            },
            myPackagingEditorContext,
            false,
            artifact.getArtifactType()
        )) {
            return;
        }
        myPackagingEditorContext.editLayout(
            artifact,
            () -> {
                ModifiableArtifact modifiableArtifact =
                    myPackagingEditorContext.getOrCreateModifiableArtifactModel().getOrCreateModifiableArtifact(artifact);
                ArtifactUtil.processPackagingElements(
                    modifiableArtifact,
                    LibraryElementType.getInstance(),
                    new PackagingElementProcessor<>() {
                        @Override
                        public boolean process(@Nonnull LibraryPackagingElement element, @Nonnull PackagingElementPath path) {
                            if (isResolvedToLibrary(element, library, oldName)) {
                                element.setLibraryName(newName);
                            }
                            return true;
                        }
                    },
                    myPackagingEditorContext,
                    false
                );
            }
        );
        ArtifactEditorImpl artifactEditor = myPackagingEditorContext.getArtifactEditor(artifact);
        if (artifactEditor != null) {
            artifactEditor.rebuildTries();
        }
    }

    private static boolean isResolvedToLibrary(LibraryPackagingElement element, Library library, String name) {
        if (!element.getLibraryName().equals(name)) {
            return false;
        }

        LibraryTable table = library.getTable();
        if (table != null) {
            return table.getTableLevel().equals(element.getLevel());
        }
        return element.getLevel().equals(LibraryEx.MODULE_LEVEL);
    }

    private void onElementDeleted() {
        for (ArtifactEditorImpl editor : myPackagingEditorContext.getArtifactEditors()) {
            editor.getSourceItemsTree().rebuildTree();
            editor.queueValidation();
        }
    }

    @Override
    protected MasterDetailsState getState() {
        ((ArtifactStructureConfigurableState)myState).setDefaultArtifactSettings(myDefaultSettings.getState());
        return super.getState();
    }

    @Override
    public void loadState(MasterDetailsState object) {
        super.loadState(object);
        myDefaultSettings.loadState(((ArtifactStructureConfigurableState)myState).getDefaultArtifactSettings());
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return ProjectLocalize.displayNameArtifacts().get();
    }

    @Override
    protected void loadTree() {
        myTree.setRootVisible(false);
        myTree.setShowsRootHandles(false);
        for (Artifact artifact : myPackagingEditorContext.getArtifactModel().getAllArtifactsIncludingInvalid()) {
            addArtifactNode(artifact);
        }
    }

    @Nonnull
    @Override
    protected Collection<? extends ProjectStructureElement> getProjectStructureElements() {
        List<ProjectStructureElement> elements = new ArrayList<>();
        for (Artifact artifact : myPackagingEditorContext.getArtifactModel().getAllArtifactsIncludingInvalid()) {
            elements.add(myPackagingEditorContext.getOrCreateArtifactElement(artifact));
        }
        return elements;
    }

    private MyNode addArtifactNode(Artifact artifact) {
        NamedConfigurable<Artifact> configurable;
        if (artifact instanceof InvalidArtifact invalidArtifact) {
            configurable = new InvalidArtifactConfigurable(invalidArtifact, myPackagingEditorContext, TREE_UPDATER);
        }
        else {
            configurable = new ArtifactConfigurable(artifact, myPackagingEditorContext, TREE_UPDATER);
        }
        MyNode node = new MyNode(configurable);
        addNode(node, myRoot);
        return node;
    }

    @Override
    @RequiredUIAccess
    public void reset() {
        loadComponentState();
        myPackagingEditorContext.resetModifiableModel();
        super.reset();
    }

    @Override
    @RequiredUIAccess
    public boolean isModified() {
        ModifiableArtifactModel modifiableModel = myPackagingEditorContext.getActualModifiableModel();
        return modifiableModel != null && modifiableModel.isModified() || super.isModified();
    }

    public ArtifactsStructureConfigurableContext getArtifactsStructureContext() {
        return myPackagingEditorContext;
    }

    public ModifiableArtifactModel getModifiableArtifactModel() {
        return myPackagingEditorContext.getOrCreateModifiableArtifactModel();
    }

    @Override
    protected AbstractAddGroup createAddAction() {
        return new AbstractAddGroup(ProjectLocalize.addNewHeaderText()) {
            @Nonnull
            @Override
            public AnAction[] getChildren(@Nullable AnActionEvent e) {
                ProjectStructureSettingsUtil showSettingsUtil = ShowSettingsUtil.getInstance();

                ModulesConfigurator modulesModel = showSettingsUtil.getModulesModel(myProject);

                List<AnAction> list = myProject.getApplication().getExtensionPoint(ArtifactType.class)
                    .collectMapped(type -> type.isAvailableForAdd(modulesModel) ? createAddArtifactAction(type) : null);
                return list.isEmpty() ? AnAction.EMPTY_ARRAY : list.toArray(new AnAction[list.size()]);
            }
        };
    }

    private AnAction createAddArtifactAction(@Nonnull ArtifactType type) {
        List<? extends ArtifactTemplate> templates = type.getNewArtifactTemplates(myPackagingEditorContext);
        ArtifactTemplate emptyTemplate = new ArtifactTemplate() {
            @Override
            public LocalizeValue getPresentableName() {
                return LocalizeValue.localizeTODO("Empty");
            }

            @Override
            public NewArtifactConfiguration createArtifact() {
                String name = "unnamed";
                return new NewArtifactConfiguration(
                    type.createRootElement(PackagingElementFactory.getInstance(myProject), name),
                    name,
                    type
                );
            }
        };

        if (templates.isEmpty()) {
            return new AddArtifactAction(type, emptyTemplate, type.getPresentableName(), type.getIcon());
        }
        DefaultActionGroup group = new DefaultActionGroup(type.getPresentableName(), true);
        group.getTemplatePresentation().setIcon(type.getIcon());
        group.add(new AddArtifactAction(type, emptyTemplate, emptyTemplate.getPresentableName(), null));
        group.addSeparator();
        for (ArtifactTemplate template : templates) {
            group.add(new AddArtifactAction(type, template, template.getPresentableName(), null));
        }
        return group;
    }

    private void addArtifact(@Nonnull ArtifactType type, @Nonnull ArtifactTemplate artifactTemplate) {
        ArtifactTemplate.NewArtifactConfiguration configuration = artifactTemplate.createArtifact();
        if (configuration == null) {
            return;
        }

        String baseName = configuration.getArtifactName();
        String name = baseName;
        int i = 2;
        while (myPackagingEditorContext.getArtifactModel().findArtifact(name) != null) {
            name = baseName + i;
            i++;
        }

        ArtifactType actualType = configuration.getArtifactType();
        if (actualType == null) {
            actualType = type;
        }
        ModifiableArtifact artifact =
            myPackagingEditorContext.getOrCreateModifiableArtifactModel().addArtifact(name, actualType, configuration.getRootElement());
        artifactTemplate.setUpArtifact(artifact, configuration);
        selectNodeInTree(findNodeByObject(myRoot, artifact));
    }

    @Override
    @RequiredUIAccess
    public void apply() throws ConfigurationException {
        myPackagingEditorContext.saveEditorSettings();
        super.apply();

        ModifiableArtifactModel modifiableModel = myPackagingEditorContext.getActualModifiableModel();
        if (modifiableModel != null) {
            WriteAction.run(() -> modifiableModel.commit());
            myPackagingEditorContext.resetModifiableModel();
        }

        reset(); // TODO: fix to not reset on apply!
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        if (myPackagingEditorContext != null) {
            myPackagingEditorContext.saveEditorSettings();
        }
        super.disposeUIResources();
        if (myPackagingEditorContext != null) {
            myPackagingEditorContext.disposeUIResources();
        }
    }

    @Override
    public String getHelpTopic() {
        String topic = super.getHelpTopic();
        return topic != null ? topic : null;
    }

    @Override
    protected void removeArtifact(Artifact artifact) {
        myPackagingEditorContext.getOrCreateModifiableArtifactModel().removeArtifact(artifact);
        /// todo myContext.getDaemonAnalyzer().removeElement(myPackagingEditorContext.getOrCreateArtifactElement(artifact));
    }

    @Override
    protected void processRemovedItems() {
    }

    @Override
    protected boolean wasObjectStored(Object editableObject) {
        return false;
    }

    @Override
    @Nonnull
    public String getId() {
        return ID;
    }

    @Override
    public int getConfigurableWeight() {
        return ProjectConfigurableWeights.ARTIFACTS;
    }

    private class AddArtifactAction extends DumbAwareAction {
        private final ArtifactType myType;
        private final ArtifactTemplate myArtifactTemplate;

        public AddArtifactAction(
            @Nonnull ArtifactType type,
            @Nonnull ArtifactTemplate artifactTemplate,
            @Nonnull LocalizeValue actionText,
            Image icon
        ) {
            super(actionText, LocalizeValue.empty(), icon);
            myType = type;
            myArtifactTemplate = artifactTemplate;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            addArtifact(myType, myArtifactTemplate);
        }
    }
}

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
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactManager;
import consulo.compiler.artifact.ArtifactPointerManager;
import consulo.compiler.artifact.ArtifactProperties;
import consulo.compiler.artifact.ArtifactPropertiesProvider;
import consulo.compiler.artifact.ArtifactTemplate;
import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.ModifiableArtifact;
import consulo.compiler.artifact.ModifiableArtifactModel;
import consulo.compiler.artifact.PackagingElementPath;
import consulo.compiler.artifact.PackagingElementProcessor;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.LibraryElementType;
import consulo.compiler.artifact.element.LibraryPackagingElement;
import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.compiler.artifact.event.ArtifactListener;
import consulo.compiler.artifact.internal.InvalidArtifact;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.MasterDetailsConfigurable;
import consulo.configurable.NonDefaultProjectConfigurable;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.configurable.UnifiedMasterDetailsComponent;
import consulo.configurable.internal.ConfigurableWeight;
import consulo.content.internal.LibraryEx;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.disposer.Disposable;
import consulo.ide.impl.roots.ui.configuration.ProjectConfigurableWeights;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.ide.setting.module.event.LibraryEditorListener;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.InputBoxBuilder;
import consulo.ui.InputProblem;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.image.Image;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author nik
 */
@ExtensionImpl
public class ArtifactsStructureConfigurable extends UnifiedMasterDetailsComponent
    implements ConfigurableWeight, ProjectConfigurable, NonDefaultProjectConfigurable, Configurable.NoMargin, Configurable.NoScroll {

    public static final String ID = "project.artifacts";

    private final Project myProject;
    private final ArtifactManager myArtifactManager;
    private final ArtifactPointerManager myArtifactPointerManager;
    private final ShowSettingsUtil myShowSettingsUtil;
    private final ArtifactEditorSettings myDefaultSettings;

    private @Nullable ArtifactsStructureConfigurableContextImpl myPackagingEditorContext;
    private @Nullable Disposable myContextDisposable;

    @Inject
    public ArtifactsStructureConfigurable(
        Project project,
        ArtifactManager artifactManager,
        ArtifactPointerManager artifactPointerManager,
        ShowSettingsUtil showSettingsUtil
    ) {
        myProject = project;
        myArtifactManager = artifactManager;
        myArtifactPointerManager = artifactPointerManager;
        myShowSettingsUtil = showSettingsUtil;
        myDefaultSettings = new ArtifactEditorSettings(project);
        myDefaultSettings.load(ProjectPropertiesComponent.getInstance(project));
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.PROJECT_GROUP;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return ProjectLocalize.displayNameArtifacts();
    }

    @Override
    public int getConfigurableWeight() {
        return ProjectConfigurableWeights.ARTIFACTS;
    }

    @Override
    protected LocalizeValue getEmptySelectionText() {
        return ProjectLocalize.artifactsEmptyText();
    }

    @Override
    protected boolean isMultipleSelectAllowed() {
        return true;
    }

    @Override
    protected @Nullable Comparator<Object> getValueComparator() {
        return Comparator.comparing(this::getValueText, String.CASE_INSENSITIVE_ORDER);
    }

    @Override
    protected String getValueText(Object value) {
        if (value instanceof Artifact artifact) {
            return getCurrentArtifact(artifact).getName();
        }
        return String.valueOf(value);
    }

    @Override
    protected void renderValue(Object value, TextItemPresentation presentation) {
        if (value instanceof Artifact artifact) {
            Artifact current = getCurrentArtifact(artifact);
            presentation.withIcon(current.getArtifactType().getIcon());
            presentation.append(current.getName(), current instanceof InvalidArtifact ? TextAttribute.ERROR : TextAttribute.REGULAR);
        }
    }

    @RequiredUIAccess
    @Override
    protected void loadTree() {
        ArtifactsStructureConfigurableContextImpl context = getOrCreateContext();
        for (Artifact artifact : context.getArtifactModel().getAllArtifactsIncludingInvalid()) {
            addValue(null, context.getOriginalArtifact(artifact));
        }
    }

    @RequiredUIAccess
    @Override
    protected @Nullable MasterDetailsConfigurable<?> createConfigurable(Object value) {
        if (!(value instanceof Artifact artifact)) {
            return null;
        }

        ArtifactsStructureConfigurableContextImpl context = getOrCreateContext();
        if (artifact instanceof InvalidArtifact invalidArtifact) {
            return new InvalidArtifactConfigurable(invalidArtifact, context);
        }
        return new ArtifactConfigurable(artifact, context, this::onArtifactRenamed);
    }

    @Override
    protected List<AnAction> createActions() {
        return List.of(new AddArtifactAction(), new RemoveArtifactAction(), new CopyArtifactAction());
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        getOrCreateContext().resetModifiableModel();
        super.reset();
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        ArtifactsStructureConfigurableContextImpl context = myPackagingEditorContext;
        ModifiableArtifactModel modifiableModel = context == null ? null : context.getActualModifiableModel();
        return modifiableModel != null && modifiableModel.isModified() || super.isModified();
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        ArtifactsStructureConfigurableContextImpl context = getOrCreateContext();
        context.saveEditorSettings();

        checkArtifactNames();

        super.apply();

        ModifiableArtifactModel modifiableModel = context.getActualModifiableModel();
        if (modifiableModel != null) {
            WriteAction.run(modifiableModel::commit);
        }

        reset();
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        ArtifactsStructureConfigurableContextImpl context = myPackagingEditorContext;
        if (context != null) {
            context.saveEditorSettings();
        }
        myDefaultSettings.save(ProjectPropertiesComponent.getInstance(myProject));

        super.disposeUIResources();

        if (context != null) {
            context.disposeUIResources();
        }

        Disposable contextDisposable = myContextDisposable;
        if (contextDisposable != null) {
            contextDisposable.disposeWithTree();
        }
        myContextDisposable = null;
        myPackagingEditorContext = null;
    }

    @RequiredUIAccess
    public CompletableFuture<?> selectArtifact(@Nullable Artifact artifact) {
        ArtifactsStructureConfigurableContextImpl context = myPackagingEditorContext;
        if (artifact == null || context == null) {
            return CompletableFuture.completedFuture(null);
        }

        Artifact original = context.getOriginalArtifact(artifact);
        String name = artifact.getName();
        Object value = findValue(candidate -> candidate == original);
        if (value == null) {
            value = findValue(candidate -> candidate instanceof Artifact candidateArtifact && name.equals(getValueText(candidateArtifact)));
        }
        return select(value);
    }

    @RequiredUIAccess
    private ArtifactsStructureConfigurableContextImpl getOrCreateContext() {
        ArtifactsStructureConfigurableContextImpl context = myPackagingEditorContext;
        if (context != null) {
            return context;
        }

        ModulesConfigurator modulesModel =
            Objects.requireNonNull(myShowSettingsUtil.getModulesModel(myProject), "Called artifacts configurable without modulesConfigurator");
        LibrariesConfigurator librariesModel =
            Objects.requireNonNull(myShowSettingsUtil.getLibrariesModel(myProject), "Called artifacts configurable without librariesConfigurator");

        context = new ArtifactsStructureConfigurableContextImpl(
            modulesModel,
            librariesModel,
            myProject,
            myArtifactManager,
            myArtifactPointerManager,
            myDefaultSettings,
            new ArtifactListener() {
                @Override
                @RequiredUIAccess
                public void artifactAdded(Artifact artifact) {
                    onArtifactAdded(artifact);
                }
            }
        );

        Disposable contextDisposable = Disposable.newDisposable();
        librariesModel.addLibraryEditorListener(
            new LibraryEditorListener() {
                @Override
                @RequiredUIAccess
                public void libraryRenamed(Library library, String oldName, String newName) {
                    ArtifactsStructureConfigurableContextImpl currentContext = myPackagingEditorContext;
                    if (currentContext == null) {
                        return;
                    }
                    for (Artifact artifact : currentContext.getArtifactModel().getArtifacts()) {
                        updateLibraryElements(currentContext, artifact, library, oldName, newName);
                    }
                }
            },
            contextDisposable
        );

        myPackagingEditorContext = context;
        myContextDisposable = contextDisposable;
        return context;
    }

    private Artifact getCurrentArtifact(Artifact original) {
        ArtifactsStructureConfigurableContextImpl context = myPackagingEditorContext;
        return context == null ? original : context.getArtifactModel().getArtifactByOriginal(original);
    }

    @RequiredUIAccess
    private void onArtifactAdded(Artifact artifact) {
        ArtifactsStructureConfigurableContextImpl context = myPackagingEditorContext;
        if (context == null) {
            return;
        }

        Artifact original = context.getOriginalArtifact(artifact);
        if (!isAttached(original)) {
            addValue(null, original);
            refreshTree(original);
        }
    }

    @RequiredUIAccess
    private void onArtifactRenamed(Artifact original) {
        if (isAttached(original)) {
            refreshValue(original);
        }
    }

    @RequiredUIAccess
    private void checkArtifactNames() throws ConfigurationException {
        Set<String> names = new HashSet<>();
        for (Object value : getChildren(null)) {
            if (!(value instanceof Artifact artifact)) {
                continue;
            }

            String name = getValueText(artifact);
            if (name.isBlank()) {
                select(artifact);
                throw new ConfigurationException(ProjectLocalize.artifactNameNotSpecifiedError());
            }
            if (!names.add(name)) {
                select(artifact);
                throw new ConfigurationException(ProjectLocalize.artifactNameAlreadyExistsError(name));
            }
        }
    }

    @RequiredUIAccess
    private void updateLibraryElements(
        ArtifactsStructureConfigurableContextImpl context,
        Artifact artifact,
        Library library,
        String oldName,
        String newName
    ) {
        if (ArtifactUtil.processPackagingElements(
            context.getRootElement(artifact),
            LibraryElementType.getInstance(),
            new PackagingElementProcessor<>() {
                @Override
                public boolean process(LibraryPackagingElement element, PackagingElementPath path) {
                    return !isResolvedToLibrary(element, library, oldName);
                }
            },
            context,
            false,
            artifact.getArtifactType()
        )) {
            return;
        }

        context.editLayout(artifact, () -> {
            ModifiableArtifact modifiableArtifact = context.getOrCreateModifiableArtifactModel().getOrCreateModifiableArtifact(artifact);
            ArtifactUtil.processPackagingElements(
                modifiableArtifact,
                LibraryElementType.getInstance(),
                new PackagingElementProcessor<>() {
                    @Override
                    public boolean process(LibraryPackagingElement element, PackagingElementPath path) {
                        if (isResolvedToLibrary(element, library, oldName)) {
                            element.setLibraryName(newName);
                        }
                        return true;
                    }
                },
                context,
                false
            );
        });

        ArtifactEditorImpl artifactEditor = context.getArtifactEditor(artifact);
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

    @RequiredUIAccess
    private AnAction createAddArtifactAction(ArtifactsStructureConfigurableContextImpl context, ArtifactType type) {
        ArtifactTemplate emptyTemplate = new ArtifactTemplate() {
            @Override
            public LocalizeValue getPresentableName() {
                return ProjectLocalize.artifactTemplateEmpty();
            }

            @Override
            @RequiredUIAccess
            public CompletableFuture<NewArtifactConfiguration> createArtifact() {
                String name = "unnamed";
                CompositePackagingElement<?> rootElement = type.createRootElement(PackagingElementFactory.getInstance(myProject), name);
                return CompletableFuture.completedFuture(new NewArtifactConfiguration(rootElement, name, type));
            }
        };

        List<? extends ArtifactTemplate> templates = type.getNewArtifactTemplates(context);
        if (templates.isEmpty()) {
            return new AddArtifactFromTemplateAction(type, emptyTemplate, type.getPresentableName(), type.getIcon());
        }

        DefaultActionGroup group = new DefaultActionGroup(type.getPresentableName(), true);
        group.getTemplatePresentation().setIcon(type.getIcon());
        group.add(new AddArtifactFromTemplateAction(type, emptyTemplate, emptyTemplate.getPresentableName(), null));
        group.addSeparator();
        for (ArtifactTemplate template : templates) {
            group.add(new AddArtifactFromTemplateAction(type, template, template.getPresentableName(), null));
        }
        return group;
    }

    @RequiredUIAccess
    private void addArtifact(ArtifactType type, ArtifactTemplate artifactTemplate) {
        UIAccess uiAccess = UIAccess.current();
        artifactTemplate.createArtifact().whenCompleteAsync((configuration, error) -> {
            ArtifactsStructureConfigurableContextImpl context = myPackagingEditorContext;
            if (error != null || configuration == null || context == null) {
                return;
            }

            ArtifactType actualType = configuration.getArtifactType();
            ModifiableArtifact artifact = context.getOrCreateModifiableArtifactModel().addArtifact(
                configuration.getArtifactName(),
                actualType != null ? actualType : type,
                configuration.getRootElement()
            );
            artifactTemplate.setUpArtifact(artifact, configuration);
            selectArtifact(artifact);
        }, uiAccess);
    }

    @RequiredUIAccess
    private void copyArtifact(Artifact original, String newName) {
        ArtifactsStructureConfigurableContextImpl context = myPackagingEditorContext;
        if (context == null) {
            return;
        }

        Artifact source = context.getArtifactModel().getArtifactByOriginal(original);
        CompositePackagingElement<?> rootElement = ArtifactUtil.copyFromRoot(context.getRootElement(original), myProject);
        ModifiableArtifact copy = context.getOrCreateModifiableArtifactModel().addArtifact(newName, source.getArtifactType(), rootElement);
        copy.setBuildOnMake(source.isBuildOnMake());
        for (ArtifactPropertiesProvider provider : source.getPropertiesProviders()) {
            ArtifactProperties<?> properties = source.getProperties(provider);
            if (properties != null) {
                ArtifactProperties<?> copiedProperties = provider.createProperties(source.getArtifactType());
                ArtifactUtil.copyProperties(properties, copiedProperties);
                copy.setProperties(provider, copiedProperties);
            }
        }
        selectArtifact(copy);
    }

    private static String suggestCopyName(ArtifactsStructureConfigurableContextImpl context, String name) {
        int index = 2;
        String candidate = name + index;
        while (context.getArtifactModel().findArtifact(candidate) != null) {
            index++;
            candidate = name + index;
        }
        return candidate;
    }

    private class AddArtifactAction extends DumbAwareAction {
        private AddArtifactAction() {
            super(ProjectLocalize.addNewHeaderText(), LocalizeValue.empty(), PlatformIconGroup.generalAdd());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            ArtifactsStructureConfigurableContextImpl context = getOrCreateContext();
            ModulesConfigurator modulesModel = myShowSettingsUtil.getModulesModel(myProject);

            ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
            myProject.getApplication().getExtensionPoint(ArtifactType.class).forEach(type -> {
                if (modulesModel == null || type.isAvailableForAdd(modulesModel)) {
                    group.add(createAddArtifactAction(context, type));
                }
            });

            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                ProjectLocalize.addNewHeaderText().get(),
                group.build(),
                e.getDataContext(),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
            );
            popup.showUnderneathOf(e);
        }
    }

    private class AddArtifactFromTemplateAction extends DumbAwareAction {
        private final ArtifactType myType;
        private final ArtifactTemplate myArtifactTemplate;

        private AddArtifactFromTemplateAction(ArtifactType type, ArtifactTemplate artifactTemplate, LocalizeValue actionText, @Nullable Image icon) {
            super(actionText, LocalizeValue.empty(), icon);
            myType = type;
            myArtifactTemplate = artifactTemplate;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            addArtifact(myType, myArtifactTemplate);
        }
    }

    private class RemoveArtifactAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private RemoveArtifactAction() {
            super(CommonLocalize.buttonDelete(), CommonLocalize.buttonDelete(), PlatformIconGroup.generalRemove());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            ArtifactsStructureConfigurableContextImpl context = myPackagingEditorContext;
            List<Object> selected = getSelectedValues();
            if (context == null || !canRemove(selected)) {
                return;
            }

            ModifiableArtifactModel model = context.getOrCreateModifiableArtifactModel();
            for (Object value : selected) {
                model.removeArtifact(context.getArtifactModel().getArtifactByOriginal((Artifact) value));
            }
            removeSelected();
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(canRemove(getSelectedValues()));
        }

        private boolean canRemove(List<Object> selected) {
            if (selected.isEmpty()) {
                return false;
            }

            for (Object value : selected) {
                if (!(value instanceof Artifact)) {
                    return false;
                }
            }
            return true;
        }
    }

    private class CopyArtifactAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private CopyArtifactAction() {
            super(CommonLocalize.buttonCopy(), CommonLocalize.buttonCopy(), PlatformIconGroup.actionsCopy());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            ArtifactsStructureConfigurableContextImpl context = myPackagingEditorContext;
            if (context == null || !canCopy() || !(getSelectedValue() instanceof Artifact original)) {
                return;
            }

            UIAccess uiAccess = UIAccess.current();
            InputBoxBuilder.text()
                .title(ProjectLocalize.artifactsCopyTitle())
                .text(ProjectLocalize.artifactsCopyPrompt())
                .value(suggestCopyName(context, getValueText(original)))
                .validator(name -> {
                    String trimmed = name.trim();
                    if (trimmed.isEmpty()) {
                        return InputProblem.error(ProjectLocalize.artifactNameNotSpecifiedError());
                    }
                    if (context.getArtifactModel().findArtifact(trimmed) != null) {
                        return InputProblem.error(ProjectLocalize.artifactNameAlreadyExistsError(trimmed));
                    }
                    return null;
                })
                .showAsync(getTree())
                .whenCompleteAsync((newName, error) -> {
                    if (error != null || newName == null || myPackagingEditorContext != context) {
                        return;
                    }
                    copyArtifact(original, newName.trim());
                }, uiAccess);
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(canCopy());
        }

        private boolean canCopy() {
            return getSelectedValues().size() == 1 && getSelectedValue() instanceof Artifact artifact && !(artifact instanceof InvalidArtifact);
        }
    }
}

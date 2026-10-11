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

import consulo.application.content.impl.internal.library.LibraryImpl;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactModel;
import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.ModifiableArtifactModel;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.ui.ArtifactEditor;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.localize.LocalizeValue;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.ModuleRootModel;
import consulo.module.content.layer.ModulesProvider;
import consulo.module.content.layer.orderEntry.ModuleLibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.content.library.ProjectLibraryTable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.ChooseItemsDialogDescriptor;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author nik
 */
public class ArtifactEditorContextImpl implements ArtifactEditorContext {
    private final ArtifactsStructureConfigurableContext myParent;
    private final ArtifactEditorImpl myEditor;

    public ArtifactEditorContextImpl(ArtifactsStructureConfigurableContext parent, ArtifactEditorImpl editor) {
        myParent = parent;
        myEditor = editor;
    }

    @Override
    public ModifiableArtifactModel getOrCreateModifiableArtifactModel() {
        return myParent.getOrCreateModifiableArtifactModel();
    }

    @Override
    public @Nullable ModifiableModuleModel getModifiableModuleModel() {
        return myParent.getModifiableModuleModel();
    }

    @Override
    public ModifiableRootModel getOrCreateModifiableRootModel(Module module) {
        return myParent.getOrCreateModifiableRootModel(module);
    }

    @Override
    public Project getProject() {
        return myParent.getProject();
    }

    @Override
    public CompositePackagingElement<?> getRootElement(Artifact artifact) {
        return myParent.getRootElement(artifact);
    }

    @Override
    public void editLayout(Artifact artifact, Runnable runnable) {
        myParent.editLayout(artifact, runnable);
    }

    @Override
    public ArtifactEditor getOrCreateEditor(Artifact artifact) {
        return myParent.getOrCreateEditor(artifact);
    }

    @Override
    public ArtifactEditor getThisArtifactEditor() {
        return myEditor;
    }

    @Override
    @RequiredUIAccess
    public void selectArtifact(Artifact artifact) {
        ShowSettingsUtil.getInstance().showProjectStructureDialog(
            myParent.getProject(),
            projectStructureSelector -> projectStructureSelector.select(artifact, true)
        );
    }

    @Override
    @RequiredUIAccess
    public void selectModule(Module module) {
        ShowSettingsUtil.getInstance().showProjectStructureDialog(
            myParent.getProject(),
            projectStructureSelector -> projectStructureSelector.select(module.getName(), null, true)
        );
    }

    @Override
    @RequiredUIAccess
    public void selectLibrary(Library library) {
        LibraryTable table = library.getTable();
        if (table != null) {
            ShowSettingsUtil.getInstance().showProjectStructureDialog(
                myParent.getProject(),
                projectStructureSelector -> projectStructureSelector.selectProjectOrGlobalLibrary(library, true)
            );
            return;
        }

        Module module = (Module) ((LibraryImpl) library).getModule();
        if (module == null) {
            return;
        }

        ModuleRootModel rootModel = myParent.getModulesProvider().getRootModel(module);
        String libraryName = library.getName();
        for (OrderEntry entry : rootModel.getOrderEntries()) {
            if (entry instanceof ModuleLibraryOrderEntry libraryEntry) {
                if (libraryName != null && libraryName.equals(libraryEntry.getLibraryName())
                    || libraryName == null && library.equals(libraryEntry.getLibrary())) {
                    ShowSettingsUtil.getInstance().showProjectStructureDialog(
                        myParent.getProject(),
                        projectStructureSelector -> projectStructureSelector.selectOrderEntry(module, libraryEntry)
                    );
                    return;
                }
            }
        }
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<List<Artifact>> chooseArtifacts(List<? extends Artifact> artifacts, LocalizeValue title) {
        return ChooseItemsDialogDescriptor.<Artifact>showAsync(
            myEditor.getMainComponent(),
            title,
            LocalizeValue.empty(),
            List.copyOf(artifacts),
            (artifact, presentation) -> {
                presentation.withIcon(artifact.getArtifactType().getIcon());
                presentation.append(artifact.getName());
            },
            true,
            List.of()
        );
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<List<Module>> chooseModules(List<Module> modules, LocalizeValue title) {
        return ChooseItemsDialogDescriptor.showAsync(
            myEditor.getMainComponent(),
            title,
            LocalizeValue.empty(),
            modules,
            (module, presentation) -> {
                presentation.withIcon(PlatformIconGroup.nodesModule());
                presentation.append(module.getName());
            },
            true,
            List.of()
        );
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<List<Library>> chooseLibraries(LocalizeValue title) {
        List<Library> libraries = List.of(ProjectLibraryTable.getInstance(getProject()).getLibraries());
        return ChooseItemsDialogDescriptor.showAsync(
            myEditor.getMainComponent(),
            title,
            LocalizeValue.empty(),
            libraries,
            (library, presentation) -> {
                presentation.withIcon(PlatformIconGroup.nodesPplib());
                presentation.append(StringUtil.notNullize(library.getName()));
            },
            true,
            List.of()
        );
    }

    @Override
    public ArtifactModel getArtifactModel() {
        return myParent.getArtifactModel();
    }

    @Override
    public ModulesProvider getModulesProvider() {
        return myParent.getModulesProvider();
    }

    @Override
    public @Nullable Library findLibrary(String level, String libraryName) {
        return myParent.findLibrary(level, libraryName);
    }

    @Override
    @RequiredUIAccess
    public void queueValidation() {
        myEditor.queueValidation();
    }

    @Override
    public ArtifactType getArtifactType() {
        return myEditor.getArtifact().getArtifactType();
    }

    @Override
    public Artifact getArtifact() {
        return myEditor.getArtifact();
    }

    public ArtifactsStructureConfigurableContext getParent() {
        return myParent;
    }
}

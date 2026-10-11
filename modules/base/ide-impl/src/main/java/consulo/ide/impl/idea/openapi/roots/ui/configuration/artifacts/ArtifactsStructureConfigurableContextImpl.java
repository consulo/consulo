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

import consulo.application.WriteAction;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactManager;
import consulo.compiler.artifact.ArtifactModel;
import consulo.compiler.artifact.ArtifactPointer;
import consulo.compiler.artifact.ArtifactPointerManager;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.ModifiableArtifact;
import consulo.compiler.artifact.ModifiableArtifactModel;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.event.ArtifactListener;
import consulo.compiler.artifact.internal.DefaultPackagingElementResolvingContext;
import consulo.content.library.Library;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ModuleEditor;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ModulesConfiguratorImpl;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.ModulesProvider;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author nik
 */
public class ArtifactsStructureConfigurableContextImpl implements ArtifactsStructureConfigurableContext {
    private @Nullable ModifiableArtifactModel myModifiableModel;
    private final ArtifactListener myModifiableModelListener;
    private final ModulesConfiguratorImpl myModulesConfigurator;
    private final LibrariesConfigurator myLibrariesConfigurator;
    private final Project myProject;
    private final ArtifactManager myArtifactManager;
    private final ArtifactPointerManager myArtifactPointerManager;
    private final Map<Artifact, CompositePackagingElement<?>> myModifiableRoots = new HashMap<>();
    private final Map<Artifact, ArtifactEditorImpl> myArtifactEditors = new HashMap<>();
    private final Map<ArtifactPointer, ArtifactEditorSettings> myEditorSettings = new HashMap<>();
    private final ArtifactEditorSettings myDefaultSettings;

    public ArtifactsStructureConfigurableContextImpl(
        ModulesConfigurator modulesConfigurator,
        LibrariesConfigurator librariesConfigurator,
        Project project,
        ArtifactManager artifactManager,
        ArtifactPointerManager artifactPointerManager,
        ArtifactEditorSettings defaultSettings,
        ArtifactListener modifiableModelListener
    ) {
        myArtifactPointerManager = artifactPointerManager;
        myDefaultSettings = defaultSettings;
        myModifiableModelListener = modifiableModelListener;
        myLibrariesConfigurator = librariesConfigurator;
        myModulesConfigurator = (ModulesConfiguratorImpl) modulesConfigurator;
        myProject = project;
        myArtifactManager = artifactManager;
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public ArtifactModel getArtifactModel() {
        ModifiableArtifactModel modifiableModel = myModifiableModel;
        if (modifiableModel != null) {
            return modifiableModel;
        }
        return myArtifactManager;
    }

    @Override
    public Artifact getOriginalArtifact(Artifact artifact) {
        ModifiableArtifactModel modifiableModel = myModifiableModel;
        if (modifiableModel != null) {
            return modifiableModel.getOriginalArtifact(artifact);
        }
        return artifact;
    }

    @Override
    public @Nullable ModifiableModuleModel getModifiableModuleModel() {
        return myModulesConfigurator.getModuleModel();
    }

    @Override
    @RequiredUIAccess
    public void queueValidation(Artifact artifact) {
        ArtifactEditorImpl editor = getArtifactEditor(artifact);
        if (editor != null) {
            editor.queueValidation();
        }
    }

    @Override
    public CompositePackagingElement<?> getRootElement(Artifact artifact) {
        Artifact originalArtifact = getOriginalArtifact(artifact);
        ModifiableArtifactModel modifiableModel = myModifiableModel;
        if (modifiableModel != null) {
            Artifact modifiableArtifact = modifiableModel.getModifiableCopy(originalArtifact);
            if (modifiableArtifact != null) {
                myModifiableRoots.put(originalArtifact, modifiableArtifact.getRootElement());
            }
        }
        return getOrCreateModifiableRootElement(originalArtifact);
    }

    private CompositePackagingElement<?> getOrCreateModifiableRootElement(Artifact originalArtifact) {
        CompositePackagingElement<?> root = myModifiableRoots.get(originalArtifact);
        if (root == null) {
            root = ArtifactUtil.copyFromRoot(originalArtifact.getRootElement(), myProject);
            myModifiableRoots.put(originalArtifact, root);
        }
        return root;
    }

    @Override
    @RequiredUIAccess
    public void editLayout(Artifact artifact, Runnable action) {
        Artifact originalArtifact = getOriginalArtifact(artifact);
        WriteAction.run(() -> {
            ModifiableArtifact modifiableArtifact = getOrCreateModifiableArtifactModel().getOrCreateModifiableArtifact(originalArtifact);
            if (modifiableArtifact.getRootElement() == originalArtifact.getRootElement()) {
                modifiableArtifact.setRootElement(getOrCreateModifiableRootElement(originalArtifact));
            }
            action.run();
        });

        queueValidation(originalArtifact);
    }

    public @Nullable ArtifactEditorImpl getArtifactEditor(Artifact artifact) {
        return myArtifactEditors.get(getOriginalArtifact(artifact));
    }

    @Override
    public ArtifactEditorImpl getOrCreateEditor(Artifact artifact) {
        Artifact originalArtifact = getOriginalArtifact(artifact);
        ArtifactEditorImpl artifactEditor = myArtifactEditors.get(originalArtifact);
        if (artifactEditor == null) {
            ArtifactEditorSettings settings = myEditorSettings.get(myArtifactPointerManager.create(originalArtifact, getArtifactModel()));
            artifactEditor = new ArtifactEditorImpl(this, originalArtifact, settings != null ? settings : myDefaultSettings);
            myArtifactEditors.put(originalArtifact, artifactEditor);
        }
        return artifactEditor;
    }

    public @Nullable ModifiableArtifactModel getActualModifiableModel() {
        return myModifiableModel;
    }

    @Override
    public ModifiableArtifactModel getOrCreateModifiableArtifactModel() {
        ModifiableArtifactModel modifiableModel = myModifiableModel;
        if (modifiableModel == null) {
            modifiableModel = myArtifactManager.createModifiableModel();
            modifiableModel.addListener(myModifiableModelListener);
            myModifiableModel = modifiableModel;
        }
        return modifiableModel;
    }

    @Override
    public ArtifactEditorSettings getDefaultSettings() {
        return myDefaultSettings;
    }

    @Override
    public ModulesProvider getModulesProvider() {
        return myModulesConfigurator;
    }

    @Override
    public @Nullable Library findLibrary(String level, String libraryName) {
        Library library = DefaultPackagingElementResolvingContext.findLibrary(myProject, level, libraryName);
        return library != null ? myLibrariesConfigurator.getLibraryModel(library) : myLibrariesConfigurator.getLibrary(libraryName, level);
    }

    public void resetModifiableModel() {
        disposeUIResources();
        myModifiableModel = null;
        myModifiableRoots.clear();
    }

    public void disposeUIResources() {
        for (ArtifactEditorImpl editor : myArtifactEditors.values()) {
            Disposer.dispose(editor);
        }
        myArtifactEditors.clear();

        ModifiableArtifactModel modifiableModel = myModifiableModel;
        if (modifiableModel != null) {
            modifiableModel.dispose();
        }
    }

    public Collection<? extends ArtifactEditorImpl> getArtifactEditors() {
        return Collections.unmodifiableCollection(myArtifactEditors.values());
    }

    public void saveEditorSettings() {
        myEditorSettings.clear();
        for (ArtifactEditorImpl artifactEditor : myArtifactEditors.values()) {
            ArtifactPointer pointer = myArtifactPointerManager.create(artifactEditor.getArtifact(), getArtifactModel());
            myEditorSettings.put(pointer, artifactEditor.createSettings());
        }
    }

    @Override
    public ModifiableRootModel getOrCreateModifiableRootModel(Module module) {
        ModuleEditor editor = myModulesConfigurator.getOrCreateModuleEditor(module);
        return editor.getModifiableRootModelProxy();
    }
}

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
import consulo.compiler.artifact.*;
import consulo.compiler.artifact.event.ArtifactListener;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.project.Project;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.content.library.Library;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ModuleEditor;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ModulesConfiguratorImpl;
import consulo.module.content.layer.ModulesProvider;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.internal.DefaultPackagingElementResolvingContext;
import consulo.disposer.Disposer;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author nik
 */
public class ArtifactsStructureConfigurableContextImpl implements ArtifactsStructureConfigurableContext {
  private ModifiableArtifactModel myModifiableModel;
  private final ArtifactListener myModifiableModelListener;
  private final ModulesConfiguratorImpl myModulesConfigurator;
  private final LibrariesConfigurator myLibrariesConfigurator;
  private final Project myProject;
  private final ArtifactManager myArtifactManager;
  private final ArtifactPointerManager myArtifactPointerManager;
  private final Map<Artifact, CompositePackagingElement<?>> myModifiableRoots = new HashMap<>();
  private final Map<Artifact, ArtifactEditorImpl> myArtifactEditors = new HashMap<>();
  private final Map<ArtifactPointer, ArtifactEditorSettings> myEditorSettings = new HashMap<>();
  private final Map<Artifact, ArtifactProjectStructureElement> myArtifactElements = new HashMap<>();
  private final ArtifactEditorSettings myDefaultSettings;

  public ArtifactsStructureConfigurableContextImpl(ModulesConfigurator modulesConfigurator,
                                                   LibrariesConfigurator librariesConfigurator,
                                                   Project project,
                                                   ArtifactManager artifactManager,
                                                   ArtifactPointerManager artifactPointerManager,
                                                   ArtifactEditorSettings defaultSettings,
                                                   ArtifactListener modifiableModelListener) {
    myArtifactPointerManager = artifactPointerManager;
    myDefaultSettings = defaultSettings;
    myModifiableModelListener = modifiableModelListener;
    myLibrariesConfigurator = librariesConfigurator;
    myModulesConfigurator = (ModulesConfiguratorImpl)modulesConfigurator;
    myProject = project;
    myArtifactManager = artifactManager;
    // todo
    //context.getDaemonAnalyzer().addListener(element -> {
    //  if (element instanceof ArtifactProjectStructureElement) {
    //    final Artifact originalArtifact = ((ArtifactProjectStructureElement)element).getOriginalArtifact();
    //    final ArtifactEditorImpl artifactEditor = myArtifactEditors.get(originalArtifact);
    //    if (artifactEditor != null) {
    //      updateProblems(originalArtifact, artifactEditor);
    //    }
    //  }
    //});
  }

  //private void updateProblems(Artifact originalArtifact, ArtifactEditorImpl artifactEditor) {
  //  final ProjectStructureProblemsHolderImpl holder = myContext.getDaemonAnalyzer().getProblemsHolder(getOrCreateArtifactElement(originalArtifact));
  //  artifactEditor.getValidationManager().updateProblems(holder);
  //}

  @Override
  @Nonnull
  public Project getProject() {
    return myProject;
  }

  @Override
  @Nonnull
  public ArtifactModel getArtifactModel() {
    if (myModifiableModel != null) {
      return myModifiableModel;
    }
    return myArtifactManager;
  }

  @Override
  @Nonnull
  public Artifact getOriginalArtifact(@Nonnull Artifact artifact) {
    if (myModifiableModel != null) {
      return myModifiableModel.getOriginalArtifact(artifact);
    }
    return artifact;
  }

  @Override
  public ModifiableModuleModel getModifiableModuleModel() {
    return myModulesConfigurator.getModuleModel();
  }

  @Override
  public void queueValidation(Artifact artifact) {
    // todo myContext.getDaemonAnalyzer().queueUpdate(getOrCreateArtifactElement(artifact));
  }

  @Override
  public CompositePackagingElement<?> getRootElement(@Nonnull Artifact artifact) {
    artifact = getOriginalArtifact(artifact);
    if (myModifiableModel != null) {
      Artifact modifiableArtifact = myModifiableModel.getModifiableCopy(artifact);
      if (modifiableArtifact != null) {
        myModifiableRoots.put(artifact, modifiableArtifact.getRootElement());
      }
    }
    return getOrCreateModifiableRootElement(artifact);
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
  public void editLayout(@Nonnull Artifact artifact, Runnable action) {
    Artifact originalArtifact = getOriginalArtifact(artifact);
    WriteAction.run(() -> {
      ModifiableArtifact modifiableArtifact = getOrCreateModifiableArtifactModel().getOrCreateModifiableArtifact(originalArtifact);
      if (modifiableArtifact.getRootElement() == originalArtifact.getRootElement()) {
        modifiableArtifact.setRootElement(getOrCreateModifiableRootElement(originalArtifact));
      }
      action.run();
    });

    ArtifactProjectStructureElement element = getOrCreateArtifactElement(originalArtifact);
    // todo myContext.getDaemonAnalyzer().queueUpdate(element);
  }

  @Nullable
  public ArtifactEditorImpl getArtifactEditor(Artifact artifact) {
    return myArtifactEditors.get(getOriginalArtifact(artifact));
  }

  @Override
  public ArtifactEditorImpl getOrCreateEditor(Artifact artifact) {
    artifact = getOriginalArtifact(artifact);
    ArtifactEditorImpl artifactEditor = myArtifactEditors.get(artifact);
    if (artifactEditor == null) {
      ArtifactEditorSettings settings = myEditorSettings.get(myArtifactPointerManager.create(artifact, getArtifactModel()));
      artifactEditor = new ArtifactEditorImpl(this, artifact, settings != null ? settings : myDefaultSettings);
      myArtifactEditors.put(artifact, artifactEditor);
    }
    return artifactEditor;
  }

  @Nullable
  public ModifiableArtifactModel getActualModifiableModel() {
    return myModifiableModel;
  }

  @Override
  @Nonnull
  public ModifiableArtifactModel getOrCreateModifiableArtifactModel() {
    if (myModifiableModel == null) {
      myModifiableModel = myArtifactManager.createModifiableModel();
      myModifiableModel.addListener(myModifiableModelListener);
    }
    return myModifiableModel;
  }

  @Override
  public ArtifactEditorSettings getDefaultSettings() {
    return myDefaultSettings;
  }

  @Override
  @Nonnull
  public ModulesProvider getModulesProvider() {
    return myModulesConfigurator;
  }

  @Override
  public Library findLibrary(@Nonnull String level, @Nonnull String libraryName) {
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
    if (myModifiableModel != null) {
      myModifiableModel.dispose();
    }
    myArtifactElements.clear();
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
  @Nonnull
  public ArtifactProjectStructureElement getOrCreateArtifactElement(@Nonnull Artifact artifact) {
    ArtifactProjectStructureElement element = myArtifactElements.get(getOriginalArtifact(artifact));
    if (element == null) {
      element = new ArtifactProjectStructureElement(this, artifact);
      myArtifactElements.put(artifact, element);
    }
    return element;
  }

  @Override
  public ModifiableRootModel getOrCreateModifiableRootModel(Module module) {
    ModuleEditor editor = myModulesConfigurator.getOrCreateModuleEditor(module);
    return editor.getModifiableRootModelProxy();
  }
}

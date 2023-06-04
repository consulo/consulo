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

import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.project.Project;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.ModuleRootModel;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.impl.internal.layer.orderEntry.ModuleLibraryOrderEntryImpl;
import consulo.content.impl.internal.library.LibraryImpl;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.module.ui.awt.ChooseModulesDialog;
import consulo.module.content.layer.ModulesProvider;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactModel;
import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.ModifiableArtifactModel;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.ui.awt.ChooseArtifactsDialog;
import consulo.compiler.artifact.ui.ArtifactEditor;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.ide.impl.idea.util.ui.classpath.ChooseLibrariesFromTablesDialog;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class ArtifactEditorContextImpl implements ArtifactEditorContext {
  private final ArtifactsStructureConfigurableContext myParent;
  private final ArtifactEditorEx myEditor;

  public ArtifactEditorContextImpl(ArtifactsStructureConfigurableContext parent, ArtifactEditorEx editor) {
    myParent = parent;
    myEditor = editor;
  }

  @Override
  @Nonnull
  public ModifiableArtifactModel getOrCreateModifiableArtifactModel() {
    return myParent.getOrCreateModifiableArtifactModel();
  }

  @Override
  public ModifiableModuleModel getModifiableModuleModel() {
    return myParent.getModifiableModuleModel();
  }

  @Override
  @Nonnull
  public ModifiableRootModel getOrCreateModifiableRootModel(@Nonnull Module module) {
    return myParent.getOrCreateModifiableRootModel(module);
  }

  @Override
  @Nonnull
  public Project getProject() {
    return myParent.getProject();
  }

  @Override
  public CompositePackagingElement<?> getRootElement(@Nonnull Artifact artifact) {
    return myParent.getRootElement(artifact);
  }

  @Override
  public void editLayout(@Nonnull Artifact artifact, Runnable runnable) {
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
  public void selectArtifact(@Nonnull Artifact artifact) {
    ShowSettingsUtil.getInstance().showProjectStructureDialog(myParent.getProject(), projectStructureSelector -> {
      projectStructureSelector.select(artifact, true);
    });
  }

  @Override
  @RequiredUIAccess
  public void selectModule(@Nonnull Module module) {
    ShowSettingsUtil.getInstance().showProjectStructureDialog(myParent.getProject(), projectStructureSelector -> {
      projectStructureSelector.select(module.getName(), null, true);
    });
  }

  @Override
  @RequiredUIAccess
  public void selectLibrary(@Nonnull Library library) {
    final LibraryTable table = library.getTable();
    if (table != null) {
      ShowSettingsUtil.getInstance().showProjectStructureDialog(myParent.getProject(), projectStructureSelector -> {
        projectStructureSelector.selectProjectOrGlobalLibrary(library, true);
      });
    }
    else {
      final Module module = (Module)((LibraryImpl)library).getModule();
      if (module != null) {
        final ModuleRootModel rootModel = myParent.getModulesProvider().getRootModel(module);
        final String libraryName = library.getName();
        for (OrderEntry entry : rootModel.getOrderEntries()) {
          if (entry instanceof ModuleLibraryOrderEntryImpl) {
            final ModuleLibraryOrderEntryImpl libraryEntry = (ModuleLibraryOrderEntryImpl)entry;
            if (libraryName != null && libraryName.equals(libraryEntry.getLibraryName())
               || libraryName == null && library.equals(libraryEntry.getLibrary())) {
              ShowSettingsUtil.getInstance().showProjectStructureDialog(myParent.getProject(), projectStructureSelector -> {
                projectStructureSelector.selectOrderEntry(module, libraryEntry);
              });
              return;
            }
          }
        }
      }
    }
  }

  @Override
  public List<Artifact> chooseArtifacts(final List<? extends Artifact> artifacts, final String title) {
    ChooseArtifactsDialog dialog = new ChooseArtifactsDialog(getProject(), artifacts, title, null);
    dialog.show();
    return dialog.isOK() ? dialog.getChosenElements() : Collections.<Artifact>emptyList();
  }


  @Override
  @Nonnull
  public ArtifactModel getArtifactModel() {
    return myParent.getArtifactModel();
  }

  @Override
  @Nonnull
  public ModulesProvider getModulesProvider() {
    return myParent.getModulesProvider();
  }

  @Override
  public Library findLibrary(@Nonnull String level, @Nonnull String libraryName) {
    return myParent.findLibrary(level, libraryName);
  }

  @Override
  public void queueValidation() {
    myParent.queueValidation(getArtifact());
  }

  @Override
  @Nonnull
  public ArtifactType getArtifactType() {
    return myEditor.getArtifact().getArtifactType();
  }

  @Override
  public List<Module> chooseModules(final List<Module> modules, final String title) {
    return new ChooseModulesDialog(getProject(), modules, title, null).showAndGetResult();
  }

  @Override
  public List<Library> chooseLibraries(final String title) {
    final ChooseLibrariesFromTablesDialog dialog = ChooseLibrariesFromTablesDialog.createDialog(title, getProject(), false);
    dialog.show();
    return dialog.isOK() ? dialog.getSelectedLibraries() : Collections.<Library>emptyList();
  }

  @Override
  public Artifact getArtifact() {
    return myEditor.getArtifact();
  }

  public ArtifactsStructureConfigurableContext getParent() {
    return myParent;
  }
}

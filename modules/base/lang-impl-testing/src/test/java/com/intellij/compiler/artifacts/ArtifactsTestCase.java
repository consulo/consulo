package com.intellij.compiler.artifacts;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.DefaultModulesProvider;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.roots.ui.configuration.artifacts.ArtifactEditorSettings;
import com.intellij.openapi.roots.ui.configuration.artifacts.ArtifactProjectStructureElement;
import com.intellij.openapi.roots.ui.configuration.artifacts.ArtifactsStructureConfigurableContext;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.*;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.artifacts.PlainArtifactType;
import com.intellij.packaging.ui.ArtifactEditor;
import com.intellij.testFramework.IdeaTestCase;
import com.intellij.testFramework.PsiTestUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author nik
 */
public abstract class ArtifactsTestCase extends IdeaTestCase {
  protected boolean mySetupModule;

  protected ArtifactManager getArtifactManager() {
    return ArtifactManager.getInstance(myProject);
  }

  @Override
  protected void setUpModule() {
    if (mySetupModule) {
      super.setUpModule();
    }
  }

  protected void deleteArtifact(final Artifact artifact) {
    final ModifiableArtifactModel model = getArtifactManager().createModifiableModel();
    model.removeArtifact(artifact);
    commitModel(model);
  }

  protected static void commitModel(final ModifiableArtifactModel model) {
    WriteAction.run(() -> model.commit());
  }

  protected Artifact rename(Artifact artifact, String newName) {
    final ModifiableArtifactModel model = getArtifactManager().createModifiableModel();
    model.getOrCreateModifiableArtifact(artifact).setName(newName);
    commitModel(model);
    return artifact;
  }

  protected Artifact addArtifact(String name) {
    return addArtifact(name, null);
  }

  protected Artifact addArtifact(String name, final CompositePackagingElement<?> root) {
    return addArtifact(name, PlainArtifactType.getInstance(), root);
  }

  protected Artifact addArtifact(final String name, final ArtifactType type, final CompositePackagingElement<?> root) {
    return ArtifactsTestUtil.addArtifact(myProject, name, type, root);
  }

  protected PackagingElementResolvingContext getContext() {
    return ArtifactManager.getInstance(myProject).getResolvingContext();
  }

  public static void renameFile(final VirtualFile file, final String newName) {
    WriteAction.run(() -> {
      try {
        file.rename(IdeaTestCase.class, newName);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  protected Module addModule(final String moduleName, final @Nullable VirtualFile sourceRoot) {
    return WriteAction.compute(() -> {
      final Module module = createModule(moduleName);
      if (sourceRoot != null) {
        PsiTestUtil.addSourceContentToRoots(module, sourceRoot);
      }
      //   ModuleRootModificationUtil.setModuleSdk(module, getTestProjectJdk());
      return module;
    });
  }

  public class MockArtifactsStructureConfigurableContext implements ArtifactsStructureConfigurableContext {
    private ModifiableArtifactModel myModifiableModel;
    private final Map<Module, ModifiableRootModel> myModifiableRootModels = new HashMap<Module, ModifiableRootModel>();
    //  private final Map<CompositePackagingElement<?>, ManifestFileConfiguration> myManifestFiles =
    //   new HashMap<CompositePackagingElement<?>, ManifestFileConfiguration>();

    @Override
    @Nonnull
    public ModifiableArtifactModel getOrCreateModifiableArtifactModel() {
      if (myModifiableModel == null) {
        myModifiableModel = ArtifactManager.getInstance(myProject).createModifiableModel();
      }
      return myModifiableModel;
    }

    @Override
    public ModifiableModuleModel getModifiableModuleModel() {
      return null;
    }

    @Override
    @Nonnull
    public ModifiableRootModel getOrCreateModifiableRootModel(@Nonnull Module module) {
      ModifiableRootModel model = myModifiableRootModels.get(module);
      if (model == null) {
        model = ModuleRootManager.getInstance(module).getModifiableModel();
        myModifiableRootModels.put(module, model);
      }
      return model;
    }

    @Override
    public ArtifactEditorSettings getDefaultSettings() {
      return new ArtifactEditorSettings(myProject);
    }

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
      return ArtifactManager.getInstance(myProject);
    }

    public void commitModel() {
      if (myModifiableModel != null) {
        myModifiableModel.commit();
      }
    }

    @Override
    @Nonnull
    public ModulesProvider getModulesProvider() {
      return DefaultModulesProvider.of(myProject);
    }

    @Override
    public Library findLibrary(@Nonnull String level, @Nonnull String libraryName) {
      return ArtifactManager.getInstance(myProject).getResolvingContext().findLibrary(level, libraryName);
    }
   /*
    @Override
    public ManifestFileConfiguration getManifestFile(CompositePackagingElement<?> element, ArtifactType artifactType) {
      final VirtualFile manifestFile = ManifestFileUtil.findManifestFile(element, this, PlainArtifactType.getInstance());
      if (manifestFile == null) {
        return null;
      }

      ManifestFileConfiguration configuration = myManifestFiles.get(element);
      if (configuration == null) {
        configuration = ManifestFileUtil.createManifestFileConfiguration(manifestFile);
        myManifestFiles.put(element, configuration);
      }
      return configuration;
    }     */

    @Override
    public CompositePackagingElement<?> getRootElement(@Nonnull Artifact artifact) {
      return artifact.getRootElement();
    }

    @Override
    public void editLayout(@Nonnull Artifact artifact, Runnable action) {
      final ModifiableArtifact modifiableArtifact = getOrCreateModifiableArtifactModel().getOrCreateModifiableArtifact(artifact);
      modifiableArtifact.setRootElement(artifact.getRootElement());
      action.run();
    }

    @Override
    public ArtifactEditor getOrCreateEditor(Artifact artifact) {
      throw new UnsupportedOperationException("'getOrCreateEditor' not implemented in " + getClass().getName());
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
    public void queueValidation(Artifact artifact) {
    }

    @Override
    @Nonnull
    public ArtifactProjectStructureElement getOrCreateArtifactElement(@Nonnull Artifact artifact) {
      throw new UnsupportedOperationException("'getOrCreateArtifactElement' not implemented in " + getClass().getName());
    }
  }
}

package com.intellij.compiler.artifacts;

import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    new WriteAction() {
      @Override
      protected void run(final Result result) {
        model.commit();
      }
    }.execute();
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
    return getArtifactManager().addArtifact(name, type, root);
  }

  protected PackagingElementResolvingContext getContext() {
    return ArtifactManager.getInstance(myProject).getResolvingContext();
  }

  public static void renameFile(final VirtualFile file, final String newName) {
    new WriteAction() {
      @Override
      protected void run(final Result result) {
        try {
          file.rename(IdeaTestCase.class, newName);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }.execute();
  }

  protected Module addModule(final String moduleName, final @Nullable VirtualFile sourceRoot) {
    return new WriteAction<Module>() {
      @Override
      protected void run(final Result<Module> result) {
        final Module module = createModule(moduleName);
        if (sourceRoot != null) {
          PsiTestUtil.addSourceContentToRoots(module, sourceRoot);
        }
        ModuleRootModificationUtil.setModuleSdk(module, getTestProjectJdk());
        result.setResult(module);
      }
    }.execute().getResultObject();
  }


  public class MockArtifactsStructureConfigurableContext implements ArtifactsStructureConfigurableContext {
    private ModifiableArtifactModel myModifiableModel;
    private final Map<Module, ModifiableRootModel> myModifiableRootModels = new HashMap<Module, ModifiableRootModel>();
    private final Map<CompositePackagingElement<?>, ManifestFileConfiguration> myManifestFiles =
      new HashMap<CompositePackagingElement<?>, ManifestFileConfiguration>();

    @Override
    @NotNull
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
    @NotNull
    public ModifiableRootModel getOrCreateModifiableRootModel(@NotNull Module module) {
      ModifiableRootModel model = myModifiableRootModels.get(module);
      if (model == null) {
        model = ModuleRootManager.getInstance(module).getModifiableModel();
        myModifiableRootModels.put(module, model);
      }
      return model;
    }

    @Override
    public ArtifactEditorSettings getDefaultSettings() {
      return new ArtifactEditorSettings();
    }

    @Override
    @NotNull
    public Project getProject() {
      return myProject;
    }

    @Override
    @NotNull
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
    @NotNull
    public ModulesProvider getModulesProvider() {
      return new DefaultModulesProvider(myProject);
    }

    @Override
    public Library findLibrary(@NotNull String level, @NotNull String libraryName) {
      return ArtifactManager.getInstance(myProject).getResolvingContext().findLibrary(level, libraryName);
    }

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
    }

    @Override
    public CompositePackagingElement<?> getRootElement(@NotNull Artifact artifact) {
      return artifact.getRootElement();
    }

    @Override
    public void editLayout(@NotNull Artifact artifact, Runnable action) {
      final ModifiableArtifact modifiableArtifact = getOrCreateModifiableArtifactModel().getOrCreateModifiableArtifact(artifact);
      modifiableArtifact.setRootElement(artifact.getRootElement());
      action.run();
    }

    @Override
    public ArtifactEditor getOrCreateEditor(Artifact artifact) {
      throw new UnsupportedOperationException("'getOrCreateEditor' not implemented in " + getClass().getName());
    }

    @Override
    @NotNull
    public Artifact getOriginalArtifact(@NotNull Artifact artifact) {
      if (myModifiableModel != null) {
        return myModifiableModel.getOriginalArtifact(artifact);
      }
      return artifact;
    }

    @Override
    public void queueValidation(Artifact artifact) {
    }

    @Override
    @NotNull
    public ArtifactProjectStructureElement getOrCreateArtifactElement(@NotNull Artifact artifact) {
      throw new UnsupportedOperationException("'getOrCreateArtifactElement' not implemented in " + getClass().getName());
    }
  }
}

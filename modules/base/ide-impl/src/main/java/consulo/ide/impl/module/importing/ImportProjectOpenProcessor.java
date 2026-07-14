/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.impl.module.importing;

import consulo.application.concurrent.coroutine.WriteLock;
import consulo.disposer.Disposer;
import consulo.ide.localize.IdeLocalize;
import consulo.module.creation.NewOrImportModuleUtil;
import consulo.module.creation.importing.ModuleImportContext;
import consulo.module.creation.importing.ModuleImportProvider;
import consulo.project.Project;
import consulo.project.ProjectOpenContext;
import consulo.project.impl.internal.DefaultProjectOpenProcessor;
import consulo.project.internal.ProjectOpenProcessor;
import consulo.ui.Alert;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.AsyncResult;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineStep;
import consulo.util.concurrent.coroutine.step.CallSubroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import consulo.util.concurrent.coroutine.step.Condition;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 31-Jan-17
 */
public class ImportProjectOpenProcessor extends ProjectOpenProcessor {
  private static final Key<ImportTarget> IMPORT_TARGET = Key.create("ImportProjectOpenProcessor.target");

  private final List<ModuleImportProvider> myProviders;

  public ImportProjectOpenProcessor() {
    myProviders = ModuleImportProviders.getExtensions(false);
  }

  @Override
  public @Nullable Image getIcon(VirtualFile file) {
    File ioFile = VirtualFileUtil.virtualToIoFile(file);
    for (ModuleImportProvider provider : myProviders) {
      if (provider.canImport(ioFile)) {
        return provider.getIcon();
      }
    }
    return null;
  }

  @Override
  public boolean canOpenProject(File file) {
    for (ModuleImportProvider provider : myProviders) {
      if (provider.canImport(file)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public <I> Coroutine<I, VirtualFile> prepareSteps(UIAccess uiAccess,
                                                    ProjectOpenContext openContext,
                                                    Coroutine<I, VirtualFile> in) {
    return in
      // Build import request (available providers + expected project path)
      .then(CodeExecution.<VirtualFile, ImportRequest>apply(this::createRequest))
      // Ask "open existing or reimport?" when an existing project is present
      .then(askOpenOrReimport(uiAccess))
      // Reimport -> run import wizard; open existing -> reuse resolved directory
      .then(Condition.doIfElse(
        ImportRequest::needsImport,
        runImport(uiAccess),
        CodeExecution.<ImportRequest, VirtualFile>apply(request -> request.openExistingDir)))
      // Cancel check (null means user cancelled)
      .then(CodeExecution.<VirtualFile, VirtualFile>apply((virtualFile, continuation) -> {
        if (virtualFile == null) {
          continuation.cancel();
          return null;
        }
        return virtualFile;
      }));
  }

  private ImportRequest createRequest(VirtualFile virtualFile) {
    File ioPath = VirtualFileUtil.virtualToIoFile(virtualFile);

    List<ModuleImportProvider> targetProviders =
      ContainerUtil.filter(myProviders, moduleImportProvider -> moduleImportProvider.canImport(ioPath));

    if (targetProviders.isEmpty()) {
      throw new IllegalArgumentException("must be not empty");
    }

    String expectedProjectPath = ModuleImportProvider.getDefaultPath(virtualFile);
    return new ImportRequest(virtualFile, targetProviders, expectedProjectPath);
  }

  private CoroutineStep<ImportRequest, ImportRequest> askOpenOrReimport(UIAccess uiAccess) {
    return CompletableFutureStep.await(request -> {
      // Headless or no existing project — go straight to import
      if (uiAccess.isHeadless() || !DefaultProjectOpenProcessor.getInstance().canOpenProject(new File(request.expectedProjectPath))) {
        return CompletableFuture.completedFuture(request);
      }

      CompletableFuture<ImportRequest> future = new CompletableFuture<>();
      uiAccess.give(() -> createReimportAlert(request).showAsync().doWhenDone(threeState -> {
        switch (threeState) {
          case YES:
            // Open existing project — resolve VirtualFile for the project directory
            request.openExistingDir = LocalFileSystem.getInstance().refreshAndFindFileByPath(request.expectedProjectPath);
            break;
          case NO:
            // Reimport — leave openExistingDir null so needsImport() is true
            break;
          case UNSURE:
            request.cancelled = true;
            break;
        }
        future.complete(request);
      }));
      return future;
    });
  }

  private Alert<ThreeState> createReimportAlert(ImportRequest request) {
    File ioPath = VirtualFileUtil.virtualToIoFile(request.file);

    Alert<ThreeState> alert = Alert.create();
    alert.title(IdeLocalize.titleOpenProject());
    alert.text(IdeLocalize.projectImportOpenExisting(
      "an existing project",
      FileUtil.toSystemDependentName(ioPath.getPath()),
      request.file.getName()
    ));
    alert.asQuestion();

    alert.button(IdeLocalize.projectImportOpenExistingOpenexisting().get(), ThreeState.YES);
    alert.asDefaultButton();

    alert.button(IdeLocalize.projectImportOpenExistingReimport().get(), ThreeState.NO);

    alert.button(Alert.CANCEL, ThreeState.UNSURE);
    alert.asExitButton();
    return alert;
  }

  private CoroutineStep<ImportRequest, VirtualFile> runImport(UIAccess uiAccess) {
    return CallSubroutine.<ImportRequest, VirtualFile>call(continuation -> importCoroutine(uiAccess));
  }

  private Coroutine<ImportRequest, VirtualFile> importCoroutine(UIAccess uiAccess) {
    return Coroutine.<ImportRequest, ImportTarget>first(
        CompletableFutureStep.await(request -> showImportChooser(request, uiAccess)))
      // Stash the chosen target for the import subroutine
      .then(CodeExecution.<ImportTarget, ImportTarget>apply((target, continuation) -> {
        if (target != null) {
          continuation.putCopyableUserData(IMPORT_TARGET, target);
        }
        return target;
      }))
      // A target was chosen -> import; otherwise cancelled -> null
      .then(Condition.doIfElse(
        target -> target != null,
        importProjectStep(uiAccess),
        CodeExecution.<ImportTarget, VirtualFile>apply(target -> null)));
  }

  private CompletableFuture<ImportTarget> showImportChooser(ImportRequest request, UIAccess uiAccess) {
    CompletableFuture<ImportTarget> future = new CompletableFuture<>();
    uiAccess.give(() -> {
      AsyncResult<Pair<ModuleImportContext, ModuleImportProvider<ModuleImportContext>>> result = AsyncResult.undefined();
      ModuleImportProcessor.showImportChooser(null, request.file, request.providers, result);

      result.doWhenDone(pair -> future.complete(new ImportTarget(pair.getFirst(), pair.getSecond())));
      result.doWhenRejected((pair, error) -> {
        if (pair != null) {
          pair.getFirst().dispose();
        }
        future.complete(null);
      });
    });
    return future;
  }

  private CoroutineStep<ImportTarget, VirtualFile> importProjectStep(UIAccess uiAccess) {
    return CallSubroutine.<ImportTarget, VirtualFile>call(continuation -> importProjectSubroutine(uiAccess));
  }

  private Coroutine<ImportTarget, VirtualFile> importProjectSubroutine(UIAccess uiAccess) {
    return Coroutine.<ImportTarget, Void>first(CodeExecution.<ImportTarget, Void>apply(target -> null))
      // Create the temp project + module via the import provider
      .then(CallSubroutine.<Void, Project>call(continuation -> {
        ImportTarget target = continuation.getCopyableUserData(IMPORT_TARGET);
        return NewOrImportModuleUtil.importProject(target.context(), target.provider(), uiAccess);
      }))
      // Resolve the project directory and dispose the temp project — ProjectOpenService will create a real one
      .then(WriteLock.<Project, VirtualFile>apply(project -> {
        VirtualFile projectDir = LocalFileSystem.getInstance().refreshAndFindFileByPath(project.getBasePath());
        Disposer.dispose(project);
        return projectDir;
      }));
  }

  private record ImportTarget(ModuleImportContext context, ModuleImportProvider<ModuleImportContext> provider) {
  }

  private static class ImportRequest {
    private final VirtualFile file;
    private final List<ModuleImportProvider> providers;
    private final String expectedProjectPath;

    private @Nullable VirtualFile openExistingDir;
    private boolean cancelled;

    ImportRequest(VirtualFile file, List<ModuleImportProvider> providers, String expectedProjectPath) {
      this.file = file;
      this.providers = providers;
      this.expectedProjectPath = expectedProjectPath;
    }

    boolean needsImport() {
      return !cancelled && openExistingDir == null;
    }
  }
}

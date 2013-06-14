/*
 * Copyright 2013 Consulo.org
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
package org.jetbrains.idea.devkit.run.stepBefore;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointer;
import com.intellij.packaging.impl.compiler.ArtifactCompileScope;
import com.intellij.packaging.impl.run.AbstractArtifactsBeforeRunTaskProvider;
import com.intellij.util.concurrency.Semaphore;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.devkit.DevKitBundle;
import org.jetbrains.idea.devkit.compiler.CopyArtifactCompiler;
import org.jetbrains.idea.devkit.run.PluginRunConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 14:54/14.06.13
 */
public class CopyArtifactsBeforeRunTaskProvider extends AbstractArtifactsBeforeRunTaskProvider<CopyArtifactsBeforeRunTask> {
  @NonNls
  public static final String COPY_ARTIFACTS_ID = "CopyArtifacts";
  public static final Key<CopyArtifactsBeforeRunTask> ID = Key.create(COPY_ARTIFACTS_ID);

  public CopyArtifactsBeforeRunTaskProvider(Project project) {
    super(project, ID);
  }

  @Override
  public Key<CopyArtifactsBeforeRunTask> getId() {
    return ID;
  }

  @Override
  public String getDescription(CopyArtifactsBeforeRunTask task) {
    final List<ArtifactPointer> pointers = task.getArtifactPointers();
    if (pointers.isEmpty()) {
      return DevKitBundle.message("copy.artifacts.before.run.description.empty");
    }
    if (pointers.size() == 1) {
      return DevKitBundle.message("copy.artifacts.before.run.description.single", pointers.get(0).getArtifactName());
    }
    return DevKitBundle.message("copy.artifacts.before.run.description.multiple", pointers.size());
  }

  @Nullable
  @Override
  public CopyArtifactsBeforeRunTask createTask(RunConfiguration runConfiguration) {
    if(myProject.isDefault()) {
      return null;
    }
    return new CopyArtifactsBeforeRunTask(myProject);
  }

  @Override
  public boolean executeTask(DataContext context,
                             RunConfiguration configuration,
                             ExecutionEnvironment env,
                             final CopyArtifactsBeforeRunTask task) {
    if(!(configuration instanceof PluginRunConfiguration)) {
      return true;
    }
    final PluginRunConfiguration runConfiguration = (PluginRunConfiguration) configuration;
    final Ref<Boolean> result = Ref.create(false);
    final Semaphore finished = new Semaphore();

    final List<Artifact> artifacts = new ArrayList<Artifact>();
    new ReadAction() {
      @Override
      protected void run(final Result result) {
        for (ArtifactPointer pointer : task.getArtifactPointers()) {
          ContainerUtil.addIfNotNull(pointer.getArtifact(), artifacts);
        }
      }
    }.execute();

    final CompileStatusNotification callback = new CompileStatusNotification() {
      @Override
      public void finished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
        result.set(!aborted && errors == 0);
        finished.up();
      }
    };
    final CompilerFilter compilerFilter = new CompilerFilter() {
      @Override
      public boolean acceptCompiler(com.intellij.openapi.compiler.Compiler compiler) {
        return compiler instanceof CopyArtifactCompiler;
      }
    };

    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
      @Override
      public void run() {
        final CompilerManager manager = CompilerManager.getInstance(myProject);
        finished.down();

        final CompileScope artifactsScope = ArtifactCompileScope.createArtifactsScope(myProject, artifacts);
        artifactsScope.putUserData(CopyArtifactCompiler.TARGET_MODULE, runConfiguration.getModule());
        manager.make(artifactsScope, compilerFilter, callback);
      }
    }, ModalityState.NON_MODAL);

    finished.waitFor();
    return result.get();
  }

  @Override
  public String getName() {
    return DevKitBundle.message("copy.artifacts.before.run.description.empty");
  }
}

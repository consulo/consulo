/*
 * Copyright 2013 must-be.org
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
package consulo.packaging.impl.run;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.ConfigurationSettingsEditorWrapper;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.compiler.Compiler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointer;
import com.intellij.packaging.impl.compiler.ArtifactAwareCompiler;
import com.intellij.packaging.impl.compiler.ArtifactCompileScope;
import com.intellij.packaging.impl.compiler.ArtifactsCompiler;
import com.intellij.packaging.impl.run.AbstractArtifactsBeforeRunTask;
import com.intellij.packaging.impl.run.AbstractArtifactsBeforeRunTaskProvider;
import com.intellij.util.concurrency.Semaphore;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 15:11/14.06.13
 */
public class BuildArtifactsBeforeRunTaskProvider extends AbstractArtifactsBeforeRunTaskProvider<BuildArtifactsBeforeRunTask> {
  @NonNls public static final String BUILD_ARTIFACTS_ID = "BuildArtifacts";
  public static final Key<BuildArtifactsBeforeRunTask> ID = Key.create(BUILD_ARTIFACTS_ID);

  public BuildArtifactsBeforeRunTaskProvider(Project project) {
    super(project, ID);
  }

  @Override
  public BuildArtifactsBeforeRunTask createTask(RunConfiguration runConfiguration) {
    if (myProject.isDefault()) {
      return null;
    }
    return new BuildArtifactsBeforeRunTask(myProject);
  }

  @Override
  public String getName() {
    return CompilerBundle.message("build.artifacts.before.run.description.empty");
  }

  @Override
  public String getDescription(BuildArtifactsBeforeRunTask task) {
    final List<ArtifactPointer> pointers = task.getArtifactPointers();
    if (pointers.isEmpty()) {
      return CompilerBundle.message("build.artifacts.before.run.description.empty");
    }
    if (pointers.size() == 1) {
      return CompilerBundle.message("build.artifacts.before.run.description.single", pointers.get(0).getName());
    }
    return CompilerBundle.message("build.artifacts.before.run.description.multiple", pointers.size());
  }

  @Override
  public boolean executeTask(DataContext context,
                             RunConfiguration configuration,
                             ExecutionEnvironment env,
                             final BuildArtifactsBeforeRunTask task) {
    final Ref<Boolean> result = Ref.create(false);
    final Semaphore finished = new Semaphore();

    final List<Artifact> artifacts = new ArrayList<Artifact>();
    new ReadAction() {
      @Override
      protected void run(final Result result) {
        for (ArtifactPointer pointer : task.getArtifactPointers()) {
          ContainerUtil.addIfNotNull(pointer.get(), artifacts);
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
    final Condition<Compiler> compilerFilter = new Condition<Compiler>() {
      @Override
      public boolean value(com.intellij.openapi.compiler.Compiler compiler) {
        return compiler instanceof ArtifactsCompiler ||
               compiler instanceof ArtifactAwareCompiler && ((ArtifactAwareCompiler)compiler).shouldRun(artifacts);
      }
    };

    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
      @Override
      public void run() {
        final CompilerManager manager = CompilerManager.getInstance(myProject);
        finished.down();
        manager.make(ArtifactCompileScope.createArtifactsScope(myProject, artifacts), compilerFilter, callback);
      }
    }, ModalityState.NON_MODAL);

    finished.waitFor();
    return result.get();
  }

  public static void setBuildArtifactBeforeRunOption(@NotNull JComponent runConfigurationEditorComponent,
                                                     Project project,
                                                     @NotNull Artifact artifact,
                                                     final boolean enable) {
    final DataContext dataContext = DataManager.getInstance().getDataContext(runConfigurationEditorComponent);
    final ConfigurationSettingsEditorWrapper editor = ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY.getData(dataContext);
    if (editor != null) {
      List<BeforeRunTask> tasks = editor.getStepsBeforeLaunch();
      List<AbstractArtifactsBeforeRunTask> myTasks = new ArrayList<AbstractArtifactsBeforeRunTask>();
      for (BeforeRunTask task : tasks) {
        if (task instanceof AbstractArtifactsBeforeRunTask) {
          myTasks.add((AbstractArtifactsBeforeRunTask)task);
        }
      }
      if (enable && myTasks.isEmpty()) {
        AbstractArtifactsBeforeRunTask task = new BuildArtifactsBeforeRunTask(project);
        task.addArtifact(artifact);
        task.setEnabled(true);
        editor.addBeforeLaunchStep(task);
      }
      else {
        for (AbstractArtifactsBeforeRunTask task : myTasks) {
          if (enable) {
            task.addArtifact(artifact);
            task.setEnabled(true);
          }
          else {
            task.removeArtifact(artifact);
            if (task.getArtifactPointers().isEmpty()) {
              task.setEnabled(false);
            }
          }
        }
      }
    }
  }

  public static void setBuildArtifactBeforeRun(@NotNull Project project,
                                               @NotNull RunConfiguration configuration,
                                               @NotNull Artifact artifact) {
    RunManagerEx runManager = RunManagerEx.getInstanceEx(project);
    final List<BuildArtifactsBeforeRunTask> buildArtifactsTasks = runManager.getBeforeRunTasks(configuration, ID);
    if (buildArtifactsTasks.isEmpty()) { //Add new task if absent
      BuildArtifactsBeforeRunTask task = new BuildArtifactsBeforeRunTask(project);
      buildArtifactsTasks.add(task);
      List<BeforeRunTask> tasks = runManager.getBeforeRunTasks(configuration);
      tasks.add(task);
      runManager.setBeforeRunTasks(configuration, tasks, false);
    }

    for (AbstractArtifactsBeforeRunTask task : buildArtifactsTasks) {
      task.setEnabled(true);
      task.addArtifact(artifact);

    }
  }
}

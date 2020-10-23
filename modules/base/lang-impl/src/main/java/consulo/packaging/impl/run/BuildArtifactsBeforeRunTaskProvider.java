/*
 * Copyright 2013-2016 consulo.io
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
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.Compiler;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointer;
import com.intellij.packaging.impl.compiler.ArtifactAwareCompiler;
import com.intellij.packaging.impl.compiler.ArtifactCompileScope;
import com.intellij.packaging.impl.compiler.ArtifactsCompiler;
import com.intellij.packaging.impl.run.AbstractArtifactsBeforeRunTask;
import com.intellij.packaging.impl.run.AbstractArtifactsBeforeRunTaskProvider;
import consulo.application.AccessRule;
import consulo.ui.UIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 15:11/14.06.13
 */
public class BuildArtifactsBeforeRunTaskProvider extends AbstractArtifactsBeforeRunTaskProvider<BuildArtifactsBeforeRunTask> {
  @NonNls
  public static final String BUILD_ARTIFACTS_ID = "BuildArtifacts";
  public static final Key<BuildArtifactsBeforeRunTask> ID = Key.create(BUILD_ARTIFACTS_ID);

  @Inject
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

  @Nonnull
  @Override
  public String getName() {
    return CompilerBundle.message("build.artifacts.before.run.description.empty");
  }

  @Nonnull
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

  @Nonnull
  @Override
  public AsyncResult<Void> executeTaskAsync(UIAccess uiAccess, DataContext context, RunConfiguration configuration, ExecutionEnvironment env, BuildArtifactsBeforeRunTask task) {
    AsyncResult<Void> result = AsyncResult.undefined();

    final List<Artifact> artifacts = new ArrayList<>();
    AccessRule.read(() -> {
      for (ArtifactPointer pointer : task.getArtifactPointers()) {
        ContainerUtil.addIfNotNull(artifacts, pointer.get());
      }
    });

    final CompileStatusNotification callback = (aborted, errors, warnings, compileContext) -> {
      if(!aborted && errors == 0) {
        result.setDone();
      }
      else {
        result.setRejected();
      }
    };

    final Condition<Compiler> compilerFilter = compiler -> compiler instanceof ArtifactsCompiler || compiler instanceof ArtifactAwareCompiler && ((ArtifactAwareCompiler)compiler).shouldRun(artifacts);

    uiAccess.give(() -> {
      final CompilerManager manager = CompilerManager.getInstance(myProject);
      manager.make(ArtifactCompileScope.createArtifactsScope(myProject, artifacts), compilerFilter, callback);
    }).doWhenRejectedWithThrowable(result::rejectWithThrowable);

    return result;
  }

  public static void setBuildArtifactBeforeRunOption(@Nonnull JComponent runConfigurationEditorComponent, Project project, @Nonnull Artifact artifact, final boolean enable) {
    final DataContext dataContext = DataManager.getInstance().getDataContext(runConfigurationEditorComponent);
    final ConfigurationSettingsEditorWrapper editor = dataContext.getData(ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY);
    if (editor != null) {
      List<BeforeRunTask> tasks = editor.getStepsBeforeLaunch();
      List<AbstractArtifactsBeforeRunTask> myTasks = new ArrayList<>();
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

  public static void setBuildArtifactBeforeRun(@Nonnull Project project, @Nonnull RunConfiguration configuration, @Nonnull Artifact artifact) {
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

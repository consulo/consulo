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
package consulo.compiler.impl.internal.exection;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AccessRule;
import consulo.compiler.CompileStatusNotification;
import consulo.compiler.Compiler;
import consulo.compiler.CompilerManager;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactAwareCompiler;
import consulo.compiler.artifact.ArtifactPointer;
import consulo.compiler.impl.internal.artifact.ArtifactCompileScope;
import consulo.compiler.impl.internal.artifact.ArtifactsCompiler;
import consulo.compiler.localize.CompilerLocalize;
import consulo.dataContext.DataContext;
import consulo.execution.BeforeRunTask;
import consulo.execution.RunManager;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.internal.ConfigurationSettingsEditorWrapper;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2013-06-14
 */
@ExtensionImpl(order = "after compileBeforeRunNoErrorCheck")
public class BuildArtifactsBeforeRunTaskProvider extends AbstractArtifactsBeforeRunTaskProvider<BuildArtifactsBeforeRunTask> {
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
        return CompilerLocalize.buildArtifactsBeforeRunDescriptionEmpty().get();
    }

    @Nonnull
    @Override
    public String getDescription(BuildArtifactsBeforeRunTask task) {
        List<ArtifactPointer> pointers = task.getArtifactPointers();
        if (pointers.isEmpty()) {
            return CompilerLocalize.buildArtifactsBeforeRunDescriptionEmpty().get();
        }
        if (pointers.size() == 1) {
            return CompilerLocalize.buildArtifactsBeforeRunDescriptionSingle(pointers.get(0).getName()).get();
        }
        return CompilerLocalize.buildArtifactsBeforeRunDescriptionMultiple(pointers.size()).get();
    }

    @Nonnull
    @Override
    public AsyncResult<Void> executeTaskAsync(
        UIAccess uiAccess,
        DataContext context,
        RunConfiguration configuration,
        ExecutionEnvironment env,
        BuildArtifactsBeforeRunTask task
    ) {
        AsyncResult<Void> result = AsyncResult.undefined();

        List<Artifact> artifacts = new ArrayList<>();
        AccessRule.read(() -> {
            for (ArtifactPointer pointer : task.getArtifactPointers()) {
                ContainerUtil.addIfNotNull(artifacts, pointer.get());
            }
        });

        CompileStatusNotification callback = (aborted, errors, warnings, compileContext) -> {
            if (!aborted && errors == 0) {
                result.setDone();
            }
            else {
                result.setRejected();
            }
        };

        Predicate<Compiler> compilerFilter = compiler -> compiler instanceof ArtifactsCompiler
            || compiler instanceof ArtifactAwareCompiler artifactAwareCompiler && artifactAwareCompiler.shouldRun(artifacts);

        uiAccess.give(() -> {
            CompilerManager manager = CompilerManager.getInstance(myProject);
            manager.make(ArtifactCompileScope.createArtifactsScope(myProject, artifacts), compilerFilter, callback);
        }).doWhenRejectedWithThrowable(result::rejectWithThrowable);

        return result;
    }

    public static void setBuildArtifactBeforeRunOption(
        @Nonnull DataContext dataContext,
        Project project,
        @Nonnull Artifact artifact,
        boolean enable
    ) {
        ConfigurationSettingsEditorWrapper editor = dataContext.getData(ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY);
        if (editor != null) {
            List<BeforeRunTask> tasks = editor.getStepsBeforeLaunch();
            List<AbstractArtifactsBeforeRunTask> myTasks = new ArrayList<>();
            for (BeforeRunTask task : tasks) {
                if (task instanceof AbstractArtifactsBeforeRunTask abstractArtifactsBeforeRunTask) {
                    myTasks.add(abstractArtifactsBeforeRunTask);
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

    public static void setBuildArtifactBeforeRun(
        @Nonnull Project project,
        @Nonnull RunConfiguration configuration,
        @Nonnull Artifact artifact
    ) {
        RunManager runManager = RunManager.getInstance(project);
        List<BuildArtifactsBeforeRunTask> buildArtifactsTasks = runManager.getBeforeRunTasks(configuration, ID);
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

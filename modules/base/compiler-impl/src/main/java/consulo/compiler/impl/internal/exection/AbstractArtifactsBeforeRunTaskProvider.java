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
package consulo.compiler.impl.internal.exection;

import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactManager;
import consulo.compiler.artifact.event.ArtifactListener;
import consulo.disposer.Disposable;
import consulo.execution.BeforeRunFragment;
import consulo.execution.RunManager;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nik
 */
public abstract class AbstractArtifactsBeforeRunTaskProvider<T extends AbstractArtifactsBeforeRunTask> implements BeforeRunFragment<T> {
    protected final Project myProject;

    public AbstractArtifactsBeforeRunTaskProvider(Project project) {
        myProject = project;
        project.getMessageBus().connect().subscribe(
            ArtifactListener.class,
            new ArtifactListener() {
                @Override
                public void artifactRemoved(Artifact artifact) {
                    String artifactName = artifact.getName();
                    if (ArtifactManager.getInstance(myProject).findArtifact(artifactName) != null) {
                        return;
                    }

                    RunManager runManager = RunManager.getInstance(myProject);
                    for (RunConfiguration configuration : runManager.getAllConfigurationsList()) {
                        for (T task : runManager.getBeforeRunTasks(configuration, getStateClass())) {
                            task.removeArtifact(artifactName);
                        }
                    }
                }
            }
        );
    }

    protected List<Artifact> resolve(T task) {
        ArtifactManager manager = ArtifactManager.getInstance(myProject);
        List<Artifact> result = new ArrayList<>();
        for (String name : task.getArtifactNames()) {
            Artifact artifact = manager.findArtifact(name);
            if (artifact != null) {
                result.add(artifact);
            }
        }
        return result;
    }

    @Override
    public Image getIcon(RunConfiguration configuration) {
        return PlatformIconGroup.nodesArtifact();
    }

    @Override
    public @Nullable Image getTaskIcon(RunConfiguration configuration, T task) {
        List<Artifact> artifacts = resolve(task);
        return artifacts.isEmpty() ? getIcon(configuration) : artifacts.get(0).getArtifactType().getIcon();
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<T> createEditor(RunConfiguration configuration, Disposable parent) {
        Map<String, CheckBox> boxes = new LinkedHashMap<>();
        VerticalLayout list = VerticalLayout.create();
        for (Artifact artifact : ArtifactManager.getInstance(myProject).getArtifacts()) {
            CheckBox box = CheckBox.create(LocalizeValue.of(artifact.getName()));
            boxes.put(artifact.getName(), box);
            list.add(box);
        }

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addBottom(ScrollableLayout.create(list));
            }

            @RequiredUIAccess
            @Override
            public void reset(T task) {
                boxes.forEach((name, box) -> box.setValue(task.getArtifactNames().contains(name)));
            }

            @RequiredUIAccess
            @Override
            public void apply(T task) {
                List<String> names = new ArrayList<>();
                for (String name : task.getArtifactNames()) {
                    if (!boxes.containsKey(name)) {
                        names.add(name);
                    }
                }
                boxes.forEach((name, box) -> {
                    if (box.getValueOrError()) {
                        names.add(name);
                    }
                });
                task.setArtifactNames(names);
            }
        };
    }

    @Override
    public boolean canExecuteTask(RunConfiguration configuration, T task) {
        return !resolve(task).isEmpty();
    }
}

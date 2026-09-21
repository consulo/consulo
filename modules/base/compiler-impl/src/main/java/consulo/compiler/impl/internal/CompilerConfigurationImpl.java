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
package consulo.compiler.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.compiler.CompilerConfiguration;
import consulo.compiler.setting.ExcludedEntriesConfiguration;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.module.Module;
import consulo.module.event.ModuleListener;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.util.io.URLUtil;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2013-06-10
 */
@Singleton
@ServiceImpl
@State(name = "CompilerManager", storages = @Storage("compiler.xml"))
public class CompilerConfigurationImpl extends CompilerConfiguration implements PersistentStateComponent<CompilerManagerState>, Disposable {
    private static final String DEFAULT_OUTPUT_URL = "out";

    private final Project myProject;
    private @Nullable String myOutputDirUrl;

    private final ExcludedEntriesConfiguration myExcludedEntriesConfiguration = new ExcludedEntriesConfiguration();

    private final Map<String, CompilerManagerModuleState> myModulesConfiguration = new ConcurrentSkipListMap<>();

    @Inject
    public CompilerConfigurationImpl(Project project) {
        myProject = project;

        project.getMessageBus().connect(this).subscribe(ModuleListener.class, new ModuleListener() {
            @Override
            public void moduleRemoved(Project project, Module module) {
                myModulesConfiguration.remove(module.getName());
            }

            @Override
            public void modulesRenamed(Project project, Map<Module, String> modulesWithOldName) {
                for (Map.Entry<Module, String> entry : modulesWithOldName.entrySet()) {
                    Module module = entry.getKey();
                    String oldName = entry.getValue();

                    CompilerManagerModuleState managerModuleState = forModule(oldName);
                    if (managerModuleState != null) {
                        managerModuleState.name = module.getName();
                    }
                }
            }
        });
        
        Disposer.register(this, myExcludedEntriesConfiguration);
    }

    @Override
    public void dispose() {
    }

    public @Nullable CompilerManagerModuleState forModule(String moduleName) {
        return myModulesConfiguration.get(moduleName);
    }

    public void editModuleState(String moduleName, Consumer<CompilerManagerModuleState> consumer) {
        consumer.accept(myModulesConfiguration.computeIfAbsent(moduleName, s -> {
            CompilerManagerModuleState state = new CompilerManagerModuleState();
            state.name = moduleName;
            return state;
        }));
    }

    @Override
    public boolean isExcludedFromCompilation(Path file) {
        return myExcludedEntriesConfiguration.isExcluded(file);
    }

    @Override
    public ExcludedEntriesConfiguration getExcludedEntriesConfiguration() {
        return myExcludedEntriesConfiguration;
    }

    @Override
    public String getCompilerOutputUrl() {
        if (myOutputDirUrl == null) {
            return VirtualFileManager.constructUrl(
                URLUtil.FILE_PROTOCOL,
                FileUtil.toSystemIndependentName(myProject.getBasePath()) + "/" + DEFAULT_OUTPUT_URL
            );
        }
        return myOutputDirUrl;
    }

    @Override
    public void setCompilerOutputUrl(@Nullable String compilerOutputUrl) {
        myOutputDirUrl = compilerOutputUrl;
    }

    @Override
    public CompilerManagerState getState() {
        CompilerManagerState state = new CompilerManagerState();

        state.url = myOutputDirUrl;

        if (!myExcludedEntriesConfiguration.isEmpty()) {
            state.excludeFromCompilation = myExcludedEntriesConfiguration.getState();
        }

        for (CompilerManagerModuleState managerModuleState : myModulesConfiguration.values()) {
            state.modules.add(managerModuleState);
        }

        return state;
    }

    @Override
    public void loadState(CompilerManagerState state) {
        if (state.url != null) {
            setCompilerOutputUrl(state.url);
        }

        if (state.excludeFromCompilation != null) {
            myExcludedEntriesConfiguration.loadState(state.excludeFromCompilation);
        }

        myModulesConfiguration.clear();

        for (CompilerManagerModuleState moduleState : state.modules) {
            String name = moduleState.name;
            if (name == null) {
                continue;
            }

            myModulesConfiguration.put(name, moduleState);
        }
    }
}

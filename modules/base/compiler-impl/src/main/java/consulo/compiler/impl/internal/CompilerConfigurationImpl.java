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
import consulo.module.ModuleManager;
import consulo.module.event.ModuleListener;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.util.io.URLUtil;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.UUID;
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

    private final CompilerModuleStateRegistry myModuleStates;

    @Inject
    public CompilerConfigurationImpl(Project project) {
        myProject = project;
        myModuleStates = new CompilerModuleStateRegistry(id -> isLoadedModuleId(project, id));

        project.getMessageBus().connect(this).subscribe(ModuleListener.class, new ModuleListener() {
            @Override
            public void moduleRemoved(Project project, Module module) {
                myModuleStates.moduleRemoved(module);
            }
        });
        
        Disposer.register(this, myExcludedEntriesConfiguration);
    }

    @Override
    public void dispose() {
    }

    private static boolean isLoadedModuleId(Project project, UUID id) {
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            if (module.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public @Nullable CompilerManagerModuleState forModule(Module module) {
        return myModuleStates.get(module);
    }

    public void editModuleState(Module module, Consumer<CompilerManagerModuleState> consumer) {
        consumer.accept(myModuleStates.getOrCreate(module));
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
    public @Nullable String getCompilerOutputUrl() {
        if (myOutputDirUrl == null) {
            String basePath = myProject.getBasePath();
            if (basePath == null) {
                return null;
            }

            // the url of the base directory on the machine of the project - platform://<id>/... for a remote one
            String baseUrl = VirtualFileManager.getInstance().getUrl(myProject.getPlatform().fs().getPath(basePath));
            if (baseUrl != null) {
                return baseUrl + "/" + DEFAULT_OUTPUT_URL;
            }
            return VirtualFileManager.constructUrl(
                URLUtil.FILE_PROTOCOL,
                FileUtil.toSystemIndependentName(basePath) + "/" + DEFAULT_OUTPUT_URL
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

        state.modules.addAll(myModuleStates.getAll());

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

        myModuleStates.load(state.modules);
    }

    @Override
    public @Nullable Path getCompilerOutputPath() {
        String url = getCompilerOutputUrl();
        return url == null ? null : CompilerOutputPaths.toPath(myProject, url);
    }
}

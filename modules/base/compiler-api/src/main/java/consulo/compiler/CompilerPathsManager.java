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
package consulo.compiler;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.content.ContentFolderTypeProvider;
import consulo.module.Module;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2013-05-26
 */
@Deprecated
@DeprecationInfo(value = "Use CompilerConfiguration for projects, and ModuleCompilerPathsManager for modules")
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class CompilerPathsManager {
    
    public static CompilerPathsManager getInstance(Project project) {
        return project.getComponent(CompilerPathsManager.class);
    }

    private final Project myProject;

    @Inject
    public CompilerPathsManager(Project project) {
        myProject = project;
    }

    public @Nullable Path getCompilerOutputPath() {
        return CompilerConfiguration.getInstance(myProject).getCompilerOutputPath();
    }

    public @Nullable String getCompilerOutputUrl() {
        return CompilerConfiguration.getInstance(myProject).getCompilerOutputUrl();
    }

    public void setCompilerOutputUrl(@Nullable String compilerOutputUrl) {
        CompilerConfiguration.getInstance(myProject).setCompilerOutputUrl(compilerOutputUrl);
    }

    public boolean isInheritedCompilerOutput(Module module) {
        return ModuleCompilerPathsManager.getInstance(module).isInheritedCompilerOutput();
    }

    public void setInheritedCompilerOutput(Module module, boolean val) {
        ModuleCompilerPathsManager.getInstance(module).setInheritedCompilerOutput(val);
    }

    public boolean isExcludeOutput(Module module) {
        return ModuleCompilerPathsManager.getInstance(module).isExcludeOutput();
    }

    public void setExcludeOutput(Module module, boolean val) {
        ModuleCompilerPathsManager.getInstance(module).setExcludeOutput(val);
    }

    public void setCompilerOutputUrl(
        Module module,
        ContentFolderTypeProvider contentFolderType,
        @Nullable String compilerOutputUrl
    ) {
        ModuleCompilerPathsManager.getInstance(module).setCompilerOutputUrl(contentFolderType, compilerOutputUrl);
    }

    public String getCompilerOutputUrl(Module module, ContentFolderTypeProvider contentFolderType) {
        return ModuleCompilerPathsManager.getInstance(module).getCompilerOutputUrl(contentFolderType);
    }

    public @Nullable Path getCompilerOutputPath(Module module, ContentFolderTypeProvider contentFolderType) {
        return ModuleCompilerPathsManager.getInstance(module).getCompilerOutputPath(contentFolderType);
    }
}

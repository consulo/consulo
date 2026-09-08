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
import consulo.compiler.ModuleCompilerPathsManager;
import consulo.content.ContentFolderTypeProvider;
import consulo.module.Module;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2013-10-20
 */
@Singleton
@ServiceImpl
public class ModuleCompilerPathsManagerImpl extends ModuleCompilerPathsManager {
    private final Module myModule;

    private boolean myInheritOutput = true;
    private boolean myExcludeOutput = true;

    private final Map<String, String> myOutputUrls = new LinkedHashMap<>();
    private final CompilerConfiguration myCompilerConfiguration;

    @Inject
    public ModuleCompilerPathsManagerImpl(Module module, CompilerConfiguration compilerConfiguration) {
        myModule = module;
        myCompilerConfiguration = compilerConfiguration;
    }

    @Override
    public boolean isInheritedCompilerOutput() {
        return myInheritOutput;
    }

    @Override
    public void setInheritedCompilerOutput(boolean val) {
        myInheritOutput = val;
    }

    @Override
    public boolean isExcludeOutput() {
        return myExcludeOutput;
    }

    @Override
    public void setExcludeOutput(boolean val) {
        myExcludeOutput = val;
    }

    @Override
    public void setCompilerOutputUrl(ContentFolderTypeProvider contentFolderType, @Nullable String compilerOutputUrl) {
        if (myInheritOutput) {
            throw new IllegalArgumentException();
        }
        if (compilerOutputUrl == null) {
            return;
        }

        myOutputUrls.put(contentFolderType.getId(), compilerOutputUrl);
    }

    @Override
    public @Nullable String getCompilerOutputUrl(ContentFolderTypeProvider contentFolderType) {
        if (!myInheritOutput) {
            String url = myOutputUrls.get(contentFolderType.getId());
            if (url != null) {
                return url;
            }
        }

        return myCompilerConfiguration.getCompilerOutputUrl() + "/" + getRelativePathForProvider(contentFolderType, myModule);
    }

    public @Nullable CompilerManagerModuleState getState() {
        if (myInheritOutput) {
            return null;
        }

        CompilerManagerModuleState moduleState = new CompilerManagerModuleState();
        moduleState.name = myModule.getName();
        moduleState.exclude = isExcludeOutput();

        for (Map.Entry<String, String> tempEntry : myOutputUrls.entrySet()) {
            CompilerManagerOutputState outputState = new CompilerManagerOutputState();
            outputState.url = tempEntry.getValue();
            outputState.type = tempEntry.getKey();
            moduleState.outputs.add(outputState);
        }

        return moduleState;
    }

    public void loadState(CompilerManagerModuleState moduleState) {
        myInheritOutput = false;
        myExcludeOutput = moduleState.exclude;
        for (CompilerManagerOutputState outputState : moduleState.outputs) {
            myOutputUrls.put(outputState.type, outputState.url);
        }
    }
}

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

import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2013-10-20
 */
@Singleton
@ServiceImpl
public class ModuleCompilerPathsManagerImpl extends ModuleCompilerPathsManager {
    private static final CompilerManagerModuleState DEFAULT_VALUE = new CompilerManagerModuleState();

    private final Module myModule;

    private final CompilerConfigurationImpl myCompilerConfiguration;

    @Inject
    public ModuleCompilerPathsManagerImpl(Module module, CompilerConfiguration compilerConfiguration) {
        myModule = module;
        myCompilerConfiguration = (CompilerConfigurationImpl) compilerConfiguration;
    }

    private CompilerManagerModuleState get() {
        return Objects.requireNonNullElse(myCompilerConfiguration.forModule(myModule.getName()), DEFAULT_VALUE);
    }

    private void doChange(Consumer<CompilerManagerModuleState> consumer) {
        myCompilerConfiguration.editModuleState(myModule.getName(), consumer);
    }

    @Override
    public boolean isInheritedCompilerOutput() {
        return get().inherit;
    }

    @Override
    public void setInheritedCompilerOutput(boolean val) {
        doChange(it -> it.inherit = val);
    }

    @Override
    public boolean isExcludeOutput() {
        return get().exclude;
    }

    @Override
    public void setExcludeOutput(boolean val) {
        doChange(it -> it.exclude = val);
    }

    @Override
    public void setCompilerOutputUrl(ContentFolderTypeProvider contentFolderType, @Nullable String compilerOutputUrl) {
        if (isInheritedCompilerOutput()) {
            throw new IllegalArgumentException("Can't change path if it inherited");
        }

        if (compilerOutputUrl == null) {
            return;
        }

        doChange(compilerManagerModuleState -> {
            CompilerManagerOutputState outputState = compilerManagerModuleState.findByType(contentFolderType.getId());
            if (outputState != null) {
                outputState.url = compilerOutputUrl;
            } else {
                outputState = new CompilerManagerOutputState();
                outputState.type = contentFolderType.getId();
                outputState.url = compilerOutputUrl;

                compilerManagerModuleState.add(outputState);
            }
        });
    }

    @Override
    public @Nullable String getCompilerOutputUrl(ContentFolderTypeProvider contentFolderType) {
        if (!isInheritedCompilerOutput()) {
            CompilerManagerModuleState moduleState = get();

            CompilerManagerOutputState outputState = moduleState.findByType(contentFolderType.getId());
            if (outputState != null && outputState.url != null) {
                return outputState.url;
            }
        }

        return myCompilerConfiguration.getCompilerOutputUrl() + "/" + getRelativePathForProvider(contentFolderType, myModule);
    }
}

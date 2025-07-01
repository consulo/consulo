/*
 * Copyright 2013-2022 consulo.io
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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.compiler.CompilerConfiguration;
import consulo.project.content.WatchedRootsProvider;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.Set;

/**
 * @author VISTALL
 * @since 2022-06-18
 */
@ExtensionImpl
public class CompilerWatchedRootsProvider implements WatchedRootsProvider {
    private final Provider<CompilerConfiguration> myCompilerConfigurationProvider;

    @Inject
    public CompilerWatchedRootsProvider(Provider<CompilerConfiguration> compilerConfigurationProvider) {
        myCompilerConfigurationProvider = compilerConfigurationProvider;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public Set<String> getRootsToWatch() {
        return ((CompilerConfigurationImpl) myCompilerConfigurationProvider.get()).getRootsToWatch();
    }
}

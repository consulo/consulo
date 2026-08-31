/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.module.Module;
import consulo.module.content.internal.ModuleScopeProviderInternal;
import consulo.module.content.scope.ModuleAwareSearchScope;
import consulo.module.content.scope.ModuleWithDependenciesScope;
import jakarta.inject.Singleton;

/**
 * Headless {@code ModuleScopeProvider}: the production impl lives in {@code ide-impl}, which the headless
 * application deliberately does not depend on. Creating a module reaches this service only through
 * {@code ProjectRootManagerComponent#clearScopesCachesForModules}, which needs nothing but
 * {@link #clearCache()} - so the scopes themselves are left unimplemented rather than faked, and a test that
 * starts needing real search scopes fails loudly instead of silently searching the wrong thing.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessModuleScopeProvider implements ModuleScopeProviderInternal {
    @Override
    public void clearCache() {
    }

    @Override
    public ModuleWithDependenciesScope getModuleScope() {
        throw unsupported();
    }

    @Override
    public ModuleWithDependenciesScope getModuleScope(boolean includeTests) {
        throw unsupported();
    }

    @Override
    public ModuleWithDependenciesScope getModuleWithLibrariesScope() {
        throw unsupported();
    }

    @Override
    public ModuleWithDependenciesScope getModuleWithDependenciesScope() {
        throw unsupported();
    }

    @Override
    public ModuleWithDependenciesScope getModuleContentScope() {
        throw unsupported();
    }

    @Override
    public ModuleWithDependenciesScope getModuleContentWithDependenciesScope() {
        throw unsupported();
    }

    @Override
    public ModuleWithDependenciesScope getModuleWithDependenciesAndLibrariesScope(boolean includeTests) {
        throw unsupported();
    }

    @Override
    public ModuleAwareSearchScope getModuleWithDependentsScope() {
        throw unsupported();
    }

    @Override
    public ModuleAwareSearchScope getModuleTestsWithDependentsScope() {
        throw unsupported();
    }

    @Override
    public ModuleWithDependenciesScope getModuleRuntimeScope(boolean includeTests) {
        throw unsupported();
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Search scopes are not available in the headless application");
    }
}

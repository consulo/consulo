// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint;

import consulo.content.scope.SearchScope;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScopesCore;
import consulo.module.Module;
import consulo.project.content.TestSourcesFilter;
import consulo.project.content.scope.ProjectScopes;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * Filter for items defined in a project module.
 */
public final class ModuleEndpointFilter implements SearchScopeEndpointFilter {
    private final Module myModule;
    private final boolean myFromLibraries;
    private final boolean myFromTests;

    public ModuleEndpointFilter(Module module, boolean fromLibraries, boolean fromTests) {
        myModule = module;
        myFromLibraries = fromLibraries;
        myFromTests = fromTests;
    }

    public Module getModule() {
        return myModule;
    }

    public boolean isFromLibraries() {
        return myFromLibraries;
    }

    public boolean isFromTests() {
        return myFromTests;
    }

    @Override
    public GlobalSearchScope getTransitiveSearchScope() {
        if (myFromLibraries) {
            GlobalSearchScope contentScope = GlobalSearchScope.moduleContentScope(myModule);
            GlobalSearchScope allModuleScope =
                GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(myModule, true).union(contentScope);
            if (myFromTests) {
                return allModuleScope;
            }

            GlobalSearchScope testScope = GlobalSearchScopesCore.projectTestScope(myModule.getProject());
            return allModuleScope.intersectWith(GlobalSearchScope.notScope(testScope));
        }
        if (myFromTests) {
            return GlobalSearchScope.moduleContentWithDependenciesScope(myModule);
        }

        GlobalSearchScope testScope = GlobalSearchScopesCore.projectTestScope(myModule.getProject());
        return GlobalSearchScope.moduleContentWithDependenciesScope(myModule)
            .intersectWith(GlobalSearchScope.notScope(testScope));
    }

    @Override
    public GlobalSearchScope getContentSearchScope() {
        if (myFromTests) {
            return GlobalSearchScope.moduleContentScope(myModule);
        }

        GlobalSearchScope testScope = GlobalSearchScopesCore.projectTestScope(myModule.getProject());
        return GlobalSearchScope.moduleContentScope(myModule)
            .intersectWith(GlobalSearchScope.notScope(testScope));
    }

    public <T> Iterable<T> filterByScope(Collection<T> items, Function<? super T, ? extends @Nullable PsiFile> containingFileGetter) {
        if (myFromTests && myFromLibraries) {
            return items;
        }

        return filterByScope((Iterable<T>) items, containingFileGetter);
    }

    public <T> Iterable<T> filterByScope(Iterable<T> sequence, Function<? super T, ? extends @Nullable PsiFile> containingFileGetter) {
        if (myFromTests && myFromLibraries) {
            return sequence;
        }

        SearchScope librariesScope = ProjectScopes.getLibrariesScope(myModule.getProject());
        return () -> StreamSupport.stream(sequence.spliterator(), false)
            .filter(it -> {
                PsiFile containingFile = containingFileGetter.apply(it);
                VirtualFile file = containingFile != null ? containingFile.getVirtualFile() : null;

                return file == null
                    || ((myFromLibraries || !librariesScope.contains(file))
                    && (myFromTests || !TestSourcesFilter.isTestSources(file, myModule.getProject())));
            })
            .iterator();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ModuleEndpointFilter that)) {
            return false;
        }
        return myFromLibraries == that.myFromLibraries && myFromTests == that.myFromTests && myModule.equals(that.myModule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myModule, myFromLibraries, myFromTests);
    }

    @Override
    public String toString() {
        return "ModuleEndpointFilter(module=" + myModule + ", fromLibraries=" + myFromLibraries + ", fromTests=" + myFromTests + ")";
    }
}

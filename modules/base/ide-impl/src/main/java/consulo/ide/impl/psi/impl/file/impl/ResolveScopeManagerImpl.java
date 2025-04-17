/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.psi.impl.file.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.util.ConcurrentFactoryMap;
import consulo.content.scope.SearchScope;
import consulo.language.internal.LibraryRuntimeClasspathScope;
import consulo.language.psi.scope.LibraryScopeCache;
import consulo.language.impl.internal.psi.PsiManagerEx;
import consulo.language.impl.psi.ResolveScopeManager;
import consulo.language.psi.*;
import consulo.language.psi.scope.DelegatingGlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.orderEntry.*;
import consulo.project.Project;
import consulo.project.content.TestSourcesFilter;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Singleton
@ServiceImpl
public class ResolveScopeManagerImpl extends ResolveScopeManager {
    private final Project myProject;
    private final ProjectRootManager myProjectRootManager;
    private final PsiManager myManager;

    private final Map<VirtualFile, GlobalSearchScope> myDefaultResolveScopesCache;

    @Inject
    public ResolveScopeManagerImpl(Project project, ProjectRootManager projectRootManager, PsiManager psiManager) {
        myProject = project;
        myProjectRootManager = projectRootManager;
        myManager = psiManager;

        myDefaultResolveScopesCache = ConcurrentFactoryMap.createMap((key) -> {
            GlobalSearchScope scope = null;
            for (ResolveScopeProvider resolveScopeProvider : ResolveScopeProvider.EP_NAME.getExtensionList()) {
                scope = resolveScopeProvider.getResolveScope(key, myProject);
                if (scope != null) {
                    break;
                }
            }
            if (scope == null) {
                scope = getInherentResolveScope(key);
            }
            for (ResolveScopeEnlarger enlarger : ResolveScopeEnlarger.EP_NAME.getExtensionList()) {
                SearchScope extra = enlarger.getAdditionalResolveScope(key, myProject);
                if (extra != null) {
                    scope = scope.union(extra);
                }
            }

            return scope;
        });

        ((PsiManagerEx)psiManager).registerRunnableToRunOnChange(myDefaultResolveScopesCache::clear);
    }

    @Nonnull
    private GlobalSearchScope getInherentResolveScope(VirtualFile vFile) {
        ProjectFileIndex projectFileIndex = myProjectRootManager.getFileIndex();
        Module module = projectFileIndex.getModuleForFile(vFile);
        if (module != null) {
            boolean includeTests = TestSourcesFilter.isTestSources(vFile, myProject);
            return GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, includeTests);
        }
        else {
            // resolve references in libraries in context of all modules which contain it
            List<Module> modulesLibraryUsedIn = new ArrayList<>();
            List<OrderEntry> orderEntries = projectFileIndex.getOrderEntriesForFile(vFile);

            LibraryOrderEntry lib = null;
            for (OrderEntry entry : orderEntries) {
                if (entry instanceof ModuleExtensionWithSdkOrderEntry) {
                    modulesLibraryUsedIn.add(entry.getOwnerModule());
                }
                else if (entry instanceof LibraryOrderEntry libraryOrderEntry) {
                    lib = libraryOrderEntry;
                    modulesLibraryUsedIn.add(entry.getOwnerModule());
                }
                else if (entry instanceof ModuleOrderEntry) {
                    modulesLibraryUsedIn.add(entry.getOwnerModule());
                }
                else if (entry instanceof OrderEntryWithTracking) {
                    modulesLibraryUsedIn.add(entry.getOwnerModule());
                }
            }

            GlobalSearchScope allCandidates = LibraryScopeCache.getInstance(myProject).getScopeForLibraryUsedIn(modulesLibraryUsedIn);
            if (lib != null) {
                LibraryRuntimeClasspathScope preferred = new LibraryRuntimeClasspathScope(myProject, lib);
                // prefer current library
                return new DelegatingGlobalSearchScope(allCandidates, preferred) {
                    @Override
                    public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
                        boolean c1 = preferred.contains(file1);
                        boolean c2 = preferred.contains(file2);
                        if (c1 && !c2) {
                            return 1;
                        }
                        if (c2 && !c1) {
                            return -1;
                        }

                        return super.compare(file1, file2);
                    }
                };
            }
            return allCandidates;
        }
    }

    @Override
    @Nonnull
    public GlobalSearchScope getResolveScope(@Nonnull PsiElement element) {
        ProgressIndicatorProvider.checkCanceled();

        VirtualFile vFile;
        PsiFile contextFile;
        if (element instanceof PsiDirectory directory) {
            vFile = directory.getVirtualFile();
            contextFile = null;
        }
        else {
            PsiFile containingFile = element.getContainingFile();
            if (containingFile instanceof PsiCodeFragment codeFragment) {
                GlobalSearchScope forcedScope = codeFragment.getForcedResolveScope();
                if (forcedScope != null) {
                    return forcedScope;
                }
                PsiElement context = containingFile.getContext();
                if (context == null) {
                    return GlobalSearchScope.allScope(myProject);
                }
                return getResolveScope(context);
            }

            contextFile = containingFile != null ? FileContextUtil.getContextFile(containingFile) : null;
            if (contextFile == null) {
                return GlobalSearchScope.allScope(myProject);
            }
            else if (contextFile instanceof FileResolveScopeProvider fileResolveScopeProvider) {
                return fileResolveScopeProvider.getFileResolveScope();
            }
            vFile = contextFile.getOriginalFile().getVirtualFile();
        }
        if (vFile == null || contextFile == null) {
            return GlobalSearchScope.allScope(myProject);
        }

        return myDefaultResolveScopesCache.get(vFile);
    }

    @Nonnull
    @RequiredReadAction
    @Override
    public GlobalSearchScope getDefaultResolveScope(VirtualFile vFile) {
        PsiFile psiFile = myManager.findFile(vFile);
        assert psiFile != null;
        return myDefaultResolveScopesCache.get(vFile);
    }

    @Override
    @Nonnull
    public GlobalSearchScope getUseScope(@Nonnull PsiElement element) {
        Pair<GlobalSearchScope, VirtualFile> pair = getDefaultResultScopeInfo(element);
        if (pair.getSecond() == null) {
            return pair.getFirst();
        }

        GlobalSearchScope targetScope = pair.getFirst();
        for (ResolveScopeEnlarger scopeEnlarger : ResolveScopeEnlarger.EP_NAME.getExtensionList()) {
            SearchScope scope = scopeEnlarger.getAdditionalUseScope(pair.getSecond(), element.getProject());
            if (scope != null) {
                targetScope = targetScope.union(scope);
            }
        }
        return targetScope;
    }

    @Nonnull
    private Pair<GlobalSearchScope, VirtualFile> getDefaultResultScopeInfo(@Nonnull PsiElement element) {
        VirtualFile vFile;
        GlobalSearchScope allScope = GlobalSearchScope.allScope(myManager.getProject());
        if (element instanceof PsiDirectory directory) {
            vFile = directory.getVirtualFile();
        }
        else {
            PsiFile containingFile = element.getContainingFile();
            if (containingFile == null) {
                return Pair.create(allScope, null);
            }
            vFile = containingFile.getVirtualFile();
        }

        if (vFile == null) {
            return Pair.create(allScope, null);
        }
        ProjectFileIndex projectFileIndex = myProjectRootManager.getFileIndex();
        Module module = projectFileIndex.getModuleForFile(vFile);
        if (module != null) {
            boolean isTest = TestSourcesFilter.isTestSources(vFile, element.getProject());
            GlobalSearchScope scope =
                isTest ? GlobalSearchScope.moduleTestsWithDependentsScope(module) : GlobalSearchScope.moduleWithDependentsScope(module);
            return Pair.create(scope, vFile);
        }
        else {
            PsiFile f = element.getContainingFile();
            VirtualFile vf = f == null ? null : f.getVirtualFile();

            GlobalSearchScope scope = f == null || vf == null || vf.isDirectory() || allScope.contains(vf)
                ? allScope
                : GlobalSearchScope.fileScope(f).uniteWith(allScope);
            return Pair.create(scope, vf);
        }
    }
}

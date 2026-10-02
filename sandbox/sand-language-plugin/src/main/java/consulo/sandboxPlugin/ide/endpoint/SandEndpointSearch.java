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
package consulo.sandboxPlugin.ide.endpoint;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.component.util.ModificationTracker;
import consulo.endpoint.EndpointProvider;
import consulo.endpoint.url.UrlTargetInfo;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FileTypeIndex;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.module.content.ProjectRootManager;
import consulo.module.extension.ModuleExtensionHelper;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.sandboxPlugin.ide.module.extension.SandModuleExtension;
import consulo.sandboxPlugin.lang.SandFileType;
import consulo.sandboxPlugin.lang.SandLanguage;
import consulo.sandboxPlugin.lang.psi.SandClass;
import consulo.sandboxPlugin.lang.psi.SandStringExpression;
import consulo.virtualFileSystem.VirtualFile;

import java.util.ArrayList;
import java.util.List;

public final class SandEndpointSearch {
    private SandEndpointSearch() {
    }

    public static EndpointProvider.Status getStatus(Project project) {
        return ModuleExtensionHelper.getInstance(project).hasModuleExtension(SandModuleExtension.class)
            ? EndpointProvider.Status.HAS_ENDPOINTS
            : EndpointProvider.Status.UNAVAILABLE;
    }

    public static ModificationTracker getModificationTracker(Project project) {
        return PsiModificationTracker.getInstance(project).forLanguage(SandLanguage.INSTANCE);
    }

    @RequiredReadAction
    public static List<SandClass> findClasses(Project project, GlobalSearchScope scope, boolean client) {
        if (DumbService.isDumb(project)) {
            return List.of();
        }

        PsiManager psiManager = PsiManager.getInstance(project);
        List<SandClass> result = new ArrayList<>();
        for (VirtualFile file : FileTypeIndex.getFiles(SandFileType.INSTANCE, scope)) {
            PsiFile psiFile = psiManager.findFile(file);
            if (psiFile == null) {
                continue;
            }
            for (SandClass sandClass : PsiTreeUtil.findChildrenOfType(psiFile, SandClass.class)) {
                if (!findLiterals(sandClass, client).isEmpty()) {
                    result.add(sandClass);
                }
            }
        }
        return result;
    }

    @RequiredReadAction
    public static List<SandStringExpression> findLiterals(SandClass sandClass, boolean client) {
        List<SandStringExpression> result = new ArrayList<>();
        for (SandStringExpression expression : PsiTreeUtil.getChildrenOfTypeAsList(sandClass, SandStringExpression.class)) {
            SandEndpointLiteral literal = SandEndpointLiteral.of(expression);
            if (literal != null && literal.isClient() == client) {
                result.add(expression);
            }
        }
        return result;
    }

    @RequiredReadAction
    public static List<UrlTargetInfo> getServerTargets(Project project) {
        if (DumbService.isDumb(project)) {
            return List.of();
        }

        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            () -> CachedValueProvider.Result.create(
                collectServerTargets(project),
                getModificationTracker(project),
                DumbService.getInstance(project).getModificationTracker(),
                ProjectRootManager.getInstance(project)
            )
        );
    }

    @RequiredReadAction
    private static List<UrlTargetInfo> collectServerTargets(Project project) {
        List<UrlTargetInfo> result = new ArrayList<>();
        for (SandClass sandClass : findClasses(project, GlobalSearchScope.projectScope(project), false)) {
            for (SandStringExpression expression : findLiterals(sandClass, false)) {
                SandEndpointLiteral literal = SandEndpointLiteral.of(expression);
                if (literal != null) {
                    result.add(SandUrlTargetInfo.create(sandClass, expression, literal));
                }
            }
        }
        return List.copyOf(result);
    }
}

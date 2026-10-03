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
import consulo.codeEditor.CodeInsightColors;
import consulo.component.util.ModificationTracker;
import consulo.endpoint.EndpointFilter;
import consulo.endpoint.EndpointUrlTargetProvider;
import consulo.endpoint.ModuleEndpointFilter;
import consulo.endpoint.SearchScopeEndpointFilter;
import consulo.endpoint.presentation.HttpMethodPresentation;
import consulo.endpoint.presentation.HttpUrlPresentation;
import consulo.endpoint.url.UrlTargetInfo;
import consulo.language.psi.PsiElement;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.navigation.ItemPresentation;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.psi.SandClass;
import consulo.sandboxPlugin.lang.psi.SandStringExpression;
import org.jspecify.annotations.Nullable;

import java.util.List;

public abstract class SandHttpEndpointProviderBase implements EndpointUrlTargetProvider<SandClass, SandStringExpression> {
    protected final Project myProject;
    private final boolean myClient;

    protected SandHttpEndpointProviderBase(Project project, boolean client) {
        myProject = project;
        myClient = client;
    }

    @Override
    public Status getStatus() {
        return SandEndpointSearch.getStatus(myProject);
    }

    @Override
    @RequiredReadAction
    public Iterable<SandClass> getEndpointGroups(EndpointFilter filter) {
        if (filter instanceof ModuleEndpointFilter moduleFilter) {
            if (!SandEndpointSearch.hasSandExtension(moduleFilter.getModule())) {
                return List.of();
            }
            GlobalSearchScope scope = moduleFilter.isFromLibraries()
                ? moduleFilter.getTransitiveSearchScope()
                : moduleFilter.getContentSearchScope();
            return moduleFilter.filterByScope(SandEndpointSearch.findClasses(myProject, scope, myClient), PsiElement::getContainingFile);
        }
        if (filter instanceof SearchScopeEndpointFilter scopeFilter) {
            return SandEndpointSearch.findClasses(myProject, scopeFilter.getContentSearchScope(), myClient);
        }
        return List.of();
    }

    @Override
    @RequiredReadAction
    public Iterable<SandStringExpression> getEndpoints(SandClass group) {
        return SandEndpointSearch.findLiterals(group, myClient);
    }

    @Override
    @RequiredReadAction
    public boolean isValidEndpoint(SandClass group, SandStringExpression endpoint) {
        if (!group.isValid() || !endpoint.isValid()) {
            return false;
        }
        SandEndpointLiteral literal = SandEndpointLiteral.of(endpoint);
        return literal != null && literal.isClient() == myClient;
    }

    @Override
    @RequiredReadAction
    public ItemPresentation getEndpointPresentation(SandClass group, SandStringExpression endpoint) {
        SandEndpointLiteral literal = SandEndpointLiteral.of(endpoint);
        if (literal == null) {
            return new HttpUrlPresentation(SandEndpointLiteral.getValue(endpoint), group.getName(), null);
        }
        return new HttpMethodPresentation(
            literal.getUrl(),
            literal.getMethod(),
            group.getName(),
            getPresentation().getIcon(),
            literal.isDeprecated() ? CodeInsightColors.DEPRECATED_ATTRIBUTES : null
        );
    }

    @Override
    public ModificationTracker getModificationTracker() {
        return SandEndpointSearch.getModificationTracker(myProject);
    }

    @Override
    public @Nullable PsiElement getDocumentationElement(SandClass group, SandStringExpression endpoint) {
        return endpoint;
    }

    @Override
    @RequiredReadAction
    public Iterable<UrlTargetInfo> getUrlTargetInfo(SandClass group, SandStringExpression endpoint) {
        SandEndpointLiteral literal = SandEndpointLiteral.of(endpoint);
        if (literal == null) {
            return List.of();
        }
        return List.of(SandUrlTargetInfo.create(group, endpoint, literal));
    }
}

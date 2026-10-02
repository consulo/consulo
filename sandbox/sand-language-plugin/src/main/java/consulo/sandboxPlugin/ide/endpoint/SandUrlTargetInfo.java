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
import consulo.endpoint.url.Authority;
import consulo.endpoint.url.UrlConstants;
import consulo.endpoint.url.UrlPath;
import consulo.endpoint.url.UrlTargetInfo;
import consulo.endpoint.url.reference.DefaultFrameworkUrlPathSpecification;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.sandboxPlugin.lang.psi.SandClass;
import consulo.sandboxPlugin.lang.psi.SandStringExpression;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

public final class SandUrlTargetInfo implements UrlTargetInfo {
    public static final String LOCAL_AUTHORITY = "localhost:8080";

    private final List<String> mySchemes;
    private final List<Authority> myAuthorities;
    private final UrlPath myPath;
    private final Set<String> myMethods;
    private final boolean myDeprecated;
    private final String mySource;
    private final @Nullable SmartPsiElementPointer<PsiElement> myPointer;

    public SandUrlTargetInfo(
        List<String> schemes,
        List<Authority> authorities,
        UrlPath path,
        Set<String> methods,
        boolean deprecated,
        String source,
        @Nullable SmartPsiElementPointer<PsiElement> pointer
    ) {
        mySchemes = schemes;
        myAuthorities = authorities;
        myPath = path;
        myMethods = methods;
        myDeprecated = deprecated;
        mySource = source;
        myPointer = pointer;
    }

    @RequiredReadAction
    public static SandUrlTargetInfo create(SandClass sandClass, SandStringExpression expression, SandEndpointLiteral literal) {
        String url = literal.getUrl();
        List<String> schemes = UrlConstants.HTTP_SCHEMES;
        String authority = LOCAL_AUTHORITY;
        String path = url;
        if (literal.isAbsoluteUrl()) {
            int schemeEnd = url.indexOf("://") + 3;
            int pathStart = url.indexOf('/', schemeEnd);
            schemes = List.of(url.substring(0, schemeEnd));
            authority = url.substring(schemeEnd, pathStart < 0 ? url.length() : pathStart);
            path = pathStart < 0 ? "/" : url.substring(pathStart);
        }

        return new SandUrlTargetInfo(
            schemes,
            List.of(new Authority.Exact(authority)),
            parsePath(path),
            Set.of(literal.getMethod()),
            literal.isDeprecated(),
            StringUtil.notNullize(sandClass.getName()),
            SmartPointerManager.<PsiElement>createPointer(expression)
        );
    }

    public static UrlPath parsePath(String path) {
        return DefaultFrameworkUrlPathSpecification.INSTANCE.parsePath(path);
    }

    @Override
    public List<String> getSchemes() {
        return mySchemes;
    }

    @Override
    public List<Authority> getAuthorities() {
        return myAuthorities;
    }

    @Override
    public UrlPath getPath() {
        return myPath;
    }

    @Override
    public Set<String> getMethods() {
        return myMethods;
    }

    @Override
    public boolean isDeprecated() {
        return myDeprecated;
    }

    @Override
    public String getSource() {
        return mySource;
    }

    @Override
    public @Nullable PsiElement resolveToPsiElement() {
        return myPointer != null ? myPointer.getElement() : null;
    }
}

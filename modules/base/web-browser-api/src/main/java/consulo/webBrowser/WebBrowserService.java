/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.webBrowser;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.language.psi.PsiElement;
import consulo.util.io.Url;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;

@ServiceAPI(ComponentScope.APPLICATION)
public interface WebBrowserService {
    public static WebBrowserService getInstance() {
        return Application.get().getInstance(WebBrowserService.class);
    }

    @Nonnull
    public abstract Collection<Url> getUrlsToOpen(@Nonnull OpenInBrowserRequest request, boolean preferLocalUrl)
        throws WebBrowserUrlProvider.BrowserException;

    @Nullable
    public abstract WebBrowserUrlProvider getProvider(@Nonnull OpenInBrowserRequest request);

    @Nullable
    public abstract Url getUrlForContext(@Nonnull PsiElement sourceElement);

    @Nonnull
    default Collection<Url> getUrlsToOpen(@Nonnull final PsiElement element, boolean preferLocalUrl)
        throws WebBrowserUrlProvider.BrowserException {
        OpenInBrowserRequest request = OpenInBrowserRequest.create(element);
        return request == null ? Collections.<Url>emptyList() : getUrlsToOpen(request, preferLocalUrl);
    }
}
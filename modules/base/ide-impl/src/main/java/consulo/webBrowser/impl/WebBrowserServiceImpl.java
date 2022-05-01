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
package consulo.webBrowser.impl;

import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.virtualFileSystem.http.HttpVirtualFile;
import consulo.language.psi.PsiElement;
import consulo.project.DumbService;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.Url;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.light.LightVirtualFileBase;
import consulo.webBrowser.OpenInBrowserRequest;
import consulo.webBrowser.WebBrowserService;
import consulo.webBrowser.WebBrowserUrlProvider;
import consulo.webBrowser.WebFileFilter;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

@Singleton
public class WebBrowserServiceImpl extends WebBrowserService {
  @Nonnull
  @Override
  public Collection<Url> getUrlsToOpen(@Nonnull OpenInBrowserRequest request, boolean preferLocalUrl) throws WebBrowserUrlProvider.BrowserException {
    VirtualFile virtualFile = request.getVirtualFile();
    if (virtualFile instanceof HttpVirtualFile) {
      return Collections.singleton(VfsUtil.newFromVirtualFile(virtualFile));
    }

    if (!preferLocalUrl || !WebFileFilter.isFileAllowed(request.getFile())) {
      WebBrowserUrlProvider provider = getProvider(request);
      if (provider != null) {
        if (request.getResult() != null) {
          return request.getResult();
        }

        try {
          Collection<Url> urls = provider.getUrls(request);
          if (!urls.isEmpty()) {
            return urls;
          }
        }
        catch (WebBrowserUrlProvider.BrowserException e) {
          if (!WebFileFilter.isFileAllowed(request.getFile())) {
            throw e;
          }
        }
      }
    }
    return virtualFile instanceof LightVirtualFileBase || !request.getFile().getViewProvider().isPhysical() ? Collections.<Url>emptySet() : Collections.singleton(VfsUtil.newFromVirtualFile(virtualFile));
  }

  @Override
  @Nullable
  public WebBrowserUrlProvider getProvider(@Nonnull OpenInBrowserRequest request) {
    DumbService dumbService = DumbService.getInstance(request.getProject());
    for (WebBrowserUrlProvider urlProvider : WebBrowserUrlProvider.EP_NAME.getExtensionList()) {
      if ((!dumbService.isDumb() || DumbService.isDumbAware(urlProvider)) && urlProvider.canHandleElement(request)) {
        return urlProvider;
      }
    }
    return null;
  }

  @Nullable
  public static Url getUrlForContext(@Nonnull PsiElement sourceElement) {
    Url url;
    try {
      Collection<Url> urls = WebBrowserService.getInstance().getUrlsToOpen(sourceElement, false);
      url = ContainerUtil.getFirstItem(urls);
      if (url == null) {
        return null;
      }
    }
    catch (WebBrowserUrlProvider.BrowserException ignored) {
      return null;
    }

    VirtualFile virtualFile = sourceElement.getContainingFile().getVirtualFile();
    if (virtualFile == null) {
      return null;
    }

    return !url.isInLocalFileSystem() || WebFileFilter.isFileAllowed(sourceElement.getProject(), virtualFile) ? url : null;
  }
}

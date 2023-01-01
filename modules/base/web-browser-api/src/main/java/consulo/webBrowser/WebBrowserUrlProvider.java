/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiFile;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.Url;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class WebBrowserUrlProvider {
  public static final ExtensionPointName<WebBrowserUrlProvider> EP_NAME = ExtensionPointName.create(WebBrowserUrlProvider.class);

  /**
   * Browser exceptions are printed in Error Dialog when user presses any browser button
   */
  public static class BrowserException extends Exception {
    public BrowserException(final String message) {
      super(message);
    }
  }

  public boolean canHandleElement(@Nonnull OpenInBrowserRequest request) {
    try {
      Collection<Url> urls = getUrls(request);
      if (!urls.isEmpty()) {
        request.setResult(urls);
        return true;
      }
    }
    catch (BrowserException ignored) {
    }

    return false;
  }

  @Nullable
  protected Url getUrl(@Nonnull OpenInBrowserRequest request, @Nonnull VirtualFile virtualFile) throws BrowserException {
    return null;
  }

  @Nonnull
  public Collection<Url> getUrls(@Nonnull OpenInBrowserRequest request) throws BrowserException {
    return ContainerUtil.createMaybeSingletonList(getUrl(request, request.getVirtualFile()));
  }

  @Nullable
  public String getOpenInBrowserActionDescription(@Nonnull PsiFile file) {
    return null;
  }
}
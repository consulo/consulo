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
package consulo.webBrowser.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.webBrowser.BrowserLauncher;
import consulo.webBrowser.UrlOpener;
import consulo.webBrowser.WebBrowser;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

@ExtensionImpl(order = "last")
public final class DefaultUrlOpener implements UrlOpener {
    private final Provider<BrowserLauncher> myBrowserLauncher;

    @Inject
    public DefaultUrlOpener(Provider<BrowserLauncher> browserLauncher) {
        myBrowserLauncher = browserLauncher;
    }

    @Override
    public boolean openUrl(@Nonnull WebBrowser browser, @Nonnull String url, @Nullable Project project) {
        return myBrowserLauncher.get().browseUsingPath(url, null, browser, project, ArrayUtil.EMPTY_STRING_ARRAY);
    }
}
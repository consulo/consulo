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
package consulo.ide.impl.idea.ide.browsers.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.webBrowser.BrowserLauncher;
import consulo.webBrowser.UrlOpener;
import consulo.webBrowser.WebBrowser;
import consulo.project.Project;
import consulo.ide.impl.idea.util.ArrayUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl(order = "last")
public final class DefaultUrlOpener implements UrlOpener {
  @Override
  public boolean openUrl(@Nonnull WebBrowser browser, @Nonnull String url, @Nullable Project project) {
    return BrowserLauncher.getInstance().browseUsingPath(url, null, browser, project, ArrayUtil.EMPTY_STRING_ARRAY);
  }
}
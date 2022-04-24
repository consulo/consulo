/*
 * Copyright 2013-2021 consulo.io
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

import consulo.application.Application;
import consulo.component.util.ModificationTracker;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 28/11/2021
 */
public interface WebBrowserManager extends ModificationTracker {
  public static WebBrowserManager getInstance() {
    return Application.get().getInstance(WebBrowserManager.class);
  }

  @Nonnull
  List<WebBrowser> getBrowsers();

  @Nonnull
  List<WebBrowser> getActiveBrowsers();

  boolean isActive(@Nonnull WebBrowser webBrowser);

  @Nonnull
  DefaultBrowserPolicy getDefaultBrowserPolicy();

  @Nullable
  WebBrowser getFirstActiveBrowser();

  /**
   * @param idOrFamilyName UUID or, due to backward compatibility, browser family name or JS debugger engine ID
   */
  @Nullable
  WebBrowser findBrowserById(@Nullable String idOrFamilyName);

  @Nonnull
  String getAlternativeBrowserPath();
}

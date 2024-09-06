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
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.net.URI;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class BrowserLauncher {
    @Nonnull
    public static BrowserLauncher getInstance() {
        return Application.get().getInstance(BrowserLauncher.class);
    }

    public abstract void open(@Nonnull String url);

    public abstract void browse(@Nonnull URI uri);

    public abstract void browse(@Nonnull File file);

    public abstract void browse(@Nonnull String url, @Nullable WebBrowser browser);

    public abstract void browse(@Nonnull String url, @Nullable WebBrowser browser, @Nullable Project project);

    public abstract boolean browseUsingPath(
        @Nullable String url,
        @Nullable String browserPath,
        @Nullable WebBrowser browser,
        @Nullable Project project,
        @Nonnull String[] additionalParameters
    );
}
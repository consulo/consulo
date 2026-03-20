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
package consulo.project.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.project.Project;
import consulo.ui.UIAccess;

import org.jspecify.annotations.Nullable;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-03-08
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface WelcomeProjectManager {
    /**
     * Stub path used to open the welcome project through the standard {@link ProjectOpenService} flow.
     */
    Path WELCOME_PATH = Path.of("___WELCOME_STUB___");

    static WelcomeProjectManager getInstance() {
        return Application.get().getInstance(WelcomeProjectManager.class);
    }

    CompletableFuture<?> openWelcomeProjectAsync(UIAccess uiAccess);

    CompletableFuture<Boolean> closeWelcomeProjectAsync(UIAccess uiAccess);

    boolean isWelcomeProjectOpened();

    /**
     * Returns the currently open welcome project, or {@code null} if no welcome project is open.
     */
    @Nullable Project getOpenWelcomeProject();
}

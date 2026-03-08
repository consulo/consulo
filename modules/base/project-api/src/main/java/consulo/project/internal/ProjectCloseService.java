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
import consulo.project.Project;
import consulo.ui.UIAccess;
import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

/**
 * Service for asynchronously closing projects using coroutine-based flow.
 *
 * @author VISTALL
 * @since 2026-03-08
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ProjectCloseService {
    /**
     * Close a project asynchronously.
     *
     * @param project       the project to close
     * @param uiAccess      UI access for dialogs and EDT operations
     * @param checkCanClose whether to check close veto listeners
     * @param save          whether to save documents and project before closing
     * @param dispose       whether to dispose the project after closing
     * @return a future that resolves to true if the project was closed,
     * false if close was vetoed or cancelled
     */
    @Nonnull
    CompletableFuture<Boolean> closeProjectAsync(
        @Nonnull Project project,
        @Nonnull UIAccess uiAccess,
        boolean checkCanClose,
        boolean save,
        boolean dispose
    );
}

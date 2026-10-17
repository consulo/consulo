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
package consulo.versionControlSystem.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.concurrent.CompletableFuture;

/**
 * A dialog showing changes, with their diff.
 *
 * @author VISTALL
 * @since 2026-10-17
 */
public interface ShowChangesDialogBuilder {
    ShowChangesDialogBuilder title(LocalizeValue title);

    /**
     * A text shown above the changes. Simple html markup is allowed.
     */
    ShowChangesDialogBuilder description(LocalizeValue description);

    /**
     * @return a future done once the dialog is closed
     */
    @RequiredUIAccess
    CompletableFuture<?> showAsync();
}

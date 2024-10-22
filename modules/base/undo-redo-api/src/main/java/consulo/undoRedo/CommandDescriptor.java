/*
 * Copyright 2013-2024 consulo.io
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
package consulo.undoRedo;

import consulo.document.Document;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

/**
 * @author UNV
 * @since 2024-10-21
 */
public record CommandDescriptor(
    @Nonnull Runnable command,
    @Nonnull LocalizeValue name,
    Object groupId,
    Project project,
    Document document,
    @Nonnull UndoConfirmationPolicy undoConfirmationPolicy,
    boolean shouldRecordActionForActiveDocument
) {
}

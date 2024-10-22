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
package consulo.undoRedo.builder;

import consulo.document.Document;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.undoRedo.CommandDescriptor;
import consulo.undoRedo.UndoConfirmationPolicy;
import jakarta.annotation.Nonnull;

/**
 * @author UNV
 * @since 2024-10-21
 */
public interface CommandBuilder<THIS extends CommandBuilder> {
    THIS withName(@Nonnull LocalizeValue name);

    THIS withGroupId(Object groupId);

    THIS withProject(Project project);

    THIS withDocument(Document document);

    THIS withUndoConfirmationPolicy(@Nonnull UndoConfirmationPolicy undoConfirmationPolicy);

    THIS withShouldRecordActionForActiveDocument(boolean shouldRecordActionForActiveDocument);

    CommandDescriptor build();
}

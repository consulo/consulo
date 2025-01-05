/*
 * Copyright 2013-2025 consulo.io
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
package consulo.execution.debug.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.Document;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.evaluation.EvaluationMode;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2025-01-05
 */
public class UnsupportedDebuggerEditorsProvider extends XDebuggerEditorsProvider {
    public static final UnsupportedDebuggerEditorsProvider INSTANCE = new UnsupportedDebuggerEditorsProvider();

    @Nonnull
    @Override
    public FileType getFileType() {
        throw new UnsupportedOperationException();
    }

    @RequiredReadAction
    @Nonnull
    @Override
    public Document createDocument(@Nonnull Project project, @Nonnull String text, @Nullable XSourcePosition sourcePosition, @Nonnull EvaluationMode mode) {
        throw new UnsupportedOperationException();
    }
}

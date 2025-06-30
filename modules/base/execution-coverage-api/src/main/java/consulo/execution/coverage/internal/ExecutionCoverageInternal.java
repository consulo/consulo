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
package consulo.execution.coverage.internal;

import com.intellij.rt.coverage.data.LineData;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.colorScheme.TextAttributesKey;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2025-06-29
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ExecutionCoverageInternal {
    static ExecutionCoverageInternal getInstance() {
        return Application.get().getInstance(ExecutionCoverageInternal.class);
    }

    @Nonnull
    @RequiredUIAccess
    CompletableFuture<?> showExportDialog(@Nonnull Project project, @Nonnull String presentableName);

    @RequiredUIAccess
    void showCoverageHit(JPanel panel,
                         Editor editor,
                         Point point,
                         LineData lineData,
                         String reportText);
    @RequiredUIAccess
    void showColorsSettings(@Nonnull Project project,
                            @Nonnull LineData lineData,
                            Function<LineData, TextAttributesKey> attributesKeyFunc);
}

/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor.rawHighlight;

import consulo.colorScheme.TextAttributes;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Comparator;

/**
 * @author VISTALL
 * @since 13-Feb-22
 */
public interface SeverityRegistrar extends Comparator<HighlightSeverity> {
    @Nonnull
    public static SeverityRegistrar getSeverityRegistrar(@Nullable Project project) {
        return project == null
            ? InspectionProfileManager.getInstance().getSeverityRegistrar()
            : InspectionProjectProfileManager.getInstance(project).getSeverityRegistrar();
    }

    int getSeveritiesCount();

    @Nullable
    HighlightSeverity getSeverityByIndex(final int i);

    @Nullable
    TextAttributes getTextAttributesBySeverity(@Nonnull HighlightSeverity severity);

    @Nullable
    HighlightSeverity getSeverity(@Nonnull String name);

    boolean isSeverityValid(@Nonnull String severityName);

    @Nonnull
    HighlightInfoType getHighlightInfoTypeBySeverity(@Nonnull HighlightSeverity severity);
}

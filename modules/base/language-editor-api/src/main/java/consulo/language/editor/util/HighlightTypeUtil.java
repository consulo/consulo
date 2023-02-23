/*
 * Copyright 2013-2023 consulo.io
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
package consulo.language.editor.util;

import consulo.annotation.UsedInPlugin;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 22/02/2023
 */
@UsedInPlugin
public class HighlightTypeUtil {
  @Nonnull
  @SuppressWarnings("deprecation")
  public static HighlightInfoType convertSeverity(@Nonnull HighlightSeverity severity) {
    if (severity == HighlightSeverity.ERROR) return HighlightInfoType.ERROR;
    else if (severity == HighlightSeverity.WARNING) return HighlightInfoType.WARNING;
    else if (severity == HighlightSeverity.INFO) return HighlightInfoType.INFO;
    else if (severity == HighlightSeverity.WEAK_WARNING) return HighlightInfoType.WEAK_WARNING;
    else if (severity == HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING) return HighlightInfoType.GENERIC_WARNINGS_OR_ERRORS_FROM_SERVER;
    else return HighlightInfoType.INFORMATION;
  }

  @Nonnull
  @SuppressWarnings("deprecation")
  public static ProblemHighlightType convertSeverityToProblemHighlight(@NotNull HighlightSeverity severity) {
    if (severity == HighlightSeverity.ERROR) return ProblemHighlightType.ERROR;
    else if (severity == HighlightSeverity.WARNING) return ProblemHighlightType.WARNING;
    else if (severity == HighlightSeverity.INFO) return ProblemHighlightType.INFO;
    else if (severity == HighlightSeverity.WEAK_WARNING) return ProblemHighlightType.WEAK_WARNING;
    else return ProblemHighlightType.INFORMATION;
  }
}

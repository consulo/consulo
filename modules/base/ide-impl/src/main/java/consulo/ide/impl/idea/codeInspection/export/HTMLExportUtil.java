/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInspection.export;

import consulo.application.Application;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.component.ProcessCanceledException;
import consulo.language.editor.inspection.HTMLExporter;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.function.ThrowableRunnable;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.IOException;

public class HTMLExportUtil {
  public static void writeFile(final String folder, @NonNls final String fileName, CharSequence buf, final Project project) {
    try {
      HTMLExporter.writeFileImpl(folder, fileName, buf);
    }
    catch (IOException e) {
      Runnable showError = () -> {
        final String fullPath = folder + File.separator + fileName;
        Messages.showMessageDialog(
          project,
          InspectionLocalize.inspectionExportErrorWritingTo(fullPath).get(),
          InspectionLocalize.inspectionExportResultsErrorTitle().get(),
          UIUtil.getErrorIcon()
        );
      };
      Application.get().invokeLater(showError, IdeaModalityState.nonModal());
      throw new ProcessCanceledException();
    }
  }

  public static void runExport(final Project project, @Nonnull ThrowableRunnable<IOException> runnable) {
    try {
      runnable.run();
    }
    catch (IOException e) {
      Runnable showError = () -> Messages.showMessageDialog(
        project,
        InspectionLocalize.inspectionExportErrorWritingTo("export file").get(),
        InspectionLocalize.inspectionExportResultsErrorTitle().get(),
        UIUtil.getErrorIcon()
      );
      Application.get().invokeLater(showError, IdeaModalityState.nonModal());
      throw new ProcessCanceledException();
    }
  }
}

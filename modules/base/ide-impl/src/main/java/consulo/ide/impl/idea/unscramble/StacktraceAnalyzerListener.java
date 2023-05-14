/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.unscramble;

import consulo.application.Application;
import consulo.execution.unscramble.StacktraceAnalyzer;
import consulo.ide.impl.idea.openapi.application.ex.ClipboardAnalyzeListener;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public class StacktraceAnalyzerListener extends ClipboardAnalyzeListener<StacktraceAnalyzer> {

  @Override
  protected void handle(@Nonnull Project project, @Nonnull String value, @Nonnull StacktraceAnalyzer stacktraceAnalyzer) {
    final UnscrambleDialog dialog = new UnscrambleDialog(project, stacktraceAnalyzer);
    dialog.createNormalizeTextAction().actionPerformed(null);
    dialog.doOKAction();
  }

  @Override
  @Nullable
  public StacktraceAnalyzer canHandle(@Nonnull String value) {
    for (StacktraceAnalyzer stacktraceAnalyzer : Application.get().getExtensionList(StacktraceAnalyzer.class)) {
        if (stacktraceAnalyzer.isStacktrace(value)) {
          return stacktraceAnalyzer;
        }
    }
    return null;
  }
}

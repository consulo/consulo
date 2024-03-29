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
package consulo.fileEditor.highlight;

import consulo.application.progress.ProgressIndicator;
import consulo.annotation.access.RequiredReadAction;
import consulo.component.ProcessCanceledException;

import jakarta.annotation.Nonnull;

public interface HighlightingPass {
  HighlightingPass[] EMPTY_ARRAY = new HighlightingPass[0];
  /**
   * pass is intended to perform analysis stuff and hold collected information internally
   * until {@link #collectInformation(ProgressIndicator)} is called.
   * This method is called from a background thread.
   *
   * @param progress to check for highlighting process is cancelled. Pass is to check progress.isCanceled() as often as possible and
   * throw {@link ProcessCanceledException} if <code>true</code> is returned.
   */
  @RequiredReadAction
  void collectInformation(@Nonnull ProgressIndicator progress);

  /**
   * Called to apply information collected by {@linkplain #collectInformation(ProgressIndicator)} to the editor.
   * This method is called from the event dispatch thread.
   */ 
  void applyInformationToEditor();
}

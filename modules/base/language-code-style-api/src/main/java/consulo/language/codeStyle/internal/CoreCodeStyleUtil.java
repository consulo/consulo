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
package consulo.language.codeStyle.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.CompositeLanguage;
import consulo.language.Language;
import consulo.language.codeStyle.FormattingModelBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nullable;

public class CoreCodeStyleUtil {
  private static final ThreadLocal<ProcessingUnderProgressInfo> SEQUENTIAL_PROCESSING_ALLOWED =
    ThreadLocal.withInitial(ProcessingUnderProgressInfo::new);

  @Nullable
  @RequiredReadAction
  public static PsiElement findElementInTreeWithFormatterEnabled(final PsiFile file, final int offset) {
    final PsiElement bottomost = file.findElementAt(offset);
    if (bottomost != null && FormattingModelBuilder.forContext(bottomost) != null) {
      return bottomost;
    }

    final Language fileLang = file.getLanguage();
    if (fileLang instanceof CompositeLanguage) {
      return file.getViewProvider().findElementAt(offset, fileLang);
    }

    return bottomost;
  }

  public static boolean isSequentialProcessingAllowed() {
    return SEQUENTIAL_PROCESSING_ALLOWED.get().isAllowed();
  }

  /**
   * Allows to define if {@link #isSequentialProcessingAllowed() sequential processing} should be allowed.
   * <p/>
   * Current approach is not allow to stop sequential processing for more than predefine amount of time (couple of seconds).
   * That means that call to this method with {@code 'true'} argument is not mandatory for successful processing even
   * if this method is called with {@code 'false'} argument before.
   *
   * @param allowed flag that defines if {@link #isSequentialProcessingAllowed() sequential processing} should be allowed
   */
  public static void setSequentialProcessingAllowed(boolean allowed) {
    ProcessingUnderProgressInfo info = SEQUENTIAL_PROCESSING_ALLOWED.get();
    if (allowed) {
      info.decrement();
    }
    else {
      info.increment();
    }
  }
}

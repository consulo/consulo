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
package consulo.diff.impl.internal.content;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionActionFilter;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
* @author VISTALL
* @since 22-Jun-22
*/
@ExtensionImpl
public class DiffFileIntentionFilter implements IntentionActionFilter {
  @Override
  public boolean accept(@Nonnull IntentionAction intentionAction, @Nullable PsiFile file) {
    return !DiffPsiFileSupport.isDiffFile(file);
  }
}

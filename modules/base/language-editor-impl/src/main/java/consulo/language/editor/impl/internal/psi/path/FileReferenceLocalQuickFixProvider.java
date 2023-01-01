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
package consulo.language.editor.impl.internal.psi.path;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.PsiReferenceLocalQuickFixProvider;
import consulo.language.psi.path.FileReference;
import consulo.language.psi.PsiReference;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 27-Mar-22
 */
@ExtensionImpl
public class FileReferenceLocalQuickFixProvider implements PsiReferenceLocalQuickFixProvider {
  @Override
  public void addQuickFixes(@Nonnull PsiReference reference, @Nonnull Consumer<LocalQuickFix> consumer) {
    if (reference instanceof FileReference fileReference) {
      List<? extends LocalQuickFix> fixes = FileReferenceQuickFixProvider.registerQuickFix(fileReference);
      for (LocalQuickFix fix : fixes) {
        consumer.accept(fix);
      }
    }
  }
}

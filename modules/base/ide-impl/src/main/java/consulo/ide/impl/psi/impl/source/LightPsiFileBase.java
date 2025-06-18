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

package consulo.ide.impl.psi.impl.source;

import consulo.language.Language;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiElementVisitor;
import jakarta.annotation.Nonnull;

public abstract class LightPsiFileBase extends LightPsiFileImpl {
  public LightPsiFileBase(final FileViewProvider provider, final Language language) {
    super(provider, language);
  }

  @Override
  public boolean isDirectory() {
    return false;
  }

  @Override
  public void accept(@Nonnull PsiElementVisitor visitor) {
    visitor.visitFile(this);
  }
}

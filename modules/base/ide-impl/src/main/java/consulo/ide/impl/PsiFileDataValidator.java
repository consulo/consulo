/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataValidator;
import consulo.util.dataholder.Key;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 16-Oct-17
 */
@ExtensionImpl
public class PsiFileDataValidator implements DataValidator<PsiFile> {
  @Nonnull
  @Override
  public Key<PsiFile> getKey() {
    return PsiFile.KEY;
  }

  @Nullable
  @Override
  public PsiFile findInvalid(Key<PsiFile> key, PsiFile data, Object dataSource) {
    return data.isValid() ? null : data;
  }
}

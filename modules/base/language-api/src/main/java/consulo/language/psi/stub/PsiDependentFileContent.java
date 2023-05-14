// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.stub;

import consulo.language.ast.LighterAST;
import consulo.language.psi.PsiFile;

import jakarta.annotation.Nonnull;

public interface PsiDependentFileContent extends FileContent {
  @Override
  @Nonnull
  PsiFile getPsiFile();

  @Nonnull
  LighterAST getLighterAST();
}

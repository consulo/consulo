// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.wrapper;

import consulo.language.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public interface StreamChain {

  @NotNull
  QualifierExpression getQualifierExpression();

  @NotNull
  List<IntermediateStreamCall> getIntermediateCalls();

  @NotNull
  StreamCall getCall(int index);

  @NotNull
  TerminatorStreamCall getTerminationCall();

  @NotNull String getText();

  @NotNull String getCompactText();

  int length();

  @NotNull
  PsiElement getContext();
}

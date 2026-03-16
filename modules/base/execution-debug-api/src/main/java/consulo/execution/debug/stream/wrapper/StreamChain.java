// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.wrapper;

import consulo.language.psi.PsiElement;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public interface StreamChain {

  
  QualifierExpression getQualifierExpression();

  
  List<IntermediateStreamCall> getIntermediateCalls();

  
  StreamCall getCall(int index);

  
  TerminatorStreamCall getTerminationCall();

  String getText();

  String getCompactText();

  int length();

  
  PsiElement getContext();
}

// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.parameterInfo;

import consulo.language.psi.PsiElement;
import consulo.util.dataholder.UserDataHolderEx;

public interface UpdateParameterInfoContext extends ParameterInfoContext {
  void removeHint();

  void setParameterOwner(PsiElement o);

  PsiElement getParameterOwner();

  void setHighlightedParameter(Object parameter);

  Object getHighlightedParameter();

  void setCurrentParameter(int index);

  boolean isUIComponentEnabled(int index);

  void setUIComponentEnabled(int index, boolean enabled);

  int getParameterListStart();

  Object[] getObjectsToView();

  boolean isPreservedOnHintHidden();

  void setPreservedOnHintHidden(boolean value);

  boolean isInnermostContext();

  boolean isSingleParameterInfo();

  UserDataHolderEx getCustomContext();
}

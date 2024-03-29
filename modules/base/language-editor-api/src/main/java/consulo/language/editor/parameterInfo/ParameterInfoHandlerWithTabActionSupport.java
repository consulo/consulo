// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.parameterInfo;

import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import java.util.Set;

public interface ParameterInfoHandlerWithTabActionSupport<ParameterOwner extends PsiElement, ParameterType, ActualParameterType extends PsiElement>
        extends ParameterInfoHandler<ParameterOwner, ParameterType> {

  @Nonnull
  ActualParameterType[] getActualParameters(@Nonnull ParameterOwner o);

  @Nonnull
  IElementType getActualParameterDelimiterType();

  @Nonnull
  IElementType getActualParametersRBraceType();

  @Nonnull
  Set<Class<?>> getArgumentListAllowedParentClasses();

  @SuppressWarnings("TypeParameterExtendsFinalClass") // keep historical signature for compatibility
  @Nonnull
  Set<? extends Class<?>> getArgListStopSearchClasses();

  @Nonnull
  Class<ParameterOwner> getArgumentListClass();

  @Override
  default boolean isWhitespaceSensitive() {
    return getActualParameterDelimiterType() == TokenType.WHITE_SPACE;
  }
}

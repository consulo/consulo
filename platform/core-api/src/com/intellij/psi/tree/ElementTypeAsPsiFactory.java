/*
 * Copyright 2013 must-be.org
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
package com.intellij.psi.tree;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ReflectionUtil;
import consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;

/**
 * @author VISTALL
 * @since 13:28/29.08.13
 */
@Logger
public class ElementTypeAsPsiFactory extends IElementType implements IElementTypeAsPsiFactory {
  private Constructor<? extends PsiElement> myConstructor;

  public ElementTypeAsPsiFactory(@NotNull @NonNls String debugName, @Nullable Language language, @NotNull Class<? extends PsiElement> clazz) {
    this(debugName, language, true, clazz);
  }

  public ElementTypeAsPsiFactory(@NotNull @NonNls String debugName,
                                 @Nullable Language language,
                                 boolean register,
                                 @NotNull Class<? extends PsiElement> clazz) {
    super(debugName, language, register);

    try {
      myConstructor = clazz.getConstructor(ASTNode.class);
    }
    catch (NoSuchMethodException e) {
      LOGGER.error("Cant find constructor for " + clazz.getName() + " with argument: " + ASTNode.class.getName() + ", or it not public.", e);
    }
  }

  @Override
  @NotNull
  public PsiElement createElement(@NotNull ASTNode astNode) {
    if (myConstructor == null) {
      return PsiUtilCore.NULL_PSI_ELEMENT;
    }
    return ReflectionUtil.createInstance(myConstructor, astNode);
  }
}

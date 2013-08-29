/*
 * Copyright 2013 Consulo.org
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
import com.intellij.lang.LanguageVersion;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ReflectionUtil;
import org.consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;

/**
 * @author VISTALL
 * @since 13:28/29.08.13
 */
@Logger
public class IElementTypeAsPsiFactory extends IElementType {
  private Constructor<? extends PsiElement> myConstructor;

  public IElementTypeAsPsiFactory(@NotNull @NonNls String debugName,
                                  @Nullable Language language,
                                  @NotNull Class<? extends PsiElement> clazz) {
    this(debugName, language, null, true, clazz);
  }

  public IElementTypeAsPsiFactory(@NotNull @NonNls String debugName,
                                  @Nullable Language language,
                                  @Nullable LanguageVersion languageVersion,
                                  @NotNull Class<? extends PsiElement> clazz) {
    this(debugName, language, languageVersion, true, clazz);
  }

  public IElementTypeAsPsiFactory(@NotNull @NonNls String debugName,
                                  @Nullable Language language,
                                  @Nullable LanguageVersion languageVersion,
                                  boolean register,
                                  @NotNull Class<? extends PsiElement> clazz) {
    super(debugName, language, languageVersion, register);

    try {
      myConstructor = clazz.getConstructor(ASTNode.class);
    }
    catch (NoSuchMethodException e) {
      LOGGER.error("Cant find constructor for " + clazz.getName() + " with argument: " + ASTNode.class.getName() + ", or it not public.", e);
    }
  }

  @NotNull
  public PsiElement createElement(ASTNode astNode) {
    if(myConstructor == null) {
      return PsiUtilCore.NULL_PSI_ELEMENT;
    }
    return ReflectionUtil.createInstance(myConstructor, astNode);
  }
}

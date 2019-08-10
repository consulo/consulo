/*
 * Copyright 2013-2016 consulo.io
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
package consulo.psi.tree;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ReflectionUtil;
import consulo.logging.Logger;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.lang.reflect.Constructor;

/**
 * @author VISTALL
 * @since 13:28/29.08.13
 */
public class ElementTypeAsPsiFactory extends IElementType implements IElementTypeAsPsiFactory {
  public static final Logger LOGGER = Logger.getInstance(ElementTypeAsPsiFactory.class);

  private Constructor<? extends PsiElement> myConstructor;

  public ElementTypeAsPsiFactory(@Nonnull @NonNls String debugName, @Nullable Language language, @Nonnull Class<? extends PsiElement> clazz) {
    this(debugName, language, true, clazz);
  }

  public ElementTypeAsPsiFactory(@Nonnull @NonNls String debugName,
                                 @Nullable Language language,
                                 boolean register,
                                 @Nonnull Class<? extends PsiElement> clazz) {
    super(debugName, language, register);

    try {
      myConstructor = clazz.getConstructor(ASTNode.class);
    }
    catch (NoSuchMethodException e) {
      ElementTypeAsPsiFactory
              .LOGGER.error("Cant find constructor for " + clazz.getName() + " with argument: " + ASTNode.class.getName() + ", or it not public.", e);
    }
  }

  @Override
  @Nonnull
  public PsiElement createElement(@Nonnull ASTNode astNode) {
    if (myConstructor == null) {
      return PsiUtilCore.NULL_PSI_ELEMENT;
    }
    return ReflectionUtil.createInstance(myConstructor, astNode);
  }
}

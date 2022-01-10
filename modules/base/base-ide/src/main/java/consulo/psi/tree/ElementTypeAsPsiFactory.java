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
import consulo.annotation.DeprecationInfo;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 13:28/29.08.13
 */
public class ElementTypeAsPsiFactory extends IElementType implements IElementTypeAsPsiFactory {
  private static final Logger LOG = Logger.getInstance(ElementTypeAsPsiFactory.class);

  @Nonnull
  private final Function<ASTNode, ? extends PsiElement> myFactory;

  @Deprecated(forRemoval = true)
  @DeprecationInfo("Use constructor with Function parameter")
  public ElementTypeAsPsiFactory(@Nonnull String debugName, @Nullable Language language, @Nonnull Class<? extends PsiElement> clazz) {
    this(debugName, language, true, clazz);
  }

  @Deprecated(forRemoval = true)
  @DeprecationInfo("Use constructor with Function parameter")
  public ElementTypeAsPsiFactory(@Nonnull String debugName, @Nullable Language language, boolean register, @Nonnull Class<? extends PsiElement> clazz) {
    super(debugName, language, register);

    Function<ASTNode, PsiElement> function = null;
    try {
      Constructor<? extends PsiElement> constructor = clazz.getConstructor(ASTNode.class);
      function = it -> ReflectionUtil.createInstance(constructor, it);
    }
    catch (NoSuchMethodException e) {
      LOG.error("Cant find constructor for " + clazz.getName() + " with argument: " + ASTNode.class.getName() + ", or it not public.", e);
    }

    if (function == null) {
      function = it -> PsiUtilCore.NULL_PSI_ELEMENT;
    }

    myFactory = function;
  }

  public ElementTypeAsPsiFactory(@Nonnull String debugName, @Nullable Language language, @Nonnull Function<ASTNode, ? extends PsiElement> factory) {
    this(debugName, language, true, factory);
  }

  public ElementTypeAsPsiFactory(@Nonnull String debugName, @Nullable Language language, boolean register, @Nonnull Function<ASTNode, ? extends PsiElement> factory) {
    super(debugName, language, register);

    myFactory = factory;
  }

  @Override
  @Nonnull
  public PsiElement createElement(@Nonnull ASTNode node) {
    return myFactory.apply(node);
  }
}

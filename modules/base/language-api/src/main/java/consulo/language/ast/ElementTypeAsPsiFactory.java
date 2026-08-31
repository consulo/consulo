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
package consulo.language.ast;

import consulo.language.Language;
import consulo.language.psi.PsiElement;

import org.jspecify.annotations.Nullable;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 13:28/29.08.13
 */
public class ElementTypeAsPsiFactory extends IElementType implements IElementTypeAsPsiFactory {
  
  private final Function<ASTNode, ? extends PsiElement> myFactory;

  public ElementTypeAsPsiFactory(String debugName, @Nullable Language language, Function<ASTNode, ? extends PsiElement> factory) {
    this(debugName, language, true, factory);
  }

  public ElementTypeAsPsiFactory(
    String debugName,
    @Nullable Language language,
    boolean register,
    Function<ASTNode, ? extends PsiElement> factory
  ) {
    super(debugName, language, register);

    myFactory = factory;
  }

  @Override
  public PsiElement createElement(ASTNode node) {
    return myFactory.apply(node);
  }
}

/*
 * Copyright 2013-2022 consulo.io
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
package consulo.sandboxPlugin.lang.psi;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.psi.AbstractElementManipulator;
import consulo.language.util.IncorrectOperationException;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 16-Jul-22
 */
@ExtensionImpl
public class SandStringExpressionElementManipulator extends AbstractElementManipulator<SandStringExpression> {
  @Override
  public SandStringExpression handleContentChange(@Nonnull SandStringExpression element, @Nonnull TextRange range, String newContent) throws IncorrectOperationException {
    return element;
  }

  @Nonnull
  @Override
  public Class<SandStringExpression> getElementClass() {
    return SandStringExpression.class;
  }
}

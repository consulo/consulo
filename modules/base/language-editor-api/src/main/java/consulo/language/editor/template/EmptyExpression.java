/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.editor.template;

import consulo.language.editor.completion.lookup.LookupElement;

/**
 * @author ven
 */
public class EmptyExpression extends Expression {
  @Override
  public Result calculateResult(ExpressionContext context) {
    return null;
  }

  @Override
  public Result calculateQuickResult(ExpressionContext context) {
    return null;
  }

  @Override
  public LookupElement[] calculateLookupItems(ExpressionContext context) {
    return null;
  }
}
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
package consulo.language.editor.template.macro;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.template.Expression;
import consulo.language.editor.template.ExpressionContext;
import consulo.language.editor.template.Result;

import javax.annotation.Nonnull;
import java.util.LinkedHashSet;
import java.util.Set;

@ExtensionImpl
public class EnumMacro extends Macro {
  @Override
  public String getName() {
    return "enum";
  }

  @Override
  public String getPresentableName() {
    return CodeInsightBundle.message("macro.enum");
  }

  @Override
  public Result calculateResult(@Nonnull Expression[] params, ExpressionContext context) {
    if (params.length == 0) return null;
    return params[0].calculateResult(context);
  }

  @Override
  public Result calculateQuickResult(@Nonnull Expression[] params, ExpressionContext context) {
    if (params.length == 0) return null;
    return params[0].calculateQuickResult(context);
  }

  @Override
  public LookupElement[] calculateLookupItems(@Nonnull Expression[] params, ExpressionContext context) {
    if (params.length ==0) return null;
    Set<LookupElement> set = new LinkedHashSet<LookupElement>();

    for (Expression param : params) {
      Result object = param.calculateResult(context);
      if (object != null) {
        set.add(LookupElementBuilder.create(object.toString()));
      }
    }
    return set.toArray(new LookupElement[set.size()]);
  }

}
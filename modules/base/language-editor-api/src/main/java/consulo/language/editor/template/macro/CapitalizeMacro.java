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
import consulo.language.editor.template.Expression;
import consulo.language.editor.template.ExpressionContext;
import consulo.language.editor.template.Result;
import consulo.language.editor.template.TextResult;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class CapitalizeMacro extends MacroBase {
  public CapitalizeMacro() {
    super("capitalize", CodeInsightBundle.message("macro.capitalize.string"));
  }

  @Override
  protected Result calculateResult(@Nonnull Expression[] params, ExpressionContext context, boolean quick) {
    String text = getTextResult(params, context);
    if (text != null) {
      if (text.length() > 0) {
        text = StringUtil.toUpperCase(text.substring(0, 1)) + text.substring(1, text.length());
      }
      return new TextResult(text);
    }
    return null;
  }
}

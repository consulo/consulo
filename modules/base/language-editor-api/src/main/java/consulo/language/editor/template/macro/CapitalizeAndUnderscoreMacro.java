/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
import consulo.application.util.matcher.NameUtil;
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
public class CapitalizeAndUnderscoreMacro extends MacroBase {
  public CapitalizeAndUnderscoreMacro() {
    super("capitalizeAndUnderscore", CodeInsightBundle.message("macro.capitalizeAndUnderscore.string"));
  }

  @Override
  protected Result calculateResult(@Nonnull Expression[] params, ExpressionContext context, boolean quick) {
    String text = MacroBase.getTextResult(params, context, true);
    if (text != null && text.length() > 0) {
      String[] words = NameUtil.nameToWords(text);
      boolean insertUnderscore = false;
      StringBuilder buf = new StringBuilder();
      for (String word : words) {
        if (insertUnderscore) {
          buf.append("_");
        } else {
          insertUnderscore = true;
        }
        buf.append(StringUtil.toUpperCase(word));
      }
      return new TextResult(buf.toString());
    }
    return null;
  }
}

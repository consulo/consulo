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

package consulo.ide.impl.idea.codeInsight.template.macro;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.template.Expression;
import consulo.language.editor.template.ExpressionContext;
import consulo.language.editor.template.Result;
import consulo.logging.Logger;
import consulo.ide.impl.idea.openapi.util.Clock;
import consulo.ide.impl.idea.util.text.DateFormatUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author yole
 */
@ExtensionImpl
public class CurrentDateMacro extends SimpleMacro {
  private static final Logger LOG = Logger.getInstance(CurrentDateMacro.class);

  public CurrentDateMacro() {
    super("date");
  }

  @Override
  protected String evaluateSimpleMacro(Expression[] params, final ExpressionContext context) {
    return formatUserDefined(params, context, true);
  }

  static String formatUserDefined(Expression[] params, ExpressionContext context, boolean date) {
    long time = Clock.getTime();
    if (params.length == 1) {
      Result format = params[0].calculateResult(context);
      if (format != null) {
        String pattern = format.toString();
        try {
          return new SimpleDateFormat(pattern).format(new Date(time));
        }
        catch (Exception e) {
          return "Problem when formatting date/time for pattern \"" + pattern + "\": " + e.getMessage();
        }
      }
    }
    return date ? DateFormatUtil.formatDate(time) : DateFormatUtil.formatTime(time);
  }
}
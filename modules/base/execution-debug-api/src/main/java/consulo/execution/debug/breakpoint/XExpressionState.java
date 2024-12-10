/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.execution.debug.breakpoint;

import consulo.execution.debug.internal.breakpoint.XExpressionImpl;
import consulo.language.Language;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Text;
import jakarta.annotation.Nonnull;

/**
 * @author egor
 */
public class XExpressionState {
  @Attribute("disabled")
  public boolean myDisabled;

  @Attribute("expression")
  public String myExpression;

  @Attribute("language")
  public String myLanguage;

  @Attribute("custom")
  public String myCustomInfo;

  @Text
  public String myOldExpression;

  public XExpressionState() {
  }

  public XExpressionState(boolean disabled, @Nonnull String expression, String language, String customInfo) {
    myDisabled = disabled;
    myExpression = expression;
    myLanguage = language;
    myCustomInfo = customInfo;
  }

  public XExpressionState(boolean disabled, XExpression expression) {
    this(disabled, expression.getExpression(), expression.getLanguage() != null ? expression.getLanguage().getID() : null, expression.getCustomInfo());
  }

  public XExpressionState(XExpression expression) {
    this(false, expression);
  }

  public void checkConverted() {
    if (myOldExpression != null) {
      myExpression = myOldExpression;
      myOldExpression = null;
    }
  }

  public XExpression toXExpression() {
    checkConverted();
    return new XExpressionImpl(myExpression, Language.findLanguageByID(myLanguage), myCustomInfo);
  }
}

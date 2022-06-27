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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.component.extension.ExtensionPointName;
import consulo.language.editor.template.Expression;
import consulo.language.editor.template.ExpressionContext;
import consulo.language.editor.template.Result;
import consulo.language.editor.template.context.TemplateContextType;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A macro which can be used in live templates.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class Macro {
  public static final ExtensionPointName<Macro> EP_NAME = ExtensionPointName.create(Macro.class);
  
  @NonNls
  public abstract String getName();

  /**
   * @return a presentable string that will be shown in the combobox in Edit Template Variables dialog
   */
  public abstract String getPresentableName();

  @NonNls
  @Nonnull
  public String getDefaultValue() {
    return "";
  }

  @Nullable
  public abstract Result calculateResult(@Nonnull Expression[] params, ExpressionContext context);

  @Nullable
  public Result calculateQuickResult(@Nonnull Expression[] params, ExpressionContext context) {
    return null;
  }

  @Nullable
  public LookupElement[] calculateLookupItems(@Nonnull Expression[] params, ExpressionContext context) {
    return null;
  }

  public boolean isAcceptableInContext(TemplateContextType context) {
    return true;
  }
}

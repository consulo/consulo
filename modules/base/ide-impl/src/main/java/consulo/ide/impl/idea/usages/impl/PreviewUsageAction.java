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
package consulo.ide.impl.idea.usages.impl;

import consulo.application.AllIcons;
import consulo.usage.RuleAction;
import consulo.usage.UsageViewBundle;
import consulo.usage.UsageView;
import consulo.usage.UsageViewSettings;
import jakarta.annotation.Nonnull;

/**
* @author cdr
*/
class PreviewUsageAction extends RuleAction {
  PreviewUsageAction(@Nonnull UsageView usageView) {
    super(usageView, UsageViewBundle.message("preview.usages.action.text"), AllIcons.Actions.PreviewDetails);
  }

  @Override
  protected boolean getOptionValue() {
    return UsageViewSettings.getInstance().IS_PREVIEW_USAGES;
  }

  @Override
  protected void setOptionValue(final boolean value) {
    UsageViewSettings.getInstance().IS_PREVIEW_USAGES = value;
  }
}

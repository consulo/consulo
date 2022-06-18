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

package consulo.ide.impl.idea.ide.impl.dataRules;

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataProvider;
import consulo.dataContext.GetDataRule;
import consulo.util.dataholder.Key;
import consulo.usage.UsageTarget;
import consulo.usage.UsageTargetUtil;
import consulo.usage.UsageView;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author max
 */
@ExtensionImpl
public class UsageTargetsRule implements GetDataRule<UsageTarget[]> {
  @Nonnull
  @Override
  public Key<UsageTarget[]> getKey() {
    return UsageView.USAGE_TARGETS_KEY;
  }

  @Override
  @Nullable
  public UsageTarget[] getData(@Nonnull DataProvider dataProvider) {
    return UsageTargetUtil.findUsageTargets(dataProvider);
  }
}
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
import consulo.usage.UsageInfo;
import consulo.usage.UsageView;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author cdr
 */
@ExtensionImpl
public class UsageInfo2ListRule implements GetDataRule<List<UsageInfo>> {
  @Nonnull
  @Override
  public Key<List<UsageInfo>> getKey() {
    return UsageView.USAGE_INFO_LIST_KEY;
  }

  @Override
  @Nullable
  public List<UsageInfo> getData(@Nonnull final DataProvider dataProvider) {
    UsageInfo usageInfo = dataProvider.getDataUnchecked(UsageView.USAGE_INFO_KEY);
    if (usageInfo != null) return Collections.singletonList(usageInfo);
    return null;
  }
}

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

import consulo.dataContext.DataSnapshot;
import consulo.usage.UsageInfo;
import consulo.usage.UsageView;

import org.jspecify.annotations.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author cdr
 */
public final class UsageInfo2ListRule {
  static @Nullable List<UsageInfo> getData(DataSnapshot dataProvider) {
    UsageInfo usageInfo = dataProvider.get(UsageView.USAGE_INFO_KEY);
    if (usageInfo != null) return Collections.singletonList(usageInfo);
    return null;
  }
}

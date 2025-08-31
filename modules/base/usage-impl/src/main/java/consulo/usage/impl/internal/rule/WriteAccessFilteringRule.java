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
package consulo.usage.impl.internal.rule;

import consulo.usage.ReadWriteAccessUsage;
import consulo.usage.Usage;
import consulo.usage.UsageTarget;
import consulo.usage.rule.UsageFilteringRule;
import jakarta.annotation.Nonnull;

/**
 * @author Eugene Zhuravlev
 * @since 2005-01-17
 */
public class WriteAccessFilteringRule implements UsageFilteringRule{
  @Override
  public boolean isVisible(@Nonnull Usage usage, @Nonnull UsageTarget[] targets) {
    if (usage instanceof ReadWriteAccessUsage) {
      ReadWriteAccessUsage readWriteAccessUsage = (ReadWriteAccessUsage)usage;
      boolean isForWritingOnly = readWriteAccessUsage.isAccessedForWriting() && !readWriteAccessUsage.isAccessedForReading();
      return !isForWritingOnly;
    }
    return true;
  }
}

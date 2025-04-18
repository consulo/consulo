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
package consulo.usage.rule;

import consulo.usage.Usage;
import consulo.usage.UsageTarget;
import jakarta.annotation.Nonnull;

public interface UsageFilteringRule {
    UsageFilteringRule[] EMPTY_ARRAY = new UsageFilteringRule[0];

    default boolean isVisible(@Nonnull Usage usage, @Nonnull UsageTarget[] targets) {
        return isVisible(usage);
    }

    /**
     * @deprecated implement {@link #isVisible(Usage, UsageTarget[])} instead
     */
    default boolean isVisible(@Nonnull Usage usage) {
        throw new UnsupportedOperationException();
    }
}

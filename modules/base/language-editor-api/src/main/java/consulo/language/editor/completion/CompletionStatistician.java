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
package consulo.language.editor.completion;

import consulo.application.ApplicationManager;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.statistician.Statistician;
import consulo.language.statistician.StatisticsInfo;
import consulo.language.statistician.StatisticsManager;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public abstract class CompletionStatistician extends Statistician<LookupElement, CompletionLocation, CompletionStatistician> {
    public static final Key<CompletionStatistician> STATISTICS_KEY = Key.create("completion");
    private static final Key<StatisticsInfo> BASE_STATISTICS_INFO = Key.create("Base statistics info");
    private static final Logger LOG = Logger.getInstance(CompletionStatistician.class);

    @Override
    public abstract StatisticsInfo serialize(final LookupElement element, final CompletionLocation location);

    @Nonnull
    @Override
    public Key<CompletionStatistician> getKey() {
        return STATISTICS_KEY;
    }

    public static void clearBaseStatisticsInfo(LookupElement item) {
        item.putUserData(BASE_STATISTICS_INFO, null);
    }

    @Nonnull
    public static StatisticsInfo getBaseStatisticsInfo(LookupElement item, @Nullable CompletionLocation location) {
        StatisticsInfo info = BASE_STATISTICS_INFO.get(item);
        if (info == null) {
            if (location == null) {
                return StatisticsInfo.EMPTY;
            }
            BASE_STATISTICS_INFO.set(item, info = calcBaseInfo(item, location));
        }
        return info;
    }

    @Nonnull
    private static StatisticsInfo calcBaseInfo(LookupElement item, @Nonnull CompletionLocation location) {
        if (!ApplicationManager.getApplication().isUnitTestMode()) {
            LOG.assertTrue(!ApplicationManager.getApplication().isDispatchThread());
        }
        StatisticsInfo info = StatisticsManager.serialize(STATISTICS_KEY, item, location);
        return info == null ? StatisticsInfo.EMPTY : info;
    }
}

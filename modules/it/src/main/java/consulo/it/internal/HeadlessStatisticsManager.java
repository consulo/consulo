/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.language.statistician.StatisticsInfo;
import consulo.language.statistician.StatisticsManager;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Headless {@code StatisticsManager}: the production impl lives in {@code ide-impl} and persists the statistics;
 * this one keeps them in memory, most recent use first.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessStatisticsManager extends StatisticsManager {
  private final Map<String, List<String>> myValuesByContext = new HashMap<>();

  @Override
  public synchronized int getUseCount(StatisticsInfo info) {
    List<String> values = myValuesByContext.get(info.getContext());
    if (values == null) {
      return 0;
    }
    int count = 0;
    for (int i = 0; i < values.size() && i < OBLIVION_THRESHOLD; i++) {
      if (values.get(i).equals(info.getValue())) {
        count++;
      }
    }
    return count;
  }

  @Override
  public synchronized int getLastUseRecency(StatisticsInfo info) {
    List<String> values = myValuesByContext.get(info.getContext());
    int index = values == null ? -1 : values.indexOf(info.getValue());
    return index < 0 || index >= RECENCY_OBLIVION_THRESHOLD ? Integer.MAX_VALUE : index;
  }

  @Override
  public synchronized void incUseCount(StatisticsInfo info) {
    if (info == StatisticsInfo.EMPTY) {
      return;
    }
    myValuesByContext.computeIfAbsent(info.getContext(), k -> new ArrayList<>()).add(0, info.getValue());
  }

  @Override
  public synchronized StatisticsInfo[] getAllValues(String context) {
    List<String> values = myValuesByContext.get(context);
    if (values == null) {
      return new StatisticsInfo[0];
    }
    return values.stream().distinct().map(value -> new StatisticsInfo(context, value)).toArray(StatisticsInfo[]::new);
  }
}

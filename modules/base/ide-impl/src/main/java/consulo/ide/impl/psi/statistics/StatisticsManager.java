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
package consulo.ide.impl.psi.statistics;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.component.persist.SettingsSavingComponent;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class StatisticsManager implements SettingsSavingComponent {
  /**
   * The number of last entries stored for each context in {@link #getUseCount(StatisticsInfo)}.
   * If more entries are used over time, use count will be 0 for the most ancient entries.
   */
  public static final int OBLIVION_THRESHOLD = 7;

  /**
   * The number of last entries stored for each context in {@link #getLastUseRecency(StatisticsInfo)}.
   * If more entries are used over time, recency will be {@code Integer.MAX_INT} for the most ancient entries.
   */
  public static final int RECENCY_OBLIVION_THRESHOLD = 10000;

  @Nullable
  @SuppressWarnings("unchecked")
  public static <T, Loc, Stat extends Statistician<T, Loc, Stat>> StatisticsInfo serialize(Key<? extends Statistician<T, Loc, Stat>> key, T element, Loc location) {
    for (Statistician<T, Loc, Stat> statistician : Statistician.forKey(key)) {
      final StatisticsInfo info = statistician.serialize(element, location);
      if (info != null) return info;
    }
    return null;
  }

  public static StatisticsManager getInstance() {
    return Application.get().getInstance(StatisticsManager.class);
  }

  /**
   * @return how many times {@code info.getValue()} was used among all recently registered entries with the same {@code info.getContext()}.
   * @see #incUseCount(StatisticsInfo)
   * @see #OBLIVION_THRESHOLD
   */
  public abstract int getUseCount(@Nonnull StatisticsInfo info);

  /**
   * @return the position of {@code info.getValue()} in all recently registered entries with the same {@code info.getContext()}. 0 if it it's the most recent entry, {@code Integer.MAX_INT} if it was never used, or was used too long ago (more than {@link #RECENCY_OBLIVION_THRESHOLD} other entries with the same context have been registered with {@link #incUseCount(StatisticsInfo)} since.
   */
  public abstract int getLastUseRecency(@Nonnull StatisticsInfo info);

  /**
   * Register a usage of an <context, value> entry represented by info parameter. This will affect subsequent {@link #getUseCount(StatisticsInfo)} and {@link #getLastUseRecency(StatisticsInfo)} results.
   */
  public abstract void incUseCount(@Nonnull StatisticsInfo info);

  public <T, Loc, Stat extends Statistician<T, Loc, Stat>> int getUseCount(final Key<? extends Statistician<T, Loc, Stat>> key, final T element, final Loc location) {
    final StatisticsInfo info = serialize(key, element, location);
    return info == null ? 0 : getUseCount(info);
  }

  public <T, Loc, Stat extends Statistician<T, Loc, Stat>> void incUseCount(final Key<? extends Statistician<T, Loc, Stat>> key, final T element, final Loc location) {
    final StatisticsInfo info = serialize(key, element, location);
    if (info != null) {
      incUseCount(info);
    }
  }

  /**
   * @return infos by this context ordered by usage time: recent first
   */
  public abstract StatisticsInfo[] getAllValues(@Nonnull String context);
}

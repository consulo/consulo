/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change;

import com.google.common.primitives.Ints;
import consulo.util.collection.ContainerUtil;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.LocalChangeList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public class ChangeListUtil {

  @Nullable
  public static LocalChangeList getPredefinedChangeList(@Nonnull String defaultName, @Nonnull ChangeListManager changeListManager) {
    LocalChangeList sameNamedList = changeListManager.findChangeList(defaultName);
    if (sameNamedList != null) return sameNamedList;
    return tryToMatchWithExistingChangelist(changeListManager, defaultName);
  }

  @Nullable
  private static LocalChangeList tryToMatchWithExistingChangelist(@Nonnull ChangeListManager changeListManager,
                                                                  @Nonnull String defaultName) {
    List<LocalChangeList> matched = ContainerUtil.findAll(changeListManager.getChangeListsCopy(),
                                                          list -> defaultName.contains(list.getName().trim()));

    return matched.isEmpty() ? null : Collections.max(matched,
                                                      (o1, o2) -> Ints.compare(o1.getName().trim().length(), o2.getName().trim().length()));
  }
}

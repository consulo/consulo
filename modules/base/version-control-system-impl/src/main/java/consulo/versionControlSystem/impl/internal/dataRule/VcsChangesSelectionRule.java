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
package consulo.versionControlSystem.impl.internal.dataRule;

import consulo.dataContext.DataSnapshot;
import consulo.util.collection.ArrayUtil;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangesSelection;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;

public final class VcsChangesSelectionRule {
  @Nullable
  public static ChangesSelection getData(@Nonnull DataSnapshot dataProvider) {
    return getChangesSelection(dataProvider);
  }

  @Nullable
  public static ChangesSelection getChangesSelection(@Nonnull DataSnapshot dataProvider) {
    Change currentChange = dataProvider.get(VcsDataKeys.CURRENT_CHANGE);

    Change[] selectedChanges = dataProvider.get(VcsDataKeys.SELECTED_CHANGES);
    if (selectedChanges != null) {
      int index = Math.max(ArrayUtil.indexOf(selectedChanges, currentChange), 0);
      return new ChangesSelection(Arrays.asList(selectedChanges), index);
    }

    Change[] changes = dataProvider.get(VcsDataKeys.CHANGES);
    if (changes != null) {
      int index = Math.max(ArrayUtil.indexOf(changes, currentChange), 0);
      return new ChangesSelection(Arrays.asList(changes), index);
    }

    if (currentChange != null) {
      return new ChangesSelection(Collections.singletonList(currentChange), 0);
    }
    return null;
  }
}

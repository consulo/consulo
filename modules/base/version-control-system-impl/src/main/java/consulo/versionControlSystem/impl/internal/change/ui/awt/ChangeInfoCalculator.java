/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change.ui.awt;

import consulo.application.util.function.Processor;
import consulo.versionControlSystem.change.Change;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

public class ChangeInfoCalculator implements CommitLegendPanel.InfoCalculator {
  @Nonnull
  private List<Change> myDisplayedChanges;
  @Nonnull
  private List<Change> myIncludedChanges;
  private int myUnversionedFilesCount;
  private int myIncludedUnversionedFilesCount;

  public ChangeInfoCalculator() {
    myDisplayedChanges = Collections.emptyList();
    myIncludedChanges = Collections.emptyList();
    myUnversionedFilesCount = 0;
    myIncludedUnversionedFilesCount = 0;
  }

  public void update(@Nonnull List<Change> displayedChanges, @Nonnull List<Change> includedChanges) {
    update(displayedChanges, includedChanges, 0, 0);
  }

  public void update(@Nonnull List<Change> displayedChanges,
                     @Nonnull List<Change> includedChanges,
                     int unversionedFilesCount,
                     int includedUnversionedFilesCount) {
    myDisplayedChanges = displayedChanges;
    myIncludedChanges = includedChanges;
    myUnversionedFilesCount = unversionedFilesCount;
    myIncludedUnversionedFilesCount = includedUnversionedFilesCount;
  }

  public int getNew() {
    return countMatchingItems(myDisplayedChanges, NEW_FILTER);
  }

  public int getModified() {
    return countMatchingItems(myDisplayedChanges, MODIFIED_FILTER);
  }

  public int getDeleted() {
    return countMatchingItems(myDisplayedChanges, DELETED_FILTER);
  }

  @Override
  public int getUnversioned() {
    return myUnversionedFilesCount;
  }

  public int getIncludedNew() {
    return countMatchingItems(myIncludedChanges, NEW_FILTER);
  }

  public int getIncludedModified() {
    return countMatchingItems(myIncludedChanges, MODIFIED_FILTER);
  }

  public int getIncludedDeleted() {
    return countMatchingItems(myIncludedChanges, DELETED_FILTER);
  }

  @Override
  public int getIncludedUnversioned() {
    return myIncludedUnversionedFilesCount;
  }

  private static final Processor<Change> MODIFIED_FILTER = new Processor<Change>() {
    public boolean process(@Nonnull Change item) {
      return item.getType() == Change.Type.MODIFICATION || item.getType() == Change.Type.MOVED;
    }
  };
  private static final Processor<Change> NEW_FILTER = new Processor<Change>() {
    public boolean process(@Nonnull Change item) {
      return item.getType() == Change.Type.NEW;
    }
  };
  private static final Processor<Change> DELETED_FILTER = new Processor<Change>() {
    public boolean process(@Nonnull Change item) {
      return item.getType() == Change.Type.DELETED;
    }
  };

  private static <T> int countMatchingItems(@Nonnull List<T> items, @Nonnull Processor<T> filter) {
    int count = 0;

    for (T item : items) {
      if (filter.process(item)) count++;
    }

    return count;
  }
}

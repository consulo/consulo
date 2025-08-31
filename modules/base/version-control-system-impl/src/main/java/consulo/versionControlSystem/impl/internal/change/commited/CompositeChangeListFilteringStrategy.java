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
package consulo.versionControlSystem.impl.internal.change.commited;

import consulo.versionControlSystem.change.commited.ChangeListFilteringStrategy;
import consulo.versionControlSystem.change.commited.CommittedChangesFilterKey;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class CompositeChangeListFilteringStrategy implements ChangeListFilteringStrategy {
  private final TreeMap<CommittedChangesFilterKey, ChangeListFilteringStrategy> myDelegates;
  private boolean myInSetBase;

  public CompositeChangeListFilteringStrategy() {
    myDelegates = new TreeMap<CommittedChangesFilterKey, ChangeListFilteringStrategy>();
    myInSetBase = false;
  }

  public JComponent getFilterUI() {
    return null;
  }

  @Override
  public CommittedChangesFilterKey getKey() {
    throw new UnsupportedOperationException();
  }

  public void setFilterBase(List<CommittedChangeList> changeLists) {
    setFilterBaseImpl(changeLists, true);
  }

  private List<CommittedChangeList> setFilterBaseImpl(List<CommittedChangeList> changeLists, boolean setFirst) {
    if (myInSetBase) {
      return changeLists;
    }
    myInSetBase = true;

    List<CommittedChangeList> list = new ArrayList<CommittedChangeList>(changeLists);
    boolean callSetFilterBase = setFirst;
    for (ChangeListFilteringStrategy delegate : myDelegates.values()) {
      if (callSetFilterBase) {
        delegate.setFilterBase(list);
      }
      callSetFilterBase = true;
      list = delegate.filterChangeLists(list);
    }
    myInSetBase = false;
    return list;
  }

  public void addChangeListener(ChangeListener listener) {
    // not used
    for (ChangeListFilteringStrategy delegate : myDelegates.values()) {
      delegate.addChangeListener(listener);
    }
  }

  public void removeChangeListener(ChangeListener listener) {
    // not used
    for (ChangeListFilteringStrategy delegate : myDelegates.values()) {
      delegate.removeChangeListener(listener);
    }
  }

  public void resetFilterBase() {
    for (ChangeListFilteringStrategy delegate : myDelegates.values()) {
      delegate.resetFilterBase();
    }
  }

  public void appendFilterBase(List<CommittedChangeList> changeLists) {
    List<CommittedChangeList> list = new ArrayList<CommittedChangeList>(changeLists);
    for (ChangeListFilteringStrategy delegate : myDelegates.values()) {
      delegate.appendFilterBase(list);
      list = delegate.filterChangeLists(list);
    }
  }

  @Nonnull
  public List<CommittedChangeList> filterChangeLists(List<CommittedChangeList> changeLists) {
    return setFilterBaseImpl(changeLists, false);
  }

  public void addStrategy(CommittedChangesFilterKey key, ChangeListFilteringStrategy strategy) {
    myDelegates.put(key, strategy);
  }

  public ChangeListFilteringStrategy removeStrategy(CommittedChangesFilterKey key) {
    return myDelegates.remove(key);
  }
}

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

package consulo.ide.impl.idea.history.core.tree;

import consulo.ide.impl.idea.history.core.Paths;
import consulo.ide.impl.idea.history.core.StreamUtil;
import consulo.ide.impl.idea.history.core.revisions.Difference;
import consulo.ide.impl.idea.history.utils.LocalHistoryLog;
import consulo.index.io.data.DataInputOutputUtil;
import consulo.ide.impl.idea.util.text.CaseInsensitiveStringHashingStrategy;
import consulo.util.collection.Maps;
import gnu.trove.TIntObjectHashMap;

import jakarta.annotation.Nonnull;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DirectoryEntry extends Entry {
  private final ArrayList<Entry> myChildren;

  public DirectoryEntry(String name) {
    this(toNameId(name));
  }

  public DirectoryEntry(int nameId) {
    super(nameId);
    myChildren = new ArrayList<>(3);
  }

  public DirectoryEntry(DataInput in, @SuppressWarnings("unused") boolean dummy /* to distinguish from general constructor*/) throws IOException {
    super(in);
    int count = DataInputOutputUtil.readINT(in);
    myChildren = new ArrayList<>(count);
    while (count-- > 0) {
      unsafeAddChild(StreamUtil.readEntry(in));
    }
  }

  @Override
  public void write(DataOutput out) throws IOException {
    super.write(out);
    DataInputOutputUtil.writeINT(out, myChildren.size());
    for (Entry child : myChildren) {
      StreamUtil.writeEntry(out, child);
    }
  }

  @Override
  public long getTimestamp() {
    return -1;
  }

  @Override
  public boolean isDirectory() {
    return true;
  }

  @Override
  public void addChild(Entry child) {
    if (!checkDoesNotExist(child, child.getName())) return;
    unsafeAddChild(child);
  }

  public void addChildren(Collection<Entry> children) {
    myChildren.ensureCapacity(myChildren.size() + children.size());
    for (Entry each : children) {
      unsafeAddChild(each);
    }
  }

  private void unsafeAddChild(Entry child) {
    myChildren.add(child);
    child.setParent(this);
  }

  protected boolean checkDoesNotExist(Entry e, String name) {
    Entry found = findChild(name);
    if (found == null) return true;
    if (found == e) return false;

    removeChild(found);
    LocalHistoryLog.LOG.warn(String.format("entry '%s' already exists in '%s'", name, getPath()));
    return true;
  }

  @Override
  public void removeChild(Entry child) {
    myChildren.remove(child);
    child.setParent(null);
  }

  @Override
  public List<Entry> getChildren() {
    return myChildren;
  }

  @Override
  public boolean hasUnavailableContent(List<Entry> entriesWithUnavailableContent) {
    for (Entry e : myChildren) {
      e.hasUnavailableContent(entriesWithUnavailableContent);
    }
    return !entriesWithUnavailableContent.isEmpty();
  }

  @Nonnull
  @Override
  public DirectoryEntry copy() {
    DirectoryEntry result = copyEntry();
    result.myChildren.ensureCapacity(myChildren.size());
    for (Entry child : myChildren) {
      result.unsafeAddChild(child.copy());
    }
    return result;
  }

  protected DirectoryEntry copyEntry() {
    return new DirectoryEntry(getNameId());
  }

  @Override
  public void collectDifferencesWith(Entry right, List<Difference> result) {
    DirectoryEntry e = (DirectoryEntry)right;

    if (!getPath().equals(e.getPath())) {
      result.add(new Difference(false, this, e));
    }

    // most often we have the same children, so try processing it directly
    int commonIndex = 0;
    final int myChildrenSize = myChildren.size();
    final int rightChildrenSize = e.myChildren.size();
    final int minChildrenSize = Math.min(myChildrenSize, rightChildrenSize);

    while(commonIndex < minChildrenSize) {
      Entry childEntry = myChildren.get(commonIndex);
      Entry rightChildEntry = e.myChildren.get(commonIndex);

      if (childEntry.getNameId() == rightChildEntry.getNameId() && childEntry.isDirectory() == rightChildEntry.isDirectory()) {
        childEntry.collectDifferencesWith(rightChildEntry, result);
      } else {
        break;
      }
      ++commonIndex;
    }

    if (commonIndex == myChildrenSize && commonIndex == rightChildrenSize) return;

    TIntObjectHashMap<Entry> uniqueNameIdToMyChildEntries = new TIntObjectHashMap<>(myChildrenSize - commonIndex);
    for (int i = commonIndex; i < myChildrenSize; ++i) {
      Entry childEntry = myChildren.get(i);
      uniqueNameIdToMyChildEntries.put(childEntry.getNameId(), childEntry);
    }

    TIntObjectHashMap<Entry> uniqueNameIdToRightChildEntries = new TIntObjectHashMap<>(rightChildrenSize - commonIndex);
    TIntObjectHashMap<Entry> myNameIdToRightChildEntries = new TIntObjectHashMap<>(rightChildrenSize - commonIndex);

    for(int i = commonIndex; i < rightChildrenSize; ++i) {
      Entry rightChildEntry = e.myChildren.get(i);
      int rightChildEntryNameId = rightChildEntry.getNameId();
      Entry myChildEntry = uniqueNameIdToMyChildEntries.get(rightChildEntryNameId);

      if (myChildEntry != null && myChildEntry.isDirectory() == rightChildEntry.isDirectory()) {
        uniqueNameIdToMyChildEntries.remove(rightChildEntryNameId);
        myNameIdToRightChildEntries.put(rightChildEntryNameId, rightChildEntry);
      } else {
        uniqueNameIdToRightChildEntries.put(rightChildEntryNameId, rightChildEntry);
      }
    }

    if (!Paths.isCaseSensitive()  && uniqueNameIdToMyChildEntries.size() > 0 && uniqueNameIdToRightChildEntries.size() > 0) {
      Map<String, Entry> nameToEntryMap = Maps.newHashMap(uniqueNameIdToMyChildEntries.size(), CaseInsensitiveStringHashingStrategy.INSTANCE);

      uniqueNameIdToMyChildEntries.forEachValue(myChildEntry -> {
        nameToEntryMap.put(myChildEntry.getName(), myChildEntry);
        return true;
      });

      uniqueNameIdToRightChildEntries.forEachValue(rightChildEntry -> {
        Entry myChildEntry = nameToEntryMap.get(rightChildEntry.getName());
        if (myChildEntry != null && rightChildEntry.isDirectory() == myChildEntry.isDirectory()) {
          myNameIdToRightChildEntries.put(myChildEntry.getNameId(), rightChildEntry);
          uniqueNameIdToMyChildEntries.remove(myChildEntry.getNameId());
          uniqueNameIdToRightChildEntries.remove(rightChildEntry.getNameId());
        }
        return true;
      });
    }

    for (Entry child : e.myChildren) {
      if (uniqueNameIdToRightChildEntries.containsKey(child.getNameId())) {
        child.collectCreatedDifferences(result);
      }
    }

    for (Entry child : myChildren) {
      if (uniqueNameIdToMyChildEntries.containsKey(child.getNameId())) {
        child.collectDeletedDifferences(result);
      } else {
        Entry itsChild = myNameIdToRightChildEntries.get(child.getNameId());
        if (itsChild != null) child.collectDifferencesWith(itsChild, result);
      }
    }
  }

  Entry findDirectChild(String name, boolean isDirectory) {
    for (Entry child : getChildren()) {
      if (child.isDirectory() == isDirectory && child.nameEquals(name)) return child;
    }
    return null;
  }

  @Override
  protected void collectCreatedDifferences(List<Difference> result) {
    result.add(new Difference(false, null, this));

    for (Entry child : myChildren) {
      child.collectCreatedDifferences(result);
    }
  }

  @Override
  protected void collectDeletedDifferences(List<Difference> result) {
    result.add(new Difference(false, this, null));

    for (Entry child : myChildren) {
      child.collectDeletedDifferences(result);
    }
  }
}

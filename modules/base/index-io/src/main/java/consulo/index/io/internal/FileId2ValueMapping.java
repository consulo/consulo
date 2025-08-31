/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.index.io.internal;

import consulo.index.io.InvertedIndexValueIterator;
import consulo.index.io.ValueContainer;
import consulo.index.io.internal.DebugAssertions;
import consulo.index.io.internal.ValueContainerImpl;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.ints.IntObjectMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maxim.Mossienko on 7/4/2014.
 */
public class FileId2ValueMapping<Value> {
  private IntObjectMap<Value> id2ValueMap;
  private ValueContainerImpl<Value> valueContainer;
  private boolean myOnePerFileValidationEnabled = true;

  public FileId2ValueMapping(ValueContainerImpl<Value> _valueContainer) {
    id2ValueMap = IntMaps.newIntObjectHashMap();
    valueContainer = _valueContainer;

    IntList removedFileIdList = null;
    List<Value> removedValueList = null;

    for (ValueContainer.ValueIterator<Value> valueIterator = _valueContainer.getValueIterator(); valueIterator.hasNext(); ) {
      Value value = valueIterator.next();

      for (ValueContainer.IntIterator intIterator = valueIterator.getInputIdsIterator(); intIterator.hasNext(); ) {
        int id = intIterator.next();
        Value previousValue = id2ValueMap.put(id, value);
        if (previousValue != null) {  // delay removal of duplicated id -> value mapping since it will affect valueIterator we are using
          if (removedFileIdList == null) {
            removedFileIdList = IntLists.newArrayList();
            removedValueList = new ArrayList<Value>();
          }
          removedFileIdList.add(id);
          removedValueList.add(previousValue);
        }
      }
    }

    if (removedFileIdList != null) {
      for (int i = 0, size = removedFileIdList.size(); i < size; ++i) {
        valueContainer.removeValue(removedFileIdList.get(i), removedValueList.get(i));
      }
    }
  }

  public void associateFileIdToValue(int fileId, Value value) {
    Value previousValue = id2ValueMap.put(fileId, value);
    if (previousValue != null) {
      valueContainer.removeValue(fileId, previousValue);
    }
  }

  public boolean removeFileId(int inputId) {
    Value mapped = id2ValueMap.remove(inputId);
    if (mapped != null) {
      valueContainer.removeValue(inputId, mapped);
    }
    if (DebugAssertions.EXTRA_SANITY_CHECKS && myOnePerFileValidationEnabled) {
      for (InvertedIndexValueIterator<Value> valueIterator = valueContainer.getValueIterator(); valueIterator.hasNext(); ) {
        valueIterator.next();
        DebugAssertions.assertTrue(!valueIterator.getValueAssociationPredicate().contains(inputId));
      }
    }
    return mapped != null;
  }

  public void disableOneValuePerFileValidation() {
    myOnePerFileValidationEnabled = false;
  }
}

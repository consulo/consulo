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
package consulo.ide.impl.idea.openapi.editor.richcopy.model;

import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;
import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Denis Zhdanov
 * @since 3/23/13 3:17 PM
 */
public abstract class AbstractRegistry<T> implements Serializable {

  @Nonnull
  private final TIntObjectHashMap<T> myDataById = new TIntObjectHashMap<T>();

  private transient TObjectIntHashMap<T> myIdsByData = new TObjectIntHashMap<T>();

  @Nonnull
  public T dataById(int id) throws IllegalArgumentException {
    T result = myDataById.get(id);
    if (result == null) {
      throw new IllegalArgumentException("No data is registered for id " + id);
    }
    return result;
  }
  
  public int getId(@Nonnull T data) throws IllegalStateException {
    if (myIdsByData == null) {
      throw new IllegalStateException(String.format(
        "Can't register data '%s'. Reason: the %s registry is already sealed", data, getClass().getName()
      ));
    }
    int id = myIdsByData.get(data);
    if (id <= 0) {
      id = myIdsByData.size() + 1;
      myDataById.put(id, data);
      myIdsByData.put(data, id);
    }
    return id;
  }

  public int[] getAllIds() {
    int[] result = myDataById.keys();
    Arrays.sort(result);
    return result;
  }

  public int size() {
    return myDataById.size();
  }

  public void seal() {
    myIdsByData = null;
    myDataById.compact();
  }
}

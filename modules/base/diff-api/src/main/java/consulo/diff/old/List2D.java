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
package consulo.diff.old;

import java.util.ArrayList;
import java.util.List;

@Deprecated
class List2D {
  private final List<List> myRows = new ArrayList<>();
  private List myCurrentRow = null;

  public void add(DiffFragmentOld element) {
    ensureRowExists();
    myCurrentRow.add(element);
  }

  private void ensureRowExists() {
    if (myCurrentRow == null) {
      myCurrentRow = new ArrayList();
      myRows.add(myCurrentRow);
    }
  }

  public void newRow() {
    myCurrentRow = null;
  }

  //
  public DiffFragmentOld[][] toArray() {

    DiffFragmentOld[][] result = new DiffFragmentOld[myRows.size()][];
    for (int i = 0; i < result.length; i++) {
      List row = myRows.get(i);
      result[i] = new DiffFragmentOld[row.size()];
      System.arraycopy(row.toArray(), 0, result[i], 0, row.size());
    }
    return result;
  }

  public void addAll(DiffFragmentOld[] line) {
    ensureRowExists();
    for (int i = 0; i < line.length; i++) {
      DiffFragmentOld value = line[i];
      myCurrentRow.add(value);
    }
  }
}

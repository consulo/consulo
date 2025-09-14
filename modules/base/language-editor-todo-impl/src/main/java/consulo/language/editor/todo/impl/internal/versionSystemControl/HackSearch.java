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
package consulo.language.editor.todo.impl.internal.versionSystemControl;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * if T and S are compared by convertion to Z, we can find where some t should be placed in s list
 *
 * @author irengrig
 * @since 2011-02-21
 */
public class HackSearch<T,S,Z> {
  private final Function<T,Z> myTZConvertor;
  private final Function<S,Z> mySZConvertor;
  private final Comparator<Z> myZComparator;
  private S myFake;
  private Z myFakeConverted;
  private final Comparator<S> myComparator;

  public HackSearch(Function<T, Z> TZConvertor, Function<S, Z> SZConvertor, Comparator<Z> zComparator) {
    myTZConvertor = TZConvertor;
    mySZConvertor = SZConvertor;
    myZComparator = zComparator;
    myComparator = new Comparator<S>() {
    @Override
    public int compare(S o1, S o2) {
      Z z1 = mySZConvertor.apply(o1);
      Z z2 = mySZConvertor.apply(o2);
      if (o1 == myFake) {
        z1 = myFakeConverted;
      } else if (o2 == myFake) {
        z2 = myFakeConverted;
      }
      return myZComparator.compare(z1, z2);
    }
  };
  }

  public int search(List<S> list, T item) {
    if (list.isEmpty()) return 0;
    myFake = list.get(0);
    myFakeConverted = myTZConvertor.apply(item);
    if (myZComparator.compare(mySZConvertor.apply(myFake), myTZConvertor.apply(item)) >= 0) {
      return 0;
    }

    int idx = Collections.binarySearch(list.subList(1, list.size()), myFake, myComparator);
    if (idx >= 0) {
      return 1 + idx;
    } else {
      return - idx;
    }
  }
}

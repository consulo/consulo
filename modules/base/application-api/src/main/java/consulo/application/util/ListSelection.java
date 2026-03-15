// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Utility class used to preserve index during 'map' operations
 */
public final class ListSelection<T> {
  
  private final List<T> myList;
  private final int mySelectedIndex;

  private ListSelection(List<T> list, int selectedIndex) {
    myList = list;
    if (selectedIndex >= 0 && selectedIndex < list.size()) {
      mySelectedIndex = selectedIndex;
    }
    else {
      mySelectedIndex = 0;
    }
  }

  
  public static <V> ListSelection<V> createAt(List<V> list, int selectedIndex) {
    return new ListSelection<>(list, selectedIndex);
  }

  
  public static <V> ListSelection<V> create(List<V> list, @Nullable V selected) {
    return createAt(list, list.indexOf(selected));
  }

  
  public static <V> ListSelection<V> create(V[] array, V selected) {
    return create(Arrays.asList(array), selected);
  }

  
  public static <V> ListSelection<V> createSingleton(V element) {
    return createAt(Collections.singletonList(element), 0);
  }

  
  public static <V> ListSelection<V> empty() {
    return new ListSelection<>(Collections.emptyList(), -1);
  }


  
  public List<T> getList() {
    return myList;
  }

  public int getSelectedIndex() {
    return mySelectedIndex;
  }

  public boolean isEmpty() {
    return myList.isEmpty();
  }

  /**
   * Map all elements in the list and remove elements for which converter returned null.
   * If selected element was removed, select remaining element before it.
   */
  
  public <V> ListSelection<V> map(Function<? super T, ? extends V> convertor) {
    int newSelectionIndex = -1;
    List<V> result = new ArrayList<>();
    for (int i = 0; i < myList.size(); i++) {
      if (i == mySelectedIndex) newSelectionIndex = result.size();
      V out = convertor.apply(myList.get(i));
      if (out != null) result.add(out);
    }
    return new ListSelection<>(result, newSelectionIndex);
  }
}

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

package consulo.ide.impl.idea.ui.navigation;

import consulo.component.util.ComparableObject;
import consulo.component.util.ComparableObjectCheck;
import consulo.util.concurrent.AsyncResult;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class Place implements ComparableObject {
  private LinkedHashMap<String, Object> myPath = new LinkedHashMap<>();

  @Override
  @Nonnull
  public final Object[] getEqualityObjects() {
    return new Object[] {myPath};
  }

  @Override
  public final boolean equals(final Object obj) {
    return ComparableObjectCheck.equals(this, obj);
  }

  @Override
  public final int hashCode() {
    return ComparableObjectCheck.hashCode(this, super.hashCode());
  }

  @Nonnull
  public Place putPath(String name, Object value) {
    myPath.put(name, value);
    return this;
  }

  @Nullable
  public
  Object getPath(String name) {
    return myPath.get(name);
  }

  public Place cloneForElement(final String name, Object value) {
    final Place clone = new Place();
    clone.myPath = (LinkedHashMap<String, Object>)myPath.clone();
    clone.myPath.put(name, value);
    return clone;
  }

  public void copyFrom(final Place from) {
    myPath = (LinkedHashMap<String, Object>)from.myPath.clone();
  }

  public boolean isMoreGeneralFor(final Place place) {
    if (myPath.size() >= place.myPath.size()) return false;

    final Iterator<String> thisIterator = myPath.keySet().iterator();
    final Iterator<String> otherIterator = place.myPath.keySet().iterator();

    while (thisIterator.hasNext()) {
      String thisKey = thisIterator.next();
      String otherKey = otherIterator.next();
      if (thisKey == null || !thisKey.equals(otherKey)) return false;

      final Object thisValue = myPath.get(thisKey);
      final Object otherValue = place.myPath.get(otherKey);

      if (thisValue == null || !thisValue.equals(otherValue)) return false;

    }

    return true;
  }

  public interface Navigator {
    default void setHistory(History history) {
    }

    AsyncResult<Void> navigateTo(@Nullable Place place, final boolean requestFocus);

    void queryPlace(@Nonnull Place place);
  }

  public static AsyncResult<Void> goFurther(Object object, Place place, final boolean requestFocus) {
    if (object instanceof Navigator) {
      return ((Navigator)object).navigateTo(place, requestFocus);
    }
    return AsyncResult.resolved();
  }

  public static void queryFurther(final Object object, final Place place) {
    if (object instanceof Navigator) {
      ((Navigator)object).queryPlace(place);
    }
  }
}

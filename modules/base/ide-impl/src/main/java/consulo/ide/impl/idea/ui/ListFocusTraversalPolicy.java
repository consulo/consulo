// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui;

import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.util.collection.primitive.objects.ObjectMaps;

import org.jspecify.annotations.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Policy which defines explicit focus component cycle.
 */
public class ListFocusTraversalPolicy extends LayoutFocusTraversalPolicy {

  private final Component[] myComponents;
  private final ObjectIntMap<Component> myComponentToIndex;

  public ListFocusTraversalPolicy(List<? extends Component> components) {
    myComponents = components.toArray(Component[]::new);
    myComponentToIndex = indexMap(myComponents);
  }

  @Override
  protected boolean accept(Component aComponent) {
    return super.accept(aComponent) && aComponent.isShowing();
  }

  @Override
  public Component getFirstComponent(Container aContainer) {
    return getNextComponent(0);
  }

  @Override
  public Component getLastComponent(Container aContainer) {
    return getPreviousComponent(myComponents.length - 1);
  }

  @Override
  public Component getComponentAfter(Container aContainer, Component aComponent) {
    if (!myComponentToIndex.containsKey(aComponent)) {
      return null;
    }
    return getNextComponent(myComponentToIndex.getInt(aComponent) + 1);
  }

  @Override
  public Component getComponentBefore(Container aContainer, Component aComponent) {
    if (!myComponentToIndex.containsKey(aComponent)) {
      return null;
    }
    return getPreviousComponent(myComponentToIndex.getInt(aComponent) - 1);
  }

  private @Nullable Component getNextComponent(int startIndex) {
    for (int index = startIndex; index < myComponents.length; index++) {
      Component result = myComponents[index];
      if (accept(result)) {
        return result;
      }
    }
    for (int index = 0; index < startIndex; index++) {
      Component result = myComponents[index];
      if (accept(result)) {
        return result;
      }
    }
    return null;
  }

  private @Nullable Component getPreviousComponent(int startIndex) {
    for (int index = startIndex; index >= 0; index--) {
      Component result = myComponents[index];
      if (accept(result)) {
        return result;
      }
    }
    for (int index = myComponents.length - 1; index > startIndex; index--) {
      Component result = myComponents[index];
      if (accept(result)) {
        return result;
      }
    }
    return null;
  }

  
  private static <X> ObjectIntMap<X> indexMap(X[] array) {
    ObjectIntMap<X> map = ObjectMaps.newObjectIntHashMap(array.length);
    for (X x : array) {
      if (!map.containsKey(x)) {
        map.putInt(x, map.size());
      }
    }

    ObjectMaps.trimToSize(map);
    return map;
  }
}

/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ui.ex.awt.internal.laf;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author VISTALL
 * @since 10-Jun-22
 */
class UIElementWeakStorage<E> {
  private final List<Reference<E>> myList = new ArrayList<>();

  /**
   * Processes references in the static list of references synchronously.
   * This method removes all cleared references and the reference specified to remove,
   * collects objects from other references into the specified list and
   * adds the reference specified to add.
   *
   * @param toAdd    the object to add to the static list of references (ignored if {@code null})
   * @param toRemove the object to remove from the static list of references (ignored if {@code null})
   * @param list     the list to collect all available objects (ignored if {@code null})
   */
  void processReferences(E toAdd, E toRemove, List<? super E> list) {
    synchronized (myList) {
      Iterator<Reference<E>> iterator = myList.iterator();
      while (iterator.hasNext()) {
        Reference<E> reference = iterator.next();
        E ui = reference.get();
        if (ui == null || ui == toRemove) {
          iterator.remove();
        }
        else if (list != null) {
          list.add(ui);
        }
      }
      if (toAdd != null) {
        myList.add(new WeakReference<>(toAdd));
      }
    }
  }
}

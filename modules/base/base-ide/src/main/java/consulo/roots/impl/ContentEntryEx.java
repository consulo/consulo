/*
 * Copyright 2013-2020 consulo.io
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
package consulo.roots.impl;

import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.roots.impl.ClonableContentEntry;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author VISTALL
 * @since 2020-08-31
 */
public interface ContentEntryEx extends ContentEntry, Comparable<ContentEntryEx>, ClonableContentEntry {
  @Nonnull
  Collection<ContentFolder> getContentFolders();

  @Override
  ContentEntryEx cloneEntry(ModuleRootLayerImpl layer);

  @Override
  default int compareTo(@Nonnull ContentEntryEx other) {
    int i = getUrl().compareTo(other.getUrl());
    if (i != 0) return i;
    return lexicographicCompare(getContentFolders(), other.getContentFolders());
  }

  public static <T> int lexicographicCompare(@Nonnull Collection<T> obj1, @Nonnull Collection<T> obj2) {
    Iterator<T> it1 = obj1.iterator();
    Iterator<T> it2 = obj2.iterator();

    for (int i = 0; i < Math.max(obj1.size(), obj2.size()); i++) {
      T o1 = it1.hasNext() ? it1.next() : null;
      T o2 = it2.hasNext() ? it2.next() : null;
      if (o1 == null) {
        return -1;
      }
      if (o2 == null) {
        return 1;
      }
      int res = ((Comparable)o1).compareTo(o2);
      if (res != 0) {
        return res;
      }
    }
    return 0;
  }
}

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

package com.intellij.openapi.roots.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentFolderType;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.RootPolicy;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayFactory;
import com.intellij.util.ArrayUtil;
import com.intellij.util.BitUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class DirectoryInfo {
  public static final byte LIBRARY_SOURCE_FLAG     = 1 << 6; // set if it's a directory with sources of some library

  private final Module myModule; // module to which content it belongs or null
  private final VirtualFile myLibraryClassRoot; // class root in library
  private final VirtualFile myContentRoot;
  private final VirtualFile mySourceRoot;

  private final byte myFlags;

  /**
   * orderEntry to (classes of) which a directory belongs
   * MUST BE SORTED WITH {@link #BY_OWNER_MODULE}
   */
  private final OrderEntry[] myOrderEntries;

  public static DirectoryInfo createNew() {
    return new DirectoryInfo(null, null, null, null, (byte)0, null);
  }

  private DirectoryInfo(Module module,
                        VirtualFile contentRoot,
                        VirtualFile sourceRoot,
                        VirtualFile libraryClassRoot,
                        byte myFlags,
                        OrderEntry[] orderEntries) {
    this.myModule = module;
    this.myLibraryClassRoot = libraryClassRoot;
    this.myContentRoot = contentRoot;
    this.mySourceRoot = sourceRoot;
    this.myFlags = myFlags;
    this.myOrderEntries = orderEntries;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DirectoryInfo info = (DirectoryInfo)o;

    return myFlags == info.myFlags &&
           Comparing.equal(myContentRoot, info.myContentRoot) &&
           Comparing.equal(myLibraryClassRoot, info.myLibraryClassRoot) &&
           Comparing.equal(myModule, info.myModule) &&
           Arrays.equals(myOrderEntries, info.myOrderEntries) &&
           Comparing.equal(mySourceRoot, info.mySourceRoot);
  }

  @Override
  public int hashCode() {
    int result = myModule != null ? myModule.hashCode() : 0;
    result = 31 * result + (myLibraryClassRoot != null ? myLibraryClassRoot.hashCode() : 0);
    result = 31 * result + (myContentRoot != null ? myContentRoot.hashCode() : 0);
    result = 31 * result + (mySourceRoot != null ? mySourceRoot.hashCode() : 0);
    result = 31 * result + (int)myFlags;
    return result;
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public String toString() {
    return "DirectoryInfo{" +
           "module=" + getModule() +
           ", sourceFlag=" + myFlags +
           ", libraryClassRoot=" + getLibraryClassRoot() +
           ", contentRoot=" + getContentRoot() +
           ", sourceRoot=" + getSourceRoot() +
           "}";
  }

  @NotNull
  public OrderEntry[] getOrderEntries() {
    OrderEntry[] entries = myOrderEntries;
    return entries == null ? OrderEntry.EMPTY_ARRAY : entries;
  }

  @Nullable
  OrderEntry findOrderEntryWithOwnerModule(@NotNull Module ownerModule) {
    OrderEntry[] entries = myOrderEntries;
    if (entries == null) {
      return null;
    }
    if (entries.length < 10) {
      for (OrderEntry entry : entries) {
        if (entry.getOwnerModule() == ownerModule) return entry;
      }
      return null;
    }
    int index = Arrays.binarySearch(entries, createFakeOrderEntry(ownerModule), BY_OWNER_MODULE);
    return index < 0 ? null : entries[index];
  }

  @NotNull
  List<OrderEntry> findAllOrderEntriesWithOwnerModule(@NotNull Module ownerModule) {
    OrderEntry[] entries = myOrderEntries;
    if (entries == null) {
      return Collections.emptyList();
    }
    if (entries.length == 1) {
      OrderEntry entry = entries[0];
      return entry.getOwnerModule() == ownerModule ? Arrays.asList(entries) : Collections.<OrderEntry>emptyList();
    }
    int index = Arrays.binarySearch(entries, createFakeOrderEntry(ownerModule), BY_OWNER_MODULE);
    if (index < 0) {
      return Collections.emptyList();
    }
    int firstIndex = index;
    while (firstIndex-1 >= 0 && entries[firstIndex-1].getOwnerModule() == ownerModule) {
      firstIndex--;
    }
    int lastIndex = index+1;
    while (lastIndex < entries.length && entries[lastIndex].getOwnerModule() == ownerModule) {
      lastIndex++;
    }

    OrderEntry[] subArray = new OrderEntry[lastIndex - firstIndex];
    System.arraycopy(entries, firstIndex, subArray, 0, lastIndex - firstIndex);

    return Arrays.asList(subArray);
  }

  @NotNull
  private static OrderEntry createFakeOrderEntry(@NotNull final Module ownerModule) {
    return new OrderEntry() {
      @NotNull
      @Override
      public VirtualFile[] getFiles(OrderRootType type) {
        throw new IncorrectOperationException();
      }

      @NotNull
      @Override
      public String[] getUrls(OrderRootType rootType) {
        throw new IncorrectOperationException();
      }

      @NotNull
      @Override
      public String getPresentableName() {
        throw new IncorrectOperationException();
      }

      @Override
      public boolean isValid() {
        throw new IncorrectOperationException();
      }

      @NotNull
      @Override
      public Module getOwnerModule() {
        return ownerModule;
      }

      @Override
      public <R> R accept(RootPolicy<R> policy, @Nullable R initialValue) {
        throw new IncorrectOperationException();
      }

      @Override
      public int compareTo(OrderEntry o) {
        throw new IncorrectOperationException();
      }

      @Override
      public boolean isSynthetic() {
        throw new IncorrectOperationException();
      }
    };
  }

  // orderEntries must be sorted BY_OWNER_MODULE
  OrderEntry[] calcNewOrderEntries(@NotNull OrderEntry[] orderEntries, @Nullable DirectoryInfo parentInfo, @Nullable OrderEntry[] oldParentEntries) {
    OrderEntry[] newOrderEntries;
    if (orderEntries.length == 0) {
      newOrderEntries = null;
    }
    else if (this.myOrderEntries == null) {
      newOrderEntries = orderEntries;
    }
    else if (parentInfo != null && oldParentEntries == this.myOrderEntries) {
      newOrderEntries = parentInfo.myOrderEntries;
    }
    else {
      newOrderEntries = mergeWith(orderEntries);
    }
    return newOrderEntries;
  }

  // entries must be sorted BY_OWNER_MODULE
  @NotNull
  private OrderEntry[] mergeWith(@NotNull OrderEntry[] entries) {
    OrderEntry[] orderEntries = this.myOrderEntries;
    OrderEntry[] result = new OrderEntry[orderEntries.length + entries.length];
    int i=0;
    int j=0;
    // remove equals entries in the process
    int o = 0;
    while (i != orderEntries.length || j != entries.length) {
      OrderEntry m = i != orderEntries.length && (j == entries.length || BY_OWNER_MODULE.compare(orderEntries[i], entries[j]) < 0)
                     ? orderEntries[i++]
                     : entries[j++];
      if (o==0 || !m.equals(result[o - 1])) {
        result[o++] = m;
      }
    }
    if (o != result.length) {
      result = ArrayUtil.realloc(result, o, ORDER_ENTRY_ARRAY_FACTORY);
    }
    return result;
  }

  private static final ArrayFactory<OrderEntry> ORDER_ENTRY_ARRAY_FACTORY = new ArrayFactory<OrderEntry>() {
    @NotNull
    @Override
    public OrderEntry[] create(int count) {
      return count == 0 ? OrderEntry.EMPTY_ARRAY : new OrderEntry[count];
    }
  };

  public static final Comparator<OrderEntry> BY_OWNER_MODULE = new Comparator<OrderEntry>() {
    @Override
    public int compare(OrderEntry o1, OrderEntry o2) {
      String name1 = o1.getOwnerModule().getName();
      String name2 = o2.getOwnerModule().getName();
      return name1.compareTo(name2);
    }
  };

  public VirtualFile getSourceRoot() {
    return mySourceRoot;
  }

  public boolean hasSourceRoot() {
    return getSourceRoot() != null;
  }

  public VirtualFile getLibraryClassRoot() {
    return myLibraryClassRoot;
  }

  public boolean hasLibraryClassRoot() {
    return getLibraryClassRoot() != null;
  }

  public VirtualFile getContentRoot() {
    return myContentRoot;
  }

  public boolean hasContentFolderFlag(ContentFolderType folderType) {
    return BitUtil.isSet(myFlags, 1 << folderType.ordinal());
  }

  @Nullable
  public ContentFolderType findContentFolderType() {
    for (ContentFolderType contentFolderType : ContentFolderType.ALL_SOURCE_ROOTS) {
      if(hasContentFolderFlag(contentFolderType)) {
        return contentFolderType;
      }
    }
    return null;
  }

  public boolean isInLibrarySource() {
    return BitUtil.isSet(myFlags, LIBRARY_SOURCE_FLAG);
  }

  public Module getModule() {
    return myModule;
  }

  private static <T> T iff(T value, T defaultValue) {
    return value == null ? defaultValue : value;
  }

  @NotNull
  public DirectoryInfo with(Module module,
                            VirtualFile contentRoot,
                            VirtualFile sourceRoot,
                            VirtualFile libraryClassRoot,
                            byte sourceFlag,
                            OrderEntry[] orderEntries) {
    return new DirectoryInfo(iff(module, this.myModule), iff(contentRoot, this.myContentRoot), iff(sourceRoot, this.mySourceRoot),
                             iff(libraryClassRoot, this.myLibraryClassRoot), sourceFlag == 0 ? this.myFlags : sourceFlag,
                             iff(orderEntries, this.myOrderEntries));
  }

  @NotNull
  public DirectoryInfo withInternedEntries(@NotNull OrderEntry[] orderEntries) {
    return new DirectoryInfo(myModule, myContentRoot, mySourceRoot, myLibraryClassRoot, myFlags, orderEntries);
  }

  @TestOnly
  void assertConsistency() {
    OrderEntry[] entries = getOrderEntries();
    for (int i=1; i<entries.length; i++) {
      assert BY_OWNER_MODULE.compare(entries[i-1], entries[i]) <= 0;
    }
  }

  public byte getFlags() {
    return myFlags;
  }
}

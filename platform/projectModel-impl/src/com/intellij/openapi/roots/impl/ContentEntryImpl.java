/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.roots.ContentFolderType;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.util.ArrayUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.serialization.module.JpsModuleRootModelSerializer;

import java.util.*;

/**
 * @author dsl
 */
public class ContentEntryImpl extends RootModelComponentBase implements ContentEntry, ClonableContentEntry, Comparable<ContentEntryImpl> {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.roots.impl.SimpleContentEntryImpl");
  @NotNull
  private final VirtualFilePointer myRoot;
  @NonNls
  public static final String ELEMENT_NAME = JpsModuleRootModelSerializer.CONTENT_TAG;

  private final Set<ContentFolder> myContentFolders = new LinkedHashSet<ContentFolder>();

  @NonNls
  public static final String URL_ATTRIBUTE = JpsModuleRootModelSerializer.URL_ATTRIBUTE;

  ContentEntryImpl(@NotNull VirtualFile file, @NotNull RootModelImpl m) {
    this(file.getUrl(), m);
  }

  ContentEntryImpl(@NotNull String url, @NotNull RootModelImpl m) {
    super(m);
    myRoot = VirtualFilePointerManager.getInstance().create(url, this, null);
  }

  ContentEntryImpl(@NotNull Element e, @NotNull RootModelImpl m) throws InvalidDataException {
    this(getUrlFrom(e), m);

    for (Element child : e.getChildren("content-folder")) {
      myContentFolders.add(new ContentFolderImpl(child, this));
    }
  }

  private static String getUrlFrom(@NotNull Element e) throws InvalidDataException {
    LOG.assertTrue(ELEMENT_NAME.equals(e.getName()));

    String url = e.getAttributeValue(URL_ATTRIBUTE);
    if (url == null) throw new InvalidDataException();
    return url;
  }

  @Override
  public VirtualFile getFile() {
    //assert !isDisposed();
    final VirtualFile file = myRoot.getFile();
    return file == null || !file.isDirectory() ? null : file;
  }

  @Override
  @NotNull
  public String getUrl() {
    return myRoot.getUrl();
  }

  @NotNull
  @Override
  public ContentFolder[] getFolders(@NotNull ContentFolderType contentFolderType) {
    List<ContentFolder> list = new ArrayList<ContentFolder>();
    for (ContentFolder contentFolder : myContentFolders) {
      if (contentFolder.getType() == contentFolderType) {
        list.add(contentFolder);
      }
    }
    return list.isEmpty() ? ContentFolder.EMPTY_ARRAY : list.toArray(new ContentFolder[list.size()]);
  }

  @NotNull
  @Override
  public VirtualFile[] getFolderFiles(@NotNull ContentFolderType contentFolderType) {
    List<VirtualFile> list = new ArrayList<VirtualFile>();
    for (ContentFolder contentFolder : myContentFolders) {
      if (contentFolder.getType() == contentFolderType) {
        list.add(contentFolder.getFile());
      }
    }
    return VfsUtilCore.toVirtualFileArray(list);
  }

  @NotNull
  @Override
  public String[] getFolderUrls(@NotNull ContentFolderType contentFolderType) {
    List<String> list = new ArrayList<String>();
    for (ContentFolder contentFolder : myContentFolders) {
      if (contentFolder.getType() == contentFolderType) {
        list.add(contentFolder.getUrl());
      }
    }
    return ArrayUtil.toStringArray(list);
  }

  @Override
  public ContentFolder[] getFolders() {
    return myContentFolders.isEmpty() ? ContentFolder.EMPTY_ARRAY : myContentFolders.toArray(new ContentFolder[myContentFolders.size()]);
  }

  @NotNull
  @Override
  public ContentFolder addFolder(@NotNull VirtualFile file, @NotNull ContentFolderType contentFolderType) {
    assertCanAddFolder(file);
    return addFolder(new ContentFolderImpl(file, contentFolderType, this));
  }

  @NotNull
  @Override
  public ContentFolder addFolder(@NotNull String url, @NotNull ContentFolderType contentFolderType) {
    assertFolderUnderMe(url);
    return addFolder(new ContentFolderImpl(url, contentFolderType, this));
  }

  private ContentFolder addFolder(ContentFolderImpl f) {
    myContentFolders.add(f);
    Disposer.register(this, f); //rewire source folder dispose parent from rootmodel to this content root
    return f;
  }

  @Override
  public void clearFolders(@NotNull ContentFolderType contentFolderType) {
    assert !isDisposed();
    getRootModel().assertWritable();

    Iterator<ContentFolder> iterator = myContentFolders.iterator();
    while (iterator.hasNext()) {
      final ContentFolder next = iterator.next();
      if (next.getType() == contentFolderType) {
        iterator.remove();

        Disposer.dispose((Disposable)next);
      }
    }
  }

  @Override
  public void removeFolder(@NotNull ContentFolder contentFolder) {
    assert !isDisposed();
    assertCanRemoveFrom(contentFolder, myContentFolders);
    myContentFolders.remove(contentFolder);
    Disposer.dispose((Disposable)contentFolder);
  }

  private void assertCanAddFolder(@NotNull VirtualFile file) {
    assertCanAddFolder(file.getUrl());
  }

  private void assertCanAddFolder(@NotNull String url) {
    getRootModel().assertWritable();
    assertFolderUnderMe(url);
  }

  private <T extends ContentFolder> void assertCanRemoveFrom(T f, @NotNull Set<T> ff) {
    getRootModel().assertWritable();
    LOG.assertTrue(ff.contains(f));
  }

  private void assertFolderUnderMe(@NotNull String url) {
    final String path = VfsUtilCore.urlToPath(url);
    final String rootPath = VfsUtilCore.urlToPath(getUrl());
    if (!FileUtil.isAncestor(rootPath, path, false)) {
      LOG.error("The file '" + path + "' is not under content entry root '" + rootPath + "'");
    }
  }

  @Override
  public boolean isSynthetic() {
    return false;
  }

  @Override
  @NotNull
  public ContentEntry cloneEntry(@NotNull RootModelImpl rootModel) {
    assert !isDisposed();

    ContentEntryImpl cloned = new ContentEntryImpl(myRoot.getUrl(), rootModel);
    for (ContentFolder contentFolder : myContentFolders) {
      if (contentFolder instanceof ClonableContentFolder) {
        ContentFolderImpl folder = (ContentFolderImpl)((ClonableContentFolder)contentFolder).cloneFolder(cloned);
        cloned.addFolder(folder);
      }
    }

    return cloned;
  }

  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    assert !isDisposed();
    LOG.assertTrue(ELEMENT_NAME.equals(element.getName()));
    element.setAttribute(URL_ATTRIBUTE, myRoot.getUrl());
    for (ContentFolder contentFolder : myContentFolders) {
      final Element subElement = new Element("content-folder");
      ((ContentFolderImpl)contentFolder).writeExternal(subElement);
      element.addContent(subElement);
    }
  }

  private static final class ContentFolderComparator implements Comparator<ContentFolder> {
    public static final ContentFolderComparator INSTANCE = new ContentFolderComparator();

    @Override
    public int compare(@NotNull ContentFolder o1, @NotNull ContentFolder o2) {
      return o1.getUrl().compareTo(o2.getUrl());
    }
  }

  @Override
  public int compareTo(@NotNull ContentEntryImpl other) {
    int i = getUrl().compareTo(other.getUrl());
    if (i != 0) return i;
    return lexicographicCompare(myContentFolders, other.myContentFolders);
  }

  public static <T> int lexicographicCompare(@NotNull Set<T> obj1, @NotNull Set<T> obj2) {
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

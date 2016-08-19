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

import com.google.common.base.Predicate;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.lombok.annotations.Logger;
import consulo.roots.impl.LightContentFolderImpl;
import consulo.roots.impl.ModuleRootLayerImpl;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import consulo.roots.ContentFolderTypeProvider;
import consulo.roots.impl.ExcludedContentFolderTypeProvider;

import java.util.*;

/**
 * @author dsl
 */
@Logger
public class ContentEntryImpl extends BaseModuleRootLayerChild implements ContentEntry, ClonableContentEntry, Comparable<ContentEntryImpl> {
  @NonNls
  public static final String ELEMENT_NAME = "content";
  @NonNls
  public static final String URL_ATTRIBUTE = "url";

  @NotNull
  private final VirtualFilePointer myRoot;

  private final Set<ContentFolder> myContentFolders = new TreeSet<ContentFolder>(ContentFolderComparator.INSTANCE);

  public ContentEntryImpl(@NotNull VirtualFile file, @NotNull ModuleRootLayerImpl m) {
    this(file.getUrl(), m);
  }

  public ContentEntryImpl(@NotNull String url, @NotNull ModuleRootLayerImpl m) {
    super(m);
    myRoot = VirtualFilePointerManager.getInstance().create(url, this, null);
  }

  public ContentEntryImpl(@NotNull Element e, @NotNull ModuleRootLayerImpl m) throws InvalidDataException {
    this(getUrlFrom(e), m);

    for (Element child : e.getChildren(ContentFolderImpl.ELEMENT_NAME)) {
      myContentFolders.add(new ContentFolderImpl(child, this));
    }
  }

  private static String getUrlFrom(@NotNull Element e) throws InvalidDataException {
    LOGGER.assertTrue(ELEMENT_NAME.equals(e.getName()));

    String url = e.getAttributeValue(URL_ATTRIBUTE);
    if (url == null) throw new InvalidDataException();
    return url;
  }

  @Override
  public VirtualFile getFile() {
    return myRoot.getFile();
  }

  @Override
  @NotNull
  public String getUrl() {
    return myRoot.getUrl();
  }

  @NotNull
  @Override
  public ContentFolder[] getFolders(@NotNull Predicate<ContentFolderTypeProvider> predicate) {
    List<ContentFolder> list = new ArrayList<ContentFolder>();
    for (ContentFolder contentFolder : getFolders0(predicate)) {
      list.add(contentFolder);
    }
    return list.isEmpty() ? ContentFolder.EMPTY_ARRAY : list.toArray(new ContentFolder[list.size()]);
  }


  @NotNull
  @Override
  public VirtualFile[] getFolderFiles(@NotNull Predicate<ContentFolderTypeProvider> predicate) {
    List<VirtualFile> list = new ArrayList<VirtualFile>();
    for (ContentFolder contentFolder : getFolders0(predicate)) {
      ContainerUtil.addIfNotNull(contentFolder.getFile(), list);
    }
    return VfsUtilCore.toVirtualFileArray(list);
  }

  @NotNull
  @Override
  public String[] getFolderUrls(@NotNull Predicate<ContentFolderTypeProvider> predicate) {
    List<String> list = new ArrayList<String>();
    for (ContentFolder contentFolder : getFolders0(predicate)) {
      list.add(contentFolder.getUrl());
    }
    return ArrayUtil.toStringArray(list);
  }

  private List<ContentFolder> getFolders0(Predicate<ContentFolderTypeProvider> predicate) {
    List<ContentFolder> list = new ArrayList<ContentFolder>(myContentFolders.size());
    for (ContentFolder contentFolder : myContentFolders) {
      if (predicate.apply(contentFolder.getType())) {
        list.add(contentFolder);
      }
    }

    Module module = getModuleRootLayer().getModule();
    if(module.getModuleDirUrl() == null) {
      return list;
    }

    if (predicate.apply(ExcludedContentFolderTypeProvider.getInstance())) {
      for (DirectoryIndexExcludePolicy excludePolicy : DirectoryIndexExcludePolicy.EP_NAME.getExtensions(getRootModel().getProject())) {
        final VirtualFilePointer[] files = excludePolicy.getExcludeRootsForModule(myModuleRootLayer);
        for (VirtualFilePointer file : files) {
          list.add(new LightContentFolderImpl(file, ExcludedContentFolderTypeProvider.getInstance(), this));
        }
      }
    }
    return list;
  }

  @NotNull
  @Override
  public ContentFolder addFolder(@NotNull VirtualFile file, @NotNull ContentFolderTypeProvider contentFolderType) {
    assertCanAddFolder(file);
    return addFolder(new ContentFolderImpl(file, contentFolderType, this));
  }

  @NotNull
  @Override
  public ContentFolder addFolder(@NotNull String url, @NotNull ContentFolderTypeProvider contentFolderType) {
    assertFolderUnderMe(url);
    return addFolder(new ContentFolderImpl(url, contentFolderType, null, this));
  }

  private ContentFolder addFolder(ContentFolderImpl f) {
    myContentFolders.add(f);
    Disposer.register(this, f); //rewire source folder dispose parent from rootmodel to this content root
    return f;
  }

  @Override
  public void removeFolder(@NotNull ContentFolder contentFolder) {
    assert !isDisposed();
    assertCanRemoveFrom(contentFolder, myContentFolders);
    myContentFolders.remove(contentFolder);
    if(contentFolder instanceof Disposable) {
      Disposer.dispose((Disposable)contentFolder);
    }
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
    LOGGER.assertTrue(ff.contains(f));
  }

  private void assertFolderUnderMe(@NotNull String url) {
    final String path = VfsUtilCore.urlToPath(url);
    final String rootPath = VfsUtilCore.urlToPath(getUrl());
    if (!FileUtil.isAncestor(rootPath, path, false)) {
      LOGGER.error("The file '" + path + "' is not under content entry root '" + rootPath + "'");
    }
  }

  @Override
  public boolean isSynthetic() {
    return false;
  }

  @Override
  @NotNull
  public ContentEntry cloneEntry(@NotNull ModuleRootLayerImpl rootModel) {
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

  public void writeExternal(@NotNull Element element) {
    assert !isDisposed();
    LOGGER.assertTrue(ELEMENT_NAME.equals(element.getName()));
    element.setAttribute(URL_ATTRIBUTE, myRoot.getUrl());
    for (ContentFolder contentFolder : myContentFolders) {
      final Element subElement = new Element(ContentFolderImpl.ELEMENT_NAME);
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

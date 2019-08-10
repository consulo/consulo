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
package com.intellij.openapi.vfs.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import consulo.logging.Logger;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.TraceableDisposable;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerContainer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerListener;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author dsl
 */
public class VirtualFilePointerContainerImpl implements VirtualFilePointerContainer, Disposable {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.vfs.pointers.VirtualFilePointerContainer");
  @Nonnull
  private final List<VirtualFilePointer> myList = ContainerUtil.createLockFreeCopyOnWriteList();
  @Nonnull
  private final VirtualFilePointerManager myVirtualFilePointerManager;
  @Nonnull
  private final Disposable myParent;
  private final VirtualFilePointerListener myListener;
  private volatile Trinity<String[], VirtualFile[], VirtualFile[]> myCachedThings;
  private volatile long myTimeStampOfCachedThings = -1;
  @NonNls
  public static final String URL_ATTR = "url";
  private boolean myDisposed;
  private static final boolean TRACE_CREATION = LOG.isDebugEnabled() || ApplicationManager.getApplication().isUnitTestMode();

  private final TraceableDisposable myTraceableDisposable;

  public VirtualFilePointerContainerImpl(@Nonnull VirtualFilePointerManager manager, @Nonnull Disposable parentDisposable, @Nullable VirtualFilePointerListener listener) {
    //noinspection HardCodedStringLiteral
    myTraceableDisposable = Disposer.newTraceDisposable(TRACE_CREATION && !ApplicationInfoImpl.isInPerformanceTest());

    myVirtualFilePointerManager = manager;
    myParent = parentDisposable;
    myListener = listener;
  }

  public TraceableDisposable getTraceableDisposable() {
    return myTraceableDisposable;
  }

  @Override
  public void readExternal(@Nonnull final Element rootChild, @Nonnull final String childElements) throws InvalidDataException {
    final List<Element> urls = rootChild.getChildren(childElements);
    for (Element url : urls) {
      final String urlAttribute = url.getAttributeValue(URL_ATTR);
      if (urlAttribute == null) throw new InvalidDataException("path element without url");
      add(urlAttribute);
    }
  }

  @Override
  public void writeExternal(@Nonnull final Element element, @Nonnull final String childElementName) {
    for (VirtualFilePointer pointer : myList) {
      String url = pointer.getUrl();
      final Element rootPathElement = new Element(childElementName);
      rootPathElement.setAttribute(URL_ATTR, url);
      element.addContent(rootPathElement);
    }
  }

  @Override
  public void moveUp(@Nonnull String url) {
    int index = indexOf(url);
    if (index <= 0) return;
    dropCaches();
    ContainerUtil.swapElements(myList, index - 1, index);
  }

  @Override
  public void moveDown(@Nonnull String url) {
    int index = indexOf(url);
    if (index < 0 || index + 1 >= myList.size()) return;
    dropCaches();
    ContainerUtil.swapElements(myList, index, index + 1);
  }

  private int indexOf(@Nonnull final String url) {
    for (int i = 0; i < myList.size(); i++) {
      final VirtualFilePointer pointer = myList.get(i);
      if (url.equals(pointer.getUrl())) {
        return i;
      }
    }

    return -1;
  }

  @Override
  public void killAll() {
    myList.clear();
  }

  @Override
  public void add(@Nonnull VirtualFile file) {
    assert !myDisposed;
    dropCaches();
    final VirtualFilePointer pointer = create(file);
    myList.add(pointer);
  }

  @Override
  public void add(@Nonnull String url) {
    assert !myDisposed;
    dropCaches();
    final VirtualFilePointer pointer = create(url);
    myList.add(pointer);
  }

  @Override
  public void remove(@Nonnull VirtualFilePointer pointer) {
    assert !myDisposed;
    dropCaches();
    final boolean result = myList.remove(pointer);
    LOG.assertTrue(result);
  }

  @Override
  @Nonnull
  public List<VirtualFilePointer> getList() {
    assert !myDisposed;
    return Collections.unmodifiableList(myList);
  }

  @Override
  public void addAll(@Nonnull VirtualFilePointerContainer that) {
    assert !myDisposed;
    dropCaches();

    for (final VirtualFilePointer pointer : that.getList()) {
      myList.add(duplicate(pointer));
    }
  }

  private void dropCaches() {
    myTimeStampOfCachedThings = -1; // make it never equal to myVirtualFilePointerManager.getModificationCount()
    myCachedThings = EMPTY;
  }

  @Override
  @Nonnull
  public String[] getUrls() {
    return getOrCache().first;
  }

  @Nonnull
  private Trinity<String[], VirtualFile[], VirtualFile[]> getOrCache() {
    assert !myDisposed;
    long timeStamp = myTimeStampOfCachedThings;
    Trinity<String[], VirtualFile[], VirtualFile[]> cached = myCachedThings;
    return timeStamp == myVirtualFilePointerManager.getModificationCount() ? cached : cacheThings();
  }

  private static final Trinity<String[], VirtualFile[], VirtualFile[]> EMPTY = Trinity.create(ArrayUtil.EMPTY_STRING_ARRAY, VirtualFile.EMPTY_ARRAY, VirtualFile.EMPTY_ARRAY);

  @Nonnull
  private Trinity<String[], VirtualFile[], VirtualFile[]> cacheThings() {
    Trinity<String[], VirtualFile[], VirtualFile[]> result;
    if (myList.isEmpty()) {
      result = EMPTY;
    }
    else {
      List<VirtualFile> cachedFiles = new ArrayList<VirtualFile>(myList.size());
      List<String> cachedUrls = new ArrayList<String>(myList.size());
      List<VirtualFile> cachedDirectories = new ArrayList<VirtualFile>(myList.size() / 3);
      boolean allFilesAreDirs = true;
      for (VirtualFilePointer v : myList) {
        VirtualFile file = v.getFile();
        String url = v.getUrl();
        cachedUrls.add(url);
        if (file != null) {
          cachedFiles.add(file);
          if (file.isDirectory()) {
            cachedDirectories.add(file);
          }
          else {
            allFilesAreDirs = false;
          }
        }
      }
      VirtualFile[] directories = VfsUtilCore.toVirtualFileArray(cachedDirectories);
      VirtualFile[] files = allFilesAreDirs ? directories : VfsUtilCore.toVirtualFileArray(cachedFiles);
      String[] urlsArray = ArrayUtil.toStringArray(cachedUrls);
      result = Trinity.create(urlsArray, files, directories);
    }
    myCachedThings = result;
    myTimeStampOfCachedThings = myVirtualFilePointerManager.getModificationCount();
    return result;
  }

  @Override
  @Nonnull
  public VirtualFile[] getFiles() {
    return getOrCache().second;
  }

  @Override
  @Nonnull
  public VirtualFile[] getDirectories() {
    return getOrCache().third;
  }

  @Override
  @Nullable
  public VirtualFilePointer findByUrl(@Nonnull String url) {
    assert !myDisposed;
    for (VirtualFilePointer pointer : myList) {
      if (url.equals(pointer.getUrl())) return pointer;
    }
    return null;
  }

  @Override
  public void clear() {
    dropCaches();
    killAll();
  }

  @Override
  public int size() {
    return myList.size();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof VirtualFilePointerContainerImpl)) return false;

    final VirtualFilePointerContainerImpl virtualFilePointerContainer = (VirtualFilePointerContainerImpl)o;

    return myList.equals(virtualFilePointerContainer.myList);
  }

  public int hashCode() {
    return myList.hashCode();
  }

  protected VirtualFilePointer create(@Nonnull VirtualFile file) {
    return myVirtualFilePointerManager.create(file, myParent, myListener);
  }

  protected VirtualFilePointer create(@Nonnull String url) {
    return myVirtualFilePointerManager.create(url, myParent, myListener);
  }

  protected VirtualFilePointer duplicate(@Nonnull VirtualFilePointer virtualFilePointer) {
    return myVirtualFilePointerManager.duplicate(virtualFilePointer, myParent, myListener);
  }

  @Nonnull
  @NonNls
  @Override
  public String toString() {
    return "VFPContainer: " + myList/*+"; parent:"+myParent*/;
  }

  @Override
  @Nonnull
  public VirtualFilePointerContainer clone(@Nonnull Disposable parent) {
    return clone(parent, null);
  }

  @Override
  @Nonnull
  public VirtualFilePointerContainer clone(@Nonnull Disposable parent, @Nullable VirtualFilePointerListener listener) {
    assert !myDisposed;
    VirtualFilePointerContainer clone = myVirtualFilePointerManager.createContainer(parent, listener);
    for (VirtualFilePointer pointer : myList) {
      clone.add(pointer.getUrl());
    }
    return clone;
  }

  @Override
  public void dispose() {
    assert !myDisposed;
    myDisposed = true;
    myTraceableDisposable.kill(null);
  }
}

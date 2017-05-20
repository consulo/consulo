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

package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.projectRoots.ex.SdkRoot;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author mike
 */
class CompositeSdkRoot implements SdkRoot {
  private final List<SdkRoot> myRoots = new ArrayList<SdkRoot>();

  @NotNull 
  SdkRoot[] getProjectRoots() {
    return myRoots.toArray(new SdkRoot[myRoots.size()]);
  }

  @Override
  @NotNull
  public String getPresentableString() {
    throw new UnsupportedOperationException();
  }

  @Override
  @NotNull
  public VirtualFile[] getVirtualFiles() {
    List<VirtualFile> result = new ArrayList<VirtualFile>();
    for (SdkRoot root : myRoots) {
      ContainerUtil.addAll(result, root.getVirtualFiles());
    }

    return VfsUtilCore.toVirtualFileArray(result);
  }

  @Override
  @NotNull
  public String[] getUrls() {
    final List<String> result = new ArrayList<String>();
    for (SdkRoot root : myRoots) {
      ContainerUtil.addAll(result, root.getUrls());
    }
    return ArrayUtil.toStringArray(result);
  }

  @Override
  public boolean isValid() {
    return true;
  }

  void remove(@NotNull SdkRoot root) {
    myRoots.remove(root);
  }

  @NotNull
  SdkRoot add(@NotNull VirtualFile virtualFile) {
    final SimpleSdkRoot root = new SimpleSdkRoot(virtualFile);
    myRoots.add(root);
    return root;
  }

  void add(@NotNull SdkRoot root) {
    myRoots.add(root);
  }

  void remove(@NotNull VirtualFile root) {
    for (Iterator<SdkRoot> iterator = myRoots.iterator(); iterator.hasNext();) {
      SdkRoot sdkRoot = iterator.next();
      if (sdkRoot instanceof SimpleSdkRoot) {
        SimpleSdkRoot r = (SimpleSdkRoot)sdkRoot;
        if (root.equals(r.getFile())) {
          iterator.remove();
        }
      }
    }
  }

  void clear() {
    myRoots.clear();
  }

  public void readExternal(Element element) {
    final List<Element> children = element.getChildren();
    for (Element aChildren : children) {
      myRoots.add(SdkRootStateUtil.readRoot(aChildren));
    }
  }

  public void writeExternal(Element element)  {
    for (SdkRoot root : myRoots) {
      final Element e = SdkRootStateUtil.writeRoot(root);
      element.addContent(e);
    }
  }

  @Override
  public void update() {
    for (SdkRoot root : myRoots) {
      root.update();
    }
  }

}

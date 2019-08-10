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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.projectRoots.ProjectRootListener;
import com.intellij.openapi.projectRoots.ex.SdkRoot;
import com.intellij.openapi.projectRoots.ex.SdkRootContainer;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.ArchiveCopyingFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashMap;
import consulo.logging.Logger;
import consulo.vfs.ArchiveFileSystem;
import org.jdom.Element;
import javax.annotation.Nonnull;

import java.util.List;
import java.util.Map;

/**
 * @author mike
 */
public class SdkRootContainerImpl implements PersistentStateComponent<Element>, SdkRootContainer {
  public static final Logger LOGGER = Logger.getInstance(SdkRootContainerImpl.class);

  private final Map<OrderRootType, CompositeSdkRoot> myRoots = new HashMap<OrderRootType, CompositeSdkRoot>();
  private Map<OrderRootType, VirtualFile[]> myFiles = new HashMap<OrderRootType, VirtualFile[]>();

  private boolean myInsideChange = false;
  private final List<ProjectRootListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  private boolean myNoCopyArchive = false;

  public SdkRootContainerImpl(boolean noCopyArchive) {
    myNoCopyArchive = noCopyArchive;

    for (OrderRootType rootType : OrderRootType.getAllTypes()) {
      myRoots.put(rootType, new CompositeSdkRoot());
      myFiles.put(rootType, VirtualFile.EMPTY_ARRAY);
    }
  }

  @Override
  @Nonnull
  public VirtualFile[] getRootFiles(@Nonnull OrderRootType type) {
    return myFiles.get(type);
  }

  @Override
  @Nonnull
  public SdkRoot[] getRoots(@Nonnull OrderRootType type) {
    return myRoots.get(type).getProjectRoots();
  }

  @Override
  public void startChange() {
    LOGGER.assertTrue(!myInsideChange);

    myInsideChange = true;
  }

  @Override
  public void finishChange() {
    LOGGER.assertTrue(myInsideChange);
    HashMap<OrderRootType, VirtualFile[]> oldRoots = new HashMap<OrderRootType, VirtualFile[]>(myFiles);

    for (OrderRootType orderRootType : OrderRootType.getAllTypes()) {
      final VirtualFile[] roots = myRoots.get(orderRootType).getVirtualFiles();
      final boolean same = Comparing.equal(roots, oldRoots.get(orderRootType));

      myFiles.put(orderRootType, myRoots.get(orderRootType).getVirtualFiles());

      if (!same) {
        fireRootsChanged();
      }
    }

    myInsideChange = false;
  }

  public void addProjectRootContainerListener(ProjectRootListener listener) {
    myListeners.add(listener);
  }

  public void removeProjectRootContainerListener(ProjectRootListener listener) {
    myListeners.remove(listener);
  }

  private void fireRootsChanged() {
    /*
    ApplicationManager.getApplication().runReadAction(new Runnable() {
      public void run() {
        LOG.info("roots changed: type='" + type + "'\n    oldRoots='" + Arrays.asList(oldRoots) + "'\n    newRoots='" + Arrays.asList(newRoots) + "' ");
      }
    });
    */
    for (final ProjectRootListener listener : myListeners) {
      listener.rootsChanged();
    }
  }


  @Override
  public void removeRoot(@Nonnull SdkRoot root, @Nonnull OrderRootType type) {
    LOGGER.assertTrue(myInsideChange);
    myRoots.get(type).remove(root);
  }

  @Override
  @Nonnull
  public SdkRoot addRoot(@Nonnull VirtualFile virtualFile, @Nonnull OrderRootType type) {
    LOGGER.assertTrue(myInsideChange);
    return myRoots.get(type).add(virtualFile);
  }

  @Override
  public void addRoot(@Nonnull SdkRoot root, @Nonnull OrderRootType type) {
    LOGGER.assertTrue(myInsideChange);
    myRoots.get(type).add(root);
  }

  @Override
  public void removeAllRoots(@Nonnull OrderRootType type) {
    LOGGER.assertTrue(myInsideChange);
    myRoots.get(type).clear();
  }

  @Override
  public void removeRoot(@Nonnull VirtualFile root, @Nonnull OrderRootType type) {
    LOGGER.assertTrue(myInsideChange);
    myRoots.get(type).remove(root);
  }

  @Override
  public void removeAllRoots() {
    LOGGER.assertTrue(myInsideChange);
    for (CompositeSdkRoot myRoot : myRoots.values()) {
      myRoot.clear();
    }
  }

  @Override
  public void update() {
    LOGGER.assertTrue(myInsideChange);
    for (CompositeSdkRoot myRoot : myRoots.values()) {
      myRoot.update();
    }
  }

  @Override
  public void loadState(Element element) {
    for (OrderRootType type : OrderRootType.getAllTypes()) {
      read(element, type);
    }

    ApplicationManager.getApplication().runReadAction(new Runnable() {
      @Override
      public void run() {
        myFiles = new HashMap<OrderRootType, VirtualFile[]>();
        for (OrderRootType rootType : myRoots.keySet()) {
          CompositeSdkRoot root = myRoots.get(rootType);
          if (myNoCopyArchive) {
            addNoCopyArchiveForPaths(root);
          }
          myFiles.put(rootType, root.getVirtualFiles());
        }
      }
    });

    for (OrderRootType type : OrderRootType.getAllTypes()) {
      final VirtualFile[] newRoots = getRootFiles(type);
      final VirtualFile[] oldRoots = VirtualFile.EMPTY_ARRAY;
      if (!Comparing.equal(oldRoots, newRoots)) {
        fireRootsChanged();
      }
    }
  }

  @Nonnull
  @Override
  public Element getState() {
    Element element = new Element("state");
    List<OrderRootType> allTypes = OrderRootType.getSortedRootTypes();
    for (OrderRootType type : allTypes) {
      write(element, type);
    }
    return element;
  }

  private static void addNoCopyArchiveForPaths(SdkRoot root) {
    if (root instanceof SimpleSdkRoot) {
      String url = ((SimpleSdkRoot)root).getUrl();
      String protocolId = VirtualFileManager.extractProtocol(url);
      VirtualFileSystem fileSystem = VirtualFileManager.getInstance().getFileSystem(protocolId);
      if (fileSystem instanceof ArchiveFileSystem) {
        String path = VirtualFileManager.extractPath(url);

        ((ArchiveCopyingFileSystem)fileSystem).addNoCopyArchiveForPath(path);
      }
    }
    else if (root instanceof CompositeSdkRoot) {
      SdkRoot[] roots = ((CompositeSdkRoot)root).getProjectRoots();
      for (SdkRoot root1 : roots) {
        addNoCopyArchiveForPaths(root1);
      }
    }
  }

  private void read(Element element, OrderRootType type) {
    Element child = element.getChild(type.getName());
    if (child == null) {
      myRoots.put(type, new CompositeSdkRoot());
      return;
    }

    List<Element> children = child.getChildren();
    LOGGER.assertTrue(children.size() == 1);
    CompositeSdkRoot root = (CompositeSdkRoot)SdkRootStateUtil.readRoot(children.get(0));
    myRoots.put(type, root);
  }

  private void write(Element roots, OrderRootType type) {
    Element e = new Element(type.getName());
    roots.addContent(e);
    e.addContent(SdkRootStateUtil.writeRoot(myRoots.get(type)));
  }
}

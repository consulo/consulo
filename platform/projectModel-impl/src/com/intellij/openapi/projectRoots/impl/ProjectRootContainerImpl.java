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
import com.intellij.openapi.projectRoots.ProjectRootListener;
import com.intellij.openapi.projectRoots.ex.ProjectRoot;
import com.intellij.openapi.projectRoots.ex.ProjectRootContainer;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.PersistentOrderRootType;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashMap;
import org.consulo.lombok.annotations.Logger;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * @author mike
 */
@Logger
public class ProjectRootContainerImpl implements JDOMExternalizable, ProjectRootContainer {
  private final Map<OrderRootType, CompositeProjectRoot> myRoots = new HashMap<OrderRootType, CompositeProjectRoot>();

  private Map<OrderRootType, VirtualFile[]> myFiles = new HashMap<OrderRootType, VirtualFile[]>();

  private boolean myInsideChange = false;
  private final List<ProjectRootListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  private boolean myNoCopyArchive = false;

  public ProjectRootContainerImpl(boolean noCopyArchive) {
    myNoCopyArchive = noCopyArchive;

    for (OrderRootType rootType : OrderRootType.getAllTypes()) {
      myRoots.put(rootType, new CompositeProjectRoot());
      myFiles.put(rootType, VirtualFile.EMPTY_ARRAY);
    }
  }

  @Override
  @NotNull
  public VirtualFile[] getRootFiles(@NotNull OrderRootType type) {
    return myFiles.get(type);
  }

  @Override
  @NotNull
  public ProjectRoot[] getRoots(@NotNull OrderRootType type) {
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
  public void removeRoot(@NotNull ProjectRoot root, @NotNull OrderRootType type) {
    LOGGER.assertTrue(myInsideChange);
    myRoots.get(type).remove(root);
  }

  @Override
  @NotNull
  public ProjectRoot addRoot(@NotNull VirtualFile virtualFile, @NotNull OrderRootType type) {
    LOGGER.assertTrue(myInsideChange);
    return myRoots.get(type).add(virtualFile);
  }

  @Override
  public void addRoot(@NotNull ProjectRoot root, @NotNull OrderRootType type) {
    LOGGER.assertTrue(myInsideChange);
    myRoots.get(type).add(root);
  }

  @Override
  public void removeAllRoots(@NotNull OrderRootType type) {
    LOGGER.assertTrue(myInsideChange);
    myRoots.get(type).clear();
  }

  @Override
  public void removeRoot(@NotNull VirtualFile root, @NotNull OrderRootType type) {
    LOGGER.assertTrue(myInsideChange);
    myRoots.get(type).remove(root);
  }

  @Override
  public void removeAllRoots() {
    LOGGER.assertTrue(myInsideChange);
    for (CompositeProjectRoot myRoot : myRoots.values()) {
      myRoot.clear();
    }
  }

  @Override
  public void update() {
    LOGGER.assertTrue(myInsideChange);
    for (CompositeProjectRoot myRoot : myRoots.values()) {
      myRoot.update();
    }
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    for (PersistentOrderRootType type : OrderRootType.getAllPersistentTypes()) {
      read(element, type);
    }

    ApplicationManager.getApplication().runReadAction(new Runnable() {
      @Override
      public void run() {
        myFiles = new HashMap<OrderRootType, VirtualFile[]>();
        for (OrderRootType rootType : myRoots.keySet()) {
          CompositeProjectRoot root = myRoots.get(rootType);
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

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    List<PersistentOrderRootType> allTypes = OrderRootType.getSortedRootTypes();
    for (PersistentOrderRootType type : allTypes) {
      write(element, type);
    }
  }

  private static void addNoCopyArchiveForPaths(ProjectRoot root) {
    if (root instanceof SimpleProjectRoot) {
      String url = ((SimpleProjectRoot)root).getUrl();
      String protocolId = VirtualFileManager.extractProtocol(url);
      IVirtualFileSystem fileSystem = VirtualFileManager.getInstance().getFileSystem(protocolId);
      if (fileSystem instanceof ArchiveFileSystem) {
        String path = VirtualFileManager.extractPath(url);

        ((ArchiveCopyingFileSystem)fileSystem).addNoCopyArchiveForPath(path);
      }
    }
    else if (root instanceof CompositeProjectRoot) {
      ProjectRoot[] roots = ((CompositeProjectRoot)root).getProjectRoots();
      for (ProjectRoot root1 : roots) {
        addNoCopyArchiveForPaths(root1);
      }
    }
  }

  private void read(Element element, PersistentOrderRootType type) throws InvalidDataException {
    Element child = element.getChild(type.getSdkRootName());
    if (child == null) {
      myRoots.put(type, new CompositeProjectRoot());
      return;
    }

    List<Element> children = child.getChildren();
    LOGGER.assertTrue(children.size() == 1);
    CompositeProjectRoot root = (CompositeProjectRoot)ProjectRootUtil.read(children.get(0));
    myRoots.put(type, root);
  }

  private void write(Element roots, PersistentOrderRootType type) throws WriteExternalException {
    Element e = new Element(type.getSdkRootName());
    roots.addContent(e);
    final Element root = ProjectRootUtil.write(myRoots.get(type));
    if (root != null) {
      e.addContent(root);
    }
  }
}

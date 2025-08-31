// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.application.content.impl.internal;

import consulo.application.ApplicationManager;
import consulo.content.OrderRootType;
import consulo.content.RootProvider;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;
import consulo.virtualFileSystem.pointer.VirtualFilePointerContainer;
import consulo.virtualFileSystem.pointer.VirtualFilePointerListener;
import consulo.virtualFileSystem.pointer.VirtualFilePointerManager;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mike
 */
public class RootsAsVirtualFilePointers implements RootProvider {
  private static final Logger LOG = Logger.getInstance(RootsAsVirtualFilePointers.class);

  private final Map<OrderRootType, VirtualFilePointerContainer> myRoots = new HashMap<>();

  private final boolean myNoCopyJars;
  private final VirtualFilePointerListener myListener;
  @Nonnull
  private final Disposable myParent;

  public RootsAsVirtualFilePointers(boolean noCopyJars, VirtualFilePointerListener listener, @Nonnull Disposable parent) {
    myNoCopyJars = noCopyJars;
    myListener = listener;
    myParent = parent;
  }

  @Override
  @Nonnull
  public VirtualFile[] getFiles(@Nonnull OrderRootType type) {
    VirtualFilePointerContainer container = myRoots.get(type);
    return container == null ? VirtualFile.EMPTY_ARRAY : container.getFiles();
  }

  @Override
  @Nonnull
  public String[] getUrls(@Nonnull OrderRootType type) {
    VirtualFilePointerContainer container = myRoots.get(type);
    return container == null ? ArrayUtil.EMPTY_STRING_ARRAY : container.getUrls();
  }

  public void addRoot(@Nonnull VirtualFile virtualFile, @Nonnull OrderRootType type) {
    getOrCreateContainer(type).add(virtualFile);
  }

  public void addRoot(@Nonnull String url, @Nonnull OrderRootType type) {
    getOrCreateContainer(type).add(url);
  }

  public void removeAllRoots(@Nonnull OrderRootType type) {
    VirtualFilePointerContainer container = myRoots.get(type);
    if (container != null) {
      container.clear();
    }
  }

  public void removeRoot(@Nonnull VirtualFile root, @Nonnull OrderRootType type) {
    removeRoot(root.getUrl(), type);
  }

  public void removeRoot(@Nonnull String url, @Nonnull OrderRootType type) {
    VirtualFilePointerContainer container = myRoots.get(type);
    VirtualFilePointer pointer = container == null ? null : container.findByUrl(url);
    if (pointer != null) {
      container.remove(pointer);
    }
  }

  public void removeAllRoots() {
    for (VirtualFilePointerContainer myRoot : myRoots.values()) {
      myRoot.clear();
    }
  }

  public void readExternal(@Nonnull Element element) {
    for (OrderRootType type : OrderRootType.getAllTypes()) {
      read(element, type);
    }

    ApplicationManager.getApplication().runReadAction(() -> myRoots.values().forEach(container -> {
      if (myNoCopyJars) {
        for (String root : container.getUrls()) {
          setNoCopyJars(root);
        }
      }
    }));
  }

  public void writeExternal(@Nonnull Element element) {
    for (OrderRootType type : OrderRootType.getSortedRootTypes()) {
      write(element, type);
    }
  }

  public void copyRootsFrom(@Nonnull RootProvider rootContainer) {
    removeAllRoots();
    for (OrderRootType rootType : OrderRootType.getAllTypes()) {
      String[] newRoots = rootContainer.getUrls(rootType);
      for (String newRoot : newRoots) {
        addRoot(newRoot, rootType);
      }
    }
  }

  private static void setNoCopyJars(@Nonnull String url) {
    String protocol = VirtualFileManager.extractProtocol(url);
    if (protocol == null) {
      return;
    }

    VirtualFileSystem fs = VirtualFileManager.getInstance().getFileSystem(protocol);
    if (fs instanceof ArchiveFileSystem) {
      ((ArchiveFileSystem)fs).addNoCopyArchiveForPath(url);
    }
  }

  /**
   * <roots>
   * <sourcePath>
   * <root type="composite">
   * <root type="simple" url="jar://I:/Java/jdk1.8/src.zip!/" />
   * <root type="simple" url="jar://I:/Java/jdk1.8/javafx-src.zip!/" />
   * </root>
   * </sourcePath>
   * </roots>
   */
  private void read(@Nonnull Element roots, @Nonnull OrderRootType type) {
    String sdkRootName = type.getId();
    Element child = sdkRootName == null ? null : roots.getChild(sdkRootName);
    if (child == null) {
      return;
    }

    List<Element> composites = child.getChildren();
    if (composites.size() != 1) {
      LOG.error(composites);
    }
    Element composite = composites.get(0);
    if (!composite.getChildren("root").isEmpty()) {
      VirtualFilePointerContainer container = getOrCreateContainer(type);
      container.readExternal(composite, "root", false);
    }
  }

  /**
   * <roots>
   * <sourcePath>
   * <root type="composite">
   * <root type="simple" url="jar://I:/Java/jdk1.8/src.zip!/" />
   * <root type="simple" url="jar://I:/Java/jdk1.8/javafx-src.zip!/" />
   * </root>
   * </sourcePath>
   * </roots>
   */
  private void write(@Nonnull Element roots, @Nonnull OrderRootType type) {
    String sdkRootName = type.getId();
    if (sdkRootName == null) {
      return;
    }
    Element e = new Element(sdkRootName);
    roots.addContent(e);
    Element composite = new Element("root");
    composite.setAttribute("type", "composite");
    e.addContent(composite);
    VirtualFilePointerContainer container = myRoots.get(type);
    if (container != null) {
      container.writeExternal(composite, "root", false);
    }
    for (Element root : composite.getChildren()) {
      root.setAttribute("type", "simple");
    }
  }

  @Override
  public void addRootSetChangedListener(@Nonnull RootSetChangedListener listener) {
    throw new RuntimeException();
  }

  @Override
  public void addRootSetChangedListener(@Nonnull RootSetChangedListener listener, @Nonnull Disposable parentDisposable) {
    throw new RuntimeException();
  }

  @Override
  public void removeRootSetChangedListener(@Nonnull RootSetChangedListener listener) {
    throw new RuntimeException();
  }

  @Nonnull
  private VirtualFilePointerContainer getOrCreateContainer(@Nonnull OrderRootType rootType) {
    VirtualFilePointerContainer roots = myRoots.get(rootType);
    if (roots == null) {
      roots = VirtualFilePointerManager.getInstance().createContainer(myParent, myListener);
      myRoots.put(rootType, roots);
    }
    return roots;
  }
}

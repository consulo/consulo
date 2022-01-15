// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.impl.libraries;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ComponentSerializationUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.impl.SdkImpl;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.RootProvider;
import com.intellij.openapi.roots.impl.ProjectRootManagerImpl;
import com.intellij.openapi.roots.libraries.*;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.impl.VirtualFilePointerContainerImpl;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerContainer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerListener;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.EventDispatcher;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.XmlSerializer;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.disposer.TraceableDisposable;
import consulo.logging.Logger;
import consulo.roots.impl.ModuleRootLayerImpl;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class LibraryImpl implements LibraryEx.ModifiableModelEx, LibraryEx, RootProvider {
  private static final Logger LOG = Logger.getInstance(LibraryImpl.class);
  @NonNls
  public static final String LIBRARY_NAME_ATTR = "name";
  @NonNls
  public static final String LIBRARY_TYPE_ATTR = "type";
  @NonNls
  public static final String ROOT_PATH_ELEMENT = "root";
  @NonNls
  public static final String ELEMENT = "library";
  @NonNls
  public static final String PROPERTIES_ELEMENT = "properties";
  public static final String EXCLUDED_ROOTS_TAG = "excluded";
  private String myName;
  private final LibraryTable myLibraryTable;
  private final Map<OrderRootType, VirtualFilePointerContainer> myRoots = new HashMap<>(3);
  @Nullable
  private VirtualFilePointerContainer myExcludedRoots;
  private final LibraryImpl mySource;
  private PersistentLibraryKind<?> myKind;
  private LibraryProperties myProperties;

  @Nullable
  private final ModuleRootLayerImpl myRootModel;
  private boolean myDisposed;
  private final Disposable myPointersDisposable = Disposable.newDisposable();
  private final EventDispatcher<RootSetChangedListener> myDispatcher = EventDispatcher.create(RootSetChangedListener.class);

  private TraceableDisposable myTraceableDisposable;

  LibraryImpl(LibraryTable table, @Nonnull Element element, ModuleRootLayerImpl rootModel) throws InvalidDataException {
    this(table, rootModel, null, null, findPersistentLibraryKind(element));
    readExternal(element);
  }

  LibraryImpl(String name, @Nullable final PersistentLibraryKind<?> kind, LibraryTable table, ModuleRootLayerImpl rootModel) {
    this(table, rootModel, null, name, kind);
    if (kind != null) {
      myProperties = kind.createDefaultProperties();
    }
  }

  private LibraryImpl(@Nonnull LibraryImpl from, LibraryImpl newSource, ModuleRootLayerImpl rootModel) {
    this(from.myLibraryTable, rootModel, newSource, from.myName, from.myKind);
    from.checkDisposed();

    if (from.myKind != null && from.myProperties != null) {
      myProperties = myKind.createDefaultProperties();
      Object state = from.myProperties.getState();
      if (state != null) {
        //noinspection unchecked
        myProperties.loadState(state);
      }
    }
    for (OrderRootType rootType : getAllRootTypes()) {
      final VirtualFilePointerContainer thatContainer = from.myRoots.get(rootType);
      if (thatContainer != null && !thatContainer.isEmpty()) {
        getOrCreateContainer(rootType).addAll(thatContainer);
      }
    }
    if (from.myExcludedRoots != null) {
      myExcludedRoots = from.myExcludedRoots.clone(myPointersDisposable);
    }
  }

  // primary
  private LibraryImpl(LibraryTable table, @Nullable ModuleRootLayerImpl rootModel, LibraryImpl newSource, String name, @Nullable PersistentLibraryKind<?> kind) {
    myTraceableDisposable = TraceableDisposable.newTraceDisposable(true);
    myLibraryTable = table;
    myRootModel = rootModel;
    mySource = newSource;
    myKind = kind;
    myName = name;
    Disposer.register(this, myPointersDisposable);
  }

  @Nullable
  private static PersistentLibraryKind<?> findPersistentLibraryKind(@Nonnull Element element) {
    String typeString = element.getAttributeValue(LIBRARY_TYPE_ATTR);
    LibraryKind kind = LibraryKind.findById(typeString);
    if (kind != null && !(kind instanceof PersistentLibraryKind<?>)) {
      LOG.error("Cannot load non-persistable library kind: " + typeString);
      return null;
    }
    return (PersistentLibraryKind<?>)kind;
  }

  @Nonnull
  private Collection<OrderRootType> getAllRootTypes() {
    return OrderRootType.getAllTypes();
  }

  @Override
  public void dispose() {
    checkDisposed();

    myDisposed = true;
    myTraceableDisposable.kill(null);
  }

  private void checkDisposed() {
    if (isDisposed()) {
      myTraceableDisposable.throwDisposalError("'" + myName + "' already disposed: " + myTraceableDisposable.getStackTrace());
    }
  }

  @Override
  public boolean isDisposed() {
    return myDisposed;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  @Nonnull
  public String[] getUrls(@Nonnull OrderRootType rootType) {
    checkDisposed();

    VirtualFilePointerContainer result = myRoots.get(rootType);
    return result == null ? ArrayUtilRt.EMPTY_STRING_ARRAY : result.getUrls();
  }

  @Override
  @Nonnull
  public VirtualFile[] getFiles(@Nonnull OrderRootType rootType) {
    checkDisposed();

    VirtualFilePointerContainer container = myRoots.get(rootType);
    return container == null ? VirtualFile.EMPTY_ARRAY : container.getFiles();
  }

  @Override
  public void setName(String name) {
    LOG.assertTrue(isWritable());
    myName = name;
  }

  /* you have to commit modifiable model or dispose it by yourself! */
  @Override
  @Nonnull
  public ModifiableModelEx getModifiableModel() {
    checkDisposed();
    return new LibraryImpl(this, this, myRootModel);
  }

  @Nonnull
  public Library cloneLibrary(@Nonnull ModuleRootLayerImpl moduleRootLayer) {
    LOG.assertTrue(myLibraryTable == null);
    return new LibraryImpl(this, null, moduleRootLayer);
  }

  @Nonnull
  @Override
  public List<String> getInvalidRootUrls(@Nonnull OrderRootType type) {
    if (myDisposed) return Collections.emptyList();

    VirtualFilePointerContainer container = myRoots.get(type);
    final List<VirtualFilePointer> pointers = container == null ? Collections.emptyList() : container.getList();
    List<String> invalidPaths = null;
    for (VirtualFilePointer pointer : pointers) {
      if (!pointer.isValid()) {
        if (invalidPaths == null) {
          invalidPaths = new SmartList<>();
        }
        invalidPaths.add(pointer.getUrl());
      }
    }
    return ContainerUtil.notNullize(invalidPaths);
  }

  @Override
  public void setProperties(LibraryProperties properties) {
    LOG.assertTrue(isWritable());
    myProperties = properties;
  }

  @Override
  @Nonnull
  public RootProvider getRootProvider() {
    return this;
  }

  @Nonnull
  private VirtualFilePointerListener getListener() {
    Project project = myLibraryTable instanceof ProjectLibraryTable ? ((ProjectLibraryTable)myLibraryTable).getProject() : null;
    return myRootModel != null
           ? myRootModel.getRootsChangedListener()
           : project != null ? ProjectRootManagerImpl.getInstanceImpl(project).getRootsValidityChangedListener() : SdkImpl.getGlobalVirtualFilePointerListener();
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    readName(element);
    readProperties(element);
    readRoots(element);
    readJarDirectories(element);
  }

  @NonNls
  public static final String ROOT_TYPE_ATTR = "type";
  private static final OrderRootType DEFAULT_JAR_DIRECTORY_TYPE = OrderRootType.CLASSES;

  @Nonnull
  private VirtualFilePointerContainer getOrCreateContainer(@Nonnull OrderRootType rootType) {
    VirtualFilePointerContainer roots = myRoots.get(rootType);
    if (roots == null) {
      roots = VirtualFilePointerManager.getInstance().createContainer(myPointersDisposable, getListener());
      myRoots.put(rootType, roots);
    }
    return roots;
  }

  /**
   * @deprecated just to maintain .xml compatibility.
   * VirtualFilePointerContainerImpl does the same but stores its jar dirs attributes inside <root> element
   */
  @Deprecated // todo to remove sometime later
  private void readJarDirectories(@Nonnull Element element) {
    final List<Element> jarDirs = element.getChildren(VirtualFilePointerContainerImpl.JAR_DIRECTORY_ELEMENT);
    for (Element jarDir : jarDirs) {
      final String url = jarDir.getAttributeValue(VirtualFilePointerContainerImpl.URL_ATTR);
      if (url != null) {
        final String recursive = jarDir.getAttributeValue(VirtualFilePointerContainerImpl.RECURSIVE_ATTR);
        final OrderRootType rootType = getJarDirectoryRootType(jarDir.getAttributeValue(ROOT_TYPE_ATTR));
        VirtualFilePointerContainer roots = getOrCreateContainer(rootType);
        boolean recursively = Boolean.parseBoolean(recursive);
        roots.addJarDirectory(url, recursively);
      }
    }
  }

  @Nonnull
  private static OrderRootType getJarDirectoryRootType(@Nullable String type) {
    for (OrderRootType rootType : OrderRootType.getAllTypes()) {
      if (rootType.name().equals(type)) {
        return rootType;
      }
    }
    return DEFAULT_JAR_DIRECTORY_TYPE;
  }

  private void readProperties(@Nonnull Element element) {
    final String typeId = element.getAttributeValue(LIBRARY_TYPE_ATTR);
    if (typeId == null) return;

    myKind = (PersistentLibraryKind<?>)LibraryKind.findById(typeId);
    if (myKind == null) return;

    myProperties = myKind.createDefaultProperties();
    final Element propertiesElement = element.getChild(PROPERTIES_ELEMENT);
    if (propertiesElement != null) {
      ComponentSerializationUtil.loadComponentState(myProperties, propertiesElement);
    }
  }

  private void readName(@Nonnull Element element) {
    myName = element.getAttributeValue(LIBRARY_NAME_ATTR);
  }

  private void readRoots(@Nonnull Element element) throws InvalidDataException {
    for (OrderRootType rootType : getAllRootTypes()) {
      final Element rootChild = element.getChild(rootType.name());
      if (rootChild == null) {
        continue;
      }
      if (!rootChild.getChildren(ROOT_PATH_ELEMENT).isEmpty()) {
        VirtualFilePointerContainer roots = getOrCreateContainer(rootType);
        roots.readExternal(rootChild, ROOT_PATH_ELEMENT, false);
      }
    }
    Element excludedRoot = element.getChild(EXCLUDED_ROOTS_TAG);
    if (excludedRoot != null && !excludedRoot.getChildren(ROOT_PATH_ELEMENT).isEmpty()) {
      getOrCreateExcludedRoots().readExternal(excludedRoot, ROOT_PATH_ELEMENT, false);
    }
  }

  @Nonnull
  private VirtualFilePointerContainer getOrCreateExcludedRoots() {
    VirtualFilePointerContainer excludedRoots = myExcludedRoots;
    if (excludedRoots == null) {
      myExcludedRoots = excludedRoots = VirtualFilePointerManager.getInstance().createContainer(myPointersDisposable);
    }
    return excludedRoots;
  }

  //TODO<rv> Remove the next two methods as a temporary solution. Sort in OrderRootType.
  //
  @Nonnull
  private static List<OrderRootType> sortRootTypes(@Nonnull Collection<? extends OrderRootType> rootTypes) {
    List<OrderRootType> allTypes = new ArrayList<>(rootTypes);
    Collections.sort(allTypes, (o1, o2) -> o1.name().compareToIgnoreCase(o2.name()));
    return allTypes;
  }

  @Override
  public void writeExternal(Element rootElement) {
    checkDisposed();

    Element element = new Element(ELEMENT);
    if (myName != null) {
      element.setAttribute(LIBRARY_NAME_ATTR, myName);
    }
    if (myKind != null) {
      element.setAttribute(LIBRARY_TYPE_ATTR, myKind.getKindId());
      LOG.assertTrue(myProperties != null, "Properties is 'null' in library with kind " + myKind);
      final Object state = myProperties.getState();
      if (state != null) {
        final Element propertiesElement = XmlSerializer.serialize(state);
        if (propertiesElement != null) {
          element.addContent(propertiesElement.setName(PROPERTIES_ELEMENT));
        }
      }
    }
    for (OrderRootType rootType : OrderRootType.getSortedRootTypes()) {
      VirtualFilePointerContainer roots = myRoots.get(rootType);
      if (roots == null || roots.isEmpty()) {
        continue;
      }

      final Element rootTypeElement = new Element(rootType.name());
      if (roots != null) {
        roots.writeExternal(rootTypeElement, ROOT_PATH_ELEMENT, false);
      }
      element.addContent(rootTypeElement);
    }
    if (myExcludedRoots != null && !myExcludedRoots.isEmpty()) {
      Element excluded = new Element(EXCLUDED_ROOTS_TAG);
      myExcludedRoots.writeExternal(excluded, ROOT_PATH_ELEMENT, false);
      element.addContent(excluded);
    }
    writeJarDirectories(element);
    rootElement.addContent(element);
  }

  /**
   * @deprecated just to maintain .xml compatibility.
   * VirtualFilePointerContainerImpl does the same but stores its jar dirs attributes inside <root> element
   */
  @Deprecated // todo to remove sometime later
  private void writeJarDirectories(@Nonnull Element element) {
    final List<OrderRootType> rootTypes = sortRootTypes(myRoots.keySet());
    for (OrderRootType rootType : rootTypes) {
      VirtualFilePointerContainer container = myRoots.get(rootType);
      List<Pair<String, Boolean>> jarDirectories = new ArrayList<>(container.getJarDirectories());
      Collections.sort(jarDirectories, Comparator.comparing(p -> p.getFirst(), String.CASE_INSENSITIVE_ORDER));
      for (Pair<String, Boolean> pair : jarDirectories) {
        String url = pair.getFirst();
        boolean isRecursive = pair.getSecond();
        final Element jarDirElement = new Element(VirtualFilePointerContainerImpl.JAR_DIRECTORY_ELEMENT);
        jarDirElement.setAttribute(VirtualFilePointerContainerImpl.URL_ATTR, url);
        jarDirElement.setAttribute(VirtualFilePointerContainerImpl.RECURSIVE_ATTR, Boolean.toString(isRecursive));
        if (!rootType.equals(DEFAULT_JAR_DIRECTORY_TYPE)) {
          jarDirElement.setAttribute(ROOT_TYPE_ATTR, rootType.name());
        }
        element.addContent(jarDirElement);
      }
    }
  }

  private boolean isWritable() {
    return mySource != null;
  }

  @Nullable
  @Override
  public PersistentLibraryKind<?> getKind() {
    return myKind;
  }

  @Override
  public void addExcludedRoot(@Nonnull String url) {
    VirtualFilePointerContainer roots = getOrCreateExcludedRoots();
    if (roots.findByUrl(url) == null) {
      roots.add(url);
    }
  }

  @Override
  public boolean removeExcludedRoot(@Nonnull String url) {
    if (myExcludedRoots != null) {
      VirtualFilePointer pointer = myExcludedRoots.findByUrl(url);
      if (pointer != null) {
        myExcludedRoots.remove(pointer);
        return true;
      }
    }
    return false;
  }

  @Nonnull
  @Override
  public String[] getExcludedRootUrls() {
    return myExcludedRoots != null ? myExcludedRoots.getUrls() : ArrayUtilRt.EMPTY_STRING_ARRAY;
  }

  @Nonnull
  @Override
  public VirtualFile[] getExcludedRoots() {
    return myExcludedRoots != null ? myExcludedRoots.getFiles() : VirtualFile.EMPTY_ARRAY;
  }

  @Override
  public LibraryProperties getProperties() {
    return myProperties;
  }

  @Override
  public void setKind(@Nonnull PersistentLibraryKind<?> kind) {
    LOG.assertTrue(isWritable());
    LOG.assertTrue(myKind == null || myKind == kind, "Library kind cannot be changed from " + myKind + " to " + kind);
    myKind = kind;
    myProperties = kind.createDefaultProperties();
  }

  @Override
  public void addRoot(@Nonnull String url, @Nonnull OrderRootType rootType) {
    checkDisposed();
    LOG.assertTrue(isWritable());

    final VirtualFilePointerContainer container = getOrCreateContainer(rootType);
    container.add(url);
  }

  @Override
  public void addRoot(@Nonnull VirtualFile file, @Nonnull OrderRootType rootType) {
    checkDisposed();
    LOG.assertTrue(isWritable());

    final VirtualFilePointerContainer container = getOrCreateContainer(rootType);
    container.add(file);
  }

  @Override
  public void addJarDirectory(@Nonnull final String url, final boolean recursive) {
    addJarDirectory(url, recursive, OrderRootType.CLASSES);
  }

  @Override
  public void addJarDirectory(@Nonnull final VirtualFile file, final boolean recursive) {
    addJarDirectory(file, recursive, OrderRootType.CLASSES);
  }

  @Override
  public void addJarDirectory(@Nonnull final String url, final boolean recursive, @Nonnull OrderRootType rootType) {
    checkDisposed();
    LOG.assertTrue(isWritable());

    final VirtualFilePointerContainer container = getOrCreateContainer(rootType);
    container.addJarDirectory(url, recursive);
  }

  @Override
  public void addJarDirectory(@Nonnull final VirtualFile file, final boolean recursive, @Nonnull OrderRootType rootType) {
    checkDisposed();
    LOG.assertTrue(isWritable());

    final VirtualFilePointerContainer container = getOrCreateContainer(rootType);
    container.addJarDirectory(file.getUrl(), recursive);
  }

  @Override
  public boolean isJarDirectory(@Nonnull final String url) {
    return isJarDirectory(url, OrderRootType.CLASSES);
  }

  @Override
  public boolean isJarDirectory(@Nonnull final String url, @Nonnull final OrderRootType rootType) {
    VirtualFilePointerContainer container = myRoots.get(rootType);
    if (container == null) return false;
    List<Pair<String, Boolean>> jarDirectories = container.getJarDirectories();
    return jarDirectories.contains(Pair.create(url, false)) || jarDirectories.contains(Pair.create(url, true));
  }

  @Override
  public boolean isValid(@Nonnull final String url, @Nonnull final OrderRootType rootType) {
    final VirtualFilePointerContainer container = myRoots.get(rootType);
    final VirtualFilePointer fp = container == null ? null : container.findByUrl(url);
    return fp != null && fp.isValid();
  }

  @Override
  public boolean removeRoot(@Nonnull String url, @Nonnull OrderRootType rootType) {
    checkDisposed();
    LOG.assertTrue(isWritable());

    final VirtualFilePointerContainer container = myRoots.get(rootType);
    final VirtualFilePointer byUrl = container == null ? null : container.findByUrl(url);
    if (byUrl != null) {
      container.remove(byUrl);
      if (myExcludedRoots != null) {
        for (String excludedRoot : myExcludedRoots.getUrls()) {
          if (!isUnderRoots(excludedRoot)) {
            VirtualFilePointer pointer = myExcludedRoots.findByUrl(excludedRoot);
            if (pointer != null) {
              myExcludedRoots.remove(pointer);
            }
          }
        }
      }
      container.removeJarDirectory(url);
      return true;
    }
    return false;
  }

  private boolean isUnderRoots(@Nonnull String url) {
    for (VirtualFilePointerContainer container : myRoots.values()) {
      if (VfsUtilCore.isUnder(url, Arrays.asList(container.getUrls()))) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void moveRootUp(@Nonnull String url, @Nonnull OrderRootType rootType) {
    checkDisposed();
    LOG.assertTrue(isWritable());

    final VirtualFilePointerContainer container = myRoots.get(rootType);
    if (container != null) {
      container.moveUp(url);
    }
  }

  @Override
  public void moveRootDown(@Nonnull String url, @Nonnull OrderRootType rootType) {
    checkDisposed();
    LOG.assertTrue(isWritable());

    final VirtualFilePointerContainer container = myRoots.get(rootType);
    if (container != null) {
      container.moveDown(url);
    }
  }

  @Override
  public boolean isChanged() {
    return !mySource.equals(this);
  }

  private boolean areRootsChanged(@Nonnull LibraryImpl that) {
    return !that.equals(this);
  }

  public Library getSource() {
    return mySource;
  }

  @Override
  public void commit() {
    checkDisposed();

    if (isChanged()) {
      mySource.commit(this);
    }
    Disposer.dispose(this);
  }

  private void commit(@Nonnull LibraryImpl fromModel) {
    if (myLibraryTable != null) {
      ApplicationManager.getApplication().assertWriteAccessAllowed();
    }
    if (!Comparing.equal(fromModel.myName, myName)) {
      myName = fromModel.myName;
      if (myLibraryTable instanceof LibraryTableBase) {
        ((LibraryTableBase)myLibraryTable).fireLibraryRenamed(this);
      }
    }
    myKind = fromModel.getKind();
    myProperties = fromModel.myProperties;
    if (areRootsChanged(fromModel)) {
      disposeMyPointers();
      copyRootsFrom(fromModel);
      fireRootSetChanged();
    }
  }

  private void copyRootsFrom(@Nonnull LibraryImpl fromModel) {
    Map<OrderRootType, VirtualFilePointerContainer> clonedRoots = new HashMap<>();
    for (Map.Entry<OrderRootType, VirtualFilePointerContainer> entry : fromModel.myRoots.entrySet()) {
      OrderRootType rootType = entry.getKey();
      VirtualFilePointerContainer container = entry.getValue();
      VirtualFilePointerContainer clone = container.clone(myPointersDisposable, getListener());
      clonedRoots.put(rootType, clone);
    }
    myRoots.clear();
    myRoots.putAll(clonedRoots);

    VirtualFilePointerContainer excludedRoots = fromModel.myExcludedRoots;
    myExcludedRoots = excludedRoots != null ? excludedRoots.clone(myPointersDisposable) : null;
  }

  private void disposeMyPointers() {
    for (VirtualFilePointerContainer container : new HashSet<>(myRoots.values())) {
      container.killAll();
    }
    if (myExcludedRoots != null) {
      myExcludedRoots.killAll();
    }
    Disposer.dispose(myPointersDisposable);
    Disposer.register(this, myPointersDisposable);
  }

  @Override
  public LibraryTable getTable() {
    return myLibraryTable;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final LibraryImpl library = (LibraryImpl)o;

    if (!Objects.equals(myName, library.myName)) return false;
    if (!myRoots.equals(library.myRoots)) return false;
    if (!Objects.equals(myKind, library.myKind)) return false;
    if (!Objects.equals(myProperties, library.myProperties)) return false;
    return Comparing.equal(myExcludedRoots, library.myExcludedRoots);
  }

  @Override
  public int hashCode() {
    int result = myName != null ? myName.hashCode() : 0;
    result = 31 * result + myRoots.hashCode();
    return result;
  }

  @NonNls
  @Override
  public String toString() {
    return "Library: name:" + myName + "; roots:" + myRoots.values();
  }

  @Override
  @Nullable
  public Module getModule() {
    return myRootModel == null ? null : myRootModel.getModule();
  }

  @Override
  public void addRootSetChangedListener(@Nonnull RootSetChangedListener listener) {
    myDispatcher.addListener(listener);
  }

  @Override
  public void removeRootSetChangedListener(@Nonnull RootSetChangedListener listener) {
    myDispatcher.removeListener(listener);
  }

  @Override
  public void addRootSetChangedListener(@Nonnull RootSetChangedListener listener, @Nonnull Disposable parentDisposable) {
    myDispatcher.addListener(listener, parentDisposable);
  }

  private void fireRootSetChanged() {
    myDispatcher.getMulticaster().rootSetChanged(this);
  }
}

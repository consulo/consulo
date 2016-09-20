/*
 * Copyright 2013-2016 consulo.io
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

import com.google.common.base.Predicate;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.UnknownFeaturesCollector;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import consulo.roots.ModifiableModuleRootLayer;
import consulo.roots.orderEntry.OrderEntrySerializationUtil;
import consulo.lombok.annotations.Logger;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.ModuleExtensionChangeListener;
import consulo.module.extension.ModuleExtensionWithSdk;
import consulo.module.extension.MutableModuleExtension;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import consulo.module.extension.ModuleExtensionProviderEP;
import consulo.roots.ContentFolderScopes;
import consulo.roots.ContentFolderTypeProvider;

import java.util.*;

/**
 * @author VISTALL
 * @since 29.07.14
 */
@Logger
public class ModuleRootLayerImpl implements ModifiableModuleRootLayer, Disposable {
  private static class ContentComparator implements Comparator<ContentEntry> {
    public static final ContentComparator INSTANCE = new ContentComparator();

    @Override
    public int compare(@NotNull final ContentEntry o1, @NotNull final ContentEntry o2) {
      return o1.getUrl().compareTo(o2.getUrl());
    }
  }

  private static class CollectDependentModules extends RootPolicy<List<String>> {
    @NotNull
    @Override
    public List<String> visitModuleOrderEntry(@NotNull ModuleOrderEntry moduleOrderEntry, @NotNull List<String> arrayList) {
      arrayList.add(moduleOrderEntry.getModuleName());
      return arrayList;
    }
  }

  private class Order extends ArrayList<OrderEntry> {
    @Override
    public void clear() {
      super.clear();
      clearCachedEntries();
    }

    @NotNull
    @Override
    public OrderEntry set(int i, @NotNull OrderEntry orderEntry) {
      super.set(i, orderEntry);
      ((OrderEntryBaseImpl)orderEntry).setIndex(i);
      clearCachedEntries();
      return orderEntry;
    }

    @Override
    public boolean add(@NotNull OrderEntry orderEntry) {
      super.add(orderEntry);
      ((OrderEntryBaseImpl)orderEntry).setIndex(size() - 1);
      clearCachedEntries();
      return true;
    }

    @Override
    public void add(int i, OrderEntry orderEntry) {
      super.add(i, orderEntry);
      clearCachedEntries();
      setIndicies(i);
    }

    @Override
    public OrderEntry remove(int i) {
      OrderEntry entry = super.remove(i);
      setIndicies(i);
      clearCachedEntries();
      return entry;
    }

    @Override
    public boolean remove(Object o) {
      int index = indexOf(o);
      if (index < 0) return false;
      remove(index);
      clearCachedEntries();
      return true;
    }

    @Override
    public boolean addAll(Collection<? extends OrderEntry> collection) {
      int startSize = size();
      boolean result = super.addAll(collection);
      setIndicies(startSize);
      clearCachedEntries();
      return result;
    }

    @Override
    public boolean addAll(int i, Collection<? extends OrderEntry> collection) {
      boolean result = super.addAll(i, collection);
      setIndicies(i);
      clearCachedEntries();
      return result;
    }

    @Override
    public void removeRange(int i, int i1) {
      super.removeRange(i, i1);
      clearCachedEntries();
      setIndicies(i);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> collection) {
      boolean result = super.removeAll(collection);
      setIndicies(0);
      clearCachedEntries();
      return result;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> collection) {
      boolean result = super.retainAll(collection);
      setIndicies(0);
      clearCachedEntries();
      return result;
    }

    private void clearCachedEntries() {
      myCachedOrderEntries = null;
    }

    private void setIndicies(int startIndex) {
      for (int j = startIndex; j < size(); j++) {
        ((OrderEntryBaseImpl)get(j)).setIndex(j);
      }
    }
  }

  private final Set<ContentEntry> myContent = new TreeSet<ContentEntry>(ContentComparator.INSTANCE);

  private final List<OrderEntry> myOrderEntries = new Order();
  // cleared by myOrderEntries modification, see Order
  @Nullable
  private OrderEntry[] myCachedOrderEntries;
  private final Set<ModuleExtension<?>> myExtensions = new LinkedHashSet<ModuleExtension<?>>();
  private final List<Element> myUnknownModuleExtensions = new SmartList<Element>();
  private RootModelImpl myRootModel;
  @NotNull
  private final ModuleLibraryTable myModuleLibraryTable;

  public ModuleRootLayerImpl(@Nullable ModuleRootLayerImpl originalLayer, @NotNull RootModelImpl rootModel) {
    myRootModel = rootModel;
    myModuleLibraryTable = new ModuleLibraryTable(this);

    if (originalLayer != null) {
      createMutableExtensions(originalLayer);
      setContentEntriesFrom(originalLayer);
      setOrderEntriesFrom(originalLayer);
    }
    else {
      init();
    }
  }

  public void loadState(@NotNull Element element, @Nullable ProgressIndicator progressIndicator) throws InvalidDataException {
    removeAllContentEntries();
    removeAllOrderEntries();

    List<Element> children = element.getChildren();
    int i = 0;
    boolean moduleSourceAdded = false;
    for (Element child : children) {
      i++;

      if(i % 10 == 0) {
        if (progressIndicator != null) {
          progressIndicator.checkCanceled();
        }
      }

      String name = child.getName();
      if("extension".equals(name))  {
        final String id = child.getAttributeValue("id");

        ModuleExtensionProviderEP providerEP = ModuleExtensionProviderEP.findProviderEP(id);
        if (providerEP != null) {
          ModuleExtension<?> moduleExtension = getExtensionWithoutCheck(id);
          assert moduleExtension != null;
          moduleExtension.loadState(child);
        }
        else {
          UnknownFeaturesCollector.getInstance(getProject()).registerUnknownFeature(ModuleExtensionProviderEP.EP_NAME.getName(), id);

          myUnknownModuleExtensions.add(child.clone());
        }
      }
      else if(ContentEntryImpl.ELEMENT_NAME.equals(name))  {
        ContentEntryImpl contentEntry = new ContentEntryImpl(child, this);
        myContent.add(contentEntry);
      }
      else if(OrderEntrySerializationUtil.ORDER_ENTRY_ELEMENT_NAME.equals(name)) {
        final OrderEntry orderEntry = OrderEntrySerializationUtil.loadOrderEntry(child, this);
        if (orderEntry == null) {
          continue;
        }
        if (orderEntry instanceof ModuleSourceOrderEntry) {
          if (moduleSourceAdded) {
            continue;
          }
          moduleSourceAdded = true;
        }
        myOrderEntries.add(orderEntry);
      }
    }

    if (!moduleSourceAdded) {
      myOrderEntries.add(new ModuleSourceOrderEntryImpl(this));
    }
  }

  public void writeExternal(@NotNull Element element) {
    List<Element> moduleExtensionElements = new ArrayList<Element>();
    for (ModuleExtension<?> extension : myExtensions) {
      final Element state = extension.getState();
      if (state == null) {
        continue;
      }
      moduleExtensionElements.add(state);
    }

    for (Element unknownModuleExtension : myUnknownModuleExtensions) {
      moduleExtensionElements.add(unknownModuleExtension.clone());
    }
    Collections.sort(moduleExtensionElements, new Comparator<Element>() {
      @Override
      public int compare(Element o1, Element o2) {
        return Comparing.compare(o1.getAttributeValue("id"), o2.getAttributeValue("id"));
      }
    });

    element.addContent(moduleExtensionElements);

    for (ContentEntry contentEntry : getContent()) {
      if (contentEntry instanceof ContentEntryImpl) {
        final Element subElement = new Element(ContentEntryImpl.ELEMENT_NAME);
        ((ContentEntryImpl)contentEntry).writeExternal(subElement);
        element.addContent(subElement);
      }
    }

    for (OrderEntry orderEntry : getOrderEntries()) {
      Element newElement = OrderEntrySerializationUtil.storeOrderEntry(orderEntry);
      element.addContent(newElement);
    }
  }

  @SuppressWarnings("unchecked")
  public boolean copy(@NotNull ModuleRootLayerImpl toSet, boolean notifyExtensionListener) {
    boolean changed = false;
    ModuleExtensionChangeListener moduleExtensionChangeListener = getModule().getProject().getMessageBus().syncPublisher(ModuleExtension.CHANGE_TOPIC);

    for (ModuleExtension extension : myExtensions) {
      MutableModuleExtension mutableExtension = (MutableModuleExtension)extension;

      ModuleExtension originalExtension = toSet.getExtensionWithoutCheck(extension.getId());
      assert originalExtension != null;
      if (mutableExtension.isModified(originalExtension)) {

        if (notifyExtensionListener) {
          moduleExtensionChangeListener.beforeExtensionChanged(originalExtension, mutableExtension);
        }

        originalExtension.commit(mutableExtension);

        changed = true;
      }
    }

    if (areOrderEntriesChanged(toSet)) {
      toSet.setOrderEntriesFrom(this);
      changed = true;
    }

    if (areContentEntriesChanged(toSet)) {
      toSet.setContentEntriesFrom(this);
      changed = true;
    }

    toSet.myUnknownModuleExtensions.addAll(myUnknownModuleExtensions);
    return changed;
  }

  @SuppressWarnings("unchecked")
  public void createMutableExtensions(@Nullable ModuleRootLayerImpl layer) {
    for (ModuleExtensionProviderEP providerEP : ModuleExtensionProviderEP.EP_NAME.getExtensions()) {
      MutableModuleExtension mutable = providerEP.createMutable(this);
      if (mutable == null) {
        continue;
      }

      if (layer != null) {
        final ModuleExtension<?> originalExtension = layer.getExtensionWithoutCheck(providerEP.getKey());
        assert originalExtension != null;
        mutable.commit(originalExtension);
      }

      myExtensions.add(mutable);
    }
  }

  private void setOrderEntriesFrom(@NotNull ModuleRootLayerImpl layer) {
    removeAllOrderEntries();
    for (OrderEntry orderEntry : layer.myOrderEntries) {
      if (orderEntry instanceof ClonableOrderEntry) {
        myOrderEntries.add(((ClonableOrderEntry)orderEntry).cloneEntry(this));
      }
    }
  }

  private void setContentEntriesFrom(@NotNull ModuleRootLayerImpl layer) {
    removeAllContentEntries();
    for (ContentEntry contentEntry : layer.myContent) {
      if (contentEntry instanceof ClonableContentEntry) {
        myContent.add(((ClonableContentEntry)contentEntry).cloneEntry(this));
      }
    }
  }


  public void init() {
    removeAllOrderEntries();
    myExtensions.clear();

    addSourceOrderEntries();
    createMutableExtensions(null);
  }

  public void addSourceOrderEntries() {
    myOrderEntries.add(new ModuleSourceOrderEntryImpl(this));
  }

  @NotNull
  private ContentEntry addContentEntry(@NotNull ContentEntry e) {
    if (myContent.contains(e)) {
      for (ContentEntry contentEntry : getContentEntries()) {
        if (ContentComparator.INSTANCE.compare(contentEntry, e) == 0) return contentEntry;
      }
    }
    myContent.add(e);
    return e;
  }

  public boolean areContentEntriesChanged(@NotNull ModuleRootLayerImpl original) {
    return ArrayUtil.lexicographicCompare(getContentEntries(), original.getContentEntries()) != 0;
  }

  public boolean areOrderEntriesChanged(@NotNull ModuleRootLayerImpl original) {
    OrderEntry[] orderEntries = getOrderEntries();
    OrderEntry[] sourceOrderEntries = original.getOrderEntries();
    if (orderEntries.length != sourceOrderEntries.length) return true;
    for (int i = 0; i < orderEntries.length; i++) {
      OrderEntry orderEntry = orderEntries[i];
      OrderEntry sourceOrderEntry = sourceOrderEntries[i];
      if (!orderEntriesEquals(orderEntry, sourceOrderEntry)) {
        return true;
      }
    }
    return false;
  }

  public RootModelImpl getRootModel() {
    return myRootModel;
  }

  public boolean areExtensionsChanged(@NotNull ModuleRootLayerImpl original) {
    for (ModuleExtension<?> extension : myExtensions) {
      MutableModuleExtension mutableExtension = (MutableModuleExtension)extension;
      ModuleExtension<?> originalExtension = original.getExtensionWithoutCheck(extension.getId());
      assert originalExtension != null;
      if (mutableExtension.isModified(originalExtension)) {
        return true;
      }
    }
    return false;
  }

  private static boolean orderEntriesEquals(@NotNull OrderEntry orderEntry1, @NotNull OrderEntry orderEntry2) {
    if (orderEntry1.getClass() != orderEntry2.getClass()) {
      return false;
    }

    if (orderEntry1 instanceof ExportableOrderEntry) {
      if (!(((ExportableOrderEntry)orderEntry1).isExported() == ((ExportableOrderEntry)orderEntry2).isExported())) {
        return false;
      }
      if (!(((ExportableOrderEntry)orderEntry1).getScope() == ((ExportableOrderEntry)orderEntry2).getScope())) {
        return false;
      }
    }

    return orderEntry1.isEquivalentTo(orderEntry2);
  }

  @NotNull
  public Collection<ContentEntry> getContent() {
    return myContent;
  }

  public Iterator<OrderEntry> getOrderIterator() {
    return Collections.unmodifiableList(myOrderEntries).iterator();
  }

  @Override
  @NotNull
  public Project getProject() {
    return getModule().getProject();
  }

  public boolean isChanged(@NotNull ModuleRootLayerImpl original) {
    return areExtensionsChanged(original) || areOrderEntriesChanged(original) || areContentEntriesChanged(original);
  }

  @NotNull
  @Override
  public Module getModule() {
    return myRootModel.getModule();
  }

  @Override
  public ContentEntry[] getContentEntries() {
    final Collection<ContentEntry> content = getContent();
    return content.toArray(new ContentEntry[content.size()]);
  }

  @Override
  public boolean iterateContentEntries(@NotNull Processor<ContentEntry> processor) {
    for (ContentEntry contentEntry : myContent) {
      if(!processor.process(contentEntry)) {
        return false;
      }
    }
    return true;
  }

  @NotNull
  @Override
  public OrderEntry[] getOrderEntries() {
    OrderEntry[] cachedOrderEntries = myCachedOrderEntries;
    if (cachedOrderEntries == null) {
      myCachedOrderEntries = cachedOrderEntries = myOrderEntries.toArray(new OrderEntry[myOrderEntries.size()]);
    }
    return cachedOrderEntries;
  }

  @NotNull
  @Override
  public VirtualFile[] getContentRoots() {
    final ArrayList<VirtualFile> result = new ArrayList<VirtualFile>();

    for (ContentEntry contentEntry : getContent()) {
      final VirtualFile file = contentEntry.getFile();
      if (file != null) {
        result.add(file);
      }
    }
    return VfsUtilCore.toVirtualFileArray(result);
  }

  @NotNull
  @Override
  public String[] getContentRootUrls() {
    if (getContent().isEmpty()) return ArrayUtil.EMPTY_STRING_ARRAY;
    final ArrayList<String> result = new ArrayList<String>(getContent().size());

    for (ContentEntry contentEntry : getContent()) {
      result.add(contentEntry.getUrl());
    }

    return ArrayUtil.toStringArray(result);
  }

  @NotNull
  @Override
  public String[] getContentFolderUrls(@NotNull Predicate<ContentFolderTypeProvider> predicate) {
    List<String> result = new SmartList<String>();
    for (ContentEntry contentEntry : getContent()) {
      Collections.addAll(result, contentEntry.getFolderUrls(predicate));
    }
    return ArrayUtil.toStringArray(result);
  }

  @NotNull
  @Override
  public VirtualFile[] getContentFolderFiles(@NotNull Predicate<ContentFolderTypeProvider> predicate) {
    List<VirtualFile> result = new SmartList<VirtualFile>();
    for (ContentEntry contentEntry : getContent()) {
      Collections.addAll(result, contentEntry.getFolderFiles(predicate));
    }
    return VfsUtilCore.toVirtualFileArray(result);
  }

  @NotNull
  @Override
  public ContentFolder[] getContentFolders(@NotNull Predicate<ContentFolderTypeProvider> predicate) {
    List<ContentFolder> result = new SmartList<ContentFolder>();
    for (ContentEntry contentEntry : getContent()) {
      Collections.addAll(result, contentEntry.getFolders(predicate));
    }
    return result.isEmpty() ? ContentFolder.EMPTY_ARRAY : result.toArray(new ContentFolder[result.size()]);
  }

  @NotNull
  @Override
  public VirtualFile[] getExcludeRoots() {
    final List<VirtualFile> result = new SmartList<VirtualFile>();
    for (ContentEntry contentEntry : getContent()) {
      Collections.addAll(result, contentEntry.getFolderFiles(ContentFolderScopes.excluded()));
    }
    return VfsUtilCore.toVirtualFileArray(result);
  }

  @NotNull
  @Override
  public String[] getExcludeRootUrls() {
    final List<String> result = new SmartList<String>();
    for (ContentEntry contentEntry : getContent()) {
      Collections.addAll(result, contentEntry.getFolderUrls(ContentFolderScopes.excluded()));
    }
    return ArrayUtil.toStringArray(result);
  }

  @NotNull
  @Override
  public VirtualFile[] getSourceRoots() {
    return getSourceRoots(true);
  }

  @NotNull
  @Override
  public VirtualFile[] getSourceRoots(boolean includingTests) {
    List<VirtualFile> result = new SmartList<VirtualFile>();
    for (ContentEntry contentEntry : getContent()) {
      Collections.addAll(result, includingTests
                                 ? contentEntry.getFolderFiles(ContentFolderScopes.productionAndTest())
                                 : contentEntry.getFolderFiles(ContentFolderScopes.production()));
    }
    return VfsUtilCore.toVirtualFileArray(result);
  }

  @NotNull
  @Override
  public String[] getSourceRootUrls() {
    return getSourceRootUrls(true);
  }

  @NotNull
  @Override
  public String[] getSourceRootUrls(boolean includingTests) {
    List<String> result = new SmartList<String>();
    for (ContentEntry contentEntry : getContent()) {
      Collections.addAll(result, includingTests
                                 ? contentEntry.getFolderUrls(ContentFolderScopes.productionAndTest())
                                 : contentEntry.getFolderUrls(ContentFolderScopes.production()));
    }
    return ArrayUtil.toStringArray(result);
  }

  @Override
  public <R> R processOrder(RootPolicy<R> policy, R initialValue) {
    R result = initialValue;
    for (OrderEntry orderEntry : getOrderEntries()) {
      result = orderEntry.accept(policy, result);
    }
    return result;
  }

  @NotNull
  @Override
  public OrderEnumerator orderEntries() {
    return new ModuleOrderEnumerator(this, null);
  }

  @NotNull
  @Override
  public String[] getDependencyModuleNames() {
    List<String> result =
            orderEntries().withoutSdk().withoutLibraries().withoutModuleSourceEntries().process(new CollectDependentModules(), new ArrayList<String>());
    return ArrayUtil.toStringArray(result);
  }

  @NotNull
  @Override
  public ModuleExtension[] getExtensions() {
    if (myExtensions.isEmpty()) {
      return ModuleExtension.EMPTY_ARRAY;
    }
    List<ModuleExtension> list = new ArrayList<ModuleExtension>(myExtensions.size());
    for (ModuleExtension<?> extension : myExtensions) {
      if (extension.isEnabled()) {
        list.add(extension);
      }
    }
    return list.toArray(new ModuleExtension[list.size()]);
  }

  @NotNull
  @Override
  public Module[] getModuleDependencies() {
    return getModuleDependencies(true);
  }

  @NotNull
  @Override
  public Module[] getModuleDependencies(boolean includeTests) {
    final List<Module> result = new ArrayList<Module>();

    for (OrderEntry entry : getOrderEntries()) {
      if (entry instanceof ModuleOrderEntry) {
        ModuleOrderEntry moduleOrderEntry = (ModuleOrderEntry)entry;
        final DependencyScope scope = moduleOrderEntry.getScope();
        if (!includeTests && !scope.isForProductionCompile() && !scope.isForProductionRuntime()) {
          continue;
        }
        final Module module1 = moduleOrderEntry.getModule();
        if (module1 != null) {
          result.add(module1);
        }
      }
    }

    return result.isEmpty() ? Module.EMPTY_ARRAY : ContainerUtil.toArray(result, new Module[result.size()]);
  }

  @Override
  @Nullable
  public <T extends ModuleExtension> T getExtension(Class<T> clazz) {
    for (ModuleExtension<?> extension : myExtensions) {
      if (extension.isEnabled() && clazz.isAssignableFrom(extension.getClass())) {
        //noinspection unchecked
        return (T)extension;
      }
    }
    return null;
  }

  @Override
  @Nullable
  public <T extends ModuleExtension> T getExtension(@NotNull String key) {
    for (ModuleExtension<?> extension : myExtensions) {
      if (extension.isEnabled() && Comparing.equal(extension.getId(), key)) {
        //noinspection unchecked
        return (T)extension;
      }
    }
    return null;
  }

  @Override
  @Nullable
  public <T extends ModuleExtension> T getExtensionWithoutCheck(Class<T> clazz) {
    for (ModuleExtension<?> extension : myExtensions) {
      if (clazz.isAssignableFrom(extension.getClass())) {
        //noinspection unchecked
        return (T)extension;
      }
    }
    return null;
  }

  @Override
  @Nullable
  public <T extends ModuleExtension> T getExtensionWithoutCheck(@NotNull String key) {
    for (ModuleExtension<?> extension : myExtensions) {
      if (Comparing.equal(extension.getId(), key)) {
        //noinspection unchecked
        return (T)extension;
      }
    }
    return null;
  }

  @NotNull
  @Override
  public ContentEntry addContentEntry(@NotNull VirtualFile file) {
    return addContentEntry(new ContentEntryImpl(file, this));
  }

  @NotNull
  @Override
  public ContentEntry addContentEntry(@NotNull String url) {
    return addContentEntry(new ContentEntryImpl(url, this));
  }

  @Override
  public void removeContentEntry(@NotNull ContentEntry entry) {
    ModuleRootLayerImpl.LOGGER.assertTrue(myContent.contains(entry));
    if (entry instanceof Disposable) {
      Disposer.dispose((Disposable)entry);
    }
    myContent.remove(entry);
  }

  @Override
  public void addOrderEntry(@NotNull OrderEntry entry) {
    ModuleRootLayerImpl.LOGGER.assertTrue(!myOrderEntries.contains(entry));
    myOrderEntries.add(entry);
  }

  @NotNull
  @Override
  public LibraryOrderEntry addLibraryEntry(@NotNull Library library) {
    final LibraryOrderEntry libraryOrderEntry = new LibraryOrderEntryImpl(library, this);
    assert libraryOrderEntry.isValid();
    myOrderEntries.add(libraryOrderEntry);
    return libraryOrderEntry;
  }

  @NotNull
  @Override
  public ModuleExtensionWithSdkOrderEntry addModuleExtensionSdkEntry(@NotNull ModuleExtensionWithSdk<?> moduleExtension) {
    final ModuleExtensionWithSdkOrderEntryImpl moduleSdkOrderEntry = new ModuleExtensionWithSdkOrderEntryImpl(moduleExtension.getId(), this);
    assert moduleSdkOrderEntry.isValid();

    // add module extension sdk entry after another SDK entry or before module source
    int sourcePosition = -1, sdkPosition = -1;
    for (int j = 0; j < myOrderEntries.size(); j++) {
      OrderEntry orderEntry = myOrderEntries.get(j);
      if (orderEntry instanceof ModuleSourceOrderEntry) {
        sourcePosition = j;
      }
      else if (orderEntry instanceof ModuleExtensionWithSdkOrderEntry) {
        sdkPosition = j;
      }
    }

    if (sdkPosition >= 0) {
      myOrderEntries.add(sdkPosition + 1, moduleSdkOrderEntry);
    }
    else if (sourcePosition >= 0) {
      myOrderEntries.add(sourcePosition, moduleSdkOrderEntry);
    }
    else {
      myOrderEntries.add(0, moduleSdkOrderEntry);
    }
    return moduleSdkOrderEntry;
  }

  @NotNull
  @Override
  public LibraryOrderEntry addInvalidLibrary(@NotNull @NonNls String name, @NotNull String level) {
    final LibraryOrderEntry libraryOrderEntry = new LibraryOrderEntryImpl(name, level, this);
    myOrderEntries.add(libraryOrderEntry);
    return libraryOrderEntry;
  }

  @NotNull
  @Override
  public ModuleOrderEntry addModuleOrderEntry(@NotNull Module module) {
    ModuleRootLayerImpl.LOGGER.assertTrue(!module.equals(getModule()));
    ModuleRootLayerImpl.LOGGER.assertTrue(Comparing.equal(myRootModel.getModule().getProject(), module.getProject()));
    final ModuleOrderEntryImpl moduleOrderEntry = new ModuleOrderEntryImpl(module, this);
    myOrderEntries.add(moduleOrderEntry);
    return moduleOrderEntry;
  }

  @NotNull
  @Override
  public ModuleOrderEntry addInvalidModuleEntry(@NotNull String name) {
    ModuleRootLayerImpl.LOGGER.assertTrue(!name.equals(getModule().getName()));
    final ModuleOrderEntryImpl moduleOrderEntry = new ModuleOrderEntryImpl(name, this);
    myOrderEntries.add(moduleOrderEntry);
    return moduleOrderEntry;
  }

  @Nullable
  @Override
  public LibraryOrderEntry findLibraryOrderEntry(@NotNull Library library) {
    for (OrderEntry orderEntry : getOrderEntries()) {
      if (orderEntry instanceof LibraryOrderEntry && library.equals(((LibraryOrderEntry)orderEntry).getLibrary())) {
        return (LibraryOrderEntry)orderEntry;
      }
    }
    return null;
  }

  @Nullable
  @Override
  public ModuleExtensionWithSdkOrderEntry findModuleExtensionSdkEntry(@NotNull ModuleExtension extension) {
    for (OrderEntry orderEntry : getOrderEntries()) {
      if (orderEntry instanceof ModuleExtensionWithSdkOrderEntry &&
          extension.getId().equals(((ModuleExtensionWithSdkOrderEntry)orderEntry).getModuleExtensionId())) {
        return (ModuleExtensionWithSdkOrderEntry)orderEntry;
      }
    }
    return null;
  }

  @Override
  public void removeOrderEntry(@NotNull OrderEntry orderEntry) {
    removeOrderEntryInternal(orderEntry);
  }

  private void removeOrderEntryInternal(OrderEntry entry) {
    ModuleRootLayerImpl.LOGGER.assertTrue(myOrderEntries.contains(entry));
    Disposer.dispose((OrderEntryBaseImpl)entry);
    myOrderEntries.remove(entry);
  }

  @Override
  public void rearrangeOrderEntries(@NotNull OrderEntry[] newEntries) {
    assertValidRearrangement(newEntries);
    myOrderEntries.clear();
    ContainerUtil.addAll(myOrderEntries, newEntries);
  }

  private void assertValidRearrangement(@NotNull OrderEntry[] newEntries) {
    String error = checkValidRearrangement(newEntries);
    ModuleRootLayerImpl.LOGGER.assertTrue(error == null, error);
  }

  @Nullable
  private String checkValidRearrangement(@NotNull OrderEntry[] newEntries) {
    if (newEntries.length != myOrderEntries.size()) {
      return "Size mismatch: old size=" + myOrderEntries.size() + "; new size=" + newEntries.length;
    }
    Set<OrderEntry> set = new HashSet<OrderEntry>();
    for (OrderEntry newEntry : newEntries) {
      if (!myOrderEntries.contains(newEntry)) {
        return "Trying to add nonexisting order entry " + newEntry;
      }

      if (set.contains(newEntry)) {
        return "Trying to add duplicate order entry " + newEntry;
      }
      set.add(newEntry);
    }
    return null;
  }

  public void removeAllContentEntries() {
    for (ContentEntry entry : myContent) {
      if (entry instanceof BaseModuleRootLayerChild) {
        Disposer.dispose((BaseModuleRootLayerChild)entry);
      }
    }
    myContent.clear();
  }


  public void removeAllOrderEntries() {
    for (OrderEntry entry : myOrderEntries) {
      Disposer.dispose((OrderEntryBaseImpl)entry);
    }
    myOrderEntries.clear();
  }

  @Override
  public void dispose() {
    removeAllContentEntries();
    removeAllOrderEntries();
    for (ModuleExtension<?> extension : myExtensions) {
      if(extension instanceof Disposable) {
        Disposer.dispose((Disposable)extension);
      }
    }
    myExtensions.clear();
  }

  @NotNull
  @Override
  public LibraryTable getModuleLibraryTable() {
    return myModuleLibraryTable;
  }

  @Override
  public <T extends OrderEntry> void replaceEntryOfType(Class<T> entryClass, T entry) {
    for (int i = 0; i < myOrderEntries.size(); i++) {
      OrderEntry orderEntry = myOrderEntries.get(i);
      if (entryClass.isInstance(orderEntry)) {
        myOrderEntries.remove(i);
        if (entry != null) {
          myOrderEntries.add(i, entry);
        }
        return;
      }
    }

    if (entry != null) {
      myOrderEntries.add(0, entry);
    }
  }
}

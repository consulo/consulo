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
package consulo.module.impl.internal.layer;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.content.ContentFolderTypeProvider;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.internal.ModuleRootLayerEx;
import consulo.module.content.internal.ProjectRootManagerImpl;
import consulo.module.content.internal.RootConfigurationAccessor;
import consulo.module.content.layer.*;
import consulo.module.content.layer.extension.ModuleExtensionBase;
import consulo.module.content.layer.orderEntry.*;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.ModuleExtensionWithSdk;
import consulo.module.extension.MutableModuleExtension;
import consulo.module.impl.internal.layer.library.ModuleLibraryTable;
import consulo.module.impl.internal.layer.orderEntry.*;
import consulo.project.Project;
import consulo.project.internal.UnknownFeaturesCollector;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.pointer.VirtualFilePointerListener;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;
import org.jdom.Element;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 29.07.14
 */
public class ModuleRootLayerImpl implements ModifiableModuleRootLayer, ModuleRootLayerEx, Disposable {
    private static final Logger LOG = Logger.getInstance(ModuleRootLayerImpl.class);

    private static class ContentComparator implements Comparator<ContentEntryEx> {
        public static final ContentComparator INSTANCE = new ContentComparator();

        @Override
        public int compare(ContentEntryEx o1, ContentEntryEx o2) {
            return o1.getUrl().compareTo(o2.getUrl());
        }
    }

    private static class CollectDependentModules extends RootPolicy<List<String>> {
        
        @Override
        public List<String> visitModuleOrderEntry(ModuleOrderEntry moduleOrderEntry, List<String> arrayList) {
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

        
        @Override
        public OrderEntry set(int i, OrderEntry orderEntry) {
            super.set(i, orderEntry);
            ((OrderEntryBaseImpl) orderEntry).setIndex(i);
            clearCachedEntries();
            return orderEntry;
        }

        @Override
        public boolean add(OrderEntry orderEntry) {
            super.add(orderEntry);
            ((OrderEntryBaseImpl) orderEntry).setIndex(size() - 1);
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
            if (index < 0) {
                return false;
            }
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
        public boolean removeAll(Collection<?> collection) {
            boolean result = super.removeAll(collection);
            setIndicies(0);
            clearCachedEntries();
            return result;
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
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
                ((OrderEntryBaseImpl) get(j)).setIndex(j);
            }
        }
    }

    private final Set<ContentEntryEx> myContent = new TreeSet<>(ContentComparator.INSTANCE);

    private final List<OrderEntry> myOrderEntries = new Order();
    // cleared by myOrderEntries modification, see Order
    private @Nullable OrderEntry[] myCachedOrderEntries;
    private Map<String, ModuleExtension> myExtensions = new HashMap<>();
    private final List<Element> myUnknownModuleExtensions = new SmartList<>();
    private RootModelImpl myRootModel;
    
    private final ModuleLibraryTable myModuleLibraryTable;

    private boolean myDisposed;

    @RequiredReadAction
    public ModuleRootLayerImpl(@Nullable ModuleRootLayerImpl originalLayer, RootModelImpl rootModel) {
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

    public void loadState(Element element, @Nullable ProgressIndicator progressIndicator) {
        removeAllContentEntries();
        removeAllOrderEntries();

        List<Element> children = element.getChildren();
        int i = 0;
        boolean moduleSourceAdded = false;
        for (Element child : children) {
            i++;

            if (i % 10 == 0) {
                if (progressIndicator != null) {
                    progressIndicator.checkCanceled();
                }
            }

            String name = child.getName();
            switch (name) {
                case ModuleExtensionBase.ELEMENT_NAME:
                    String id = child.getAttributeValue("id");

                    ModuleExtensionProvider provider = ModuleExtensionProvider.findProvider(id);
                    if (provider != null) {
                        ModuleExtension<?> moduleExtension = getExtensionWithoutCheck(id);
                        assert moduleExtension != null;
                        moduleExtension.loadState(child);
                    }
                    else {
                        UnknownFeaturesCollector.getInstance(getProject()).registerUnknownFeature(ModuleExtensionProvider.class, id);

                        myUnknownModuleExtensions.add(child.clone());
                    }
                    break;
                case ContentEntryImpl.ELEMENT_NAME:
                    boolean canUseOptimizedVersion = getModule().getModuleDirUrl() == null && child.getContentSize() == 0;
                    ContentEntryEx contentEntry;
                    if (canUseOptimizedVersion) {
                        contentEntry = new OptimizedSingleContentEntryImpl(child, this);
                    }
                    else {
                        contentEntry = new ContentEntryImpl(child, this);
                    }
                    myContent.add(contentEntry);
                    break;
                case OrderEntrySerializationUtil.ORDER_ENTRY_ELEMENT_NAME:
                    OrderEntry orderEntry = OrderEntrySerializationUtil.loadOrderEntry(child, this);
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
                    break;
            }
        }

        if (!moduleSourceAdded) {
            myOrderEntries.add(new ModuleSourceOrderEntryImpl(this));
        }
    }

    public void writeExternal(Element element) {
        List<Element> moduleExtensionElements = new ArrayList<>();
        for (ModuleExtension<?> extension : myExtensions.values()) {
            Element state = extension.getState();
            if (state == null) {
                continue;
            }
            moduleExtensionElements.add(state);
        }

        for (Element unknownModuleExtension : myUnknownModuleExtensions) {
            moduleExtensionElements.add(unknownModuleExtension.clone());
        }
        Collections.sort(moduleExtensionElements, (o1, o2) -> Comparing.compare(o1.getAttributeValue("id"), o2.getAttributeValue("id")));

        element.addContent(moduleExtensionElements);

        for (ContentEntry contentEntry : getContent()) {
            Element subElement = new Element(ContentEntryImpl.ELEMENT_NAME);
            if (contentEntry instanceof ContentEntryImpl) {
                ((ContentEntryImpl) contentEntry).writeExternal(subElement);
            }
            else if (contentEntry instanceof OptimizedSingleContentEntryImpl) {
                ((OptimizedSingleContentEntryImpl) contentEntry).writeExternal(subElement);
            }
            element.addContent(subElement);
        }

        for (OrderEntry orderEntry : getOrderEntries()) {
            Element newElement = OrderEntrySerializationUtil.storeOrderEntry(orderEntry);
            element.addContent(newElement);
        }
    }

    @SuppressWarnings("unchecked")
    @RequiredReadAction
    public void copy(ModuleRootLayerImpl toSet) {

        for (ModuleExtension extension : myExtensions.values()) {
            MutableModuleExtension mutableExtension = (MutableModuleExtension) extension;

            ModuleExtension originalExtension = toSet.getExtensionWithoutCheck(extension.getId());
            assert originalExtension != null;
            if (mutableExtension.isModified(originalExtension)) {
                originalExtension.commit(mutableExtension);
            }
        }

        if (areOrderEntriesChanged(toSet)) {
            toSet.setOrderEntriesFrom(this);
        }

        if (areContentEntriesChanged(toSet)) {
            toSet.setContentEntriesFrom(this);
        }

        toSet.myUnknownModuleExtensions.addAll(myUnknownModuleExtensions);
    }

    @SuppressWarnings("unchecked")
    @RequiredReadAction
    public void createMutableExtensions(@Nullable ModuleRootLayerImpl layer) {
        myExtensions.clear();

        List<ModuleExtensionProvider> providers = Application.get().getExtensionPoint(ModuleExtensionProvider.class).getExtensionList();
        for (ModuleExtensionProvider provider : providers) {
            MutableModuleExtension mutable = provider.createMutableExtension(this);

            if (layer != null) {
                ModuleExtension<?> originalExtension = layer.getExtensionWithoutCheck(provider.getId());
                assert originalExtension != null;
                mutable.commit(originalExtension);
            }

            myExtensions.put(provider.getId(), mutable);
        }
    }

    private void setOrderEntriesFrom(ModuleRootLayerImpl layer) {
        removeAllOrderEntries();
        for (OrderEntry orderEntry : layer.myOrderEntries) {
            if (orderEntry instanceof ClonableOrderEntry) {
                myOrderEntries.add(((ClonableOrderEntry) orderEntry).cloneEntry(this));
            }
        }
    }

    private void setContentEntriesFrom(ModuleRootLayerImpl layer) {
        removeAllContentEntries();
        for (ContentEntryEx contentEntry : layer.myContent) {
            myContent.add(contentEntry.cloneEntry(this));
        }
    }


    @RequiredReadAction
    public void init() {
        removeAllOrderEntries();
        removeAllExtensions();

        addSourceOrderEntries();
        createMutableExtensions(null);
    }

    public void addSourceOrderEntries() {
        myOrderEntries.add(new ModuleSourceOrderEntryImpl(this));
    }

    
    private ContentEntry addContentEntry(ContentEntryEx e) {
        checkDisposed();

        if (myContent.contains(e)) {
            for (ContentEntryEx contentEntry : getContentEntries()) {
                if (ContentComparator.INSTANCE.compare(contentEntry, e) == 0) {
                    return contentEntry;
                }
            }
        }
        myContent.add(e);
        return e;
    }

    public boolean areContentEntriesChanged(ModuleRootLayerImpl original) {
        return ArrayUtil.lexicographicCompare(getContentEntries(), original.getContentEntries()) != 0;
    }

    public boolean areOrderEntriesChanged(ModuleRootLayerImpl original) {
        OrderEntry[] orderEntries = getOrderEntries();
        OrderEntry[] sourceOrderEntries = original.getOrderEntries();
        if (orderEntries.length != sourceOrderEntries.length) {
            return true;
        }
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

    @SuppressWarnings("unchecked")
    public boolean areExtensionsChanged(ModuleRootLayerImpl original) {
        List<ModuleExtensionProvider> extensions = Application.get().getExtensionPoint(ModuleExtensionProvider.class).getExtensionList();
        for (ModuleExtensionProvider extensionProvider : extensions) {
            MutableModuleExtension selfExtension = getExtensionWithoutCheck(extensionProvider.getId());
            ModuleExtension originalExtension = original.getExtensionWithoutCheck(extensionProvider.getId());

            if (selfExtension == null || originalExtension == null) {
                continue;
            }

            if (selfExtension.isModified(originalExtension)) {
                return true;
            }
        }
        return false;
    }

    private static boolean orderEntriesEquals(OrderEntry orderEntry1, OrderEntry orderEntry2) {
        if (orderEntry1.getClass() != orderEntry2.getClass()) {
            return false;
        }

        if (orderEntry1 instanceof ExportableOrderEntry) {
            if (!(((ExportableOrderEntry) orderEntry1).isExported() == ((ExportableOrderEntry) orderEntry2).isExported())) {
                return false;
            }
            if (!(((ExportableOrderEntry) orderEntry1).getScope() == ((ExportableOrderEntry) orderEntry2).getScope())) {
                return false;
            }
        }

        return orderEntry1.isEquivalentTo(orderEntry2);
    }

    
    public Collection<ContentEntryEx> getContent() {
        return myContent;
    }

    public Iterator<OrderEntry> getOrderIterator() {
        return Collections.unmodifiableList(myOrderEntries).iterator();
    }

    @Override
    
    public Project getProject() {
        return getModule().getProject();
    }

    public boolean isChanged(ModuleRootLayerImpl original) {
        return areExtensionsChanged(original) || areOrderEntriesChanged(original) || areContentEntriesChanged(original);
    }

    
    @Override
    public Module getModule() {
        return myRootModel.getModule();
    }

    @Override
    public ContentEntryEx[] getContentEntries() {
        Collection<ContentEntryEx> content = getContent();
        return content.toArray(new ContentEntryEx[content.size()]);
    }

    @Override
    public boolean iterateContentEntries(Predicate<ContentEntry> processor) {
        for (ContentEntry contentEntry : myContent) {
            if (!processor.test(contentEntry)) {
                return false;
            }
        }
        return true;
    }

    
    @Override
    public OrderEntry[] getOrderEntries() {
        OrderEntry[] cachedOrderEntries = myCachedOrderEntries;
        if (cachedOrderEntries == null) {
            myCachedOrderEntries = cachedOrderEntries = myOrderEntries.toArray(new OrderEntry[myOrderEntries.size()]);
        }
        return cachedOrderEntries;
    }

    
    @Override
    public VirtualFile[] getContentRoots() {
        ArrayList<VirtualFile> result = new ArrayList<>();

        for (ContentEntry contentEntry : getContent()) {
            VirtualFile file = contentEntry.getFile();
            if (file != null) {
                result.add(file);
            }
        }
        return VirtualFileUtil.toVirtualFileArray(result);
    }

    
    @Override
    public String[] getContentRootUrls() {
        if (getContent().isEmpty()) {
            return ArrayUtil.EMPTY_STRING_ARRAY;
        }
        ArrayList<String> result = new ArrayList<>(getContent().size());

        for (ContentEntry contentEntry : getContent()) {
            result.add(contentEntry.getUrl());
        }

        return ArrayUtil.toStringArray(result);
    }

    
    @Override
    public String[] getContentFolderUrls(Predicate<ContentFolderTypeProvider> predicate) {
        List<String> result = new SmartList<>();
        for (ContentEntry contentEntry : getContent()) {
            Collections.addAll(result, contentEntry.getFolderUrls(predicate));
        }
        return ArrayUtil.toStringArray(result);
    }

    
    @Override
    public VirtualFile[] getContentFolderFiles(Predicate<ContentFolderTypeProvider> predicate) {
        List<VirtualFile> result = new SmartList<>();
        for (ContentEntry contentEntry : getContent()) {
            Collections.addAll(result, contentEntry.getFolderFiles(predicate));
        }
        return VirtualFileUtil.toVirtualFileArray(result);
    }

    
    @Override
    public ContentFolder[] getContentFolders(Predicate<ContentFolderTypeProvider> predicate) {
        List<ContentFolder> result = new SmartList<>();
        for (ContentEntry contentEntry : getContent()) {
            Collections.addAll(result, contentEntry.getFolders(predicate));
        }
        return result.isEmpty() ? ContentFolder.EMPTY_ARRAY : result.toArray(new ContentFolder[result.size()]);
    }

    
    @Override
    public VirtualFile[] getExcludeRoots() {
        List<VirtualFile> result = new SmartList<>();
        for (ContentEntry contentEntry : getContent()) {
            Collections.addAll(result, contentEntry.getFolderFiles(LanguageContentFolderScopes.excluded()));
        }
        return VirtualFileUtil.toVirtualFileArray(result);
    }

    
    @Override
    public String[] getExcludeRootUrls() {
        List<String> result = new SmartList<>();
        for (ContentEntry contentEntry : getContent()) {
            Collections.addAll(result, contentEntry.getFolderUrls(LanguageContentFolderScopes.excluded()));
        }
        return ArrayUtil.toStringArray(result);
    }

    
    @Override
    public VirtualFile[] getSourceRoots() {
        return getSourceRoots(true);
    }

    
    @Override
    public VirtualFile[] getSourceRoots(boolean includingTests) {
        List<VirtualFile> result = new SmartList<>();
        for (ContentEntry contentEntry : getContent()) {
            Collections.addAll(result, includingTests ? contentEntry.getFolderFiles(LanguageContentFolderScopes.productionAndTest()) : contentEntry.getFolderFiles(LanguageContentFolderScopes.production()));
        }
        return VirtualFileUtil.toVirtualFileArray(result);
    }

    
    @Override
    public String[] getSourceRootUrls() {
        return getSourceRootUrls(true);
    }

    
    @Override
    public String[] getSourceRootUrls(boolean includingTests) {
        List<String> result = new SmartList<>();
        for (ContentEntry contentEntry : getContent()) {
            Collections.addAll(result, includingTests ? contentEntry.getFolderUrls(LanguageContentFolderScopes.productionAndTest()) : contentEntry.getFolderUrls(LanguageContentFolderScopes.production()));
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

    
    @Override
    public OrderEnumerator orderEntries() {
        return new ModuleOrderEnumerator(this, null);
    }

    
    @Override
    public String[] getDependencyModuleNames() {
        List<String> result = orderEntries().withoutSdk().withoutLibraries().withoutModuleSourceEntries().process(new CollectDependentModules(), new ArrayList<>());
        return ArrayUtil.toStringArray(result);
    }

    
    @Override
    public List<ModuleExtension> getExtensions() {
        if (myExtensions.isEmpty()) {
            return Collections.emptyList();
        }
        List<ModuleExtension> list = new ArrayList<>(myExtensions.size());
        for (ModuleExtension<?> extension : myExtensions.values()) {
            if (extension.isEnabled()) {
                list.add(extension);
            }
        }
        return list;
    }

    
    @Override
    public Module[] getModuleDependencies() {
        return getModuleDependencies(true);
    }

    
    @Override
    public Module[] getModuleDependencies(boolean includeTests) {
        List<Module> result = new ArrayList<>();

        for (OrderEntry entry : getOrderEntries()) {
            if (entry instanceof ModuleOrderEntry) {
                ModuleOrderEntry moduleOrderEntry = (ModuleOrderEntry) entry;
                DependencyScope scope = moduleOrderEntry.getScope();
                if (!includeTests && !scope.isForProductionCompile() && !scope.isForProductionRuntime()) {
                    continue;
                }
                Module module1 = moduleOrderEntry.getModule();
                if (module1 != null) {
                    result.add(module1);
                }
            }
        }

        return result.isEmpty() ? Module.EMPTY_ARRAY : ContainerUtil.toArray(result, Module.ARRAY_FACTORY);
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends ModuleExtension> T getExtension(Class<T> clazz) {
        checkDisposed();

        if (myExtensions.isEmpty()) {
            return null;
        }

        for (ModuleExtension extension : myExtensions.values()) {
            if (extension.isEnabled() && clazz.isInstance(extension)) {
                return (T) extension;
            }
        }
        return null;
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends ModuleExtension> T getExtensionWithoutCheck(Class<T> clazz) {
        checkDisposed();

        if (myExtensions.isEmpty()) {
            return null;
        }

        for (ModuleExtension extension : myExtensions.values()) {
            if (clazz.isInstance(extension)) {
                return (T) extension;
            }
        }
        return null;
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends ModuleExtension> T getExtension(String key) {
        checkDisposed();

        ModuleExtension extension = myExtensions.get(key);
        return extension != null && extension.isEnabled() ? (T) extension : null;
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends ModuleExtension> T getExtensionWithoutCheck(String key) {
        checkDisposed();

        ModuleExtension extension = myExtensions.get(key);
        return (T) extension;
    }

    
    @Override
    public ContentEntry addContentEntry(VirtualFile file) {
        return addContentEntry(new ContentEntryImpl(file, this));
    }

    
    @Override
    public ContentEntry addContentEntry(String url) {
        return addContentEntry(new ContentEntryImpl(url, this));
    }

    
    @Override
    public ContentEntry addSingleContentEntry(VirtualFile file) {
        return addContentEntry(new OptimizedSingleContentEntryImpl(file, this));
    }

    
    @Override
    public ContentEntry addSingleContentEntry(String url) {
        return addContentEntry(new OptimizedSingleContentEntryImpl(url, this));
    }

    @Override
    public void removeContentEntry(ContentEntry entry) {
        checkDisposed();

        LOG.assertTrue(myContent.contains(entry));
        if (entry instanceof Disposable) {
            Disposer.dispose((Disposable) entry);
        }
        myContent.remove(entry);
    }

    @Override
    public void addOrderEntry(OrderEntry entry) {
        checkDisposed();

        LOG.assertTrue(!myOrderEntries.contains(entry));
        myOrderEntries.add(entry);
    }

    
    @Override
    public LibraryOrderEntry addLibraryEntry(Library library) {
        LibraryOrderEntry libraryOrderEntry = new LibraryOrderEntryImpl(library, this);
        assert libraryOrderEntry.isValid();
        myOrderEntries.add(libraryOrderEntry);
        return libraryOrderEntry;
    }

    
    @Override
    public ModuleExtensionWithSdkOrderEntry addModuleExtensionSdkEntry(ModuleExtensionWithSdk<?> moduleExtension) {
        checkDisposed();

        ModuleExtensionWithSdkOrderEntryImpl moduleSdkOrderEntry = new ModuleExtensionWithSdkOrderEntryImpl(moduleExtension.getId(), this);
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

    
    @Override
    public LibraryOrderEntry addInvalidLibrary(String name, String level) {
        checkDisposed();

        LibraryOrderEntry libraryOrderEntry = new LibraryOrderEntryImpl(name, level, this);
        myOrderEntries.add(libraryOrderEntry);
        return libraryOrderEntry;
    }

    
    @Override
    public ModuleOrderEntry addModuleOrderEntry(Module module) {
        checkDisposed();

        LOG.assertTrue(!module.equals(getModule()));
        LOG.assertTrue(Comparing.equal(myRootModel.getModule().getProject(), module.getProject()));
        ModuleOrderEntryImpl moduleOrderEntry = new ModuleOrderEntryImpl(module, this);
        myOrderEntries.add(moduleOrderEntry);
        return moduleOrderEntry;
    }

    
    @Override
    @SuppressWarnings("unchecked")
    public <M extends CustomOrderEntryModel> CustomOrderEntry<M> addCustomOderEntry(CustomOrderEntryTypeProvider<M> type, M model) {
        checkDisposed();

        OrderEntryType<?> entryType = OrderEntrySerializationUtil.findOrderEntryType(type.getId());
        CustomOrderEntryTypeProvider otherProvider = entryType instanceof CustomOrderEntryTypeWrapper ? ((CustomOrderEntryTypeWrapper) entryType).getCustomOrderEntryTypeProvider() : null;
        if (otherProvider != type) {
            throw new IllegalArgumentException("Type is not registered: " + type.getId());
        }
        model.bind(this);
        CustomOrderEntryImpl<M> entry = new CustomOrderEntryImpl<>(entryType, this, model, true);
        myOrderEntries.add(entry);
        return entry;
    }

    
    @Override
    public ModuleOrderEntry addInvalidModuleEntry(String name) {
        checkDisposed();

        LOG.assertTrue(!name.equals(getModule().getName()));
        ModuleOrderEntryImpl moduleOrderEntry = new ModuleOrderEntryImpl(name, this);
        myOrderEntries.add(moduleOrderEntry);
        return moduleOrderEntry;
    }

    @Nullable
    @Override
    public LibraryOrderEntry findLibraryOrderEntry(Library library) {
        for (OrderEntry orderEntry : getOrderEntries()) {
            if (orderEntry instanceof LibraryOrderEntry && library.equals(((LibraryOrderEntry) orderEntry).getLibrary())) {
                return (LibraryOrderEntry) orderEntry;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public ModuleExtensionWithSdkOrderEntry findModuleExtensionSdkEntry(ModuleExtension extension) {
        for (OrderEntry orderEntry : getOrderEntries()) {
            if (orderEntry instanceof ModuleExtensionWithSdkOrderEntry && extension.getId().equals(((ModuleExtensionWithSdkOrderEntry) orderEntry).getModuleExtensionId())) {
                return (ModuleExtensionWithSdkOrderEntry) orderEntry;
            }
        }
        return null;
    }

    @Override
    public void removeOrderEntry(OrderEntry orderEntry) {
        removeOrderEntryInternal(orderEntry);
    }

    private void removeOrderEntryInternal(OrderEntry entry) {
        checkDisposed();

        LOG.assertTrue(myOrderEntries.contains(entry));
        Disposer.dispose((OrderEntryBaseImpl) entry);
        myOrderEntries.remove(entry);
    }

    @Override
    public void rearrangeOrderEntries(OrderEntry[] newEntries) {
        checkDisposed();

        assertValidRearrangement(newEntries);
        myOrderEntries.clear();
        ContainerUtil.addAll(myOrderEntries, newEntries);
    }

    private void assertValidRearrangement(OrderEntry[] newEntries) {
        String error = checkValidRearrangement(newEntries);
        LOG.assertTrue(error == null, error);
    }

    private @Nullable String checkValidRearrangement(OrderEntry[] newEntries) {
        if (newEntries.length != myOrderEntries.size()) {
            return "Size mismatch: old size=" + myOrderEntries.size() + "; new size=" + newEntries.length;
        }
        Set<OrderEntry> set = new HashSet<>();
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
            BaseModuleRootLayerChild implEntry = (BaseModuleRootLayerChild) entry;
            Disposer.dispose(implEntry);

            if (!implEntry.isDisposed()) {
                LOG.error(implEntry + " is not disposed");
            }
        }
        myContent.clear();
    }


    public void removeAllOrderEntries() {
        for (OrderEntry entry : myOrderEntries) {
            OrderEntryBaseImpl implOrderEntry = (OrderEntryBaseImpl) entry;
            Disposer.dispose(implOrderEntry);

            if (!implOrderEntry.isDisposed()) {
                LOG.error(implOrderEntry + " is not disposed");
            }
        }
        myOrderEntries.clear();
    }

    private void removeAllExtensions() {
        for (ModuleExtension<?> extension : myExtensions.values()) {
            if (extension instanceof Disposable) {
                Disposer.dispose((Disposable) extension);
            }
        }
        myExtensions.clear();
    }

    @Override
    public void dispose() {
        removeAllContentEntries();
        removeAllOrderEntries();
        removeAllExtensions();
        myDisposed = true;
    }

    private void checkDisposed() {
        if (myDisposed) {
            throw new IllegalArgumentException("Already disposed");
        }
    }

    
    @Override
    public LibraryTable getModuleLibraryTable() {
        return myModuleLibraryTable;
    }

    @Override
    public <T extends OrderEntry> void replaceEntryOfType(Class<T> entryClass, T entry) {
        checkDisposed();

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

    @Override
    public RootConfigurationAccessor getConfigurationAccessor() {
        return myRootModel.getConfigurationAccessor();
    }

    
    public VirtualFilePointerListener getRootsChangedListener() {
        return ProjectRootManagerImpl.getInstanceImpl(getProject()).getRootsValidityChangedListener();
    }
}

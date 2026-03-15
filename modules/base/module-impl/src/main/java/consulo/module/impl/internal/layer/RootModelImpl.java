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

package consulo.module.impl.internal.layer;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.application.progress.ProgressIndicator;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.internal.RootConfigurationAccessor;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableModuleRootLayer;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.orderEntry.*;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.ModuleExtensionWithSdk;
import consulo.module.impl.internal.ModuleRootManagerImpl;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;
import org.jdom.Element;

import java.util.*;

/**
 * @author dsl
 */
public class RootModelImpl extends RootModelBase implements ModifiableRootModel {
    private static final Logger LOG = Logger.getInstance(RootModelImpl.class);

    public final ModuleRootManagerImpl myModuleRootManager;
    private boolean myWritable;
    private boolean myDisposed;

    private final RootConfigurationAccessor myConfigurationAccessor;

    private String myCurrentLayerName;
    private ModuleRootLayerImpl myCachedCurrentLayer;
    private final SortedMap<String, ModuleRootLayerImpl> myLayers = new TreeMap<>();

    @RequiredReadAction
    public RootModelImpl(ModuleRootManagerImpl moduleRootManager) {
        myModuleRootManager = moduleRootManager;
        myWritable = false;
        myConfigurationAccessor = new RootConfigurationAccessor();

        initDefaultLayer(null);
    }

    @RequiredReadAction
    public RootModelImpl(Element element, @Nullable ProgressIndicator progressIndicator, ModuleRootManagerImpl moduleRootManager, boolean writable) {
        myModuleRootManager = moduleRootManager;

        myConfigurationAccessor = new RootConfigurationAccessor();

        loadState(element, progressIndicator);

        myWritable = writable;
    }

    //creates modifiable model
    @RequiredReadAction
    public RootModelImpl(RootModelImpl rootModel, ModuleRootManagerImpl moduleRootManager, RootConfigurationAccessor rootConfigurationAccessor) {
        myModuleRootManager = moduleRootManager;
        myWritable = true;
        myConfigurationAccessor = rootConfigurationAccessor;

        myLayers.clear();
        for (Map.Entry<String, ModuleRootLayerImpl> entry : rootModel.myLayers.entrySet()) {
            ModuleRootLayerImpl moduleRootLayer = new ModuleRootLayerImpl(entry.getValue(), this);
            myLayers.put(entry.getKey(), moduleRootLayer);
        }
        setCurrentLayerSafe(rootModel.myCurrentLayerName);
    }

    @RequiredReadAction
    private void initDefaultLayer(Element element) {
        setCurrentLayerSafe(DEFAULT_LAYER_NAME);

        ModuleRootLayerImpl moduleRootLayer = new ModuleRootLayerImpl(null, this);
        myLayers.put(myCurrentLayerName, moduleRootLayer);

        if (element != null) {
            moduleRootLayer.loadState(element, null);
        }
    }

    @RequiredReadAction
    private void loadState(Element element, @Nullable ProgressIndicator progressIndicator) {
        String currentLayer = element.getAttributeValue("current-layer");
        if (currentLayer != null) {
            setCurrentLayerSafe(currentLayer);

            for (Element moduleLayerElement : element.getChildren("module-layer")) {
                String name = moduleLayerElement.getAttributeValue("name");

                ModuleRootLayerImpl moduleRootLayer = new ModuleRootLayerImpl(null, this);
                moduleRootLayer.loadState(moduleLayerElement, progressIndicator);

                ModuleRootLayerImpl oldLayer = myLayers.put(name, moduleRootLayer);
                if (oldLayer != null) {
                    // dispose old layout
                    Disposer.dispose(oldLayer);
                }
            }
        }

        // old format - create default profile and load it
        if (myLayers.isEmpty()) {
            initDefaultLayer(element);
        }
    }

    public void putState(Element parent) {
        parent.setAttribute("current-layer", myCurrentLayerName);

        for (Map.Entry<String, ModuleRootLayerImpl> entry : myLayers.entrySet()) {
            Element element = new Element("module-layer");
            element.setAttribute("name", entry.getKey());

            entry.getValue().writeExternal(element);

            parent.addContent(element);
        }
    }

    @Override
    public boolean isWritable() {
        return myWritable;
    }

    
    public RootConfigurationAccessor getConfigurationAccessor() {
        return myConfigurationAccessor;
    }

    @Override
    public void removeContentEntry(ContentEntry entry) {
        assertWritable();
        getCurrentLayer().removeContentEntry(entry);
    }

    @Override
    public void addOrderEntry(OrderEntry entry) {
        assertWritable();
        getCurrentLayer().addOrderEntry(entry);
    }

    
    @Override
    public LibraryOrderEntry addLibraryEntry(Library library) {
        assertWritable();
        return getCurrentLayer().addLibraryEntry(library);
    }

    
    @Override
    public ModuleExtensionWithSdkOrderEntry addModuleExtensionSdkEntry(ModuleExtensionWithSdk<?> moduleExtension) {
        assertWritable();
        return getCurrentLayer().addModuleExtensionSdkEntry(moduleExtension);
    }

    
    @Override
    public LibraryOrderEntry addInvalidLibrary(String name, String level) {
        assertWritable();
        return getCurrentLayer().addInvalidLibrary(name, level);
    }

    
    @Override
    public ModuleOrderEntry addModuleOrderEntry(Module module) {
        assertWritable();
        return getCurrentLayer().addModuleOrderEntry(module);
    }

    
    @Override
    public <M extends CustomOrderEntryModel> CustomOrderEntry<M> addCustomOderEntry(CustomOrderEntryTypeProvider<M> type, M model) {
        assertWritable();
        return getCurrentLayer().addCustomOderEntry(type, model);
    }

    
    @Override
    public ModuleOrderEntry addInvalidModuleEntry(String name) {
        assertWritable();
        return getCurrentLayer().addInvalidModuleEntry(name);
    }

    @Nullable
    @Override
    public LibraryOrderEntry findLibraryOrderEntry(Library library) {
        return getCurrentLayer().findLibraryOrderEntry(library);
    }

    @Override
    public ModuleExtensionWithSdkOrderEntry findModuleExtensionSdkEntry(ModuleExtension extension) {
        return getCurrentLayer().findModuleExtensionSdkEntry(extension);
    }

    @Override
    public void removeOrderEntry(OrderEntry entry) {
        assertWritable();
        getCurrentLayer().removeOrderEntry(entry);
    }

    @Override
    public void rearrangeOrderEntries(OrderEntry[] newEntries) {
        assertWritable();
        getCurrentLayer().rearrangeOrderEntries(newEntries);
    }

    @Override
    public void clear() {
        disposeLayers();

        initDefaultLayer(null);
    }

    private void disposeLayers() {
        for (ModuleRootLayerImpl moduleRootLayer : myLayers.values()) {
            Disposer.dispose(moduleRootLayer);
        }
        myLayers.clear();
    }

    @Override
    @RequiredWriteAction
    public void commit() {
        myModuleRootManager.commitModel(this);
        myWritable = false;
    }

    @RequiredReadAction
    public void doCommitAndDispose() {
        assert isWritable();

        RootModelImpl sourceModel = getSourceModel();
        if (!Comparing.equal(sourceModel.myCurrentLayerName, myCurrentLayerName)) {
            sourceModel.setCurrentLayerSafe(myCurrentLayerName);
        }

        // first we commit changed and new layers
        for (Map.Entry<String, ModuleRootLayerImpl> entry : myLayers.entrySet()) {
            String layerName = entry.getKey();
            ModuleRootLayerImpl sourceModuleLayer = sourceModel.myLayers.get(layerName);

            ModuleRootLayerImpl toCommit = sourceModuleLayer;
            // if layer exists
            if (toCommit == null) {
                toCommit = new ModuleRootLayerImpl(null, sourceModel);
                ModuleRootLayerImpl layer = sourceModel.myLayers.put(layerName, toCommit);
                if (layer != null) {
                    Disposer.dispose(layer);
                }
            }

            entry.getValue().copy(toCommit);
        }

        List<String> toRemove = new SmartList<>();
        // second remove non existed layers
        for (String layerName : sourceModel.myLayers.keySet()) {
            ModuleRootLayerImpl moduleRootLayer = myLayers.get(layerName);
            if (moduleRootLayer == null) {
                toRemove.add(layerName);
            }
        }

        for (String layerName : toRemove) {
            ModuleRootLayerImpl removed = sourceModel.myLayers.remove(layerName);
            assert removed != null;
            Disposer.dispose(removed);
        }

        dispose();
    }

    @Override
    
    public LibraryTable getModuleLibraryTable() {
        return getCurrentLayer().getModuleLibraryTable();
    }

    
    @Override
    public Project getProject() {
        return myModuleRootManager.getModule().getProject();
    }

    @Override
    
    public ContentEntry addContentEntry(VirtualFile file) {
        return getCurrentLayer().addContentEntry(file);
    }

    @Override
    
    public ContentEntry addContentEntry(String url) {
        return getCurrentLayer().addContentEntry(url);
    }

    @Override
    
    public ContentEntry addSingleContentEntry(VirtualFile file) {
        return getCurrentLayer().addSingleContentEntry(file);
    }

    @Override
    
    public ContentEntry addSingleContentEntry(String url) {
        return getCurrentLayer().addSingleContentEntry(url);
    }

    @Override
    public boolean isDisposed() {
        return myDisposed;
    }

    @Override
    public <T extends OrderEntry> void replaceEntryOfType(Class<T> entryClass, @Nullable T entry) {
        assertWritable();
        getCurrentLayer().replaceEntryOfType(entryClass, entry);
    }

    public void assertWritable() {
        LOG.assertTrue(myWritable);
    }

    public boolean isDependsOn(Module module) {
        for (OrderEntry entry : getOrderEntries()) {
            if (entry instanceof ModuleOrderEntry) {
                Module module1 = ((ModuleOrderEntry) entry).getModule();
                if (module1 == module) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    
    public Module getModule() {
        return myModuleRootManager.getModule();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isChanged() {
        if (!myWritable) {
            return false;
        }

        RootModelImpl sourceModel = getSourceModel();

        if (!Comparing.equal(myCurrentLayerName, sourceModel.myCurrentLayerName)) {
            return true;
        }

        for (Map.Entry<String, ModuleRootLayerImpl> entry : myLayers.entrySet()) {
            ModuleRootLayerImpl sourceLayer = sourceModel.myLayers.get(entry.getKey());
            // new layer
            if (sourceLayer == null) {
                return true;
            }

            if (entry.getValue().isChanged(sourceLayer)) {
                return true;
            }
        }
        // check for deleted layers
        for (String layerName : sourceModel.myLayers.keySet()) {
            ModuleRootLayerImpl layer = myLayers.get(layerName);
            if (layer == null) {
                return true;
            }
        }
        return false;
    }

    public void makeExternalChange(Runnable runnable) {
        if (myWritable || myDisposed) {
            return;
        }
        myModuleRootManager.makeRootsChange(runnable);
    }

    @Override
    public void dispose() {
        assert !myDisposed;
        disposeLayers();
        myWritable = false;
        myDisposed = true;
    }

    private RootModelImpl getSourceModel() {
        assertWritable();
        return myModuleRootManager.getRootModel();
    }

    
    @Override
    public ModuleRootLayerImpl getCurrentLayer() {
        if (myCachedCurrentLayer != null) {
            return myCachedCurrentLayer;
        }
        ModuleRootLayerImpl moduleRootLayer = myLayers.get(myCurrentLayerName);
        LOG.assertTrue(moduleRootLayer != null);
        return myCachedCurrentLayer = moduleRootLayer;
    }

    
    @Override
    public ModifiableModuleRootLayer addLayer(String name, @Nullable String nameForCopy, boolean activate) {
        ModuleRootLayerImpl moduleRootLayer = myLayers.get(name);
        if (moduleRootLayer != null) {
            return moduleRootLayer;
        }

        ModuleRootLayerImpl layer = new ModuleRootLayerImpl(null, this);

        if (nameForCopy != null) {
            ModuleRootLayerImpl original = myLayers.get(nameForCopy);
            if (original != null) {
                original.copy(layer);
            }
        }
        myLayers.put(name, layer);
        if (activate) {
            setCurrentLayerSafe(name);
        }
        return layer;
    }

    public void setCurrentLayerSafe(@Nullable String name) {
        myCurrentLayerName = name;
        myCachedCurrentLayer = null;
    }

    @Nullable
    @Override
    public ModifiableModuleRootLayer setCurrentLayer(String name) {
        assertWritable();
        ModuleRootLayerImpl moduleRootLayer = myLayers.get(name);
        if (moduleRootLayer == null) {
            return null;
        }
        myCurrentLayerName = name;
        myCachedCurrentLayer = moduleRootLayer;
        return moduleRootLayer;
    }

    @Override
    public boolean removeLayer(String name, boolean initDefault) {
        assertWritable();
        ModuleRootLayerImpl removedLayer = myLayers.remove(name);
        if (removedLayer != null) {
            Disposer.dispose(removedLayer);

            if (initDefault && myLayers.isEmpty()) {
                initDefaultLayer(null);
            }

            if (Comparing.equal(myCurrentLayerName, name)) {
                setCurrentLayerSafe(myLayers.isEmpty() ? null : ContainerUtil.getFirstItem(myLayers.keySet()));
            }
        }
        return removedLayer != null;
    }

    @Override
    public void removeAllLayers(boolean initDefault) {
        assertWritable();

        for (ModuleRootLayerImpl layer : myLayers.values()) {
            Disposer.dispose(layer);
        }
        myLayers.clear();

        if (initDefault) {
            initDefaultLayer(null);
        }
        else {
            setCurrentLayerSafe(null);
        }
    }

    
    @Override
    public String getCurrentLayerName() {
        LOG.assertTrue(myCurrentLayerName != null);
        return myCurrentLayerName;
    }

    
    @Override
    public Map<String, ModuleRootLayer> getLayers() {
        return Collections.<String, ModuleRootLayer>unmodifiableSortedMap(myLayers);
    }

    @Nullable
    @Override
    public ModuleRootLayer findLayerByName(String name) {
        return myLayers.get(name);
    }
}

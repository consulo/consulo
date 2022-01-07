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

import com.intellij.ProjectTopics;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.Comparing;
import consulo.disposer.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.logging.Logger;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.ModuleExtensionWithSdk;
import consulo.roots.ModifiableModuleRootLayer;
import consulo.roots.ModuleRootLayer;
import consulo.roots.ModuleRootLayerListener;
import consulo.roots.impl.ModuleRootLayerImpl;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author dsl
 */
public class RootModelImpl extends RootModelBase implements ModifiableRootModel {
  public static final Logger LOGGER = Logger.getInstance(RootModelImpl.class);

  protected final ModuleRootManagerImpl myModuleRootManager;
  private boolean myWritable;
  private boolean myDisposed;

  private final RootConfigurationAccessor myConfigurationAccessor;

  private String myCurrentLayerName;
  private ModuleRootLayerImpl myCachedCurrentLayer;
  private final SortedMap<String, ModuleRootLayerImpl> myLayers = new TreeMap<>();

  @RequiredReadAction
  RootModelImpl(@Nonnull ModuleRootManagerImpl moduleRootManager) {
    myModuleRootManager = moduleRootManager;
    myWritable = false;
    myConfigurationAccessor = new RootConfigurationAccessor();

    initDefaultLayer(null);
  }

  @RequiredReadAction
  RootModelImpl(@Nonnull Element element, @Nullable ProgressIndicator progressIndicator, @Nonnull ModuleRootManagerImpl moduleRootManager, boolean writable) {
    myModuleRootManager = moduleRootManager;

    myConfigurationAccessor = new RootConfigurationAccessor();

    loadState(element, progressIndicator);

    myWritable = writable;
  }

  //creates modifiable model
  @RequiredReadAction
  RootModelImpl(@Nonnull RootModelImpl rootModel, @Nonnull ModuleRootManagerImpl moduleRootManager, @Nonnull RootConfigurationAccessor rootConfigurationAccessor) {
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

        ModuleRootLayerImpl layer = myLayers.put(name, moduleRootLayer);
        if (layer != null) {
          // dispose old layout
          Disposer.dispose(layer);
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

  @Nonnull
  public RootConfigurationAccessor getConfigurationAccessor() {
    return myConfigurationAccessor;
  }

  @Override
  public void removeContentEntry(@Nonnull ContentEntry entry) {
    assertWritable();
    getCurrentLayer().removeContentEntry(entry);
  }

  @Override
  public void addOrderEntry(@Nonnull OrderEntry entry) {
    assertWritable();
    getCurrentLayer().addOrderEntry(entry);
  }

  @Nonnull
  @Override
  public LibraryOrderEntry addLibraryEntry(@Nonnull Library library) {
    assertWritable();
    return getCurrentLayer().addLibraryEntry(library);
  }

  @Nonnull
  @Override
  public ModuleExtensionWithSdkOrderEntry addModuleExtensionSdkEntry(@Nonnull ModuleExtensionWithSdk<?> moduleExtension) {
    assertWritable();
    return getCurrentLayer().addModuleExtensionSdkEntry(moduleExtension);
  }

  @Nonnull
  @Override
  public LibraryOrderEntry addInvalidLibrary(@Nonnull String name, @Nonnull String level) {
    assertWritable();
    return getCurrentLayer().addInvalidLibrary(name, level);
  }

  @Nonnull
  @Override
  public ModuleOrderEntry addModuleOrderEntry(@Nonnull Module module) {
    assertWritable();
    return getCurrentLayer().addModuleOrderEntry(module);
  }

  @Nonnull
  @Override
  public ModuleOrderEntry addInvalidModuleEntry(@Nonnull String name) {
    assertWritable();
    return getCurrentLayer().addInvalidModuleEntry(name);
  }

  @Nullable
  @Override
  public LibraryOrderEntry findLibraryOrderEntry(@Nonnull Library library) {
    return getCurrentLayer().findLibraryOrderEntry(library);
  }

  @Override
  public ModuleExtensionWithSdkOrderEntry findModuleExtensionSdkEntry(@Nonnull ModuleExtension extension) {
    return getCurrentLayer().findModuleExtensionSdkEntry(extension);
  }

  @Override
  public void removeOrderEntry(@Nonnull OrderEntry entry) {
    assertWritable();
    getCurrentLayer().removeOrderEntry(entry);
  }

  @Override
  public void rearrangeOrderEntries(@Nonnull OrderEntry[] newEntries) {
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

  @SuppressWarnings("unchecked")
  @RequiredReadAction
  public void doCommitAndDispose(boolean notifyEvents) {
    assert isWritable();

    RootModelImpl sourceModel = getSourceModel();

    ModuleRootLayerListener layerListener = getModule().getProject().getMessageBus().syncPublisher(ProjectTopics.MODULE_LAYERS);

    boolean extensionListenerAlreadyCalled = false;
    if (!Comparing.equal(sourceModel.myCurrentLayerName, myCurrentLayerName)) {
      ModuleRootLayerImpl oldLayer = sourceModel.getCurrentLayer();
      String oldName = sourceModel.getCurrentLayerName();

      sourceModel.setCurrentLayerSafe(myCurrentLayerName);

      if (notifyEvents) {
        layerListener.currentLayerChanged(getModule(), oldName, oldLayer, myCurrentLayerName, getCurrentLayer());
        extensionListenerAlreadyCalled = true;
      }
    }

    // first we commit changed and new layers
    for (Map.Entry<String, ModuleRootLayerImpl> entry : myLayers.entrySet()) {
      final ModuleRootLayerImpl sourceModuleLayer = sourceModel.myLayers.get(entry.getKey());

      ModuleRootLayerImpl toCommit = sourceModuleLayer;
      // if layer exists
      if (toCommit == null) {
        toCommit = new ModuleRootLayerImpl(null, sourceModel);
        ModuleRootLayerImpl layer = sourceModel.myLayers.put(entry.getKey(), toCommit);
        if(layer != null) {
          Disposer.dispose(layer);
        }
      }

      if (entry.getValue().copy(toCommit, notifyEvents && !extensionListenerAlreadyCalled && myCurrentLayerName.equals(entry.getKey())) && notifyEvents) {
        if (sourceModuleLayer == null) {
          layerListener.layerAdded(getModule(), toCommit);
        }
        else {
          layerListener.layerChanged(getModule(), toCommit);
        }
      }
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
      if (notifyEvents) {
        layerListener.layerRemove(getModule(), removed);
      }

      Disposer.dispose(removed);
    }

    dispose();
  }

  @Override
  @Nonnull
  public LibraryTable getModuleLibraryTable() {
    return getCurrentLayer().getModuleLibraryTable();
  }

  @Nonnull
  @Override
  public Project getProject() {
    return myModuleRootManager.getModule().getProject();
  }

  @Override
  @Nonnull
  public ContentEntry addContentEntry(@Nonnull VirtualFile file) {
    return getCurrentLayer().addContentEntry(file);
  }

  @Override
  @Nonnull
  public ContentEntry addContentEntry(@Nonnull String url) {
    return getCurrentLayer().addContentEntry(url);
  }

  @Override
  @Nonnull
  public ContentEntry addSingleContentEntry(@Nonnull VirtualFile file) {
    return getCurrentLayer().addSingleContentEntry(file);
  }

  @Override
  @Nonnull
  public ContentEntry addSingleContentEntry(@Nonnull String url) {
    return getCurrentLayer().addSingleContentEntry(url);
  }

  @Override
  public boolean isDisposed() {
    return myDisposed;
  }

  @Override
  public <T extends OrderEntry> void replaceEntryOfType(@Nonnull Class<T> entryClass, @Nullable final T entry) {
    assertWritable();
    getCurrentLayer().replaceEntryOfType(entryClass, entry);
  }

  public void assertWritable() {
    LOGGER.assertTrue(myWritable);
  }

  public boolean isDependsOn(final Module module) {
    for (OrderEntry entry : getOrderEntries()) {
      if (entry instanceof ModuleOrderEntry) {
        final Module module1 = ((ModuleOrderEntry)entry).getModule();
        if (module1 == module) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  @Nonnull
  public Module getModule() {
    return myModuleRootManager.getModule();
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean isChanged() {
    if (!myWritable) return false;

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

  void makeExternalChange(@Nonnull Runnable runnable) {
    if (myWritable || myDisposed) return;
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

  @Nonnull
  @Override
  public ModuleRootLayerImpl getCurrentLayer() {
    if (myCachedCurrentLayer != null) {
      return myCachedCurrentLayer;
    }
    ModuleRootLayerImpl moduleRootLayer = myLayers.get(myCurrentLayerName);
    LOGGER.assertTrue(moduleRootLayer != null);
    return myCachedCurrentLayer = moduleRootLayer;
  }

  @Nonnull
  @Override
  public ModifiableModuleRootLayer addLayer(@Nonnull String name, @Nullable String nameForCopy, boolean activate) {
    ModuleRootLayerImpl moduleRootLayer = myLayers.get(name);
    if (moduleRootLayer != null) {
      return moduleRootLayer;
    }

    ModuleRootLayerImpl layer = new ModuleRootLayerImpl(null, this);

    if (nameForCopy != null) {
      ModuleRootLayerImpl original = myLayers.get(nameForCopy);
      if (original != null) {
        original.copy(layer, false);
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
  public ModifiableModuleRootLayer setCurrentLayer(@Nonnull String name) {
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
  public boolean removeLayer(@Nonnull String name, boolean initDefault) {
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

  @Nonnull
  @Override
  public String getCurrentLayerName() {
    LOGGER.assertTrue(myCurrentLayerName != null);
    return myCurrentLayerName;
  }

  @Nonnull
  @Override
  public Map<String, ModuleRootLayer> getLayers() {
    return Collections.<String, ModuleRootLayer>unmodifiableSortedMap(myLayers);
  }

  @Nullable
  @Override
  public ModuleRootLayer findLayerByName(@Nonnull String name) {
    return myLayers.get(name);
  }
}

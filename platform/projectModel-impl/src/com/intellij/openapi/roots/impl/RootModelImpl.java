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
import com.intellij.openapi.CompositeDisposable;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.consulo.module.extension.ModuleExtension;
import org.consulo.module.extension.ModuleExtensionWithSdk;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author dsl
 */
@org.consulo.lombok.annotations.Logger
public class RootModelImpl extends RootModelBase implements ModifiableRootModel {
  final ModuleRootManagerImpl myModuleRootManager;
  private boolean myWritable;
  private boolean myDisposed = false;

  private final RootConfigurationAccessor myConfigurationAccessor;

  private final ProjectRootManagerImpl myProjectRootManager;
  // have to register all child disposables using this fake object since all clients just call ModifiableModel.dispose()
  private final CompositeDisposable myDisposable = new CompositeDisposable();

  private String myCurrentLayerName;
  private ModuleRootLayerImpl myCachedCurrentLayer;
  private final SortedMap<String, ModuleRootLayerImpl> myLayers = new TreeMap<String, ModuleRootLayerImpl>();

  RootModelImpl(@NotNull ModuleRootManagerImpl moduleRootManager, @NotNull ProjectRootManagerImpl projectRootManager) {
    myModuleRootManager = moduleRootManager;
    myProjectRootManager = projectRootManager;

    myWritable = false;

    try {
      initDefaultLayer(null, projectRootManager);
    }
    catch (InvalidDataException e) {
      //
    }

    myConfigurationAccessor = new RootConfigurationAccessor();
  }

  RootModelImpl(@NotNull Element element,
                @NotNull ModuleRootManagerImpl moduleRootManager,
                @NotNull ProjectRootManagerImpl projectRootManager,
                boolean writable) throws InvalidDataException {
    myProjectRootManager = projectRootManager;
    myModuleRootManager = moduleRootManager;

    loadState(element);

    myWritable = writable;

    myConfigurationAccessor = new RootConfigurationAccessor();
  }

  //creates modifiable model
  RootModelImpl(@NotNull RootModelImpl rootModel,
                @NotNull ModuleRootManagerImpl moduleRootManager,
                @NotNull RootConfigurationAccessor rootConfigurationAccessor,
                @NotNull ProjectRootManagerImpl projectRootManager) {
    myModuleRootManager = moduleRootManager;
    myProjectRootManager = projectRootManager;
    myWritable = true;
    myConfigurationAccessor = rootConfigurationAccessor;

    myLayers.clear();
    for (Map.Entry<String, ModuleRootLayerImpl> entry : rootModel.myLayers.entrySet()) {
      ModuleRootLayerImpl moduleRootLayer = new ModuleRootLayerImpl(entry.getValue(), this, projectRootManager);
      myLayers.put(entry.getKey(), moduleRootLayer);
    }
    setCurrentLayerSafe(rootModel.myCurrentLayerName);
  }

  private void initDefaultLayer(Element element, ProjectRootManagerImpl projectRootManager) throws InvalidDataException {
    setCurrentLayerSafe("Default");

    ModuleRootLayerImpl moduleRootLayer = new ModuleRootLayerImpl(null, this, projectRootManager);
    myLayers.put(myCurrentLayerName, moduleRootLayer);

    if (element != null) {
      moduleRootLayer.readExternal(element);
    }
  }

  public void loadState(Element element) throws InvalidDataException {
    String currentLayer = element.getAttributeValue("current-layer");
    if (currentLayer != null) {
      setCurrentLayerSafe(currentLayer);

      for (Element moduleLayerElement : element.getChildren("module-layer")) {
        String name = moduleLayerElement.getAttributeValue("name");

        ModuleRootLayerImpl moduleRootLayer = new ModuleRootLayerImpl(null, this, myProjectRootManager);
        moduleRootLayer.readExternal(moduleLayerElement);

        myLayers.put(name, moduleRootLayer);
      }
    }

    // old format - create default profile and load it
    if (myLayers.isEmpty()) {
      initDefaultLayer(element, myProjectRootManager);
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
  public void removeContentEntry(@NotNull ContentEntry entry) {
    assertWritable();
    getCurrentLayer().removeContentEntry(entry);
  }

  @Override
  public void addOrderEntry(@NotNull OrderEntry entry) {
    assertWritable();
    getCurrentLayer().addOrderEntry(entry);
  }

  @NotNull
  @Override
  public LibraryOrderEntry addLibraryEntry(@NotNull Library library) {
    assertWritable();
    return getCurrentLayer().addLibraryEntry(library);
  }

  @NotNull
  @Override
  public ModuleExtensionWithSdkOrderEntry addModuleExtensionSdkEntry(@NotNull ModuleExtensionWithSdk<?> moduleExtension) {
    assertWritable();
    return getCurrentLayer().addModuleExtensionSdkEntry(moduleExtension);
  }

  @NotNull
  @Override
  public LibraryOrderEntry addInvalidLibrary(@NotNull String name, @NotNull String level) {
    assertWritable();
    return getCurrentLayer().addInvalidLibrary(name, level);
  }

  @NotNull
  @Override
  public ModuleOrderEntry addModuleOrderEntry(@NotNull Module module) {
    assertWritable();
    return getCurrentLayer().addModuleOrderEntry(module);
  }

  @NotNull
  @Override
  public ModuleOrderEntry addInvalidModuleEntry(@NotNull String name) {
    assertWritable();
    return getCurrentLayer().addInvalidModuleEntry(name);
  }

  @Nullable
  @Override
  public LibraryOrderEntry findLibraryOrderEntry(@NotNull Library library) {
    return getCurrentLayer().findLibraryOrderEntry(library);
  }

  @Override
  public ModuleExtensionWithSdkOrderEntry findModuleExtensionSdkEntry(@NotNull ModuleExtension extension) {
    return getCurrentLayer().findModuleExtensionSdkEntry(extension);
  }

  @Override
  public void removeOrderEntry(@NotNull OrderEntry entry) {
    assertWritable();
    getCurrentLayer().removeOrderEntry(entry);
  }

  @Override
  public void rearrangeOrderEntries(@NotNull OrderEntry[] newEntries) {
    assertWritable();
    getCurrentLayer().rearrangeOrderEntries(newEntries);
  }

  @Override
  public void clear() {
    disposeLayers();

    try {
      initDefaultLayer(null, myProjectRootManager);
    }
    catch (InvalidDataException e) {
      //
    }
  }

  private void disposeLayers() {
    for (ModuleRootLayerImpl moduleRootLayer : myLayers.values()) {
      moduleRootLayer.dispose();
    }
    myLayers.clear();
  }

  @Override
  public void commit() {
    myModuleRootManager.commitModel(this);
    myWritable = false;
  }

  @SuppressWarnings("unchecked")
  public void doCommit() {
    assert isWritable();

    RootModelImpl sourceModel = getSourceModel();

    ModuleRootLayerListener layerListener = getModule().getProject().getMessageBus().syncPublisher(ProjectTopics.MODULE_LAYERS);

    if(!Comparing.equal(sourceModel.myCurrentLayerName, myCurrentLayerName)) {
      sourceModel.setCurrentLayerSafe(myCurrentLayerName);

      layerListener.currentLayerChanged(getModule(), sourceModel.myCurrentLayerName, myCurrentLayerName);
    }

    // first we commit changed and new layers
    for (Map.Entry<String, ModuleRootLayerImpl> entry : myLayers.entrySet()) {
      final ModuleRootLayerImpl sourceModuleLayer = sourceModel.myLayers.get(entry.getKey());

      ModuleRootLayerImpl toCommit = sourceModuleLayer;
      // if layer exists
      if (toCommit == null) {
        toCommit = new ModuleRootLayerImpl(null, sourceModel, myProjectRootManager);
        sourceModel.myLayers.put(entry.getKey(), toCommit);
      }

      if(entry.getValue().copy(toCommit, myCurrentLayerName.equals(entry.getKey()))) {
        if(sourceModuleLayer == null) {
          layerListener.layerAdded(getModule(), toCommit);
        }
        else {
          layerListener.layerChanged(getModule(), toCommit);
        }
      }
    }

    List<String> toRemove = new SmartList<String>();
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
      layerListener.layerRemove(getModule(), removed);
      removed.dispose();
    }
  }

  @Override
  @NotNull
  public LibraryTable getModuleLibraryTable() {
    return getCurrentLayer().getModuleLibraryTable();
  }

  @NotNull
  @Override
  public Project getProject() {
    return myProjectRootManager.getProject();
  }

  @Override
  @NotNull
  public ContentEntry addContentEntry(@NotNull VirtualFile file) {
    return getCurrentLayer().addContentEntry(file);
  }

  @Override
  @NotNull
  public ContentEntry addContentEntry(@NotNull String url) {
    return getCurrentLayer().addContentEntry(url);
  }

  @Override
  public boolean isDisposed() {
    return myDisposed;
  }

  @Override
  public <T extends OrderEntry> void replaceEntryOfType(@NotNull Class<T> entryClass, @Nullable final T entry) {
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
  @NotNull
  public Module getModule() {
    return myModuleRootManager.getModule();
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean isChanged() {
    if (!myWritable) return false;

    for (ModuleRootLayer moduleRootLayer : myLayers.values()) {
      LOGGER.assertTrue(moduleRootLayer instanceof ModifiableModuleRootLayer);
      if (((ModifiableModuleRootLayer)moduleRootLayer).isChanged()) {
        return true;
      }
    }

    RootModelImpl sourceModel = getSourceModel();

    // check for deleted layers
    for (String layerName : sourceModel.myLayers.keySet()) {
      ModuleRootLayerImpl layer = myLayers.get(layerName);
      if(layer == null) {
        return true;
      }
    }
    return false;
  }

  void makeExternalChange(@NotNull Runnable runnable) {
    if (myWritable || myDisposed) return;
    myModuleRootManager.makeRootsChange(runnable);
  }

  @Override
  public void dispose() {
    assert !myDisposed;
    Disposer.dispose(myDisposable);
    disposeLayers();
    myWritable = false;
    myDisposed = true;
  }

  private RootModelImpl getSourceModel() {
    assertWritable();
    return myModuleRootManager.getRootModel();
  }

  @NotNull
  @Override
  public ModuleRootLayerImpl getCurrentLayer() {
    if(myCachedCurrentLayer != null) {
      return myCachedCurrentLayer;
    }
    ModuleRootLayerImpl moduleRootLayer = myLayers.get(myCurrentLayerName);
    LOGGER.assertTrue(moduleRootLayer != null);
    return myCachedCurrentLayer = moduleRootLayer;
  }

  @Override
  public ModifiableModuleRootLayer addLayer(@NotNull String name, @Nullable String nameForCopy, boolean activate) {
    ModuleRootLayerImpl moduleRootLayer = myLayers.get(name);
    if(moduleRootLayer != null) {
      return moduleRootLayer;
    }

    ModuleRootLayerImpl layer = new ModuleRootLayerImpl(null, this, myProjectRootManager);

    if(nameForCopy != null) {
      ModuleRootLayerImpl original = myLayers.get(nameForCopy);
      if(original != null) {
        original.copy(layer, false);
      }
    }
    myLayers.put(name, layer);
    if(activate) {
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
  public ModifiableModuleRootLayer setCurrentLayer(@NotNull String name) {
    ModuleRootLayerImpl moduleRootLayer = myLayers.get(name);
    if(moduleRootLayer == null) {
      return null;
    }
    myCurrentLayerName = name;
    myCachedCurrentLayer = moduleRootLayer;
    return moduleRootLayer;
  }

  @Nullable
  @Override
  public ModifiableModuleRootLayer removeLayer(@NotNull String name, boolean initDefault) {
    ModuleRootLayerImpl remove = myLayers.remove(name);
    if(remove != null) {
      if(initDefault && myLayers.isEmpty()) {
        try {
          initDefaultLayer(null, myProjectRootManager);
        }
        catch (InvalidDataException e) {
          //
        }
      }

      if(Comparing.equal(myCurrentLayerName, name)) {
        setCurrentLayerSafe(myLayers.isEmpty() ? null : ContainerUtil.getFirstItem(myLayers.keySet()));
      }
    }
    return remove;
  }

  @NotNull
  @Override
  public String getCurrentLayerName() {
    LOGGER.assertTrue(myCurrentLayerName != null);
    return myCurrentLayerName;
  }

  @NotNull
  @Override
  public Map<String, ModuleRootLayer> getLayers() {
    return Collections.<String, ModuleRootLayer>unmodifiableSortedMap(myLayers);
  }

  @Nullable
  @Override
  public ModuleRootLayer findLayerByName(@NotNull String name) {
    return myLayers.get(name);
  }

  void registerOnDispose(@NotNull Disposable disposable) {
    myDisposable.add(disposable);
  }
}

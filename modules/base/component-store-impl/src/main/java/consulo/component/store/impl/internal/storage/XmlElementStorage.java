/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.component.store.impl.internal.storage;

import consulo.component.persist.RoamingType;
import consulo.component.persist.Storage;
import consulo.component.store.impl.internal.DefaultStateSerializer;
import consulo.component.store.internal.PathMacrosService;
import consulo.component.store.internal.StateStorageException;
import consulo.component.store.internal.StreamProvider;
import consulo.component.store.internal.TrackingPathMacroSubstitutor;
import consulo.util.collection.ContainerUtil;
import consulo.util.jdom.JDOMUtil;
import consulo.util.xml.serializer.WriteExternalException;
import org.jspecify.annotations.Nullable;
import org.jdom.Element;
import org.jdom.JDOMException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class XmlElementStorage extends StateStorageBase<StorageData> {
  
  protected final String myRootElementName;
  protected StorageData myLoadedData;
  protected final StreamProvider myStreamProvider;
  protected final String myFileSpec;
  protected boolean myBlockSavingTheContent = false;

  protected final RoamingType myRoamingType;
  protected final PathMacrosService myPathMacrosService;

  protected XmlElementStorage(String fileSpec,
                              @Nullable RoamingType roamingType,
                              @Nullable TrackingPathMacroSubstitutor pathMacroSubstitutor,
                              String rootElementName,
                              @Nullable StreamProvider streamProvider,
                              PathMacrosService pathMacrosService) {
    super(pathMacroSubstitutor, pathMacrosService);
    myPathMacrosService = pathMacrosService;
    myFileSpec = fileSpec;
    myRoamingType = roamingType == null ? RoamingType.DEFAULT : roamingType;
    myRootElementName = rootElementName;
    myStreamProvider = myRoamingType == RoamingType.DISABLED ? null : streamProvider;
  }

  protected abstract @Nullable Element loadLocalData();

  @Override
  protected @Nullable Element getStateAndArchive(StorageData storageData, String componentName) {
    return storageData.getStateAndArchive(componentName);
  }

  @Override
  
  protected StorageData getStorageData(boolean reloadData) {
    if (myLoadedData != null && !reloadData) {
      return myLoadedData;
    }

    myLoadedData = loadData(true);
    return myLoadedData;
  }

  
  protected StorageData loadData(boolean useProvidersData) {
    StorageData result = createStorageData();

    if (useProvidersData && myStreamProvider != null && myStreamProvider.isEnabled()) {
      try {
        Element element = loadDataFromStreamProvider();
        if (element != null) {
          loadState(result, element);
        }

        // we don't use local data if has stream provider
        return result;
      }
      catch (Exception e) {
        LOG.warn(e);
      }
    }

    Element element = loadLocalData();
    if (element != null) {
      loadState(result, element);
    }

    return result;
  }

  protected final @Nullable Element loadDataFromStreamProvider() throws IOException, JDOMException {
    assert myStreamProvider != null;
    try {
      return JDOMUtil.load(myStreamProvider.loadContent(myFileSpec, myRoamingType));
    }
    catch (JDOMException | IOException e) {
      LOG.warn("Failed to read file content, fileSpec=" + myFileSpec + ", roamingType=" + myRoamingType);
      throw e;
    }
  }

  protected final void loadState(StorageData result, Element element) {
    result.load(element, myPathMacroSubstitutor, true);
  }

  
  protected StorageData createStorageData() {
    return new StorageData(myRootElementName, myPathMacrosService);
  }

  public void setDefaultState(Element element) {
    myLoadedData = createStorageData();
    loadState(myLoadedData, element);
  }

  @Override
  public final @Nullable XmlElementStorageSaveSession startExternalization() {
    return checkIsSavingDisabled() ? null : createSaveSession(getStorageData());
  }

  protected abstract XmlElementStorageSaveSession createSaveSession(StorageData storageData);

  protected final @Nullable Element getElement(StorageData data, boolean collapsePaths, Map<String, Element> newLiveStates) {
    Element element = data.save(newLiveStates);
    if (element == null || JDOMUtil.isEmpty(element)) {
      return null;
    }

    if (collapsePaths && myPathMacroSubstitutor != null) {
      try {
        myPathMacroSubstitutor.collapsePaths(element);
      }
      finally {
        myPathMacroSubstitutor.reset();
      }
    }

    return element;
  }

  @Override
  public void analyzeExternalChangesAndUpdateIfNeed(Set<String> result) {
    StorageData oldData = myLoadedData;
    StorageData newData = getStorageData(true);
    if (oldData == null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("analyzeExternalChangesAndUpdateIfNeed: old data null, load new for " + toString());
      }
      result.addAll(newData.getComponentNames());
    }
    else {
      Set<String> changedComponentNames = oldData.getChangedComponentNames(newData, myPathMacroSubstitutor);
      if (LOG.isDebugEnabled()) {
        LOG.debug("analyzeExternalChangesAndUpdateIfNeed: changedComponentNames + " + changedComponentNames + " for " + toString());
      }
      if (!ContainerUtil.isEmpty(changedComponentNames)) {
        result.addAll(changedComponentNames);
      }
    }
  }

  protected abstract class XmlElementStorageSaveSession implements SaveSession, ExternalizationSession {
    private final StorageData myOriginalStorageData;
    private StorageData myCopiedStorageData;

    private final Map<String, Element> myNewLiveStates = new HashMap<String, Element>();

    public XmlElementStorageSaveSession(StorageData storageData) {
      myOriginalStorageData = storageData;
    }

    @Override
    public final @Nullable SaveSession createSaveSession(boolean force) {
      if(force) {
        return this;
      }
      return checkIsSavingDisabled() || myCopiedStorageData == null ? null : this;
    }

    @Override
    public final void setState(Object component, String componentName, Object state, @Nullable Storage storageSpec) {
      Element element;
      try {
        element = DefaultStateSerializer.serializeState(state, storageSpec);
      }
      catch (WriteExternalException e) {
        LOG.debug(e);
        return;
      }
      catch (Throwable e) {
        LOG.error("Unable to serialize " + componentName + " state", e);
        return;
      }

      if (myCopiedStorageData == null) {
        myCopiedStorageData = StorageData.setStateAndCloneIfNeed(componentName, element, myOriginalStorageData, myNewLiveStates);
      }
      else {
        myCopiedStorageData.setState(componentName, element, myNewLiveStates);
      }
    }

    @Override
    public final void save(boolean force) {
      if (myBlockSavingTheContent) {
        return;
      }

      try {
        if(force) {
          doSave(getElement(myOriginalStorageData, isCollapsePathsOnSave(), Map.of()));
        }
        else {
          doSave(getElement(myCopiedStorageData, isCollapsePathsOnSave(), myNewLiveStates));
          myLoadedData = myCopiedStorageData;
        }
      }
      catch (IOException e) {
        throw new StateStorageException(e);
      }
    }

    // only because default project store hack
    protected boolean isCollapsePathsOnSave() {
      return true;
    }

    protected abstract void doSave(@Nullable Element element) throws IOException;

    protected void saveForProvider(@Nullable byte[] content, @Nullable Element element) throws IOException {
      if (!myStreamProvider.isApplicable(myFileSpec, myRoamingType)) {
        return;
      }

      if (element == null) {
        myStreamProvider.delete(myFileSpec, myRoamingType);
      }
      else {
        doSaveForProvider(element, myRoamingType, content);
      }
    }

    private void doSaveForProvider(Element element, RoamingType roamingType, @Nullable byte[] content) throws IOException {
      if (content == null) {
        StorageUtil.sendContent(myStreamProvider, myFileSpec, element, roamingType);
      }
      else {
        myStreamProvider.saveContent(myFileSpec, content, myRoamingType);
      }
    }
  }
}

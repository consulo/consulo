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
package consulo.compiler.artifact.impl.internal;

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.WriteAction;
import consulo.compiler.artifact.*;
import consulo.compiler.artifact.element.*;
import consulo.compiler.artifact.event.ArtifactListener;
import consulo.compiler.artifact.impl.internal.state.ArtifactManagerState;
import consulo.compiler.artifact.impl.internal.state.ArtifactPropertiesState;
import consulo.compiler.artifact.impl.internal.state.ArtifactState;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.util.ModificationTracker;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.module.ProjectLoadingErrorsNotifier;
import consulo.module.content.internal.ProjectRootManagerEx;
import consulo.project.Project;
import consulo.project.UnknownFeaturesCollector;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import consulo.util.xml.serializer.SkipDefaultValuesSerializationFilters;
import consulo.util.xml.serializer.XmlSerializer;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import java.util.*;

/**
 * @author nik
 */
@Singleton
@State(name = "ArtifactManager", storages = @Storage(value = "artifacts", stateSplitter = ArtifactManagerStateSplitter.class))
@ServiceImpl
public class ArtifactManagerImpl extends ArtifactManager implements Disposable, PersistentStateComponent<ArtifactManagerState> {
  private static final Logger LOG = Logger.getInstance(ArtifactManagerImpl.class);
  @NonNls
  public static final String PACKAGING_ELEMENT_NAME = "element";
  @NonNls
  public static final String TYPE_ID_ATTRIBUTE = "id";
  private final ArtifactManagerModel myModel;
  private final Project myProject;
  private final PackagingElementFactory myPackagingElementFactory;
  private final PackagingElementResolvingContext myResolvingContext;
  private boolean myInsideCommit = false;
  private boolean myLoaded;
  private long myModificationCount;
  private final ModificationTracker myModificationTracker = new ModificationTracker() {
    @Override
    public long getModificationCount() {
      return myModificationCount;
    }
  };
  private final Map<String, LocalFileSystem.WatchRequest> myWatchedOutputs = new HashMap<>();

  @Inject
  public ArtifactManagerImpl(Project project, PackagingElementFactory packagingElementFactory, VirtualFileManager virtualFileManager) {
    myProject = project;
    myPackagingElementFactory = packagingElementFactory;
    myModel = new ArtifactManagerModel();
    myResolvingContext = PackagingElementResolvingContext.of(myProject, this);

    virtualFileManager.addVirtualFileListener(new ArtifactVirtualFileListener(myProject, this), myProject);

    updateWatchedRoots();
  }

  @Override
  @Nonnull
  public Artifact[] getArtifacts() {
    return myModel.getArtifacts();
  }

  @Override
  public Artifact findArtifact(@Nonnull String name) {
    return myModel.findArtifact(name);
  }

  @Override
  @Nonnull
  public Artifact getArtifactByOriginal(@Nonnull Artifact artifact) {
    return myModel.getArtifactByOriginal(artifact);
  }

  @Override
  @Nonnull
  public Artifact getOriginalArtifact(@Nonnull Artifact artifact) {
    return myModel.getOriginalArtifact(artifact);
  }

  @Override
  @Nonnull
  public Collection<? extends Artifact> getArtifactsByType(@Nonnull ArtifactType type) {
    return myModel.getArtifactsByType(type);
  }

  @Override
  public List<? extends Artifact> getAllArtifactsIncludingInvalid() {
    return myModel.getAllArtifactsIncludingInvalid();
  }

  @Override
  public ArtifactManagerState getState() {
    final ArtifactManagerState state = new ArtifactManagerState();
    for (Artifact artifact : getAllArtifactsIncludingInvalid()) {
      final ArtifactState artifactState;
      if (artifact instanceof InvalidArtifact) {
        artifactState = ((InvalidArtifact)artifact).getState();
      }
      else {
        artifactState = new ArtifactState();
        artifactState.setBuildOnMake(artifact.isBuildOnMake());
        artifactState.setName(artifact.getName());
        artifactState.setOutputPath(artifact.getOutputPath());
        artifactState.setRootElement(serializePackagingElement(artifact.getRootElement()));
        artifactState.setArtifactType(artifact.getArtifactType().getId());
        for (ArtifactPropertiesProvider provider : artifact.getPropertiesProviders()) {
          final ArtifactPropertiesState propertiesState = serializeProperties(provider, artifact.getProperties(provider));
          if (propertiesState != null) {
            artifactState.getPropertiesList().add(propertiesState);
          }
        }
        Collections.sort(artifactState.getPropertiesList(), (o1, o2) -> o1.getId().compareTo(o2.getId()));
      }
      state.getArtifacts().add(artifactState);
    }
    return state;
  }

  @Nullable
  private static <S> ArtifactPropertiesState serializeProperties(ArtifactPropertiesProvider provider, ArtifactProperties<S> properties) {
    final ArtifactPropertiesState state = new ArtifactPropertiesState();
    state.setId(provider.getId());
    final Element options = new Element("options");
    XmlSerializer.serializeInto(properties.getState(), options, new SkipDefaultValuesSerializationFilters());
    if (options.getContent().isEmpty() && options.getAttributes().isEmpty()) return null;
    state.setOptions(options);
    return state;
  }

  private static Element serializePackagingElement(PackagingElement<?> packagingElement) {
    Element element = new Element(PACKAGING_ELEMENT_NAME);
    element.setAttribute(TYPE_ID_ATTRIBUTE, packagingElement.getType().getId());
    final Object bean = packagingElement.getState();
    if (bean != null) {
      XmlSerializer.serializeInto(bean, element, new SkipDefaultValuesSerializationFilters());
    }
    if (packagingElement instanceof CompositePackagingElement) {
      for (PackagingElement<?> child : ((CompositePackagingElement<?>)packagingElement).getChildren()) {
        element.addContent(serializePackagingElement(child));
      }
    }
    return element;
  }

  private <T> PackagingElement<T> deserializeElement(Element element) throws UnknownPackagingElementTypeException {
    final String id = element.getAttributeValue(TYPE_ID_ATTRIBUTE);
    PackagingElementType<?> type = myPackagingElementFactory.findElementType(id);
    if (type == null) {
      throw new UnknownPackagingElementTypeException(id);
    }

    PackagingElement<T> packagingElement = (PackagingElement<T>)type.createEmpty(myProject);
    T state = packagingElement.getState();
    if (state != null) {
      XmlSerializer.deserializeInto(state, element);
      packagingElement.loadState(this, state);
    }
    final List children = element.getChildren(PACKAGING_ELEMENT_NAME);
    //noinspection unchecked
    for (Element child : (List<? extends Element>)children) {
      ((CompositePackagingElement<?>)packagingElement).addOrFindChild(deserializeElement(child));
    }
    return packagingElement;
  }

  @Override
  public void loadState(ArtifactManagerState managerState) {
    final List<ArtifactImpl> artifacts = new ArrayList<>();
    for (ArtifactState state : managerState.getArtifacts()) {
      artifacts.add(loadArtifact(state));
    }

    if (myLoaded) {
      final ArtifactModelImpl model = new ArtifactModelImpl(this, artifacts);
      doCommit(model);
    }
    else {
      myModel.setArtifactsList(artifacts);
      myLoaded = true;
    }
  }

  private ArtifactImpl loadArtifact(ArtifactState state) {
    ArtifactType type = ArtifactType.findById(state.getArtifactType());
    if (type == null) {
      UnknownFeaturesCollector.getInstance(myProject).registerUnknownFeature(ArtifactType.class, state.getArtifactType());
      return createInvalidArtifact(state, "Unknown artifact type: " + state.getArtifactType());
    }

    final Element element = state.getRootElement();
    final String artifactName = state.getName();
    final CompositePackagingElement<?> rootElement;
    if (element != null) {
      try {
        rootElement = (CompositePackagingElement<?>)deserializeElement(element);
      }
      catch (UnknownPackagingElementTypeException e) {
        return createInvalidArtifact(state, "Unknown element: " + e.getTypeId());
      }
    }
    else {
      rootElement = type.createRootElement(myPackagingElementFactory, artifactName);
    }

    final ArtifactImpl artifact = new ArtifactImpl(artifactName, type, state.isBuildOnMake(), rootElement, state.getOutputPath());
    final List<ArtifactPropertiesState> propertiesList = state.getPropertiesList();
    for (ArtifactPropertiesState propertiesState : propertiesList) {
      final ArtifactPropertiesProvider provider = ArtifactPropertiesProvider.findById(propertiesState.getId());
      if (provider != null) {
        deserializeProperties(artifact.getProperties(provider), propertiesState);
      }
      else {
        return createInvalidArtifact(state, "Unknown artifact properties: " + propertiesState.getId());
      }
    }
    return artifact;
  }

  private InvalidArtifact createInvalidArtifact(ArtifactState state, String errorMessage) {
    final InvalidArtifact artifact = new InvalidArtifact(myPackagingElementFactory, state, errorMessage);
    ProjectLoadingErrorsNotifier.getInstance(myProject).registerError(new ArtifactLoadingErrorDescription(this, artifact));
    return artifact;
  }

  private static <S> void deserializeProperties(ArtifactProperties<S> artifactProperties, ArtifactPropertiesState propertiesState) {
    final Element options = propertiesState.getOptions();
    if (artifactProperties == null || options == null) {
      return;
    }
    final S state = artifactProperties.getState();
    if (state != null) {
      XmlSerializer.deserializeInto(state, options);
      artifactProperties.loadState(state);
    }
  }

  @Override
  public void dispose() {
    LocalFileSystem.getInstance().removeWatchedRoots(myWatchedOutputs.values());
  }

  private void updateWatchedRoots() {
    Set<String> pathsToRemove = new HashSet<>(myWatchedOutputs.keySet());
    Set<String> toAdd = new HashSet<>();
    for (Artifact artifact : getArtifacts()) {
      final String path = artifact.getOutputPath();
      if (path != null && path.length() > 0) {
        pathsToRemove.remove(path);
        if (!myWatchedOutputs.containsKey(path)) {
          toAdd.add(path);
        }
      }
    }

    List<LocalFileSystem.WatchRequest> requestsToRemove = new ArrayList<>();
    for (String path : pathsToRemove) {
      final LocalFileSystem.WatchRequest request = myWatchedOutputs.remove(path);
      ContainerUtil.addIfNotNull(requestsToRemove, request);
    }

    Set<LocalFileSystem.WatchRequest> newRequests = LocalFileSystem.getInstance().replaceWatchedRoots(requestsToRemove, toAdd, null);
    for (LocalFileSystem.WatchRequest request : newRequests) {
      myWatchedOutputs.put(request.getRootPath(), request);
    }
  }

  @Override
  public Artifact[] getSortedArtifacts() {
    return myModel.getSortedArtifacts();
  }

  @Override
  public ModifiableArtifactModel createModifiableModel() {
    return new ArtifactModelImpl(this, getArtifactsList());
  }

  @Override
  public PackagingElementResolvingContext getResolvingContext() {
    return myResolvingContext;
  }

  public List<ArtifactImpl> getArtifactsList() {
    return myModel.myArtifactsList;
  }

  @RequiredWriteAction
  public void commit(ArtifactModelImpl artifactModel) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();

    doCommit(artifactModel);
  }

  @RequiredWriteAction
  private void doCommit(ArtifactModelImpl artifactModel) {
    boolean hasChanges;
    LOG.assertTrue(!myInsideCommit, "Recursive commit");
    myInsideCommit = true;
    try {

      final List<ArtifactImpl> allArtifacts = artifactModel.getOriginalArtifacts();

      final Set<ArtifactImpl> removed = new HashSet<>(myModel.myArtifactsList);
      final List<ArtifactImpl> added = new ArrayList<>();
      final List<Pair<ArtifactImpl, String>> changed = new ArrayList<>();

      for (ArtifactImpl artifact : allArtifacts) {
        final boolean isAdded = !removed.remove(artifact);
        final ArtifactImpl modifiableCopy = artifactModel.getModifiableCopy(artifact);
        if (isAdded) {
          added.add(artifact);
        }
        else if (modifiableCopy != null && !modifiableCopy.equals(artifact)) {
          final String oldName = artifact.getName();
          artifact.copyFrom(modifiableCopy);
          changed.add(Pair.create(artifact, oldName));
        }
      }

      myModel.setArtifactsList(allArtifacts);
      myModificationCount++;
      final ArtifactListener publisher = myProject.getMessageBus().syncPublisher(ArtifactListener.class);
      hasChanges = !removed.isEmpty() || !added.isEmpty() || !changed.isEmpty();
      ProjectRootManagerEx.getInstanceEx(myProject).mergeRootsChangesDuring(new Runnable() {
        @Override
        public void run() {
          for (ArtifactImpl artifact : removed) {
            publisher.artifactRemoved(artifact);
          }
          //it's important to send 'removed' events before 'added'. Otherwise when artifacts are reloaded from xml artifact pointers will be damaged
          for (ArtifactImpl artifact : added) {
            publisher.artifactAdded(artifact);
          }
          for (Pair<ArtifactImpl, String> pair : changed) {
            publisher.artifactChanged(pair.getFirst(), pair.getSecond());
          }
        }
      });
    }
    finally {
      myInsideCommit = false;
    }
    updateWatchedRoots();
    if (hasChanges) {
      //TODO [VISTALL] compiler BuildManager.getInstance().clearState(myProject);
    }
  }

  public Project getProject() {
    return myProject;
  }

  @Override
  public void addElementsToDirectory(@Nonnull Artifact artifact, @Nonnull String relativePath, @Nonnull PackagingElement<?> element) {
    addElementsToDirectory(artifact, relativePath, Collections.singletonList(element));
  }

  @Override
  public void addElementsToDirectory(@Nonnull Artifact artifact,
                                     @Nonnull String relativePath,
                                     @Nonnull Collection<? extends PackagingElement<?>> elements) {
    final ModifiableArtifactModel model = createModifiableModel();
    final CompositePackagingElement<?> root = model.getOrCreateModifiableArtifact(artifact).getRootElement();
    myPackagingElementFactory.getOrCreateDirectory(root, relativePath).addOrFindChildren(elements);
    WriteAction.run(model::commit);
  }

  @Override
  public ModificationTracker getModificationTracker() {
    return myModificationTracker;
  }

  private static class ArtifactManagerModel extends ArtifactModelBase {
    private List<ArtifactImpl> myArtifactsList = new ArrayList<>();
    private Artifact[] mySortedArtifacts;

    public void setArtifactsList(List<ArtifactImpl> artifactsList) {
      myArtifactsList = artifactsList;
      artifactsChanged();
    }

    @Override
    protected void artifactsChanged() {
      super.artifactsChanged();
      mySortedArtifacts = null;
    }

    @Override
    protected List<? extends Artifact> getArtifactsList() {
      return myArtifactsList;
    }

    public Artifact[] getSortedArtifacts() {
      if (mySortedArtifacts == null) {
        mySortedArtifacts = getArtifacts().clone();
        Arrays.sort(mySortedArtifacts, ARTIFACT_COMPARATOR);
      }
      return mySortedArtifacts;
    }
  }

}

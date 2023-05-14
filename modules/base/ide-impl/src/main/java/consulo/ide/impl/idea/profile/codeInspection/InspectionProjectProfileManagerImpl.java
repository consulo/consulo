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
package consulo.ide.impl.idea.profile.codeInspection;

import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.content.scope.NamedScopesHolder;
import consulo.disposer.Disposable;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.language.editor.inspection.scheme.*;
import consulo.language.editor.packageDependency.DependencyValidationManager;
import consulo.language.editor.scope.NamedScopeManager;
import consulo.project.Project;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: anna
 * Date: 30-Nov-2005
 */
@State(name = "InspectionProjectProfileManager", storages = {
        @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/inspectionProfiles/", stateSplitter = InspectionProjectProfileManagerImpl.ProfileStateSplitter.class)})
@Singleton
@ServiceImpl
public class InspectionProjectProfileManagerImpl extends InspectionProjectProfileManager implements Disposable {
  private final Map<String, InspectionProfileWrapper> myName2Profile = new ConcurrentHashMap<>();
  private final SeverityRegistrarImpl mySeverityRegistrar;
  private final NamedScopeManager myLocalScopesHolder;

  @Inject
  public InspectionProjectProfileManagerImpl(@Nonnull Project project,
                                             @Nonnull InspectionProfileManager inspectionProfileManager,
                                             @Nonnull DependencyValidationManager holder,
                                             @Nonnull NamedScopeManager localScopesHolder) {
    super(project, inspectionProfileManager, holder);
    myLocalScopesHolder = localScopesHolder;
    mySeverityRegistrar = new SeverityRegistrarImpl(project.getMessageBus());
  }

  public static InspectionProjectProfileManagerImpl getInstanceImpl(Project project) {
    return (InspectionProjectProfileManagerImpl)InspectionProjectProfileManager.getInstance(project);
  }

  @Override
  public boolean isProfileLoaded() {
    return myName2Profile.containsKey(getInspectionProfile().getName());
  }

  @Nonnull
  public synchronized InspectionProfileWrapper getProfileWrapper() {
    final InspectionProfile profile = getInspectionProfile();
    final String profileName = profile.getName();
    if (!myName2Profile.containsKey(profileName)) {
      initProfileWrapper(profile);
    }
    return myName2Profile.get(profileName);
  }

  public InspectionProfileWrapper getProfileWrapper(final String profileName) {
    return myName2Profile.get(profileName);
  }

  @Override
  public void updateProfile(@Nonnull Profile profile) {
    super.updateProfile(profile);
    initProfileWrapper(profile);
  }

  @Override
  public void deleteProfile(@Nonnull String name) {
    super.deleteProfile(name);
    final InspectionProfileWrapper profileWrapper = myName2Profile.remove(name);
    if (profileWrapper != null) {
      profileWrapper.cleanup(myProject);
    }
  }

  @Override
  public void afterLoadState() {
    final Set<Profile> profiles = new HashSet<>();
    profiles.add(getProjectProfileImpl());
    profiles.addAll(getProfiles());
    profiles.addAll(InspectionProfileManager.getInstance().getProfiles());

    for (Profile profile : profiles) {
      initProfileWrapper(profile);
    }

    NamedScopesHolder.ScopeListener scopeListener = () -> {
      for (Profile profile : getProfiles()) {
        ((InspectionProfile)profile).scopesChanged();
      }
    };
    myHolder.addScopeListener(scopeListener, myProject);
    myLocalScopesHolder.addScopeListener(scopeListener, myProject);
  }

  @Override
  public void initProfileWrapper(@Nonnull Profile profile) {
    final InspectionProfileWrapper wrapper = new InspectionProfileWrapper((InspectionProfile)profile);
    myName2Profile.put(profile.getName(), wrapper);
  }

  @Override
  public void dispose() {
    for (InspectionProfileWrapper wrapper : myName2Profile.values()) {
      wrapper.cleanup(myProject);
    }
  }

  @Nonnull
  @Override
  public SeverityRegistrarImpl getSeverityRegistrar() {
    return mySeverityRegistrar;
  }

  @Nonnull
  @Override
  public SeverityRegistrarImpl getOwnSeverityRegistrar() {
    return mySeverityRegistrar;
  }

  @Override
  public void loadState(Element state) {
    try {
      mySeverityRegistrar.readExternal(state);
    }
    catch (InvalidDataException e) {
      LOG.error(e);
    }
    super.loadState(state);
  }

  @Override
  public Element getState() {
    Element state = super.getState();
    try {
      mySeverityRegistrar.writeExternal(state);
    }
    catch (WriteExternalException e) {
      LOG.error(e);
    }
    return state;
  }

  @Override
  public Profile getProfile(@Nonnull final String name) {
    return getProfile(name, true);
  }

  @Override
  public void convert(Element element) {
    super.convert(element);
    if (myProjectProfile != null) {
      ((ProfileEx)getProjectProfileImpl()).convert(element, getProject());
    }
  }
}

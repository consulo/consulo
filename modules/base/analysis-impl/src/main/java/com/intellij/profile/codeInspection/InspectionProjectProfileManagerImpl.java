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
package com.intellij.profile.codeInspection;

import com.intellij.codeInsight.daemon.impl.SeverityRegistrar;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.ex.InspectionProfileWrapper;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.profile.Profile;
import com.intellij.profile.ProfileEx;
import com.intellij.psi.search.scope.packageSet.NamedScopeManager;
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder;
import consulo.startup.DumbAwareStartupAction;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
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
public class InspectionProjectProfileManagerImpl extends InspectionProjectProfileManager implements Disposable {
  private final Map<String, InspectionProfileWrapper> myName2Profile = new ConcurrentHashMap<>();
  private final SeverityRegistrar mySeverityRegistrar;
  @Nonnull
  private final Application myApplication;
  private final NamedScopeManager myLocalScopesHolder;
  private NamedScopesHolder.ScopeListener myScopeListener;

  @Inject
  public InspectionProjectProfileManagerImpl(@Nonnull Application application,
                                             @Nonnull Project project,
                                             @Nonnull InspectionProfileManager inspectionProfileManager,
                                             @Nonnull DependencyValidationManager holder,
                                             @Nonnull NamedScopeManager localScopesHolder,
                                             @Nonnull StartupManager startupManager) {
    super(project, inspectionProfileManager, holder);
    myApplication = application;
    myLocalScopesHolder = localScopesHolder;
    mySeverityRegistrar = new SeverityRegistrar(project.getMessageBus());

    if (project.isDefault()) {
      return;
    }

    startupManager.registerPostStartupActivity((DumbAwareStartupAction)uiAccess -> {
      final Set<Profile> profiles = new HashSet<>();
      profiles.add(getProjectProfileImpl());
      profiles.addAll(getProfiles());
      profiles.addAll(InspectionProfileManager.getInstance().getProfiles());
      Runnable initInspectionProfilesRunnable = () -> {
        for (Profile profile : profiles) {
          initProfileWrapper(profile);
        }
        fireProfilesInitialized(uiAccess);
      };

      application.executeOnPooledThread(initInspectionProfilesRunnable);
      myScopeListener = () -> {
        for (Profile profile : getProfiles()) {
          ((InspectionProfile)profile).scopesChanged();
        }
      };
      myHolder.addScopeListener(myScopeListener);
      myLocalScopesHolder.addScopeListener(myScopeListener);
      Disposer.register(myProject, new Disposable() {
        @Override
        public void dispose() {
          myHolder.removeScopeListener(myScopeListener);
          myLocalScopesHolder.removeScopeListener(myScopeListener);
        }
      });
    });
  }

  public static InspectionProjectProfileManagerImpl getInstanceImpl(Project project) {
    return (InspectionProjectProfileManagerImpl)project.getComponent(InspectionProjectProfileManager.class);
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
  public void initProfileWrapper(@Nonnull Profile profile) {
    final InspectionProfileWrapper wrapper = new InspectionProfileWrapper((InspectionProfile)profile);
    wrapper.init(myProject);
    myName2Profile.put(profile.getName(), wrapper);
  }

  @Override
  public void dispose() {
    Runnable cleanupInspectionProfilesRunnable = () -> {
      for (InspectionProfileWrapper wrapper : myName2Profile.values()) {
        wrapper.cleanup(myProject);
      }
      fireProfilesShutdown();
    };

    if (myApplication.isUnitTestMode() || myApplication.isHeadlessEnvironment()) {
      cleanupInspectionProfilesRunnable.run();
    }
    else {
      myApplication.executeOnPooledThread(cleanupInspectionProfilesRunnable);
    }
  }

  @Nonnull
  @Override
  public SeverityRegistrar getSeverityRegistrar() {
    return mySeverityRegistrar;
  }

  @Nonnull
  @Override
  public SeverityRegistrar getOwnSeverityRegistrar() {
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

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
import consulo.language.editor.inspection.scheme.InspectionProfile;
import com.intellij.codeInspection.ex.InspectionProfileWrapper;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.application.dumb.DumbAwareRunnable;
import consulo.project.Project;
import consulo.project.startup.StartupManager;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import com.intellij.packageDependencies.DependencyValidationManager;
import consulo.language.editor.inspection.scheme.Profile;
import com.intellij.profile.ProfileEx;
import consulo.language.psi.search.scope.impl.NamedScopeManager;
import consulo.language.psi.search.scope.NamedScopesHolder;
import consulo.application.ui.awt.UIUtil;
import org.jdom.Element;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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
public class InspectionProjectProfileManagerImpl extends InspectionProjectProfileManager {
  private final Map<String, InspectionProfileWrapper> myName2Profile = new ConcurrentHashMap<String, InspectionProfileWrapper>();
  private final SeverityRegistrar mySeverityRegistrar;
  private final NamedScopeManager myLocalScopesHolder;
  private NamedScopesHolder.ScopeListener myScopeListener;

  @Inject
  public InspectionProjectProfileManagerImpl(@Nonnull Project project,
                                             @Nonnull InspectionProfileManager inspectionProfileManager,
                                             @Nonnull DependencyValidationManager holder,
                                             @Nonnull NamedScopeManager localScopesHolder) {
    super(project, inspectionProfileManager, holder);
    myLocalScopesHolder = localScopesHolder;
    mySeverityRegistrar = new SeverityRegistrar(project.getMessageBus());
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
  public void projectOpened() {
    StartupManager startupManager = StartupManager.getInstance(myProject);
    if (startupManager == null) {
      return; // upsource
    }
    startupManager.registerPostStartupActivity(new DumbAwareRunnable() {
      @Override
      public void run() {
        final Set<Profile> profiles = new HashSet<Profile>();
        profiles.add(getProjectProfileImpl());
        profiles.addAll(getProfiles());
        profiles.addAll(InspectionProfileManager.getInstance().getProfiles());
        final Application app = ApplicationManager.getApplication();
        Runnable initInspectionProfilesRunnable = new Runnable() {
          @Override
          public void run() {
            for (Profile profile : profiles) {
              initProfileWrapper(profile);
            }
            fireProfilesInitialized();
          }
        };
        if (app.isUnitTestMode() || app.isHeadlessEnvironment()) {
          initInspectionProfilesRunnable.run();
          UIUtil.dispatchAllInvocationEvents(); //do not restart daemon in the middle of the test
        }
        else {
          app.executeOnPooledThread(initInspectionProfilesRunnable);
        }
        myScopeListener = new NamedScopesHolder.ScopeListener() {
          @Override
          public void scopesChanged() {
            for (Profile profile : getProfiles()) {
              ((InspectionProfile)profile).scopesChanged();
            }
          }
        };
        myHolder.addScopeListener(myScopeListener, myProject);
        myLocalScopesHolder.addScopeListener(myScopeListener, myProject);
      }
    });
  }

  @Override
  public void initProfileWrapper(@Nonnull Profile profile) {
    final InspectionProfileWrapper wrapper = new InspectionProfileWrapper((InspectionProfile)profile);
    wrapper.init(myProject);
    myName2Profile.put(profile.getName(), wrapper);
  }

  @Override
  public void projectClosed() {
    final Application app = ApplicationManager.getApplication();
    Runnable cleanupInspectionProfilesRunnable = new Runnable() {
      @Override
      public void run() {
        for (InspectionProfileWrapper wrapper : myName2Profile.values()) {
          wrapper.cleanup(myProject);
        }
        fireProfilesShutdown();
      }
    };
    if (app.isUnitTestMode() || app.isHeadlessEnvironment()) {
      cleanupInspectionProfilesRunnable.run();
    }
    else {
      app.executeOnPooledThread(cleanupInspectionProfilesRunnable);
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

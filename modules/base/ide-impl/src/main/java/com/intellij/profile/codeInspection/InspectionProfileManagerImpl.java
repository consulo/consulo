/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import consulo.language.editor.rawHighlight.SeverityProvider;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.InspectionProfileConvertor;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.SeveritiesProvider;
import com.intellij.codeInsight.daemon.impl.SeverityRegistrarImpl;
import consulo.language.editor.inspection.InspectionsBundle;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolRegistrar;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.*;
import consulo.colorScheme.TextAttributesKey;
import com.intellij.openapi.options.BaseSchemeProcessor;
import com.intellij.openapi.options.SchemesManager;
import com.intellij.openapi.options.SchemesManagerFactory;
import consulo.component.persist.*;
import consulo.project.Project;
import consulo.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import consulo.language.editor.inspection.scheme.Profile;
import com.intellij.util.ArrayUtil;
import consulo.logging.Logger;
import consulo.ui.image.Image;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
@State(
        name = "InspectionProfileManager",
        storages = {
                @Storage(file = StoragePathMacros.APP_CONFIG + "/editor.xml"),
                @Storage(file = StoragePathMacros.APP_CONFIG + "/other.xml", deprecated = true)
        },
        additionalExportFile = InspectionProfileManager.FILE_SPEC
)
public class InspectionProfileManagerImpl extends InspectionProfileManager implements SeverityProvider, PersistentStateComponent<Element> {
  private final InspectionToolRegistrar myRegistrar;
  private final SchemesManager<Profile, InspectionProfileImpl> mySchemesManager;
  private final AtomicBoolean myProfilesAreInitialized = new AtomicBoolean(false);
  private final SeverityRegistrarImpl mySeverityRegistrar;

  protected static final Logger LOG = Logger.getInstance(InspectionProfileManagerImpl.class);

  public static InspectionProfileManagerImpl getInstanceImpl() {
    return (InspectionProfileManagerImpl)ServiceManager.getService(InspectionProfileManager.class);
  }

  @Inject
  public InspectionProfileManagerImpl(@Nonnull Application application, @Nonnull InspectionToolRegistrar registrar, @Nonnull SchemesManagerFactory schemesManagerFactory) {
    myRegistrar = registrar;
    registerProvidedSeverities();

    mySchemesManager = schemesManagerFactory.createSchemesManager(FILE_SPEC, new BaseSchemeProcessor<Profile, InspectionProfileImpl>() {
      @Nonnull
      @Override
      public InspectionProfileImpl readScheme(@Nonnull Element element) {
        final InspectionProfileImpl profile = new InspectionProfileImpl(InspectionProfileLoadUtil.getProfileName(element), myRegistrar, InspectionProfileManagerImpl.this);
        try {
          profile.readExternal(element);
        }
        catch (Exception e) {
          LOG.error(e);
          ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
              Messages.showErrorDialog(InspectionsBundle.message("inspection.error.loading.message", 0, profile.getName()),
                                       InspectionsBundle.message("inspection.errors.occurred.dialog.title"));
            }
          }, ModalityState.NON_MODAL);
        }
        return profile;
      }

      @Nonnull
      @Override
      public State getState(@Nonnull InspectionProfileImpl scheme) {
        return scheme.isProjectLevel() ? State.NON_PERSISTENT : (scheme.wasInitialized() ? State.POSSIBLY_CHANGED : State.UNCHANGED);
      }

      @Override
      public Element writeScheme(@Nonnull InspectionProfileImpl scheme) {
        Element root = new Element("inspections");
        root.setAttribute("profile_name", scheme.getName());
        scheme.serializeInto(root, false);
        return root;
      }

      @Override
      public void onSchemeAdded(@Nonnull final InspectionProfileImpl scheme) {
        updateProfileImpl(scheme);
        fireProfileChanged(scheme);
      }

      @Override
      public void onSchemeDeleted(@Nonnull final InspectionProfileImpl scheme) {
      }

      @Override
      public void onCurrentSchemeChanged(final InspectionProfileImpl oldCurrentScheme) {
        Profile current = mySchemesManager.getCurrentScheme();
        if (current != null) {
          fireProfileChanged((Profile)oldCurrentScheme, current, null);
        }
      }

      @Nonnull
      @Override
      public String getName(@Nonnull Profile immutableElement) {
        return immutableElement.getName();
      }
    }, RoamingType.DEFAULT);
    mySeverityRegistrar = new SeverityRegistrarImpl(application.getMessageBus());
  }

  @Nonnull
  private static InspectionProfileImpl createSampleProfile() {
    return new InspectionProfileImpl("Default");
  }

  public static void registerProvidedSeverities() {
    SeveritiesProvider.EP_NAME.forEachExtensionSafe(provider -> {
      for (HighlightInfoType t : provider.getSeveritiesHighlightInfoTypes()) {
        HighlightSeverity highlightSeverity = t.getSeverity(null);
        SeverityRegistrarImpl.registerStandard(t, highlightSeverity);
        TextAttributesKey attributesKey = t.getAttributesKey();
        Image icon = t instanceof HighlightInfoType.Iconable ? ((HighlightInfoType.Iconable)t).getIcon() : null;
        HighlightDisplayLevel.registerSeverity(highlightSeverity, attributesKey, icon);
      }
    });
  }

  @Override
  @Nonnull
  public Collection<Profile> getProfiles() {
    initProfiles();
    return mySchemesManager.getAllSchemes();
  }

  private volatile boolean LOAD_PROFILES = !ApplicationManager.getApplication().isUnitTestMode();
  @TestOnly
  public void forceInitProfiles(boolean flag) {
    LOAD_PROFILES = flag;
    myProfilesAreInitialized.set(false);
  }

  @Override
  public void initProfiles() {
    if (myProfilesAreInitialized.getAndSet(true)) {
      if (mySchemesManager.getAllSchemes().isEmpty()) {
        createDefaultProfile();
      }
      return;
    }
    if (!LOAD_PROFILES) return;

    mySchemesManager.loadSchemes();
    Collection<Profile> profiles = mySchemesManager.getAllSchemes();
    if (profiles.isEmpty()) {
      createDefaultProfile();
    }
    else {
      for (Profile profile : profiles) {
        addProfile(profile);
      }
    }
  }

  private void createDefaultProfile() {
    final InspectionProfileImpl defaultProfile = (InspectionProfileImpl)createProfile();
    defaultProfile.setBaseProfile(InspectionProfileImpl.getDefaultProfile());
    addProfile(defaultProfile);
  }


  @Override
  public Profile loadProfile(@Nonnull String path) throws IOException, JDOMException {
    final File file = new File(path);
    if (file.exists()) {
      try {
        return InspectionProfileLoadUtil.load(file, myRegistrar, this);
      }
      catch (IOException e) {
        throw e;
      }
      catch (JDOMException e) {
        throw e;
      }
      catch (Exception ignored) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            Messages.showErrorDialog(InspectionsBundle.message("inspection.error.loading.message", 0, file),
                                     InspectionsBundle.message("inspection.errors.occurred.dialog.title"));
          }
        }, ModalityState.NON_MODAL);
      }
    }
    return getProfile(path, false);
  }

  @Override
  public void updateProfile(@Nonnull Profile profile) {
    mySchemesManager.addNewScheme(profile, true);
    updateProfileImpl(profile);
  }

  private static void updateProfileImpl(@Nonnull Profile profile) {
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      InspectionProjectProfileManager.getInstance(project).initProfileWrapper(profile);
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

  @Nullable
  @Override
  public Element getState() {
    Element state = new Element("state");
    try {
      mySeverityRegistrar.writeExternal(state);
    }
    catch (WriteExternalException e) {
      throw new RuntimeException(e);
    }
    return state;
  }

  @Override
  public void loadState(Element state) {
    try {
      mySeverityRegistrar.readExternal(state);
    }
    catch (InvalidDataException e) {
      throw new RuntimeException(e);
    }
  }

  public InspectionProfileConvertor getConverter() {
    return new InspectionProfileConvertor(this);
  }

  @Override
  public Profile createProfile() {
    return createSampleProfile();
  }

  @Override
  public void setRootProfile(String rootProfile) {
    Profile current = mySchemesManager.getCurrentScheme();
    if (current != null && !Comparing.strEqual(rootProfile, current.getName())) {
      fireProfileChanged(current, getProfile(rootProfile), null);
    }
    mySchemesManager.setCurrentSchemeName(rootProfile);
  }

  @Override
  public Profile getProfile(@Nonnull final String name, boolean returnRootProfileIfNamedIsAbsent) {
    Profile found = mySchemesManager.findSchemeByName(name);
    if (found != null) return found;
    //profile was deleted
    if (returnRootProfileIfNamedIsAbsent) {
      return getRootProfile();
    }
    return null;
  }

  @Nonnull
  @Override
  public Profile getRootProfile() {
    Profile current = mySchemesManager.getCurrentScheme();
    if (current != null) return current;
    Collection<Profile> profiles = getProfiles();
    if (profiles.isEmpty()) return createSampleProfile();
    return profiles.iterator().next();
  }

  @Override
  public void deleteProfile(@Nonnull final String profile) {
    Profile found = mySchemesManager.findSchemeByName(profile);
    if (found != null) {
      mySchemesManager.removeScheme(found);
    }
  }

  @Override
  public void addProfile(@Nonnull final Profile profile) {
    mySchemesManager.addNewScheme(profile, true);
  }

  @Override
  @Nonnull
  public String[] getAvailableProfileNames() {
    return ArrayUtil.toStringArray(mySchemesManager.getAllSchemeNames());
  }

  @Override
  public Profile getProfile(@Nonnull final String name) {
    return getProfile(name, true);
  }
}

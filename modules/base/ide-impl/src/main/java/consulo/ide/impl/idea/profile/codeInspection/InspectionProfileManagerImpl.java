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
package consulo.ide.impl.idea.profile.codeInspection;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.colorScheme.TextAttributesKey;
import consulo.component.persist.*;
import consulo.component.persist.scheme.BaseSchemeProcessor;
import consulo.component.persist.scheme.SchemeManager;
import consulo.component.persist.scheme.SchemeManagerFactory;
import consulo.ide.ServiceManager;
import consulo.util.lang.Comparing;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileConvertor;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionToolRegistrar;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.language.editor.inspection.scheme.Profile;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.SeveritiesProvider;
import consulo.language.editor.rawHighlight.SeverityProvider;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.ex.awt.Messages;
import consulo.ui.image.Image;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jdom.JDOMException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

@Singleton
@State(name = "InspectionProfileManager", storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/editor.xml"),
  @Storage(file = StoragePathMacros.APP_CONFIG + "/other.xml", deprecated = true)}, additionalExportFile = InspectionProfileManager.FILE_SPEC)
@ServiceImpl
public class InspectionProfileManagerImpl extends InspectionProfileManager implements SeverityProvider, PersistentStateComponent<Element> {
  private final InspectionToolRegistrar myRegistrar;
  private final SchemeManager<InspectionProfile, InspectionProfileImpl> mySchemeManager;
  private final SeverityRegistrarImpl mySeverityRegistrar;

  protected static final Logger LOG = Logger.getInstance(InspectionProfileManagerImpl.class);

  public static InspectionProfileManagerImpl getInstanceImpl() {
    return (InspectionProfileManagerImpl)ServiceManager.getService(InspectionProfileManager.class);
  }

  @Inject
  public InspectionProfileManagerImpl(@Nonnull Application application,
                                      @Nonnull InspectionToolRegistrar registrar,
                                      @Nonnull SchemeManagerFactory schemeManagerFactory) {
    myRegistrar = registrar;
    registerProvidedSeverities();

    mySchemeManager =
      schemeManagerFactory.createSchemeManager(FILE_SPEC, new BaseSchemeProcessor<InspectionProfile, InspectionProfileImpl>() {
        @Nonnull
        @Override
        public InspectionProfileImpl readScheme(@Nonnull Element element) {
          final InspectionProfileImpl profile =
            new InspectionProfileImpl(InspectionProfileLoadUtil.getProfileName(element), myRegistrar, InspectionProfileManagerImpl.this);
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
            }, IdeaModalityState.NON_MODAL);
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
          Profile current = mySchemeManager.getCurrentScheme();
          if (current != null) {
            fireProfileChanged(oldCurrentScheme, current, null);
          }
        }

        @Nonnull
        @Override
        public String getName(@Nonnull InspectionProfile immutableElement) {
          return immutableElement.getName();
        }
      }, RoamingType.DEFAULT);
    mySeverityRegistrar = new SeverityRegistrarImpl(application.getMessageBus());
  }

  @Nonnull
  private InspectionProfileImpl createSampleProfile() {
    return new InspectionProfileImpl("Default", myRegistrar, this, false);
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
  public Collection<InspectionProfile> getProfiles() {
    return mySchemeManager.getAllSchemes();
  }

  @Override
  public void afterLoadState() {
    mySchemeManager.loadSchemes();
    Collection<InspectionProfile> profiles = mySchemeManager.getAllSchemes();
    if (profiles.isEmpty()) {
      createDefaultProfile();
    }
    else {
      for (InspectionProfile profile : profiles) {
        addProfile(profile);
      }
    }
  }

  private void createDefaultProfile() {
    final InspectionProfileImpl defaultProfile = (InspectionProfileImpl)createProfile();
    defaultProfile.setBaseProfile(InspectionProfileImpl.getDefaultProfile(myRegistrar, this));
    addProfile(defaultProfile);
  }

  @Override
  public Profile loadProfile(@Nonnull String path) throws IOException, JDOMException {
    final File file = new File(path);
    if (file.exists()) {
      try {
        return InspectionProfileLoadUtil.load(file, myRegistrar, this);
      }
      catch (IOException | JDOMException e) {
        throw e;
      }
      catch (Exception ignored) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            Messages.showErrorDialog(InspectionsBundle.message("inspection.error.loading.message", 0, file),
                                     InspectionsBundle.message("inspection.errors.occurred.dialog.title"));
          }
        }, IdeaModalityState.NON_MODAL);
      }
    }
    return getProfile(path, false);
  }

  @Override
  public void updateProfile(@Nonnull Profile profile) {
    mySchemeManager.addNewScheme((InspectionProfile)profile, true);
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
    Profile current = mySchemeManager.getCurrentScheme();
    if (current != null && !Comparing.strEqual(rootProfile, current.getName())) {
      fireProfileChanged(current, getProfile(rootProfile), null);
    }
    mySchemeManager.setCurrentSchemeName(rootProfile);
  }

  @Override
  public Profile getProfile(@Nonnull final String name, boolean returnRootProfileIfNamedIsAbsent) {
    Profile found = mySchemeManager.findSchemeByName(name);
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
    Profile current = mySchemeManager.getCurrentScheme();
    if (current != null) return current;
    Collection<InspectionProfile> profiles = getProfiles();
    if (profiles.isEmpty()) return createSampleProfile();
    return profiles.iterator().next();
  }

  @Override
  public void deleteProfile(@Nonnull final String profile) {
    InspectionProfile found = mySchemeManager.findSchemeByName(profile);
    if (found != null) {
      mySchemeManager.removeScheme(found);
    }
  }

  @Override
  public void addProfile(@Nonnull final Profile profile) {
    mySchemeManager.addNewScheme((InspectionProfile)profile, true);
  }

  @Override
  @Nonnull
  public String[] getAvailableProfileNames() {
    return ArrayUtil.toStringArray(mySchemeManager.getAllSchemeNames());
  }

  @Override
  public Profile getProfile(@Nonnull final String name) {
    return getProfile(name, true);
  }
}

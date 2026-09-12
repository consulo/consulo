/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.content.scope.NamedScopesHolder;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.editor.inspection.scheme.Profile;
import consulo.language.editor.rawHighlight.SeverityRegistrar;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * The production project profile manager lives in ide-impl which is excluded here.
 * The headless container delegates everything to the application-level profile manager —
 * enough for the daemon's document listeners and severity queries on the editor path.
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessInspectionProjectProfileManager implements InspectionProjectProfileManager {
    private final InspectionProfileManager myApplicationManager;

    @Inject
    public HeadlessInspectionProjectProfileManager(InspectionProfileManager applicationManager) {
        myApplicationManager = applicationManager;
    }

    @Override
    public SeverityRegistrar getSeverityRegistrar() {
        return myApplicationManager.getSeverityRegistrar();
    }

    @Override
    public SeverityRegistrar getOwnSeverityRegistrar() {
        return myApplicationManager.getOwnSeverityRegistrar();
    }

    @Override
    public InspectionProfile getInspectionProfile() {
        return (InspectionProfile) myApplicationManager.getRootProfile();
    }

    @Override
    public String getProfileName() {
        return getInspectionProfile().getName();
    }

    @Override
    public @Nullable String getProjectProfile() {
        return null;
    }

    @Override
    public void setProjectProfile(@Nullable String projectProfile) {
    }

    @Override
    public NamedScopesHolder getScopesManager() {
        return myApplicationManager.getScopesManager();
    }

    @Override
    public Collection<? extends Profile> getProfiles() {
        return myApplicationManager.getProfiles();
    }

    @Override
    public Profile getProfile(String name, boolean returnRootProfileIfNamedIsAbsent) {
        return myApplicationManager.getProfile(name, returnRootProfileIfNamedIsAbsent);
    }

    @Override
    public Profile getProfile(String name) {
        return myApplicationManager.getProfile(name);
    }

    @Override
    public void updateProfile(Profile profile) {
        myApplicationManager.updateProfile(profile);
    }

    @Override
    public String[] getAvailableProfileNames() {
        return myApplicationManager.getAvailableProfileNames();
    }

    @Override
    public void deleteProfile(String name) {
        myApplicationManager.deleteProfile(name);
    }
}

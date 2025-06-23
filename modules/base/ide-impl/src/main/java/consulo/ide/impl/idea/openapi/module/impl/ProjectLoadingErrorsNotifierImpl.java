/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.module.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.localize.LocalizeValue;
import consulo.module.ConfigurationErrorDescription;
import consulo.module.ConfigurationErrorType;
import consulo.module.ProjectLoadingErrorsNotifier;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.impl.internal.ProjectNotificationGroups;
import consulo.project.localize.ProjectLocalize;
import consulo.project.startup.StartupManager;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
@Singleton
@ServiceImpl
public class ProjectLoadingErrorsNotifierImpl extends ProjectLoadingErrorsNotifier {
    private final MultiMap<ConfigurationErrorType, ConfigurationErrorDescription> myErrors = new MultiMap<ConfigurationErrorType, ConfigurationErrorDescription>();
    private final Object myLock = new Object();
    private final Project myProject;

    @Inject
    public ProjectLoadingErrorsNotifierImpl(Project project) {
        myProject = project;
    }

    @Override
    public void registerError(ConfigurationErrorDescription errorDescription) {
        registerErrors(Collections.singletonList(errorDescription));
    }

    @Override
    public void registerErrors(Collection<? extends ConfigurationErrorDescription> errorDescriptions) {
        if (myProject.isDisposed() || myProject.isDefault() || errorDescriptions.isEmpty()) {
            return;
        }

        boolean first;
        synchronized (myLock) {
            first = myErrors.isEmpty();
            for (ConfigurationErrorDescription description : errorDescriptions) {
                myErrors.putValue(description.getErrorType(), description);
            }
        }
        if (myProject.isInitialized()) {
            fireNotifications();
        }
        else if (first) {
            StartupManager.getInstance(myProject).registerPostStartupActivity(new Runnable() {
                @Override
                public void run() {
                    fireNotifications();
                }
            });
        }
    }

    private void fireNotifications() {
        final MultiMap<ConfigurationErrorType, ConfigurationErrorDescription> descriptionsMap = new MultiMap<ConfigurationErrorType, ConfigurationErrorDescription>();
        synchronized (myLock) {
            if (myErrors.isEmpty()) {
                return;
            }
            descriptionsMap.putAllValues(myErrors);
            myErrors.clear();
        }

        for (final ConfigurationErrorType type : descriptionsMap.keySet()) {
            final Collection<ConfigurationErrorDescription> descriptions = descriptionsMap.get(type);
            if (descriptions.isEmpty()) {
                continue;
            }

            String invalidElements = getInvalidElementsString(type, descriptions);

            ProjectNotificationGroups.Project.newError()
                .title(LocalizeValue.localizeTODO("Error Loading Project"))
                .content(LocalizeValue.localizeTODO(
                    ProjectLocalize.errorMessageConfigurationCannotLoad() + " " + invalidElements + " <a href=\"\">Details...</a>"
                ))
                .listener((notification, event) -> {
                    List<ConfigurationErrorDescription> validDescriptions = ContainerUtil.findAll(descriptions, ConfigurationErrorDescription::isValid);
                    RemoveInvalidElementsDialog.showDialog(myProject, CommonLocalize.titleError().get(), type, invalidElements, validDescriptions);

                    notification.expire();
                })
                .notify(myProject);
        }

    }

    private static String getInvalidElementsString(ConfigurationErrorType type, Collection<ConfigurationErrorDescription> descriptions) {
        if (descriptions.size() == 1) {
            final ConfigurationErrorDescription description = ContainerUtil.getFirstItem(descriptions);
            return type.getElementKind() + " <b>" + description.getElementName() + "</b>";
        }

        return descriptions.size() + " " + StringUtil.pluralize(type.getElementKind());
    }
}

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
package consulo.project.impl.internal.store;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.persist.RoamingType;
import consulo.component.persist.Storage;
import consulo.component.store.impl.internal.*;
import consulo.component.store.internal.*;
import consulo.component.store.internal.StateStorage.SaveSession;
import consulo.component.store.impl.internal.storage.StorageData;
import consulo.component.store.impl.internal.storage.XmlElementStorage;
import consulo.project.Project;
import consulo.project.impl.internal.ProjectImpl;
import consulo.project.impl.internal.WelcomeProjectImpl;
import consulo.project.macro.ProjectPathMacroManager;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jdom.Element;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-03-08
 */
@Singleton
@ServiceImpl(profiles = ProjectImpl.WELCOME_PROJECT)
public class WelcomeProjectStoreImpl extends ProjectStoreImpl {
    private static final String ROOT_TAG_NAME = "welcomeProject";

    @Inject
    public WelcomeProjectStoreImpl(Project project,
                                   Provider<ProjectPathMacroManager> pathMacroManager,
                                   Provider<ApplicationDefaultStoreCache> applicationDefaultStoreCache) {
        super(project, pathMacroManager, applicationDefaultStoreCache);
    }

    @Nullable
    Element getStateCopy() {
        Element element = getProject().getStateElement();
        return element != null ? element.clone() : null;
    }

    @Override
    protected WelcomeProjectImpl getProject() {
        return (WelcomeProjectImpl) super.getProject();
    }

    @Override
    protected StateStorageManager createStateStorageManager() {
        final XmlElementStorage storage =
            new XmlElementStorage("", RoamingType.DISABLED, new TrackingPathMacroSubstitutorImpl(myPathMacroManager), ROOT_TAG_NAME, null, Application.get().getInstance(PathMacrosService.class)) {
                @Override
                protected @Nullable Element loadLocalData() {
                    return getStateCopy();
                }

                @Override
                protected XmlElementStorageSaveSession createSaveSession(StorageData storageData) {
                    return new XmlElementStorageSaveSession(storageData) {
                        @Override
                        protected void doSave(@Nullable Element element) {
                            getProject().setStateElement(element == null ? new Element("empty") : element);
                        }

                        @Override
                        protected boolean isCollapsePathsOnSave() {
                            return false;
                        }
                    };
                }
            };

        //noinspection deprecation
        return new StateStorageManager() {
            @Override
            public void addMacro(String macro, String expansion) {
                throw new UnsupportedOperationException("Method addMacro not implemented in " + getClass());
            }

            @Override
            public String buildFileSpec(Storage storage1) {
                throw new UnsupportedOperationException("Method buildFileSpec not implemented in " + getClass());
            }

            @Override
            public @Nullable TrackingPathMacroSubstitutor getMacroSubstitutor() {
                return null;
            }

            @Override
            public @Nullable StateStorage getStateStorage(Storage storageSpec) throws StateStorageException {
                return storage;
            }

            @Nullable
            @Override
            public StateStorage getStateStorage(String fileSpec, RoamingType roamingType) {
                return storage;
            }

            @Override
            public void clearStateStorage(String file) {
            }

            @Nullable
            @Override
            public ExternalizationSession startExternalization() {
                StateStorage.ExternalizationSession externalizationSession = storage.startExternalization();
                return externalizationSession == null ? null : new MyExternalizationSession(externalizationSession);
            }

            @Override
            public String expandMacros(String file) {
                throw new UnsupportedOperationException("Method expandMacros not implemented in " + getClass());
            }

            @Override
            public String collapseMacros(String path) {
                throw new UnsupportedOperationException("Method collapseMacros not implemented in " + getClass());
            }

            @Override
            public void setStreamProvider(@Nullable StreamProvider streamProvider) {
                throw new UnsupportedOperationException("Method setStreamProvider not implemented in " + getClass());
            }

            @Nullable
            @Override
            public StreamProvider getStreamProvider() {
                throw new UnsupportedOperationException("Method getStreamProviders not implemented in " + getClass());
            }

            @Override
            public Collection<String> getStorageFileNames() {
                throw new UnsupportedOperationException("Method getStorageFileNames not implemented in " + getClass());
            }
        };
    }

    @Override
    public void load() throws IOException {
        Element stateElement = getProject().getStateElement();
        if (stateElement != null) {
            reload(Arrays.asList(getMainStorage()));
        }
    }

    private static class MyExternalizationSession implements StateStorageManager.ExternalizationSession {
        final StateStorage.ExternalizationSession externalizationSession;

        public MyExternalizationSession(StateStorage.ExternalizationSession externalizationSession) {
            this.externalizationSession = externalizationSession;
        }

        @Override
        public void setState(Storage[] storageSpecs, Object component, String componentName, Object state) {
            externalizationSession.setState(component, componentName, state, null);
        }

        @Override
        public List<SaveSession> createSaveSessions(boolean force) {
            return ContainerUtil.createMaybeSingletonList(externalizationSession.createSaveSession(false));
        }
    }
}

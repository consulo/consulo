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
package consulo.sandboxPlugin.ide.externalSystem;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.externalSystem.autoimport.ExternalSystemProjectAware;
import consulo.externalSystem.autoimport.ExternalSystemProjectId;
import consulo.externalSystem.autoimport.ExternalSystemProjectListener;
import consulo.externalSystem.autoimport.ExternalSystemProjectReloadContext;
import consulo.externalSystem.autoimport.ExternalSystemRefreshStatus;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class SandBuildProjectAware implements ExternalSystemProjectAware {
    public static final ProjectSystemId SYSTEM_ID =
        new ProjectSystemId("SAND_BUILD", LocalizeValue.localizeTODO("Sand Build"), PlatformIconGroup.nodesModule());

    public static final String BUILD_FILE_NAME = "sand.build";

    private final ExternalSystemProjectId myProjectId;
    private final List<ExternalSystemProjectListener> myListeners = new CopyOnWriteArrayList<>();

    public SandBuildProjectAware(String basePath) {
        myProjectId = new ExternalSystemProjectId(SYSTEM_ID, basePath);
    }

    @Override
    public ExternalSystemProjectId getProjectId() {
        return myProjectId;
    }

    @Override
    public Set<String> getSettingsFiles() {
        return Set.of(myProjectId.getExternalProjectPath() + "/" + BUILD_FILE_NAME);
    }

    @Override
    public void subscribe(ExternalSystemProjectListener listener, Disposable parentDisposable) {
        myListeners.add(listener);
        Disposer.register(parentDisposable, () -> myListeners.remove(listener));
    }

    @Override
    public void reloadProject(ExternalSystemProjectReloadContext context) {
        myListeners.forEach(ExternalSystemProjectListener::onProjectReloadStart);
        AppExecutorUtil.getAppScheduledExecutorService().schedule(
            () -> myListeners.forEach(listener -> listener.onProjectReloadFinish(ExternalSystemRefreshStatus.SUCCESS)),
            1,
            TimeUnit.SECONDS
        );
    }
}

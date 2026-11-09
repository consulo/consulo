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
package consulo.continuousIntegration;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.disposer.Disposable;
import consulo.project.Project;

import java.util.List;

/**
 * Servers of a project: configured ones plus the ones detected from accounts.
 *
 * @since 2026-11-09
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface CIManager {
    static CIManager getInstance(Project project) {
        return project.getInstance(CIManager.class);
    }

    List<CIServer> getServers();

    void addListener(CIManagerListener listener, Disposable parentDisposable);
}

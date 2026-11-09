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
package consulo.continuousIntegration.impl.internal.monitor;

import consulo.continuousIntegration.CIBuild;
import consulo.continuousIntegration.CIJob;
import consulo.continuousIntegration.CIServer;

import java.util.EventListener;

/**
 * Called from any thread when a build started, stopped or changed its status through the monitor or an action.
 *
 * @since 2026-11-09
 */
public interface CIBuildMonitorListener extends EventListener {
    void buildChanged(CIServer server, CIJob job, CIBuild build);
}

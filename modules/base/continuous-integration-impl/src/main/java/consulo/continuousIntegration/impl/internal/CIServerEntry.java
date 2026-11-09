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
package consulo.continuousIntegration.impl.internal;

import consulo.continuousIntegration.CIServer;
import consulo.continuousIntegration.CIServerOptions;
import consulo.continuousIntegration.CIServerType;
import consulo.project.Project;

/**
 * Server options with their type; {@code detected} entries come from accounts and are not stored.
 *
 * @since 2026-11-09
 */
public final class CIServerEntry {
    private final CIServerType<?> myType;
    private final CIServerOptions myOptions;
    private final boolean myDetected;

    public CIServerEntry(CIServerType<?> type, CIServerOptions options, boolean detected) {
        myType = type;
        myOptions = options;
        myDetected = detected;
    }

    public CIServerType<?> getType() {
        return myType;
    }

    public CIServerOptions getOptions() {
        return myOptions;
    }

    public boolean isDetected() {
        return myDetected;
    }

    public String getKey() {
        return myType.getDetectionKeyUnchecked(myOptions);
    }

    public CIServerEntry copy(boolean detected) {
        return new CIServerEntry(myType, myType.copyOptions(myOptions), detected);
    }

    public CIServer createServer(Project project) {
        return myType.createServerUnchecked(project, myOptions);
    }
}

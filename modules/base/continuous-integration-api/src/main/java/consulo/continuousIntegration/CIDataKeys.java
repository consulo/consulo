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

import consulo.util.dataholder.Key;

/**
 * Selection of the CI tool window, for actions added to its toolbar or popup menu. A selected build also provides
 * its job and server, a selected job provides its server.
 *
 * @since 2026-11-09
 */
public final class CIDataKeys {
    public static final Key<CIServer> SERVER = Key.create("ContinuousIntegration.Server");
    public static final Key<CIJob> JOB = Key.create("ContinuousIntegration.Job");
    public static final Key<CIBuild> BUILD = Key.create("ContinuousIntegration.Build");

    private CIDataKeys() {
    }
}

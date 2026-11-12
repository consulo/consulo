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
package consulo.remoteServer.platformAware;

import consulo.localize.LocalizeValue;

/**
 * When the bundles (SDKs) a platform target already has are detected and added - the way the local machine's are at startup.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
public enum PredefinedBundlesMode {
    /**
     * On the first connect only, kept as regular bundles - later connects add nothing.
     */
    ONCE(LocalizeValue.localizeTODO("Once")),
    /**
     * On the first connect of each session, as predefined bundles - like the ones of the local machine.
     */
    ON_FIRST_CONNECT(LocalizeValue.localizeTODO("On first connect")),
    NEVER(LocalizeValue.localizeTODO("Never"));

    private final LocalizeValue myText;

    PredefinedBundlesMode(LocalizeValue text) {
        myText = text;
    }

    public LocalizeValue getText() {
        return myText;
    }
}

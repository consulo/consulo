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
package consulo.continuousIntegration.impl.internal.setting;

import consulo.continuousIntegration.CIServerOptions;
import consulo.continuousIntegration.CIServerOptionsUtil;
import consulo.continuousIntegration.CIServerType;
import consulo.continuousIntegration.impl.internal.CIServerEntry;
import org.jspecify.annotations.Nullable;

/**
 * Server being edited in the settings: a copy of its options, the options it was loaded with, and whether it was
 * detected from an account. A detected server becomes configured once it is changed.
 *
 * @since 2026-11-09
 */
final class CIServerDraft {
    private final CIServerType<?> myType;
    private final CIServerOptions myOptions;
    private final @Nullable CIServerOptions myOriginal;
    private final boolean myDetected;

    CIServerDraft(CIServerType<?> type, CIServerOptions options, @Nullable CIServerOptions original, boolean detected) {
        myType = type;
        myOptions = options;
        myOriginal = original;
        myDetected = detected;
    }

    static CIServerDraft of(CIServerEntry entry) {
        CIServerType<?> type = entry.getType();
        return new CIServerDraft(type, type.copyOptions(entry.getOptions()), type.copyOptions(entry.getOptions()), entry.isDetected());
    }

    CIServerType<?> getType() {
        return myType;
    }

    CIServerOptions getOptions() {
        return myOptions;
    }

    boolean isDetected() {
        return myDetected;
    }

    boolean isChanged() {
        CIServerOptions original = myOriginal;
        return original == null || !CIServerOptionsUtil.isEqual(original, myOptions);
    }

    boolean isConfigured() {
        return !myDetected || isChanged();
    }

    /**
     * Key the server had when it was loaded, used to hide a detected server.
     */
    String getKey() {
        CIServerOptions original = myOriginal;
        return myType.getDetectionKeyUnchecked(original == null ? myOptions : original);
    }

    String getCurrentKey() {
        return myType.getDetectionKeyUnchecked(myOptions);
    }

    CIServerEntry toEntry() {
        return new CIServerEntry(myType, myType.copyOptions(myOptions), false);
    }
}

// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.localHistory;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * Allows to provide presentation for the activities recorded in the local history.
 *
 * @see ActivityId
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ActivityPresentationProvider {
    /**
     * Unique provider identifier.
     */
    String getId();

    /**
     * Icon to use for the activity presentation.
     *
     * @param kind activity identifier
     */
    default @Nullable Image getIcon(String kind) {
        return null;
    }
}

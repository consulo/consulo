// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.collaboration;

import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.annotation.DeprecationInfo;
import consulo.annotation.internal.MigratedExtensionsTo;
import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.PropertyKey;

@Deprecated
@DeprecationInfo("Use CollaborationToolsLocalize")
@MigratedExtensionsTo(CollaborationToolsLocalize.class)
public class CollaborationToolsBundle extends AbstractBundle {
    private static final CollaborationToolsBundle ourInstance = new CollaborationToolsBundle();

    private CollaborationToolsBundle() {
        super("consulo.collaboration.CollaborationToolsBundle");
    }

    public static String message(@PropertyKey(resourceBundle = "consulo.collaboration.CollaborationToolsBundle") String key) {
        return ourInstance.getMessage(key);
    }

    public static String message(@PropertyKey(resourceBundle = "consulo.collaboration.CollaborationToolsBundle") String key, Object... params) {
        return ourInstance.getMessage(key, params);
    }
}

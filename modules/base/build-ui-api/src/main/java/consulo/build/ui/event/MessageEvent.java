// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.event;

import consulo.annotation.DeprecationInfo;
import consulo.navigation.Navigable;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;
import org.jspecify.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 */
public interface MessageEvent extends BuildEvent {
    enum Kind {
        ERROR,
        WARNING,
        INFO,
        STATISTICS,
        SIMPLE
    }

    Kind getKind();

    NotificationGroup getGroup();

    @Nullable
    Navigable getNavigable(Project project);

    @Deprecated
    @DeprecationInfo("Use #getNavigable() with typo-fixed name")
    @SuppressWarnings({"SpellCheckingInspection", "deprecation"})
    default @Nullable Navigatable getNavigatable(Project project) {
        return (Navigatable) getNavigable(project);
    }

    MessageEventResult getResult();
}

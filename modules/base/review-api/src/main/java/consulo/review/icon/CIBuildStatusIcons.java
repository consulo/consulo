// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.icon;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;

public final class CIBuildStatusIcons {
    private CIBuildStatusIcons() {
    }

    public static Image pending() {
        return PlatformIconGroup.runconfigurationsTestnotran();
    }

    public static Image cancelled() {
        return PlatformIconGroup.runconfigurationsTestignored();
    }

    public static Image inProgress() {
        return PlatformIconGroup.processStep_1();
    }

    public static Image failed() {
        return PlatformIconGroup.runconfigurationsTesterror();
    }

    public static Image failedInProgress() {
        return PlatformIconGroup.statusFailedinprogress();
    }

    public static Image warning() {
        return PlatformIconGroup.generalWarning();
    }

    public static Image skipped() {
        return PlatformIconGroup.runconfigurationsTestskipped();
    }

    public static Image paused() {
        return PlatformIconGroup.runconfigurationsTestpaused();
    }

    public static Image info() {
        return PlatformIconGroup.generalInformation();
    }

    public static Image success() {
        return PlatformIconGroup.statusSuccess();
    }
}

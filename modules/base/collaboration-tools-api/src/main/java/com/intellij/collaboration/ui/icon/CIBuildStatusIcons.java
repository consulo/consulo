// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.icon;

import consulo.application.AllIcons;
import consulo.platform.base.icon.PlatformIconGroup;
import jakarta.annotation.Nonnull;

import javax.swing.*;

public final class CIBuildStatusIcons {
    private CIBuildStatusIcons() {
    }

    public static @Nonnull Icon getPending() {
        return PlatformIconGroup.runconfigurationsTestnotran();
    }

    public static @Nonnull Icon getCancelled() {
        return PlatformIconGroup.runconfigurationsTestignored();
    }

    public static @Nonnull Icon getInProgress() {
        return PlatformIconGroup.processStep_1();
    }

    public static @Nonnull Icon getFailed() {
        return PlatformIconGroup.runconfigurationsTesterror();
    }

    public static @Nonnull Icon getFailedInProgress() {
        return AllIcons.Status.FailedInProgress;
    }

    public static @Nonnull Icon getWarning() {
        return PlatformIconGroup.generalWarning();
    }

    public static @Nonnull Icon getSkipped() {
        return PlatformIconGroup.runconfigurationsTestskipped();
    }

    public static @Nonnull Icon getPaused() {
        return PlatformIconGroup.runconfigurationsTestpaused();
    }

    public static @Nonnull Icon getInfo() {
        return ExperimentalUI.isNewUI() ? PlatformIconGroup.generalInformation() : PlatformIconGroup.runconfigurationsTestpassed();
    }

    public static @Nonnull Icon getSuccess() {
        return ExperimentalUI.isNewUI() ? AllIcons.Status.Success : PlatformIconGroup.runconfigurationsTestpassed();
    }
}

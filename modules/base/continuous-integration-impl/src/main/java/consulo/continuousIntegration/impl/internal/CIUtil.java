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

import consulo.continuousIntegration.CIBuild;
import consulo.continuousIntegration.CIBuildStatus;
import consulo.continuousIntegration.CIJob;
import consulo.continuousIntegration.CIServer;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * Keys, icons and error texts shared by the tool window, the build monitor and the actions.
 *
 * @since 2026-11-09
 */
public final class CIUtil {
    private CIUtil() {
    }

    /**
     * Stable identity of a server: servers are recreated when accounts or settings change, their key stays.
     */
    public static String serverKey(CIServer server) {
        return server.getType().getDetectionKeyUnchecked(server.getOptions());
    }

    public static String jobKey(CIServer server, CIJob job) {
        return serverKey(server) + "\n" + job.id();
    }

    public static String buildKey(CIServer server, CIBuild build) {
        return serverKey(server) + "\n" + build.jobId() + "\n" + build.id();
    }

    public static Image statusIcon(CIBuildStatus status) {
        return switch (status) {
            case SUCCESS -> PlatformIconGroup.statusSuccess();
            case UNSTABLE -> PlatformIconGroup.generalWarning();
            case FAILURE -> PlatformIconGroup.generalError();
            case ABORTED -> PlatformIconGroup.runconfigurationsTestterminated();
            case NOT_BUILT -> PlatformIconGroup.runconfigurationsTestnotran();
            case RUNNING -> PlatformIconGroup.actionsExecute();
            case QUEUED -> PlatformIconGroup.runconfigurationsTestpaused();
            case UNKNOWN -> PlatformIconGroup.runconfigurationsTestunknown();
        };
    }

    public static Image jobIcon(CIJob job) {
        if (job.kind().isContainer()) {
            return PlatformIconGroup.nodesFolder();
        }
        CIBuild lastBuild = job.lastBuild();
        if (lastBuild != null && lastBuild.status().isActive()) {
            return statusIcon(lastBuild.status());
        }
        if (job.queued()) {
            return statusIcon(CIBuildStatus.QUEUED);
        }
        return statusIcon(job.status());
    }

    /**
     * Message of the failure, without the wrappers added by {@link java.util.concurrent.CompletableFuture}.
     */
    public static String errorMessage(Throwable error) {
        Throwable cause = error;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return StringUtil.notNullize(cause.getMessage(), cause.getClass().getSimpleName());
    }
}

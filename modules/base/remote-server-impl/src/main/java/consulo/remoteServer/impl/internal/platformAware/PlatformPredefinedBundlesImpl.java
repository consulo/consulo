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
package consulo.remoteServer.impl.internal.platformAware;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkTable;
import consulo.content.internal.PlatformBundleDetector;
import consulo.application.platform.PlatformTarget;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.remoteServer.platformAware.PlatformPredefinedBundles;
import consulo.remoteServer.platformAware.PredefinedBundlesMode;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects the bundles of a platform target on connect, as the server configuration asks - once per session.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
@Singleton
@ServiceImpl
public class PlatformPredefinedBundlesImpl implements PlatformPredefinedBundles {
    private static final Logger LOG = Logger.getInstance(PlatformPredefinedBundlesImpl.class);

    private final Application myApplication;
    private final SdkTable mySdkTable;
    private final PlatformBundleDetector myDetector;
    // target id -> its detection of this session; a caller during a running detection waits for the same one
    private final Map<String, CompletableFuture<Boolean>> myDetections = new ConcurrentHashMap<>();

    @Inject
    public PlatformPredefinedBundlesImpl(Application application, SdkTable sdkTable, PlatformBundleDetector detector) {
        myApplication = application;
        mySdkTable = sdkTable;
        myDetector = detector;
    }

    @Override
    public CompletableFuture<Boolean> targetConnected(PlatformTarget target, Platform platform, PredefinedBundlesMode mode) {
        if (mode == PredefinedBundlesMode.NEVER) {
            return CompletableFuture.completedFuture(false);
        }
        CompletableFuture<Boolean> started = new CompletableFuture<>();
        CompletableFuture<Boolean> running = myDetections.putIfAbsent(target.getId(), started);
        if (running != null) {
            return running;
        }
        boolean predefined = mode == PredefinedBundlesMode.ON_FIRST_CONNECT;

        // detecting runs the tools of the platform - off the UI thread, adding the bundles needs the write lock
        Coroutine<Void, Boolean> detection = Coroutine
            .first(CodeExecution.<Void, List<Sdk>>apply(input -> detect(target, platform, predefined)))
            .then(WriteLock.<List<Sdk>, Boolean>apply(bundles -> {
                myDetector.add(bundles, predefined);
                return true;
            }));

        CompletableFuture<Boolean> result = detection.runAsync(CoroutineScope.of(myApplication.coroutineContext()), null).toFuture();
        result.whenComplete((detected, error) -> {
            if (error != null) {
                LOG.warn("Failed to detect the bundles of " + target.getId(), error);
                myDetections.remove(target.getId());
                started.completeExceptionally(error);
            }
            else {
                started.complete(detected);
            }
        });
        return started;
    }

    private List<Sdk> detect(PlatformTarget target, Platform platform, boolean predefined) {
        List<Sdk> detected = myDetector.detect(target.getId(), platform, predefined);
        List<Sdk> result = new ArrayList<>();
        for (Sdk bundle : detected) {
            if (!exists(bundle)) {
                result.add(bundle);
            }
        }
        LOG.info("Detected " + detected.size() + " bundle(s) of " + target.getId() + ", " + result.size() + " new: "
            + result.stream().map(Sdk::getName).toList());
        return result;
    }

    /**
     * A bundle of the same type and home on the same target is there already - added by hand or detected before.
     */
    private boolean exists(Sdk bundle) {
        for (Sdk sdk : mySdkTable.getAllSdks()) {
            if (sdk.getSdkType() == bundle.getSdkType()
                && Objects.equals(sdk.getPlatformTargetId(), bundle.getPlatformTargetId())
                && Objects.equals(sdk.getHomePath(), bundle.getHomePath())) {
                return true;
            }
        }
        return false;
    }
}

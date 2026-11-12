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
package consulo.content.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.content.bundle.Sdk;
import consulo.platform.Platform;

import java.util.List;

/**
 * Creates the bundles of a platform with the providers of the predefined bundles - the policy of when and which ones is the
 * business of the platform targets.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface PlatformBundleDetector {
    /**
     * The bundles found on {@code platform}, owned by the target {@code platformTargetId} - created, not added to the table.
     * Runs the tools of the platform: not on the UI thread.
     *
     * @param predefined named like the predefined bundles of the local machine, otherwise like bundles added by hand
     */
    List<Sdk> detect(String platformTargetId, Platform platform, boolean predefined);

    /**
     * Adds detected bundles to the table - under the write lock.
     */
    void add(List<Sdk> bundles, boolean predefined);
}

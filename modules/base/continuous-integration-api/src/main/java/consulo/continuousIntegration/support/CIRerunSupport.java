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
package consulo.continuousIntegration.support;

import consulo.continuousIntegration.CIBuild;

import java.util.concurrent.CompletableFuture;

/**
 * Starts a finished build again with the same parameters and revision.
 *
 * @since 2026-11-09
 */
public interface CIRerunSupport {
    CompletableFuture<CIBuild> rerun(CIBuild build, boolean failedOnly);

    default boolean canRerunFailed(CIBuild build) {
        return false;
    }
}

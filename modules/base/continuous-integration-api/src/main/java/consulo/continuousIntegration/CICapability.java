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
package consulo.continuousIntegration;

/**
 * What a {@link CIServer} can do, used to enable actions. Optional parts with their own methods are also reachable
 * through {@link CIServer#getSupport(Class)}.
 *
 * @since 2026-11-09
 */
public enum CICapability {
    RUN,
    RUN_WITH_REF,
    STOP,
    LOG,
    /**
     * The log of a running build can be read by offset while it grows. Without it the log is read once the build or
     * stage is finished.
     */
    PROGRESSIVE_LOG
}

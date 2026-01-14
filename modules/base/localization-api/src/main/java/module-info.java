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

/**
 * @author UNV
 * @since 2026-01-12
 */
module consulo.localization.api {
    requires transitive consulo.annotation;
    requires transitive consulo.disposer.api;

    requires consulo.container.api;

    exports consulo.localization;

    uses consulo.localization.LocalizationManager;

    exports consulo.localization.internal to
        consulo.application.impl,
        consulo.localization.impl,
        consulo.localize.api;
}
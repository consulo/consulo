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
 * @author VISTALL
 * @since 2020-10-19
 */
module consulo.localize.api {
    requires transitive consulo.annotation;
    requires transitive consulo.disposer.api;
    requires transitive consulo.localization.api;

    requires consulo.container.api;

    exports consulo.localize;

    uses consulo.localize.LocalizeManager;

    exports consulo.localize.internal to
        consulo.application.impl;
}
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
import org.jspecify.annotations.NullMarked;

/**
 * @since 2026-11-09
 */
@NullMarked
@SuppressWarnings("module")
module consulo.continuous.integration.impl {
    requires transitive consulo.continuous.integration.api;
    requires consulo.project.ui.api;
    requires consulo.configurable.api;
    requires consulo.ui.ex.api;
    requires consulo.web.browser.api;
    requires consulo.execution.api;
    requires consulo.file.chooser.api;

    opens consulo.continuousIntegration.impl.internal to consulo.util.xml.serializer;
    opens consulo.continuousIntegration.impl.internal.action to consulo.component.impl;
    opens consulo.continuousIntegration.impl.internal.monitor to consulo.proxy;
}

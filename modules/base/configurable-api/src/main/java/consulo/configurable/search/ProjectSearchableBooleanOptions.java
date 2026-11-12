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
package consulo.configurable.search;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.localize.LocalizeValue;

import java.util.List;

/**
 * The check boxes of a project settings page, to be switched without opening it - {@link SearchableBooleanOptions} of
 * the check boxes a configurable binds to a project service.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface ProjectSearchableBooleanOptions {
    /**
     * The {@link consulo.configurable.Configurable#getId() id} of the page.
     */
    String getConfigurableId();

    /**
     * The name of the page - empty when it has no localized one.
     */
    LocalizeValue getConfigurableName();

    List<SearchableBooleanOption> getOptions();
}

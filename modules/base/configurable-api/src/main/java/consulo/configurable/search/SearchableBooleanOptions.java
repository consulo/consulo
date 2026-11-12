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
 * The check boxes of a settings page, to be switched without opening it - from Find Action, as {@code Page: Option}.
 * <p/>
 * Generated at compile time, as {@code <Configurable>_BooleanOptions}, for the check boxes a configurable binds to an
 * application service with a getter and a setter - the ones bound to a project service are
 * {@link ProjectSearchableBooleanOptions}.
 *
 * @author VISTALL
 * @since 2026-11-11
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface SearchableBooleanOptions {
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

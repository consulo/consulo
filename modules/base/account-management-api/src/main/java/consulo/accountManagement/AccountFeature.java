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
package consulo.accountManagement;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface AccountFeature<S extends AccountScope> {
    String getId();

    Class<? extends AccountType<S>> getAccountType();

    LocalizeValue getTitle();

    LocalizeValue getDescription();

    Set<S> getRequiredScopes();

    default Set<S> getOptionalScopes() {
        return Set.of();
    }

    default boolean isOptional() {
        return false;
    }

    @Nullable
    default String getInfoUrl() {
        return null;
    }

    default List<AccountFeatureAction> getActions() {
        return List.of();
    }
}

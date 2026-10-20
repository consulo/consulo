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
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface AccountType<S extends AccountScope> {
    String getId();

    LocalizeValue getName();

    Image getIcon();

    Class<S> getScopeClass();

    default Set<S> getBaseScopes() {
        return Set.of();
    }

    @Nullable
    default String getDefaultServerUrl() {
        return null;
    }

    default boolean isServerEditable() {
        return false;
    }

    List<? extends AccountAuthorizeConfig> getAuthorizeConfigs();

    CompletableFuture<AccountDetails> loadDetails(AccountConnection connection);

    default CompletableFuture<Set<S>> loadGrantedScopes(AccountConnection connection, Set<S> requested) {
        return CompletableFuture.completedFuture(requested);
    }

    default AccountFeatureState getState(AccountFeature<S> feature, Set<S> granted) {
        return granted.containsAll(feature.getRequiredScopes()) ? AccountFeatureState.AUTHORIZED : AccountFeatureState.MISSING_SCOPES;
    }

    default Set<S> getAllScopes() {
        S[] constants = getScopeClass().getEnumConstants();
        return constants == null ? Set.of() : new LinkedHashSet<>(List.of(constants));
    }
}

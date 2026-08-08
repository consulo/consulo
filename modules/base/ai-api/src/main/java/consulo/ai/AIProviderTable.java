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
package consulo.ai;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Registry of provider types and of the instances the user has configured - the analogue of
 * {@link consulo.content.bundle.SdkTable}.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface AIProviderTable {
    static AIProviderTable getInstance() {
        return Application.get().getInstance(AIProviderTable.class);
    }

    /**
     * Every installed provider type, configured or not.
     */
    List<AIProviderType> getTypes();

    @Nullable AIProviderType findType(String typeId);

    List<AIProvider> getProviders();

    /**
     * Instances that have credentials and can actually answer.
     */
    List<AIProvider> getConfiguredProviders();

    @Nullable AIProvider findProvider(String name);

    /**
     * @throws IllegalArgumentException when the name is already taken
     */
    AIProvider addProvider(String name, AIProviderType type);

    void removeProvider(AIProvider provider);

    /**
     * Instance chosen in settings, or the first configured one when nothing is chosen.
     */
    @Nullable AIProvider getDefaultProvider();

    /**
     * Model chosen in settings, or the first model of the default instance.
     */
    @Nullable AIModel getDefaultModel();

    void setDefault(@Nullable AIProvider provider, @Nullable AIModel model);
}

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
package consulo.it.ai;

import consulo.ai.AIModel;
import consulo.ai.AIModelCapability;
import consulo.ai.AIProvider;
import consulo.ai.AIProviderTable;
import consulo.ai.AIProviderType;
import consulo.application.Application;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.internal.HeadlessAIProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Provider types come from the extension point; the instances the user configures are persisted, so
 * both halves of the SDK-style split are covered here.
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class AIProviderTableTest {
    @AfterEach
    public void removeConfiguredProviders(Application application) {
        AIProviderTable table = application.getInstance(AIProviderTable.class);
        for (AIProvider provider : table.getProviders()) {
            table.removeProvider(provider);
        }
    }

    @Test
    public void typesComeFromTheExtensionPoint(Application application) {
        AIProviderTable table = application.getInstance(AIProviderTable.class);

        assertThat(table.getTypes()).extracting(AIProviderType::getId).contains(HeadlessAIProvider.ID);
        assertThat(table.findType(HeadlessAIProvider.ID)).isNotNull();
        assertThat(table.findType("nope")).isNull();
    }

    @Test
    public void modelsBelongToTheTypeAndCarryCapabilities(Application application) {
        AIProviderType type = application.getInstance(AIProviderTable.class).findType(HeadlessAIProvider.ID);
        assertThat(type).isNotNull();

        AIModel large = type.getModels().stream()
            .filter(model -> model.getId().equals(HeadlessAIProvider.LARGE_MODEL_ID))
            .findFirst()
            .orElseThrow();

        assertThat(large.getProviderTypeId()).isEqualTo(HeadlessAIProvider.ID);
        assertThat(large.getContextWindow()).isEqualTo(100_000);
        assertThat(large.hasCapability(AIModelCapability.TOOL_USE)).isTrue();
    }

    @Test
    public void severalInstancesOfOneTypeCanCoexist(Application application) {
        AIProviderTable table = application.getInstance(AIProviderTable.class);
        AIProviderType type = table.findType(HeadlessAIProvider.ID);
        assertThat(type).isNotNull();

        AIProvider work = table.addProvider("work", type);
        AIProvider personal = table.addProvider("personal", type);

        assertThat(table.getProviders()).containsExactly(work, personal);
        assertThat(work.getType()).isSameAs(personal.getType());

        // each instance owns its credentials
        work.setApiKey("work-key");
        assertThat(work.getApiKey()).isEqualTo("work-key");
        assertThat(personal.getApiKey()).isNull();
        assertThat(table.getConfiguredProviders()).containsExactly(work);
    }

    @Test
    public void instanceNamesAreUnique(Application application) {
        AIProviderTable table = application.getInstance(AIProviderTable.class);
        AIProviderType type = table.findType(HeadlessAIProvider.ID);
        assertThat(type).isNotNull();

        table.addProvider("only", type);
        assertThatThrownBy(() -> table.addProvider("only", type)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void removingAnInstanceDropsItsKey(Application application) {
        AIProviderTable table = application.getInstance(AIProviderTable.class);
        AIProviderType type = table.findType(HeadlessAIProvider.ID);
        assertThat(type).isNotNull();

        AIProvider provider = table.addProvider("temporary", type);
        provider.setApiKey("secret");

        table.removeProvider(provider);

        assertThat(table.findProvider("temporary")).isNull();
        // a stale key must not be handed to a later instance that happens to reuse the name
        assertThat(table.addProvider("temporary", type).getApiKey()).isNull();
    }

    @Test
    public void defaultSelectionRoundTripsAndFallsBack(Application application) {
        AIProviderTable table = application.getInstance(AIProviderTable.class);
        AIProviderType type = table.findType(HeadlessAIProvider.ID);
        assertThat(type).isNotNull();

        AIProvider provider = table.addProvider("main", type);
        provider.setApiKey("key");

        AIModel large = type.getModels().stream()
            .filter(model -> model.getId().equals(HeadlessAIProvider.LARGE_MODEL_ID))
            .findFirst()
            .orElseThrow();

        table.setDefault(provider, large);
        assertThat(table.getDefaultProvider()).isEqualTo(provider);
        assertThat(table.getDefaultModel()).isEqualTo(large);

        // clearing falls back to a usable instance rather than leaving callers with null
        table.setDefault(null, null);
        assertThat(table.getDefaultProvider()).isEqualTo(provider);
        assertThat(table.getDefaultModel()).isNotNull();
    }
}

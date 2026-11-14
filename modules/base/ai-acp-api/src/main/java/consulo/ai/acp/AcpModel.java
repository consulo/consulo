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
package consulo.ai.acp;

import consulo.ai.AIEffortLevel;
import consulo.ai.AIModel;
import consulo.ai.AIModelCapability;
import consulo.localize.LocalizeValue;

import java.util.List;
import java.util.Set;

/**
 * A model of an agent - the one the agent picks itself, or a value of the model option of its sessions. The efforts
 * are the levels the agent offered while the model was the current one of a session.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public record AcpModel(String id,
                       LocalizeValue displayName,
                       LocalizeValue description,
                       String providerTypeId,
                       List<AIEffortLevel> effortLevels) implements AIModel {
    public static final String DEFAULT_ID = "default";

    public AcpModel(String id, LocalizeValue displayName, String providerTypeId) {
        this(id, displayName, LocalizeValue.empty(), providerTypeId, List.of());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getProviderTypeId() {
        return providerTypeId;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return displayName;
    }

    @Override
    public LocalizeValue getDescription() {
        return description;
    }

    @Override
    public int getContextWindow() {
        return -1;
    }

    @Override
    public Set<AIModelCapability> getCapabilities() {
        return Set.of(AIModelCapability.STREAMING, AIModelCapability.THINKING);
    }

    @Override
    public List<AIEffortLevel> getEffortLevels() {
        return effortLevels;
    }
}

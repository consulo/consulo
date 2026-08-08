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
package consulo.sandboxPlugin.ide.ai;

import consulo.ai.AIMessage;
import consulo.ai.AIRequest;
import consulo.ai.AIResponse;
import consulo.ai.AIStopReason;
import consulo.ai.AIStreamListener;
import consulo.ai.AIUsage;
import consulo.ai.AIModel;
import consulo.ai.AIModelCapability;
import consulo.ai.AIProvider;
import consulo.ai.AIProviderType;
import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.Set;

/**
 * Fake provider used to exercise the AI settings UI and the model registry without needing a real
 * vendor plugin or a network call.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
@ExtensionImpl
@Singleton
public class SandAIProvider implements AIProviderType {
    public static final String ID = "sand";

    private static final class SandModel implements AIModel {
        private final String myId;
        private final int myContextWindow;
        private final Set<AIModelCapability> myCapabilities;

        private SandModel(String id, int contextWindow, Set<AIModelCapability> capabilities) {
            myId = id;
            myContextWindow = contextWindow;
            myCapabilities = capabilities;
        }

        @Override
        public String getId() {
            return myId;
        }

        @Override
        public String getProviderTypeId() {
            return ID;
        }

        @Override
        public LocalizeValue getDisplayName() {
            return LocalizeValue.localizeTODO("Sand " + myId);
        }

        @Override
        public int getContextWindow() {
            return myContextWindow;
        }

        @Override
        public Set<AIModelCapability> getCapabilities() {
            return myCapabilities;
        }
    }

    private final List<AIModel> myModels = List.of(
        new SandModel("small", 32_000, Set.of(AIModelCapability.STREAMING)),
        new SandModel("large", 200_000, Set.of(AIModelCapability.STREAMING,
                                               AIModelCapability.TOOL_USE,
                                               AIModelCapability.VISION,
                                               AIModelCapability.CACHING)));

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Sand AI");
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.actionsLightning();
    }

    @Override
    public List<AIModel> getModels() {
        return myModels;
    }

    /**
     * Echoes the last user turn back. Enough to drive the chat plumbing and the streaming callback
     * without a network call.
     */
    @Override
    public CompletableFuture<AIResponse> chat(AIProvider provider, AIRequest request, @Nullable AIStreamListener listener) {
        String answer = "echo: " + (request.getMessages().isEmpty()
            ? ""
            : request.getMessages().getLast().getText());

        if (listener != null) {
            for (String word : answer.split(" ")) {
                listener.onTextDelta(word + " ");
            }
        }

        return CompletableFuture.completedFuture(
            new AIResponse(AIMessage.assistant(answer), AIStopReason.END_TURN, AIUsage.UNKNOWN));
    }
}

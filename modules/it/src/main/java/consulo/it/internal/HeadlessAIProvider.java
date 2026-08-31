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
package consulo.it.internal;

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
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.Set;

/**
 * Provider used by the AI registry tests. Always reports itself configured, so the tests do not need
 * a password safe.
 */
@ExtensionImpl
@Singleton
public class HeadlessAIProvider implements AIProviderType {
    public static final String ID = "headless";
    public static final String SMALL_MODEL_ID = "headless-small";
    public static final String LARGE_MODEL_ID = "headless-large";

    private record HeadlessModel(String getId, int getContextWindow, Set<AIModelCapability> getCapabilities)
        implements AIModel {

        @Override
        public String getProviderTypeId() {
            return ID;
        }

        @Override
        public LocalizeValue getDisplayName() {
            return LocalizeValue.of(getId);
        }
    }

    private final List<AIModel> myModels = List.of(
        new HeadlessModel(SMALL_MODEL_ID, 1_000, Set.of(AIModelCapability.STREAMING)),
        new HeadlessModel(LARGE_MODEL_ID, 100_000, Set.of(AIModelCapability.STREAMING, AIModelCapability.TOOL_USE)));

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.of("Headless AI");
    }

    @Override
    public Image getIcon() {
        return ImageEffects.empty(16);
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

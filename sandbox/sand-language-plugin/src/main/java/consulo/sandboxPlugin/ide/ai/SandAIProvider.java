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

import consulo.ai.AIChatInfo;
import consulo.ai.AIMessage;
import consulo.ai.AIRequest;
import consulo.ai.AIResponse;
import consulo.ai.AIStopReason;
import consulo.ai.AIStreamListener;
import consulo.ai.AIUsage;
import consulo.ai.AIModel;
import consulo.ai.AIModelCapability;
import consulo.ai.AIProvider;
import consulo.ai.AIProviderTypeWithChats;
import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.image.Image;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.Set;

/**
 * Fake provider used to exercise the AI settings UI and the model registry without needing a real
 * vendor plugin or a network call. It keeps a few generated chats of its own - long conversations to try the chat on -
 * and leaves the chats the user starts to the platform. {@code /long} streams a long answer of markdown.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
@ExtensionImpl
@Singleton
public class SandAIProvider implements AIProviderTypeWithChats {
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

    @Override
    public CompletableFuture<List<AIChatInfo>> getChats(AIProvider provider, Project project) {
        return CompletableFuture.completedFuture(SandChats.list());
    }

    @Override
    public CompletableFuture<List<AIMessage>> loadChat(AIProvider provider, Project project, String chatId) {
        return CompletableFuture.supplyAsync(() -> SandChats.load(chatId));
    }

    /**
     * The generated chats are kept here; a chat the user starts is kept by the platform.
     */
    @Override
    public boolean keepsChats(AIProvider provider) {
        return false;
    }

    /**
     * Echoes the last user turn back. Enough to drive the chat plumbing and the streaming callback
     * without a network call.
     */
    @Override
    public CompletableFuture<AIResponse> chat(AIProvider provider, AIRequest request, @Nullable AIStreamListener listener) {
        String prompt = request.getMessages().isEmpty() ? "" : request.getMessages().getLast().getText();
        if (prompt.strip().equals("/long")) {
            return streamLong(listener);
        }
        String answer = "echo: " + prompt;

        if (listener != null) {
            for (String word : answer.split(" ")) {
                listener.onTextDelta(word + " ");
            }
        }

        return CompletableFuture.completedFuture(
            new AIResponse(AIMessage.assistant(answer), AIStopReason.END_TURN, AIUsage.UNKNOWN));
    }

    /**
     * A long answer of markdown, a few characters at a time the way a model streams - to try how the chat keeps up.
     */
    private static CompletableFuture<AIResponse> streamLong(@Nullable AIStreamListener listener) {
        StringBuilder answer = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            answer.append(SandChats.markdown(i)).append("\n").append(SandChats.paragraph(i)).append("\n\n");
        }
        String text = answer.toString();
        return CompletableFuture.supplyAsync(() -> {
            for (int start = 0; start < text.length(); start += 12) {
                if (listener != null) {
                    listener.onTextDelta(text.substring(start, Math.min(text.length(), start + 12)));
                }
                try {
                    Thread.sleep(10);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return new AIResponse(AIMessage.assistant(text), AIStopReason.END_TURN, AIUsage.UNKNOWN);
        });
    }
}

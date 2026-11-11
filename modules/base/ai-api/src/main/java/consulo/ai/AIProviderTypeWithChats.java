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

import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * A provider type which keeps the chats on its own side - a command line tool with a history of its own. The platform
 * then stores only which chats there are and how the user filed them, and reads the transcript back from the provider
 * when a chat is opened again; for any other type it stores the transcript itself.
 * <p>
 * A chat opened again keeps its {@linkplain AIChatSession#getId() id}, so the provider resumes its own conversation
 * when it sees a session id it already knows.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public interface AIProviderTypeWithChats extends AIProviderType {
    /**
     * Whether the provider keeps the chats of the account - a type speaking a protocol which leaves keeping them to
     * the agent behind it may find out only once it talks to it. While false, the platform stores the transcript
     * itself, as for any other type.
     */
    default boolean keepsChats(AIProvider provider) {
        return true;
    }

    /**
     * Starts a chat and answers its id - a provider whose agent names its sessions itself starts one here. By default
     * the platform makes the id up.
     */
    default CompletableFuture<String> newChat(AIProvider provider, Project project, @Nullable Path workingDirectory) {
        return CompletableFuture.completedFuture(UUID.randomUUID().toString());
    }

    /**
     * The chats the provider keeps for the project, newest first.
     */
    CompletableFuture<List<AIChatInfo>> getChats(AIProvider provider, Project project);

    /**
     * The chat alone - by default looked up among {@link #getChats}. The platform asks it after every answer, so a
     * title the provider generated for the chat shows up while the user has not named the chat.
     */
    default CompletableFuture<@Nullable AIChatInfo> getChat(AIProvider provider, Project project, String chatId) {
        return getChats(provider, project).thenApply(chats -> {
            for (AIChatInfo chat : chats) {
                if (chat.id().equals(chatId)) {
                    return chat;
                }
            }
            return null;
        });
    }

    /**
     * The history of the chat, empty when the provider does not know it.
     */
    CompletableFuture<List<AIMessage>> loadChat(AIProvider provider, Project project, String chatId);

    /**
     * The user renamed the chat; a provider which shows its chats elsewhere too keeps the name. By default only the
     * platform keeps it.
     */
    default CompletableFuture<?> renameChat(AIProvider provider, Project project, String chatId, String title) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Drops the chat on the provider side. By default the provider keeps it, and only the platform forgets it.
     */
    default CompletableFuture<?> deleteChat(AIProvider provider, Project project, String chatId) {
        return CompletableFuture.completedFuture(null);
    }
}

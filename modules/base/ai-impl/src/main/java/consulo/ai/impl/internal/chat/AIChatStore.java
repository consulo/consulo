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
package consulo.ai.impl.internal.chat;

import consulo.ai.AIMessage;
import consulo.ai.AIProvider;
import consulo.ai.AIProviderTable;
import consulo.ai.AIProviderTypeWithChats;
import consulo.ai.impl.internal.chat.state.AIChatEntry;
import consulo.ai.impl.internal.chat.state.AIChatGroupEntry;
import consulo.ai.impl.internal.chat.state.AIChatStoreState;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.util.ProjectUtil;
import consulo.ui.UIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The chats of a project and the groups the user filed them in. Which chats there are is kept in the workspace for
 * every provider; the transcript only for a provider which does not keep it itself - see
 * {@link AIProviderTypeWithChats} - in a file of its own under the caches of the project.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
@State(name = "AIChats", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class AIChatStore implements PersistentStateComponent<AIChatStoreState> {
    private static final Logger LOG = Logger.getInstance(AIChatStore.class);




    private final Project myProject;
    private final Provider<AIProviderTable> myProviderTable;
    private final ApplicationConcurrency myApplicationConcurrency;
    private final List<Runnable> myListeners = new CopyOnWriteArrayList<>();

    private AIChatStoreState myState = new AIChatStoreState();

    @Inject
    public AIChatStore(Project project, Provider<AIProviderTable> providerTable, ApplicationConcurrency applicationConcurrency) {
        myProject = project;
        myProviderTable = providerTable;
        myApplicationConcurrency = applicationConcurrency;
    }

    @Override
    public AIChatStoreState getState() {
        return myState;
    }

    @Override
    public void loadState(AIChatStoreState state) {
        myState = state;
        fireChanged();
    }

    /**
     * Told on the thread which changed the store, for any change of its groups or chats.
     */
    public void addListener(Runnable listener, Disposable parentDisposable) {
        myListeners.add(listener);
        Disposer.register(parentDisposable, () -> myListeners.remove(listener));
    }

    public void fireChanged() {
        for (Runnable listener : myListeners) {
            listener.run();
        }
    }

    // region groups

    public List<AIChatGroupEntry> getGroups(String parentId) {
        List<AIChatGroupEntry> groups = new ArrayList<>();
        for (AIChatGroupEntry group : myState.GROUPS) {
            if (group.PARENT_ID.equals(parentId)) {
                groups.add(group);
            }
        }
        groups.sort((a, b) -> a.NAME.compareToIgnoreCase(b.NAME));
        return groups;
    }

    public List<AIChatGroupEntry> getAllGroups() {
        return List.copyOf(myState.GROUPS);
    }

    public @Nullable AIChatGroupEntry findGroup(String id) {
        for (AIChatGroupEntry group : myState.GROUPS) {
            if (group.ID.equals(id)) {
                return group;
            }
        }
        return null;
    }

    public AIChatGroupEntry addGroup(String name, String parentId) {
        AIChatGroupEntry group = new AIChatGroupEntry();
        group.ID = UUID.randomUUID().toString();
        group.NAME = name;
        group.PARENT_ID = parentId;
        myState.GROUPS.add(group);
        fireChanged();
        return group;
    }

    public void renameGroup(AIChatGroupEntry group, String name) {
        group.NAME = name;
        fireChanged();
    }

    /**
     * Whether {@code group} is the group of {@code ancestorId} or lies below it - a group can not be moved into itself.
     */
    public boolean isInside(AIChatGroupEntry group, String ancestorId) {
        String id = group.ID;
        while (!id.isEmpty()) {
            if (id.equals(ancestorId)) {
                return true;
            }
            AIChatGroupEntry current = findGroup(id);
            id = current == null ? "" : current.PARENT_ID;
        }
        return false;
    }

    public void moveGroup(AIChatGroupEntry group, String parentId) {
        AIChatGroupEntry parent = findGroup(parentId);
        if (parent != null && isInside(parent, group.ID)) {
            return;
        }
        group.PARENT_ID = parentId;
        fireChanged();
    }

    /**
     * Drops the group; what it held moves up into its parent rather than being deleted with it.
     */
    public void removeGroup(AIChatGroupEntry group) {
        for (AIChatGroupEntry child : myState.GROUPS) {
            if (child.PARENT_ID.equals(group.ID)) {
                child.PARENT_ID = group.PARENT_ID;
            }
        }
        for (AIChatEntry chat : myState.CHATS) {
            if (chat.GROUP_ID.equals(group.ID)) {
                chat.GROUP_ID = group.PARENT_ID;
            }
        }
        myState.GROUPS.remove(group);
        fireChanged();
    }

    // endregion

    // region chats

    /**
     * The chats of a group, newest first.
     */
    public List<AIChatEntry> getChats(String groupId) {
        List<AIChatEntry> chats = new ArrayList<>();
        for (AIChatEntry chat : myState.CHATS) {
            String chatGroup = findGroup(chat.GROUP_ID) == null ? "" : chat.GROUP_ID;
            if (chatGroup.equals(groupId)) {
                chats.add(chat);
            }
        }
        chats.sort((a, b) -> Long.compare(b.UPDATED, a.UPDATED));
        return chats;
    }

    public @Nullable AIChatEntry findChat(String id) {
        for (AIChatEntry chat : myState.CHATS) {
            if (chat.ID.equals(id)) {
                return chat;
            }
        }
        return null;
    }

    public AIChatEntry addChat(String id, AIProvider provider, String modelId, String title, String groupId, @Nullable Path workingDirectory) {
        AIChatEntry chat = new AIChatEntry();
        chat.ID = id;
        chat.PROVIDER_NAME = provider.getName();
        chat.MODEL_ID = modelId;
        chat.TITLE = title;
        chat.GROUP_ID = groupId;
        chat.WORKING_DIRECTORY = workingDirectory == null ? "" : workingDirectory.toString();
        chat.CREATED = System.currentTimeMillis();
        chat.UPDATED = chat.CREATED;
        myState.CHATS.add(chat);
        fireChanged();
        return chat;
    }

    public void moveChat(AIChatEntry chat, String groupId) {
        chat.GROUP_ID = groupId;
        fireChanged();
    }

    /**
     * The user named the chat: kept here, and told to a provider which keeps its chats itself.
     */
    public CompletableFuture<?> renameChat(AIChatEntry chat, String title) {
        chat.TITLE = title;
        chat.NAMED_BY_USER = true;
        fireChanged();
        AIProvider provider = findProvider(chat);
        if (provider != null && provider.getType() instanceof AIProviderTypeWithChats withChats) {
            return withChats.renameChat(provider, myProject, chat.ID, title);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * A title which is not the user's - the first line sent, or one the provider generated. A name the user gave wins.
     */
    public void suggestTitle(AIChatEntry chat, String title) {
        if (chat.NAMED_BY_USER || title.isBlank() || title.equals(chat.TITLE)) {
            return;
        }
        chat.TITLE = title;
        fireChanged();
    }

    /**
     * Takes the title the provider keeps for the chat, when it keeps its chats itself.
     */
    public CompletableFuture<?> refreshTitle(AIChatEntry chat, UIAccess uiAccess) {
        AIProvider provider = findProvider(chat);
        if (chat.NAMED_BY_USER || provider == null || !(provider.getType() instanceof AIProviderTypeWithChats withChats)) {
            return CompletableFuture.completedFuture(null);
        }
        return withChats.getChat(provider, myProject, chat.ID).thenAcceptAsync(info -> {
            if (info != null) {
                suggestTitle(chat, info.title());
            }
        }, uiAccess);
    }

    public void changed(AIChatEntry chat) {
        chat.UPDATED = System.currentTimeMillis();
        fireChanged();
    }

    /**
     * Forgets the chat and drops its transcript - ours, or the one of the provider.
     */
    public CompletableFuture<?> removeChat(AIChatEntry chat) {
        myState.CHATS.remove(chat);
        fireChanged();

        AIProvider provider = findProvider(chat);
        CompletableFuture<?> providerDelete = provider != null && provider.getType() instanceof AIProviderTypeWithChats withChats
            ? withChats.deleteChat(provider, myProject, chat.ID)
            : CompletableFuture.completedFuture(null);
        // our transcript goes either way - the provider may not have kept the chat while it was written
        return providerDelete.thenRunAsync(() -> {
            try {
                Files.deleteIfExists(transcriptFile(chat.ID));
            }
            catch (IOException e) {
                LOG.warn("Cannot delete the transcript of the chat " + chat.ID, e);
            }
        }, myApplicationConcurrency.executor());
    }

    public @Nullable AIProvider findProvider(AIChatEntry chat) {
        return myProviderTable.get().findProvider(chat.PROVIDER_NAME);
    }

    public @Nullable Path getWorkingDirectory(AIChatEntry chat) {
        return chat.WORKING_DIRECTORY.isEmpty() ? null : Path.of(chat.WORKING_DIRECTORY);
    }

    // endregion

    // region transcripts

    public static boolean keepsOwnChats(AIProvider provider) {
        return provider.getType() instanceof AIProviderTypeWithChats withChats && withChats.keepsChats(provider);
    }

    /**
     * The history of the chat - from the provider when it keeps chats, from our file otherwise and whenever the
     * provider has nothing for the chat: it may keep only some chats, or not have kept this one when it was written.
     */
    public CompletableFuture<List<AIMessage>> loadTranscript(AIChatEntry chat, AIProvider provider) {
        // a provider may keep some chats - the ones it started itself - and leave the others to us
        if (provider.getType() instanceof AIProviderTypeWithChats withChats) {
            return withChats.loadChat(provider, myProject, chat.ID)
                .exceptionally(error -> {
                    LOG.warn("The provider can not load the chat " + chat.ID, error);
                    return List.of();
                })
                .thenCompose(messages -> messages.isEmpty() ? readTranscript(chat.ID) : CompletableFuture.completedFuture(messages));
        }
        return readTranscript(chat.ID);
    }

    private CompletableFuture<List<AIMessage>> readTranscript(String id) {
        return CompletableFuture.supplyAsync(() -> {
            Path file = transcriptFile(id);
            if (!Files.exists(file)) {
                return List.of();
            }
            try {
                return AIChatTranscript.read(Files.readString(file));
            }
            catch (IOException | RuntimeException e) {
                LOG.warn("Cannot read the transcript of the chat " + id, e);
                return List.of();
            }
        }, myApplicationConcurrency.executor());
    }

    /**
     * Keeps the history of the chat, unless the provider keeps it itself.
     */
    public CompletableFuture<?> saveTranscript(AIChatEntry chat, AIProvider provider, List<AIMessage> messages) {
        if (keepsOwnChats(provider)) {
            return CompletableFuture.completedFuture(null);
        }
        String id = chat.ID;
        List<AIMessage> copy = List.copyOf(messages);
        return CompletableFuture.runAsync(() -> {
            try {
                Path file = transcriptFile(id);
                Files.createDirectories(Objects.requireNonNull(file.getParent()));
                Files.writeString(file, AIChatTranscript.write(copy));
            }
            catch (IOException e) {
                LOG.warn("Cannot write the transcript of the chat " + id, e);
            }
        }, myApplicationConcurrency.executor());
    }

    private Path transcriptFile(String chatId) {
        return ProjectUtil.getProjectCachePath(myProject, "aiChats").resolve(chatId + ".json");
    }

    // endregion
}

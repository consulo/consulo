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

import consulo.ai.AIChatInfo;
import consulo.ai.AIMessage;
import consulo.ai.AIModel;
import consulo.ai.AIProvider;
import consulo.ai.AIProviderTable;
import consulo.ai.AIProviderTypeWithChats;
import consulo.ai.localize.AILocalize;
import consulo.ai.impl.internal.chat.state.AIChatEntry;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditorManager;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.InputBoxBuilder;
import consulo.ui.Label;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LoadingLayout;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Opens the chats of a project in editor tabs - one tab per chat, its title the title of the chat - and starts, renames
 * and deletes them for the tree of the AI chat tool window.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class AIChatView {
    private static final Logger LOG = Logger.getInstance(AIChatView.class);
    private static final int TITLE_LENGTH = 40;

    /**
     * A chat the provider keeps which the store does not know yet - one started outside of the IDE.
     */
    public record ProviderChat(AIProvider provider, AIChatInfo info) {
    }

    private final Project myProject;
    private final AIChatStore myStore;
    private final Provider<AIProviderTable> myProviderTable;
    private final Provider<FileEditorManager> myFileEditorManager;
    private final Provider<VirtualFileManager> myVirtualFileManager;
    private final Map<String, AIChatVirtualFile> myFiles = new ConcurrentHashMap<>();

    @Inject
    public AIChatView(Project project,
                      AIChatStore store,
                      Provider<AIProviderTable> providerTable,
                      Provider<FileEditorManager> fileEditorManager,
                      Provider<VirtualFileManager> virtualFileManager) {
        myProject = project;
        myStore = store;
        myProviderTable = providerTable;
        myFileEditorManager = fileEditorManager;
        myVirtualFileManager = virtualFileManager;

        store.addListener(this::updateTitles, project);
    }

    public AIChatStore getStore() {
        return myStore;
    }

    @RequiredUIAccess
    public void openChat(AIChatEntry chat, boolean focus) {
        myFileEditorManager.get().openFile(file(chat), focus);
    }

    /**
     * The tab file of a chat the store knows - the one the editor manager reopens with the project.
     */
    @Nullable AIChatVirtualFile findFile(String chatId) {
        AIChatEntry chat = myStore.findChat(chatId);
        return chat == null ? null : file(chat);
    }

    private AIChatVirtualFile file(AIChatEntry chat) {
        AIProvider provider = myStore.findProvider(chat);
        return myFiles.computeIfAbsent(chat.ID, id -> new AIChatVirtualFile(
            Objects.requireNonNull(myVirtualFileManager.get().getFileSystem(AIChatVirtualFileSystem.PROTOCOL)),
            AIChatVirtualFileSystem.path(myProject, id),
            id,
            title(chat),
            provider == null ? null : provider.getType().getIcon()
        ));
    }

    /**
     * Starts a chat with the account and opens it.
     */
    @RequiredUIAccess
    public CompletableFuture<AIChatEntry> startChat(AIProvider provider, @Nullable AIModel model, String groupId) {
        AIModel chatModel = model != null ? model : defaultModel(provider);
        String basePath = myProject.getBasePath();
        Path workingDirectory = basePath == null ? null : Path.of(basePath);
        CompletableFuture<String> id = provider.getType() instanceof AIProviderTypeWithChats withChats
            ? withChats.newChat(provider, myProject, workingDirectory)
            : CompletableFuture.completedFuture(UUID.randomUUID().toString());
        UIAccess uiAccess = UIAccess.current();
        return id.thenApplyAsync(chatId -> {
            AIChatEntry chat = myStore.addChat(
                chatId,
                provider,
                chatModel == null ? "" : chatModel.getId(),
                "",
                groupId,
                workingDirectory
            );
            openChat(chat, true);
            return chat;
        }, uiAccess);
    }

    /**
     * Files a chat of the provider into the store - at the top, where the user moves it from - and opens it.
     */
    @RequiredUIAccess
    public AIChatEntry openProviderChat(ProviderChat providerChat, String groupId) {
        AIChatEntry chat = myStore.findChat(providerChat.info().id());
        if (chat == null) {
            AIModel model = defaultModel(providerChat.provider());
            chat = myStore.addChat(
                providerChat.info().id(),
                providerChat.provider(),
                model == null ? "" : model.getId(),
                providerChat.info().title(),
                groupId,
                providerChat.info().workingDirectory()
            );
            chat.UPDATED = providerChat.info().updated().toEpochMilli();
        }
        openChat(chat, true);
        return chat;
    }

    /**
     * The chats every provider which keeps its chats itself has for the project, less the ones the store knows.
     */
    public CompletableFuture<List<ProviderChat>> loadProviderChats() {
        List<CompletableFuture<List<ProviderChat>>> futures = new ArrayList<>();
        for (AIProvider provider : myProviderTable.get().getProviders()) {
            if (provider.getType() instanceof AIProviderTypeWithChats withChats) {
                futures.add(withChats.getChats(provider, myProject)
                    .thenApply(chats -> chats.stream().map(info -> new ProviderChat(provider, info)).toList())
                    .exceptionally(error -> {
                        LOG.warn("Cannot list the chats of " + provider.getName(), error);
                        return List.of();
                    }));
            }
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(ignored -> {
            LOG.debug("Listing the chats of the providers of " + myProject.getName());
            List<ProviderChat> result = new ArrayList<>();
            Set<String> seen = ConcurrentHashMap.newKeySet();
            for (CompletableFuture<List<ProviderChat>> future : futures) {
                for (ProviderChat chat : future.join()) {
                    if (myStore.findChat(chat.info().id()) == null && seen.add(chat.info().id())) {
                        result.add(chat);
                    }
                }
            }
            result.sort((a, b) -> b.info().updated().compareTo(a.info().updated()));
            return result;
        });
    }

    @RequiredUIAccess
    public void renameChat(AIChatEntry chat, Component parent) {
        UIAccess uiAccess = UIAccess.current();
        InputBoxBuilder.text()
            .title(AILocalize.dialogRenameChatTitle())
            .text(AILocalize.dialogRenameChatPrompt())
            .value(title(chat))
            .showAsync(parent)
            .whenCompleteAsync((name, error) -> {
                if (error == null && name != null && !name.isBlank()) {
                    myStore.renameChat(chat, name.strip());
                }
            }, uiAccess);
    }

    @RequiredUIAccess
    public CompletableFuture<?> deleteChat(AIChatEntry chat) {
        AIChatVirtualFile file = myFiles.remove(chat.ID);
        if (file != null) {
            myFileEditorManager.get().closeFile(file);
        }
        return myStore.removeChat(chat);
    }

    /**
     * Deletes a chat the provider keeps which nobody filed - from the provider, as there is nothing of ours to drop.
     */
    public CompletableFuture<?> deleteProviderChat(ProviderChat providerChat) {
        AIProvider provider = providerChat.provider();
        if (!(provider.getType() instanceof AIProviderTypeWithChats withChats)) {
            return CompletableFuture.completedFuture(null);
        }
        AIChatVirtualFile file = myFiles.remove(providerChat.info().id());
        if (file != null) {
            myFileEditorManager.get().closeFile(file);
        }
        return withChats.deleteChat(provider, myProject, providerChat.info().id());
    }

    /**
     * Fills the editor tab of the chat once its history is loaded.
     */
    @RequiredUIAccess
    void fill(AIChatVirtualFile file, LoadingLayout<DockLayout> root, AIChatFileEditor editor) {
        AIChatEntry chat = myStore.findChat(file.getChatId());
        if (chat == null) {
            root.stopLoading(layout -> layout.center(hint(AILocalize.labelChatRemoved())));
            return;
        }
        AIProvider provider = myStore.findProvider(chat);
        if (provider == null) {
            root.stopLoading(layout -> layout.center(hint(AILocalize.labelChatAccountMissing(chat.PROVIDER_NAME))));
            return;
        }

        root.startLoading(AILocalize.labelChatLoading());
        UIAccess uiAccess = UIAccess.current();
        myStore.loadTranscript(chat, provider).whenCompleteAsync((history, error) -> {
            if (editor.isDisposed()) {
                return;
            }
            if (error != null) {
                LOG.warn("Cannot load the chat " + chat.ID, error);
            }
            try {
                AIChatPanel panel = new AIChatPanel(
                    myProject,
                    uiAccess,
                    provider,
                    findModel(provider, chat.MODEL_ID),
                    chat.ID,
                    history == null ? List.of() : history,
                    myStore.getWorkingDirectory(chat),
                    chat.EFFORT,
                    new ChatListener(chat, provider, uiAccess)
                );
                Disposer.register(editor, panel);
                root.stopLoading(layout -> layout.center(panel.getComponent()));
            }
            catch (RuntimeException e) {
                // a failure here would stay in the future nobody waits for, with the tab loading forever
                LOG.error("Cannot open the chat " + chat.ID, e);
                root.stopLoading(layout -> layout.center(hint(AILocalize.labelFailed(String.valueOf(e.getMessage())))));
            }
        }, uiAccess);
    }

    public static String title(AIChatEntry chat) {
        return chat.TITLE.isBlank() ? AILocalize.actionNewChat().get() : chat.TITLE;
    }

    @RequiredUIAccess
    private void updateTitles() {
        for (AIChatVirtualFile file : myFiles.values()) {
            AIChatEntry chat = myStore.findChat(file.getChatId());
            if (chat == null) {
                continue;
            }
            String title = title(chat);
            if (!title.equals(file.getName())) {
                try {
                    file.rename(this, title);
                }
                catch (IOException e) {
                    LOG.warn(e);
                }
                myFileEditorManager.get().updateFilePresentation(file);
            }
        }
    }

    private static Component hint(LocalizeValue text) {
        DockLayout layout = DockLayout.create();
        layout.top(Label.create(text));
        return layout;
    }

    private static @Nullable AIModel defaultModel(AIProvider provider) {
        List<AIModel> models = provider.getModels();
        return models.isEmpty() ? null : models.getFirst();
    }

    private static @Nullable AIModel findModel(AIProvider provider, String modelId) {
        AIModel model = provider.getType().resolveModel(provider.getModels(), modelId);
        return model != null ? model : defaultModel(provider);
    }

    private static String firstLine(String message) {
        String line = message.strip().lines().findFirst().orElse("").strip();
        return line.length() <= TITLE_LENGTH ? line : line.substring(0, TITLE_LENGTH).strip() + "…";
    }

    private class ChatListener implements AIChatPanel.Listener {
        private final AIChatEntry myChat;
        private final AIProvider myProvider;
        private final UIAccess myUIAccess;

        ChatListener(AIChatEntry chat, AIProvider provider, UIAccess uiAccess) {
            myChat = chat;
            myProvider = provider;
            myUIAccess = uiAccess;
        }

        @RequiredUIAccess
        @Override
        public void firstMessage(String text) {
            myStore.suggestTitle(myChat, firstLine(text));
        }

        @RequiredUIAccess
        @Override
        public void modelChanged(AIModel model) {
            myChat.MODEL_ID = model.getId();
            myStore.changed(myChat);
        }

        @RequiredUIAccess
        @Override
        public void effortChanged(String effort) {
            myChat.EFFORT = effort;
            myStore.changed(myChat);
        }

        @RequiredUIAccess
        @Override
        public void workingDirectoryChanged(Path workingDirectory) {
            myChat.WORKING_DIRECTORY = workingDirectory.toString();
            myStore.changed(myChat);
        }

        @RequiredUIAccess
        @Override
        public void turnFinished(List<AIMessage> messages) {
            myStore.changed(myChat);
            myStore.saveTranscript(myChat, myProvider, messages);
            // a provider may have generated a title for the chat by now
            myStore.refreshTitle(myChat, myUIAccess);
        }

        @RequiredUIAccess
        @Override
        public void titleSuggested(String title) {
            myStore.suggestTitle(myChat, title);
        }
    }
}

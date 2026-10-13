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

import consulo.ai.AIChatSession;
import consulo.ai.AIContent;
import consulo.ai.AIMessage;
import consulo.ai.AIModel;
import consulo.ai.AIProvider;
import consulo.ai.AIQuestion;
import consulo.ai.AIResponse;
import consulo.ai.AIStreamListener;
import consulo.ai.AIToolPermissionDecision;
import consulo.ai.AIToolPermissionRequest;
import consulo.ai.icon.AIIconGroup;
import consulo.ai.localize.AILocalize;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.ComboBox;
import consulo.ui.ComboBoxStyle;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.messageList.MessageInput;
import consulo.ui.messageList.MessageInputStyle;
import consulo.ui.messageList.MessageList;
import consulo.ui.messageList.MessageListItem;
import consulo.ui.messageList.MessageListItemStyle;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * One chat with one account - a tab of the AI chat tool window. Built only from {@code consulo.ui}, so it runs on
 * the AWT, Qt and web frontends alike.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
public class AIChatPanel implements Disposable {
    private final Project myProject;
    private final UIAccess myUIAccess;
    private final AIProvider myProvider;
    private final Consumer<String> myFirstMessageListener;
    private final Runnable myAnswerListener;

    private final DockLayout myRoot = DockLayout.create();
    private final MessageList myMessages = MessageList.create();
    private final MessageInput myInput = MessageInput.create();
    private final DockLayout myFooter = DockLayout.create();
    private final Deque<AIChatPrompt<?>> myPrompts = new ArrayDeque<>();
    private final Set<String> myAllowedTools = ConcurrentHashMap.newKeySet();

    private @Nullable AIChatSession mySession;
    private @Nullable Path myWorkingDirectory;
    private final Button myWorkingDirectoryButton;
    private @Nullable MessageListItem myAnswer;
    private boolean myFirstMessageSent;

    /**
     * @param firstMessageListener told the first line the user sends, which names the chat
     * @param answerListener       told when an answer has finished
     */
    @RequiredUIAccess
    public AIChatPanel(Project project,
                       UIAccess uiAccess,
                       AIProvider provider,
                       @Nullable AIModel model,
                       Consumer<String> firstMessageListener,
                       Runnable answerListener) {
        myProject = project;
        myUIAccess = uiAccess;
        myProvider = provider;
        myFirstMessageListener = firstMessageListener;
        myAnswerListener = answerListener;

        List<AIModel> models = provider.getModels();
        AIModel chatModel = model != null ? model : (models.isEmpty() ? null : models.getFirst());

        ComboBox<AIModel> modelBox = ComboBox.create(models);
        modelBox.addStyle(ComboBoxStyle.INPLACE);
        modelBox.setTextRenderer(value -> value == null ? LocalizeValue.empty() : value.getDisplayName());
        modelBox.setValue(chatModel);
        modelBox.addValueListener(event -> switchModel(event.getValue()));

        myMessages.setMarkdown(true);

        myInput.setPlaceholder(AILocalize.labelPromptPlaceholder());
        myInput.setSendText(AILocalize.buttonSend());
        myInput.setSendIcon(AIIconGroup.chatSend());
        myInput.addSubmitListener(event -> send(event.getValue()));

        myRoot.center(myMessages);
        String basePath = project.getBasePath();
        myWorkingDirectory = basePath == null ? null : Path.of(basePath);
        myWorkingDirectoryButton = Button.create(LocalizeValue.empty(), event -> chooseWorkingDirectory());
        myWorkingDirectoryButton.setIcon(PlatformIconGroup.nodesFolder());
        myWorkingDirectoryButton.addStyle(ButtonStyle.INPLACE);
        updateWorkingDirectoryTooltip();

        HorizontalLayout suffix = HorizontalLayout.create(Space.NONE);
        suffix.add(myWorkingDirectoryButton);
        suffix.add(modelBox);
        myInput.setSuffixComponent(suffix);
        myInput.addStyle(MessageInputStyle.INPLACE);
        myFooter.center(myInput);
        myFooter.borderBuilder().topSet().apply();
        myRoot.bottom(myFooter);

        switchModel(chatModel);
    }

    public Component getComponent() {
        return myRoot;
    }

    public AIProvider getProvider() {
        return myProvider;
    }

    private void switchModel(@Nullable AIModel model) {
        AIChatSession previous = mySession;
        if (previous != null && model != null && previous.getModel().equals(model)) {
            return;
        }
        if (previous != null) {
            previous.close();
        }
        mySession = model == null ? null : new AIChatSession(myProject, myProvider, model).workingDirectory(myWorkingDirectory);
    }

    @RequiredUIAccess
    private void chooseWorkingDirectory() {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withTitle(AILocalize.dialogWorkingDirectoryTitle());
        FileChooser.choosePaths(descriptor, myProject, myWorkingDirectory).whenCompleteAsync((paths, error) -> {
            if (error != null || paths.isEmpty() || paths.getFirst().equals(myWorkingDirectory)) {
                return;
            }
            myWorkingDirectory = paths.getFirst();
            updateWorkingDirectoryTooltip();
            AIChatSession session = mySession;
            if (session != null) {
                session.close();
                mySession = new AIChatSession(myProject, myProvider, session.getModel()).workingDirectory(myWorkingDirectory);
            }
            append(LocalizeValue.empty(), AILocalize.labelWorkingDirectory(myWorkingDirectory.toString()).get(), MessageListItemStyle.SYSTEM);
        }, myUIAccess);
    }

    @RequiredUIAccess
    private void updateWorkingDirectoryTooltip() {
        Path directory = myWorkingDirectory;
        myWorkingDirectoryButton.setToolTipText(AILocalize.labelWorkingDirectory(directory == null ? "" : directory.toString()));
    }

    @Override
    public void dispose() {
        AIChatSession session = mySession;
        mySession = null;
        if (session != null) {
            session.close();
        }
    }

    @RequiredUIAccess
    private void send(String text) {
        AIChatSession session = mySession;
        if (session == null) {
            return;
        }

        if (!myFirstMessageSent) {
            myFirstMessageSent = true;
            myFirstMessageListener.accept(text);
        }

        myInput.setEnabled(false);

        append(AILocalize.labelYou(), text, MessageListItemStyle.OWN);
        myAnswer = newAnswer();

        int historySize = session.getMessages().size();
        AIStreamListener listener = new AIStreamListener() {
            @Override
            public void onTextDelta(String delta) {
                myUIAccess.give(() -> answer().appendText(delta));
            }

            @Override
            public CompletableFuture<AIToolPermissionDecision> onToolPermission(AIToolPermissionRequest request) {
                if (myAllowedTools.contains(request.toolName())) {
                    return CompletableFuture.completedFuture(AIToolPermissionDecision.allow());
                }
                CompletableFuture<AIToolPermissionDecision> decision = new CompletableFuture<>();
                myUIAccess.give(() -> ask(new AIChatPermissionPrompt(request, decision)));
                decision.whenCompleteAsync((value, error) -> {
                    if (value != null) {
                        if (value.always()) {
                            myAllowedTools.add(request.toolName());
                        }
                        LocalizeValue tool = LocalizeValue.of(describe(request));
                        String text = (value.allowed() ? AILocalize.labelPermissionAllowed(tool) : AILocalize.labelPermissionDenied(tool)).get();
                        log(text);
                    }
                }, myUIAccess);
                return decision;
            }

            @Override
            public CompletableFuture<Map<String, String>> onQuestion(List<AIQuestion> questions) {
                CompletableFuture<Map<String, String>> answers = new CompletableFuture<>();
                myUIAccess.give(() -> ask(new AIChatQuestionPrompt(questions, answers)));
                answers.whenCompleteAsync((value, error) -> {
                    if (value != null) {
                        value.forEach((question, reply) -> log(AILocalize.labelQuestionAnswered(question, reply).get()));
                    }
                    else if (!answers.isCancelled()) {
                        log(AILocalize.labelQuestionSkipped().get());
                    }
                }, myUIAccess);
                return answers;
            }
        };

        session.send(text, listener)
            .whenComplete((response, throwable) -> myUIAccess.give(() -> finish(session, historySize, answer(), response, throwable)));
    }

    @RequiredUIAccess
    private void finish(AIChatSession session,
                        int historySize,
                        MessageListItem answer,
                        @Nullable AIResponse response,
                        @Nullable Throwable throwable) {
        myAnswer = null;
        myInput.setEnabled(true);
        myAnswerListener.run();

        if (throwable != null) {
            Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null ? throwable.getCause() : throwable;
            answer.setText(AILocalize.labelFailed(String.valueOf(cause.getMessage())).get());
            answer.setStyle(MessageListItemStyle.ERROR);
            return;
        }

        // a non-streaming provider never fed the item, so fill it in from the finished answer
        if (response != null && answer.getText().isEmpty()) {
            answer.setText(response.getText());
        }

        // show what the model ran during this turn, since tool calls happen without the user asking
        List<AIMessage> messages = session.getMessages();
        for (AIMessage message : messages.subList(Math.min(historySize, messages.size()), messages.size())) {
            for (AIContent.ToolUse toolUse : message.getToolUses()) {
                append(LocalizeValue.empty(), AILocalize.labelToolCall(toolUse.toolName()).get(), MessageListItemStyle.SYSTEM);
            }
        }
    }

    @RequiredUIAccess
    private void ask(AIChatPrompt<?> prompt) {
        myPrompts.addLast(prompt);
        if (myPrompts.size() == 1) {
            myFooter.top(prompt.getComponent());
        }
        prompt.getResult().whenCompleteAsync((value, error) -> {
            boolean shown = myPrompts.peekFirst() == prompt;
            myPrompts.remove(prompt);
            if (!shown) {
                return;
            }
            myFooter.remove(prompt.getComponent());
            AIChatPrompt<?> next = myPrompts.peekFirst();
            if (next != null) {
                myFooter.top(next.getComponent());
            }
        }, myUIAccess);
    }

    @RequiredUIAccess
    private MessageListItem newAnswer() {
        MessageListItem answer = append(LocalizeValue.of(myProvider.getName()), "", MessageListItemStyle.OTHER);
        answer.setAuthorImage(myProvider.getType().getIcon());
        return answer;
    }

    @RequiredUIAccess
    private MessageListItem answer() {
        MessageListItem answer = myAnswer;
        if (answer == null) {
            answer = newAnswer();
            myAnswer = answer;
        }
        return answer;
    }

    @RequiredUIAccess
    private void log(String text) {
        MessageListItem answer = myAnswer;
        if (answer == null || !answer.getText().isEmpty()) {
            append(LocalizeValue.empty(), text, MessageListItemStyle.SYSTEM);
            return;
        }
        myMessages.remove(answer);
        append(LocalizeValue.empty(), text, MessageListItemStyle.SYSTEM);
        myAnswer = newAnswer();
    }

    private static String describe(AIToolPermissionRequest request) {
        String description = request.description();
        return description == null || description.isBlank() ? request.displayName() : request.displayName() + ": " + description;
    }

    @RequiredUIAccess
    private MessageListItem append(LocalizeValue author, String text, MessageListItemStyle style) {
        MessageListItem item = myMessages.add(author, text);
        item.setTime(Instant.now());
        item.setStyle(style);
        return item;
    }
}

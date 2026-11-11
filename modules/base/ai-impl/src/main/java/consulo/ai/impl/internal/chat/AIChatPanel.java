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
import consulo.ai.AIEffortLevel;
import consulo.ai.AIMessage;
import consulo.ai.AIModel;
import consulo.ai.AIProvider;
import consulo.ai.AIQuestion;
import consulo.ai.AIResponse;
import consulo.ai.AIRole;
import consulo.ai.AIStreamListener;
import consulo.ai.AIToolPermissionDecision;
import consulo.ai.AIToolPermissionRequest;
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
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.LightPopup;
import consulo.ui.ListBox;
import consulo.ui.PopupOptions;
import consulo.ui.PopupPosition;
import consulo.ui.Space;
import consulo.ui.TextAttribute;
import consulo.ui.ToggleButton;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.HorizontalLayoutStyle;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.messageList.MessageInput;
import consulo.ui.messageList.MessageInputStyle;
import consulo.ui.messageList.MessageList;
import consulo.ui.messageList.MessageListItem;
import consulo.ui.messageList.MessageListItemStyle;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * One chat with one account - shown in an editor tab, opened from the tree of the AI chat tool window. Built only from {@code consulo.ui}, so it runs on
 * the AWT, Qt and web frontends alike.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
public class AIChatPanel implements Disposable {
    /**
     * How much of the history of a chat opened again is shown.
     */
    private static final int MAX_HISTORY_ENTRIES = 2000;

    /**
     * Told what changes in the chat, so the store keeps it.
     */
    public interface Listener {
        /**
         * The first line the user sends in a chat without history, which names the chat.
         */
        @RequiredUIAccess
        void firstMessage(String text);

        @RequiredUIAccess
        void modelChanged(AIModel model);

        /**
         * @param effort the {@linkplain AIEffortLevel#id() effort} chosen, empty for the provider's default
         */
        @RequiredUIAccess
        void effortChanged(String effort);

        @RequiredUIAccess
        void workingDirectoryChanged(Path workingDirectory);

        /**
         * An answer has finished, or failed; {@code messages} is the whole history of the chat.
         */
        @RequiredUIAccess
        void turnFinished(List<AIMessage> messages);

        /**
         * The provider named the chat while answering.
         */
        @RequiredUIAccess
        void titleSuggested(String title);
    }

    private final Project myProject;
    private final UIAccess myUIAccess;
    private final AIProvider myProvider;
    private final String myChatId;
    private final Listener myListener;

    private final DockLayout myRoot = DockLayout.create();
    private final MessageList myMessages = MessageList.create();
    private final MessageInput myInput = MessageInput.create();
    private final DockLayout myFooter = DockLayout.create();
    private final Deque<AIChatPrompt<?>> myPrompts = new ArrayDeque<>();
    private final Set<String> myAllowedTools = ConcurrentHashMap.newKeySet();

    /**
     * The effort the provider picks when none is chosen.
     */
    private static final long POPUP_REOPEN_DELAY_NANOS = TimeUnit.MILLISECONDS.toNanos(300);

    private static final AIEffortLevel DEFAULT_EFFORT = new AIEffortLevel("", AILocalize.labelEffortDefault());

    private @Nullable AIChatSession mySession;
    private List<AIModel> myModels;
    private @Nullable AIModel myModel;
    /**
     * The {@linkplain AIEffortLevel#id() effort} the user chose for the chat, empty for the provider's default - kept
     * while a model without a level of it is chosen, for the model chosen next.
     */
    private String myWantedEffort;
    /**
     * The effort the model is asked for - the wanted one when the model has a level of it.
     */
    private String myEffort;
    /**
     * The model and the effort in one - a click opens the choice of both.
     */
    private final Button myModelButton;
    private @Nullable LightPopup myModelPopup;
    /**
     * When the popup of the button closed last, in {@link System#nanoTime()}.
     */
    private long myModelPopupClosed;
    private @Nullable Path myWorkingDirectory;
    private final Button myWorkingDirectoryButton;
    private @Nullable MessageListItem myAnswer;
    /**
     * Whether an answer is on its way - the answer shows it is still being written until it is there.
     */
    private boolean myAnswering;
    private boolean myFirstMessageSent;

    /**
     * @param chatId           the id of the conversation, kept across model and directory changes and restarts
     * @param history          what the chat said before it was opened again
     * @param workingDirectory where the chat works, the project directory when null
     * @param effort           the {@linkplain AIEffortLevel#id() effort} of the chat, empty for the provider's default
     */
    @RequiredUIAccess
    public AIChatPanel(Project project,
                       UIAccess uiAccess,
                       AIProvider provider,
                       @Nullable AIModel model,
                       String chatId,
                       List<AIMessage> history,
                       @Nullable Path workingDirectory,
                       String effort,
                       Listener listener) {
        myProject = project;
        myUIAccess = uiAccess;
        myProvider = provider;
        myChatId = chatId;
        myListener = listener;
        myFirstMessageSent = !history.isEmpty();

        myModels = provider.getModels();
        myModel = model != null ? model : (myModels.isEmpty() ? null : myModels.getFirst());
        myWantedEffort = effort;
        myEffort = effortOf(myModel, effort);

        myModelButton = Button.create(LocalizeValue.empty(), event -> showModelPopup());
        myModelButton.addStyle(ButtonStyle.INPLACE);
        updateModelButton();

        myMessages.setMarkdown(true);
        // the names tell the user and the assistant apart - the tab already shows the icon of the provider
        myMessages.setShowAvatars(false);

        myInput.setPlaceholder(AILocalize.labelPromptPlaceholder());
        myInput.setSendText(AILocalize.buttonSend());
        myInput.setSendIcon(PlatformIconGroup.actionsSend());
        myInput.addSubmitListener(event -> send(event.getValue()));

        myRoot.center(myMessages);
        String basePath = project.getBasePath();
        myWorkingDirectory = workingDirectory != null ? workingDirectory : (basePath == null ? null : Path.of(basePath));
        myWorkingDirectoryButton = Button.create(LocalizeValue.empty(), event -> chooseWorkingDirectory());
        myWorkingDirectoryButton.setIcon(PlatformIconGroup.nodesFolder());
        myWorkingDirectoryButton.addStyle(ButtonStyle.INPLACE);
        updateWorkingDirectoryTooltip();

        HorizontalLayout suffix = HorizontalLayout.create(Space.NONE);
        suffix.addStyle(HorizontalLayoutStyle.TRANSPARENT_BACKGROUND);
        suffix.add(myWorkingDirectoryButton);
        suffix.add(myModelButton);
        myInput.setSuffixComponent(suffix);
        myInput.addStyle(MessageInputStyle.INPLACE);
        myFooter.center(myInput);
        myFooter.borderBuilder().topSet().apply();
        myRoot.bottom(myFooter);

        showHistory(history);
        mySession = myModel == null ? null : newSession(myModel, history);

        // the models known so far may be a fallback - the ones the vendor tells replace them
        provider.loadModels().whenCompleteAsync((models, error) -> {
            if (error == null && models != null && !models.isEmpty()) {
                modelsLoaded(models);
            }
        }, uiAccess);
    }

    public Component getComponent() {
        return myRoot;
    }

    public AIProvider getProvider() {
        return myProvider;
    }

    @RequiredUIAccess
    private void switchModel(AIModel model) {
        if (model.equals(myModel)) {
            return;
        }
        AIChatSession previous = mySession;
        List<AIMessage> history = previous == null ? List.of() : previous.getMessages();
        if (previous != null) {
            previous.close();
        }
        myModel = model;
        // an effort the new model has no level of is the default for it
        myEffort = effortOf(model, myWantedEffort);
        mySession = newSession(model, history);
        myListener.modelChanged(model);
        updateModelButton();
    }

    /**
     * The models the vendor told - the chosen one is taken from them, with the efforts it has.
     */
    @RequiredUIAccess
    private void modelsLoaded(List<AIModel> models) {
        myModels = models;
        AIModel current = myModel;
        AIModel loaded = current == null ? null : myProvider.getType().resolveModel(models, current.getId());
        if (loaded != null) {
            myModel = loaded;
        }
        String effort = effortOf(myModel, myWantedEffort);
        if (!effort.equals(myEffort)) {
            myEffort = effort;
            AIChatSession session = mySession;
            if (session != null) {
                session.effort(effort.isEmpty() ? null : effort);
            }
        }
        updateModelButton();
    }

    @RequiredUIAccess
    private void switchEffort(String effort) {
        if (effort.equals(myEffort)) {
            return;
        }
        myWantedEffort = effort;
        myEffort = effort;
        // the effort is given when the provider starts its session - the conversation goes on in a new one
        AIChatSession session = mySession;
        if (session != null) {
            session.close();
            mySession = newSession(session.getModel(), session.getMessages());
        }
        myListener.effortChanged(effort);
        updateModelButton();
    }

    /**
     * The effort if the model has a level of it, the default otherwise.
     */
    private static String effortOf(@Nullable AIModel model, String effort) {
        AIEffortLevel level = findEffort(model, effort);
        return level == null ? "" : level.id();
    }

    private static @Nullable AIEffortLevel findEffort(@Nullable AIModel model, String effort) {
        if (model == null || effort.isEmpty()) {
            return null;
        }
        for (AIEffortLevel level : model.getEffortLevels()) {
            if (level.id().equals(effort)) {
                return level;
            }
        }
        return null;
    }

    @RequiredUIAccess
    private void updateModelButton() {
        AIModel model = myModel;
        if (model == null) {
            myModelButton.setText(LocalizeValue.empty());
            return;
        }
        AIEffortLevel effort = findEffort(model, myEffort);
        myModelButton.setText(
            effort == null ? model.getDisplayName() : LocalizeValue.join(" \u00b7 ", model.getDisplayName(), effort.displayName())
        );
        LocalizeValue description = model.getDescription();
        myModelButton.setToolTipText(description.isEmpty() ? model.getDisplayName() : description);
    }

    /**
     * The models with their descriptions over the efforts of the chosen one - a model chosen keeps the popup up for
     * the effort, an effort chosen closes it.
     */
    @RequiredUIAccess
    private void showModelPopup() {
        LightPopup shown = myModelPopup;
        if (shown != null && shown.isVisible()) {
            shown.close();
            return;
        }
        // the press on the button closed the popup as a click outside of it - the click is not to open it again
        if (System.nanoTime() - myModelPopupClosed < POPUP_REOPEN_DELAY_NANOS) {
            return;
        }
        if (myModels.isEmpty()) {
            return;
        }
        LightPopup popup = LightPopup.create(PopupOptions.builder().position(PopupPosition.TOP).arrow().build());
        popup.setContent(modelPopupContent(popup));
        popup.addCloseListener(event -> {
            if (myModelPopup == popup) {
                myModelPopup = null;
            }
            myModelPopupClosed = System.nanoTime();
        });
        myModelPopup = popup;
        popup.showBy(myModelButton);
    }

    @RequiredUIAccess
    private Component modelPopupContent(LightPopup popup) {
        VerticalLayout content = VerticalLayout.create(Space.SMALL);
        content.paddingBuilder().allSet(Space.LARGE).apply();
        content.add(sectionLabel(AILocalize.labelModel()));

        ListBox<AIModel> models = ListBox.create(myModels);
        models.setRenderer((presentation, item) -> {
            AIModel value = item.getValue();
            if (value == null) {
                return;
            }
            presentation.append(value.getDisplayName());
            LocalizeValue description = value.getDescription();
            if (description.isNotEmpty()) {
                presentation.append("  ");
                presentation.append(description, TextAttribute.GRAYED);
            }
        });
        models.setValue(myModel);
        models.addValueListener(event -> {
            AIModel value = event.getValue();
            if (value != null && !value.equals(myModel)) {
                switchModel(value);
                // the efforts are the new model's
                popup.setContent(modelPopupContent(popup));
                popup.pack();
            }
        });
        content.add(models);

        List<AIEffortLevel> levels = myModel == null ? List.of() : myModel.getEffortLevels();
        if (!levels.isEmpty()) {
            Label effortLabel = sectionLabel(AILocalize.labelEffort());
            effortLabel.paddingBuilder().topSet(Space.SMALL).apply();
            content.add(effortLabel);
            HorizontalLayout efforts = HorizontalLayout.create(Space.X_SMALL);
            efforts.add(effortButton(popup, "", AILocalize.labelEffortDefault()));
            for (AIEffortLevel level : levels) {
                efforts.add(effortButton(popup, level.id(), level.displayName()));
            }
            content.add(efforts);
        }
        return content;
    }

    @RequiredUIAccess
    private ToggleButton effortButton(LightPopup popup, String effort, LocalizeValue text) {
        ToggleButton button = ToggleButton.create(text, effort.equals(myEffort));
        button.addValueListener(event -> {
            switchEffort(effort);
            popup.close();
        });
        return button;
    }

    private static Label sectionLabel(LocalizeValue text) {
        Label label = Label.create(text);
        label.addStyle(LabelStyle.FONT_SMALL);
        label.addStyle(LabelStyle.FONT_BOLD);
        return label;
    }

    private AIChatSession newSession(AIModel model, List<AIMessage> history) {
        return new AIChatSession(myProject, myProvider, model, myChatId, history)
            .workingDirectory(myWorkingDirectory)
            .effort(myEffort.isEmpty() ? null : myEffort);
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
                mySession = newSession(session.getModel(), session.getMessages());
            }
            myListener.workingDirectoryChanged(myWorkingDirectory);
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
            myListener.firstMessage(text);
        }

        myInput.setEnabled(false);

        append(AILocalize.labelYou(), text, MessageListItemStyle.OWN);
        myAnswering = true;
        myAnswer = newAnswer();

        int historySize = session.getMessages().size();
        AIStreamListener listener = new AIStreamListener() {
            @Override
            public void onTextDelta(String delta) {
                myUIAccess.give(() -> {
                    MessageListItem answer = answer();
                    answer.appendText(delta);
                    answer.setWritingStatus(LocalizeValue.empty());
                });
            }

            @Override
            public void onThinkingDelta(String delta) {
                myUIAccess.give(() -> answer().setWritingStatus(AILocalize.labelWritingThinking()));
            }

            @Override
            public void onToolUse(AIContent.ToolUse toolUse) {
                myUIAccess.give(() -> answer().setWritingStatus(AILocalize.labelWritingTool(toolUse.toolName())));
            }

            @Override
            public void onTitle(String title) {
                myUIAccess.give(() -> myListener.titleSuggested(title));
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
        myAnswering = false;
        answer.setWriting(false);
        myInput.setEnabled(true);
        myListener.turnFinished(session.getMessages());

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

    /**
     * Shows what the chat said before it was opened again: the text of both sides and the tools the model called.
     */
    @RequiredUIAccess
    private void showHistory(List<AIMessage> history) {
        record Entry(MessageListItemStyle style, String text) {
        }

        List<Entry> entries = new ArrayList<>();
        for (AIMessage message : history) {
            String text = message.getText();
            if (message.role() == AIRole.USER) {
                if (!text.isEmpty()) {
                    entries.add(new Entry(MessageListItemStyle.OWN, text));
                }
                continue;
            }
            if (!text.isEmpty()) {
                entries.add(new Entry(MessageListItemStyle.OTHER, text));
            }
            for (AIContent.ToolUse toolUse : message.getToolUses()) {
                entries.add(new Entry(MessageListItemStyle.SYSTEM, AILocalize.labelToolCall(toolUse.toolName()).get()));
            }
        }

        // a long conversation shows its end - rendering every message of it would hold the ui for long
        int hidden = Math.max(0, entries.size() - MAX_HISTORY_ENTRIES);
        if (hidden > 0) {
            append(LocalizeValue.empty(), AILocalize.labelHistoryHidden(hidden).get(), MessageListItemStyle.SYSTEM);
        }
        for (Entry entry : entries.subList(hidden, entries.size())) {
            switch (entry.style()) {
                case OWN -> append(AILocalize.labelYou(), entry.text(), MessageListItemStyle.OWN);
                case OTHER -> newAnswer().setText(entry.text());
                default -> append(LocalizeValue.empty(), entry.text(), entry.style());
            }
        }
        for (MessageListItem item : myMessages.getItems()) {
            item.setTime(null);
        }
    }

    @RequiredUIAccess
    private MessageListItem newAnswer() {
        MessageListItem answer = append(LocalizeValue.of(myProvider.getName()), "", MessageListItemStyle.OTHER);
        if (myAnswering) {
            answer.setWriting(true);
        }
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

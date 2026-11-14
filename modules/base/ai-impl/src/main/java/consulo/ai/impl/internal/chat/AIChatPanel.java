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

import consulo.ai.AIChatAttachment;
import consulo.ai.AIChatCheckpoint;
import consulo.ai.AIChatCheckpointManager;
import consulo.ai.AIChatHiddenContext;
import consulo.ai.AIChatInput;
import consulo.ai.AIChatInputFactory;
import consulo.ai.AIChatSession;
import consulo.ai.AIChatSessionListener;
import consulo.ai.AIContent;
import consulo.ai.AIContextUsage;
import consulo.ai.AIEffortLevel;
import consulo.ai.AIMessage;
import consulo.ai.AIModel;
import consulo.ai.AIPlanEntry;
import consulo.ai.AIProvider;
import consulo.ai.AIQuestion;
import consulo.ai.AIResponse;
import consulo.ai.AIRole;
import consulo.ai.AISessionOption;
import consulo.ai.AISessionOptionValue;
import consulo.ai.AIStreamListener;
import consulo.ai.AIToolCallStatus;
import consulo.ai.AIToolPermissionDecision;
import consulo.ai.AIToolPermissionRequest;
import consulo.ai.AIUsage;
import consulo.ai.impl.internal.debug.AIDebugEndpoint;
import consulo.ai.impl.internal.debug.AIDebugLogManager;
import consulo.ai.impl.internal.setting.AISettings;
import consulo.ai.localize.AILocalize;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.colorScheme.EditorColorsManager;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileEditor.FileEditorManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.CheckBox;
import consulo.ui.CheckBoxStyle;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.LightPopup;
import consulo.ui.ListBox;
import consulo.ui.MessageBoxes;
import consulo.ui.PopupOptions;
import consulo.ui.PopupPosition;
import consulo.ui.ProgressBar;
import consulo.ui.ProgressBarStyle;
import consulo.ui.Space;
import consulo.ui.TextAttribute;
import consulo.ui.ToggleButton;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.HorizontalLayoutStyle;
import consulo.ui.layout.LayoutStyle;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.messageList.MessageList;
import consulo.ui.messageList.MessageListItem;
import consulo.ui.messageList.MessageListItemPart;
import consulo.ui.messageList.MessageListItemStyle;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One chat with one account - shown in an editor tab, opened from the tree of the AI chat tool window. Built only from {@code consulo.ui}, so it runs on
 * the AWT, Qt and web frontends alike.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
public class AIChatPanel implements Disposable {
    private static final String DEBUG_MODE = "consulo.debug";
    private static final int MAX_VISIBLE_MODELS = 12;
    private static final String PLAN_MODE = "plan";
    private static final Set<String> NOT_EDITING_MODES = Set.of(PLAN_MODE, "ask");

    private static final Logger LOG = Logger.getInstance(AIChatPanel.class);

    /**
     * How much of the history of a chat opened again is shown.
     */
    private static final int MAX_HISTORY_ENTRIES = 2000;

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final Map<String, String> IMAGE_TYPES = Map.of(
        "png", "image/png",
        "jpg", "image/jpeg",
        "jpeg", "image/jpeg",
        "gif", "image/gif",
        "webp", "image/webp"
    );

    /**
     * A file named in a message, {@code @src/Main.java}.
     */
    private static final Pattern FILE_REFERENCE = Pattern.compile("(?<![^\\s(\\[])@([^\\s@]+)");

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

        /**
         * The chat goes on in another session of the provider - it is opened with that one from now on.
         */
        @RequiredUIAccess
        void sessionChanged(String sessionId);
    }

    private final Project myProject;
    private final UIAccess myUIAccess;
    private final AIProvider myProvider;
    private final String myChatId;
    private final Listener myListener;

    private final DockLayout myRoot = DockLayout.create();
    private final MessageList myMessages = MessageList.create();
    private final AIChatInput myInput;
    /**
     * What the user sent in this chat, for the arrows of the box.
     */
    private final List<String> mySentTexts = new ArrayList<>();
    private final DockLayout myFooter = DockLayout.create();
    /**
     * Over the message box: the plan, the queued messages and the question of the moment.
     */
    private final VerticalLayout myFooterTop = VerticalLayout.create(Space.SMALL);
    private final DockLayout myPromptHolder = DockLayout.create();
    private final AIChatPlanPanel myPlan;
    private final AIChatQueuePanel myQueue;
    /**
     * How full the context of the session is, as a ring - the details of the usage are a click away.
     */
    private final ProgressBar myUsage = ProgressBar.create();
    private @Nullable ProgressBarStyle myUsageTone;
    private final Button myStopButton;
    private final Button myModeButton;
    private List<AISessionOption> mySessionOptions = List.of();
    /**
     * What goes with the next message - shown as chips over the message box.
     */
    private final List<AIChatAttachment> myAttachments = new ArrayList<>();
    private final HorizontalLayout myChips = HorizontalLayout.create(Space.SMALL);
    /**
     * Shown while an earlier message is edited - sending it drops that message and all that followed.
     */
    private final HorizontalLayout myEditBanner = HorizontalLayout.create(Space.SMALL);
    /**
     * What to do after a turn of Debug or plan mode - reproduce, clean up, build the plan.
     */
    private final HorizontalLayout myFollowUps = HorizontalLayout.create(Space.SMALL);
    /**
     * Debug mode of the platform - the agent stays in a mode it edits in, the prompts carry the instructions.
     */
    private boolean myDebugMode;
    private @Nullable AIDebugEndpoint myDebugEndpoint;
    private boolean myDebugTurn;
    private boolean myPlanTurn;
    /**
     * The file the agent saved its last plan to - what View Plan opens.
     */
    private @Nullable Path myPlanFile;
    private @Nullable CompletableFuture<Map<String, String>> myPlanQuestions;
    /**
     * The place of the edited message in the history of the session, -1 while none is edited.
     */
    private int myEditIndex = -1;
    private @Nullable MessageListItem myEditItem;
    /**
     * The file the user works on, which goes with the message while it is ticked.
     */
    private final CheckBox myActiveFileBox = CheckBox.create(LocalizeValue.empty(), true);
    private @Nullable VirtualFile myActiveFile;
    private @Nullable AIUsage myTurnUsage;
    private @Nullable AIContextUsage myContextUsage;
    private final Deque<AIChatPrompt<?>> myPrompts = new ArrayDeque<>();
    private final Set<String> myAllowedTools = ConcurrentHashMap.newKeySet();
    private volatile boolean myRunEverything;

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
    private @Nullable AIChatAnswer myAnswer;
    /**
     * Whether an answer is on its way - the answer shows it is still being written until it is there.
     */
    private boolean myAnswering;
    /**
     * The files before the turn which runs now.
     */
    private @Nullable AIChatCheckpoint myTurnCheckpoint;
    /**
     * Stop was pressed before the turn reached the provider - it does not start then.
     */
    private boolean myStopRequested;
    /**
     * The answer of the last turn which can be undone - only the last one can.
     */
    private @Nullable MessageListItem myUndoItem;
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

        myInput = AIChatInputFactory.getInstance().create(
            project,
            AILocalize.labelPromptPlaceholder(),
            () -> provider.getType().getCommands(provider, chatId),
            this
        );
        myInput.addSubmitListener(this::send);
        myInput.addAttachListener(this::attach);
        for (AIMessage message : history) {
            String said = message.role() == AIRole.USER ? AIChatHiddenContext.strip(message.getText()).strip() : "";
            if (!said.isEmpty()) {
                mySentTexts.add(said);
            }
        }
        myInput.setHistory(mySentTexts);

        myRoot.center(myMessages);
        String basePath = project.getBasePath();
        myWorkingDirectory = workingDirectory != null ? workingDirectory : (basePath == null ? null : Path.of(basePath));
        myWorkingDirectoryButton = Button.create(LocalizeValue.empty(), event -> chooseWorkingDirectory());
        myWorkingDirectoryButton.setIcon(PlatformIconGroup.nodesFolder());
        myWorkingDirectoryButton.addStyle(ButtonStyle.INPLACE);
        updateWorkingDirectoryTooltip();

        myModeButton = Button.create(LocalizeValue.empty(), event -> showModePopup());
        myModeButton.addStyle(ButtonStyle.INPLACE);
        myModeButton.setVisible(false);

        myStopButton = Button.create(LocalizeValue.empty(), event -> stop());
        myStopButton.setIcon(PlatformIconGroup.actionsSuspend());
        myStopButton.setToolTipText(AILocalize.buttonStop());
        myStopButton.addStyle(ButtonStyle.INPLACE);
        myStopButton.setVisible(false);

        Button attachImage = Button.create(LocalizeValue.empty(), event -> attachImages());
        attachImage.setIcon(PlatformIconGroup.actionsAddfile());
        attachImage.setToolTipText(AILocalize.buttonAttachImage());
        attachImage.addStyle(ButtonStyle.INPLACE);

        myUsage.addStyle(ProgressBarStyle.SPINNER);
        myUsage.addStyle(ProgressBarStyle.TRANSPARENT_BACKGROUND);
        myUsage.setMinimum(0);
        myUsage.setMaximum(100);
        myUsage.setVisible(false);
        myUsage.paddingBuilder().rightSet(Space.SMALL).apply();
        myUsage.addClickListener(event -> showUsagePopup());

        HorizontalLayout suffix = HorizontalLayout.create(Space.NONE);
        suffix.addStyle(HorizontalLayoutStyle.TRANSPARENT_BACKGROUND);
        suffix.add(myUsage);
        suffix.add(attachImage);
        suffix.add(myWorkingDirectoryButton);
        suffix.add(myModeButton);
        suffix.add(myModelButton);
        suffix.add(myStopButton);
        myInput.setSuffixComponent(suffix);

        myPlan = new AIChatPlanPanel();
        myQueue = new AIChatQueuePanel(this::sendNow);
        myFooterTop.add(myPlan.getComponent());
        myFooterTop.add(myQueue.getComponent());
        myFooterTop.add(myPromptHolder);
        myChips.addStyle(HorizontalLayoutStyle.TRANSPARENT_BACKGROUND);
        myActiveFileBox.addStyle(CheckBoxStyle.TRANSPARENT_BACKGROUND);
        myChips.setVisible(false);
        myFooterTop.add(myChips);
        myEditBanner.addStyle(HorizontalLayoutStyle.TRANSPARENT_BACKGROUND);
        Label editing = Label.create(AILocalize.labelEditingMessage());
        editing.addStyle(LabelStyle.FONT_SMALL);
        myEditBanner.add(editing);
        myEditBanner.add(Hyperlink.create(AILocalize.linkCancelEdit(), event -> cancelEdit()));
        myEditBanner.setVisible(false);
        myFooterTop.add(myEditBanner);
        myFollowUps.addStyle(HorizontalLayoutStyle.TRANSPARENT_BACKGROUND);
        myFollowUps.setVisible(false);
        myFooterTop.add(myFollowUps);


        // the box at the bottom reads as one editor - every row of it on the background of the editor
        myFooter.setBackgroundColor(ColorValue.lazy(() -> EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground()));
        myFooterTop.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);
        myPromptHolder.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);
        myFooter.top(myFooterTop);
        myFooter.center(myInput.getComponent());
        myFooter.borderBuilder().topSet().apply();
        // the rows of the box keep off its edges and off each other
        myFooter.paddingBuilder().leftSet(Space.LARGE).rightSet(Space.LARGE).topSet(Space.MEDIUM).bottomSet(Space.MEDIUM).apply();
        myFooterTop.paddingBuilder().bottomSet(Space.SMALL).apply();
        myRoot.bottom(myFooter);

        // the agent tells its settings and commands as a session starts, and while it runs
        provider.getType().addSessionListener(provider, chatId, new AIChatSessionListener() {
            @Override
            public void optionsChanged(List<AISessionOption> options) {
                uiAccess.give(() -> sessionOptionsChanged(options));
            }

            @Override
            public void sessionChanged(String sessionId) {
                uiAccess.give(() -> myListener.sessionChanged(sessionId));
            }
        }, this);
        sessionOptionsChanged(provider.getType().getSessionOptions(provider, chatId));

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
        applyToSession(true, model.getId());
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
        if (!effort.isEmpty()) {
            applyToSession(false, effort);
        }
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
        // a long list of models scrolls - the efforts and the settings below it stay on screen
        if (myModels.size() > MAX_VISIBLE_MODELS) {
            models.setVisibleRowCount(MAX_VISIBLE_MODELS);
            content.add(ScrollableLayout.create(models));
        }
        else {
            content.add(models);
        }

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

        // the other settings the session offers for the model - context size, fast, thinking
        for (AISessionOption option : mySessionOptions) {
            if (option.isModel() || option.isMode() || option.isEffort() || option.values().isEmpty()) {
                continue;
            }
            Label optionLabel = sectionLabel(LocalizeValue.of(option.name()));
            optionLabel.paddingBuilder().topSet(Space.SMALL).apply();
            if (option.description() != null) {
                optionLabel.setToolTipText(LocalizeValue.of(option.description()));
            }
            content.add(optionLabel);
            HorizontalLayout values = HorizontalLayout.create(Space.X_SMALL);
            for (AISessionOptionValue value : option.values()) {
                ToggleButton button = ToggleButton.create(LocalizeValue.of(value.name()), value.value().equals(option.currentValue()));
                if (value.description() != null) {
                    button.setToolTipText(LocalizeValue.of(value.description()));
                }
                button.addValueListener(event -> {
                    setSessionOption(option, value.value());
                    popup.close();
                });
                values.add(button);
            }
            content.add(values);
        }
        return content;
    }

    /**
     * The settings of the session changed - the mode shows on its own button, the rest in the popup of the model.
     */
    @RequiredUIAccess
    private void sessionOptionsChanged(List<AISessionOption> options) {
        mySessionOptions = options;
        AISessionOption mode = findMode(options);
        String currentValue = mode == null ? null : mode.currentValue();
        AISessionOptionValue current = mode == null || currentValue == null ? null : mode.findValue(currentValue);
        myModeButton.setVisible(mode != null && current != null);
        if (mode != null && myDebugMode) {
            myModeButton.setText(AILocalize.modeDebug());
            myModeButton.setToolTipText(AILocalize.modeDebugDescription());
        }
        else if (mode != null && current != null) {
            String description = current.description();
            myModeButton.setText(LocalizeValue.of(current.name()));
            myModeButton.setToolTipText(description == null ? LocalizeValue.of(mode.name()) : LocalizeValue.of(description));
        }
    }

    private static @Nullable AISessionOption findMode(List<AISessionOption> options) {
        for (AISessionOption option : options) {
            if (option.isMode()) {
                return option;
            }
        }
        return null;
    }

    @RequiredUIAccess
    private void showModePopup() {
        AISessionOption mode = findMode(mySessionOptions);
        if (mode == null) {
            return;
        }
        LightPopup popup = LightPopup.create(PopupOptions.builder().position(PopupPosition.TOP).arrow().build());
        VerticalLayout content = VerticalLayout.create(Space.SMALL);
        content.paddingBuilder().allSet(Space.LARGE).apply();
        content.add(sectionLabel(LocalizeValue.of(mode.name())));
        // Debug is a mode of the platform, offered next to the ones of the agent
        AISessionOptionValue debug = new AISessionOptionValue(DEBUG_MODE, AILocalize.modeDebug().get(), AILocalize.modeDebugDescription().get());
        List<AISessionOptionValue> all = new ArrayList<>(mode.values());
        all.add(debug);
        ListBox<AISessionOptionValue> values = ListBox.create(all);
        values.setRenderer((presentation, item) -> {
            AISessionOptionValue value = item.getValue();
            if (value == null) {
                return;
            }
            presentation.append(value.name());
            if (value.description() != null) {
                presentation.append("  ");
                presentation.append(value.description(), TextAttribute.GRAYED);
            }
        });
        values.setValue(myDebugMode ? debug : mode.currentValue() == null ? null : mode.findValue(mode.currentValue()));
        values.addValueListener(event -> {
            AISessionOptionValue value = event.getValue();
            popup.close();
            if (value == null) {
                return;
            }
            if (value == debug) {
                enterDebugMode(mode);
                return;
            }
            myDebugMode = false;
            if (!value.value().equals(mode.currentValue())) {
                setSessionOption(mode, value.value());
            }
            else {
                sessionOptionsChanged(mySessionOptions);
            }
        });
        content.add(values);
        popup.setContent(content);
        popup.showBy(myModeButton);
    }

    /**
     * Debug mode needs an agent which edits - out of a mode which only plans or answers, the agent goes to the first
     * mode which does. A new log starts with the first turn.
     */
    @RequiredUIAccess
    private void enterDebugMode(AISessionOption mode) {
        myDebugMode = true;
        myDebugEndpoint = null;
        String current = mode.currentValue();
        AISessionOptionValue editing = editingMode(mode);
        if (current != null && !isEditingMode(current) && editing != null) {
            setSessionOption(mode, editing.value());
        }
        else {
            sessionOptionsChanged(mySessionOptions);
        }
    }

    private static boolean isEditingMode(String value) {
        return !NOT_EDITING_MODES.contains(value.toLowerCase(Locale.ROOT));
    }

    private static @Nullable AISessionOptionValue editingMode(AISessionOption mode) {
        for (AISessionOptionValue value : mode.values()) {
            if (isEditingMode(value.value())) {
                return value;
            }
        }
        return null;
    }

    private boolean isPlanMode() {
        AISessionOption mode = findMode(mySessionOptions);
        String current = mode == null ? null : mode.currentValue();
        return current != null && PLAN_MODE.equalsIgnoreCase(current);
    }

    /**
     * The hidden instructions of the mode of the turn - in Debug mode with the log the instrumented code writes.
     */
    private CompletableFuture<@Nullable String> modeInstructions(boolean debugTurn, boolean planTurn, @Nullable Path directory) {
        if (planTurn) {
            return CompletableFuture.completedFuture(AIChatHiddenContext.modeInstructions(AILocalize.promptPlanInstructions().get()));
        }
        if (!debugTurn || directory == null) {
            return CompletableFuture.completedFuture(null);
        }
        AIDebugEndpoint known = myDebugEndpoint;
        CompletableFuture<AIDebugEndpoint> endpoint = known != null
            ? CompletableFuture.completedFuture(known)
            : CompletableFuture.supplyAsync(() -> {
                try {
                    return myProject.getApplication().getInstance(AIDebugLogManager.class).beginTurn(directory, myChatId);
                }
                catch (IOException e) {
                    throw new CompletionException(e);
                }
            }, myProject.getApplication().getInstance(ApplicationConcurrency.class).executor());
        return endpoint.<@Nullable String>thenApplyAsync(value -> {
            myDebugEndpoint = value;
            String url = value.url();
            String instructions = AILocalize.promptDebugInstructions(
                value.logFile().toString(),
                url != null ? url : AILocalize.promptDebugNoEndpoint().get()
            ).get();
            return AIChatHiddenContext.modeInstructions(instructions);
        }, myUIAccess).exceptionally(error -> {
            LOG.warn("Cannot start the debug log", error);
            return null;
        });
    }

    @RequiredUIAccess
    private void showFollowUps(boolean debugTurn) {
        myFollowUps.removeAll();
        if (debugTurn) {
            myFollowUps.add(followUp(AILocalize.buttonDebugReproduced(), true, () -> send(AILocalize.promptDebugReproduced().get())));
            myFollowUps.add(followUp(AILocalize.buttonDebugCleanup(), false, () -> send(AILocalize.promptDebugCleanup().get())));
            Button viewLogs = Button.create(AILocalize.buttonDebugViewLogs(), event -> viewDebugLog());
            myFollowUps.add(viewLogs);
        }
        else {
            myFollowUps.add(followUp(AILocalize.buttonPlanBuild(), true, this::buildPlan));
            Path planFile = myPlanFile;
            if (planFile != null) {
                myFollowUps.add(Button.create(AILocalize.buttonPlanView(), event -> openFile(planFile)));
            }
        }
        myFollowUps.setVisible(true);
    }

    @RequiredUIAccess
    private Button followUp(LocalizeValue text, boolean primary, Runnable action) {
        Button button = Button.create(text, event -> {
            clearFollowUps();
            action.run();
        });
        if (primary) {
            button.addStyle(ButtonStyle.PRIMARY);
        }
        return button;
    }

    /**
     * The choices the agent asked about in prose, as a question to answer - the answers go as the next prompt. Writing
     * a message instead drops the question.
     */
    @RequiredUIAccess
    private void askPlanQuestions(List<AIQuestion> questions) {
        if (questions.isEmpty()) {
            return;
        }
        CompletableFuture<Map<String, String>> answers = new CompletableFuture<>();
        myPlanQuestions = answers;
        ask(new AIChatQuestionPrompt(questions, answers));
        answers.thenAcceptAsync(values -> {
            List<String> lines = new ArrayList<>();
            values.forEach((question, reply) -> {
                if (!reply.isBlank()) {
                    lines.add(question.startsWith("#") ? reply : question + " " + reply);
                }
            });
            if (!lines.isEmpty()) {
                send(String.join("\n", lines));
            }
        }, myUIAccess);
    }

    @RequiredUIAccess
    private void clearFollowUps() {
        myFollowUps.removeAll();
        myFollowUps.setVisible(false);
    }

    /**
     * The plan is done - the agent goes to a mode which edits and implements it.
     */
    @RequiredUIAccess
    private void buildPlan() {
        AISessionOption mode = findMode(mySessionOptions);
        AISessionOptionValue editing = mode == null ? null : editingMode(mode);
        if (mode == null || editing == null) {
            send(AILocalize.promptPlanBuild().get());
            return;
        }
        myProvider.getType().setSessionOption(myProvider, myChatId, mode.id(), editing.value()).whenCompleteAsync((options, error) -> {
            if (options != null) {
                sessionOptionsChanged(options);
            }
            else if (error != null) {
                LOG.warn("Cannot leave plan mode", error);
            }
            send(AILocalize.promptPlanBuild().get());
        }, myUIAccess);
    }

    @RequiredUIAccess
    private void viewDebugLog() {
        AIDebugEndpoint endpoint = myDebugEndpoint;
        Path directory = myWorkingDirectory != null ? myWorkingDirectory : projectDirectory();
        Path logFile = endpoint != null ? endpoint.logFile()
            : directory == null ? null : myProject.getApplication().getInstance(AIDebugLogManager.class).getLogFile(directory, myChatId);
        if (logFile == null) {
            log(AILocalize.labelDebugLogsMissing().get());
            return;
        }
        openFile(logFile);
    }

    @RequiredUIAccess
    private void openFile(Path path) {
        CompletableFuture.supplyAsync(() -> LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path),
                myProject.getApplication().getInstance(ApplicationConcurrency.class).executor())
            .whenCompleteAsync((file, error) -> {
                if (file == null) {
                    log(AILocalize.labelFileMissing(path.toString()).get());
                }
                else if (!myProject.isDisposed()) {
                    FileEditorManager.getInstance(myProject).openFile(file, true);
                }
            }, myUIAccess);
    }

    /**
     * Sets a setting of the session - the agent answers with all its settings, which may have changed with it.
     */
    @RequiredUIAccess
    private void setSessionOption(AISessionOption option, String value) {
        myProvider.getType().setSessionOption(myProvider, myChatId, option.id(), value).whenCompleteAsync((options, error) -> {
            if (error != null) {
                Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                append(LocalizeValue.empty(), AILocalize.labelOptionFailed(option.name(), String.valueOf(cause.getMessage())).get(), MessageListItemStyle.ERROR);
                return;
            }
            sessionOptionsChanged(options);
        }, myUIAccess);
    }

    /**
     * Sets the value of the session option the model or the effort stands for at once, when the session has one - the
     * settings of the new model show then, before the next turn.
     */
    @RequiredUIAccess
    private void applyToSession(boolean model, String value) {
        for (AISessionOption option : mySessionOptions) {
            if ((model ? option.isModel() : option.isEffort()) && !value.equals(option.currentValue()) && option.findValue(value) != null) {
                setSessionOption(option, value);
                return;
            }
        }
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
        // what the user sends while the assistant answers waits for the answer
        if (myAnswering) {
            myQueue.add(text);
            return;
        }
        if (myEditIndex >= 0) {
            dropFromEdited(session);
        }
        clearFollowUps();
        CompletableFuture<Map<String, String>> planQuestions = myPlanQuestions;
        myPlanQuestions = null;
        if (planQuestions != null && !planQuestions.isDone()) {
            planQuestions.cancel(false);
        }
        boolean debugTurn = myDebugMode;
        boolean planTurn = !debugTurn && isPlanMode();
        myDebugTurn = debugTurn;
        myPlanTurn = planTurn;

        if (!myFirstMessageSent) {
            myFirstMessageSent = true;
            myListener.firstMessage(text);
        }

        mySentTexts.add(text);
        myInput.setHistory(mySentTexts);

        List<AIChatAttachment> attachments = new ArrayList<>(myAttachments);
        attachments.addAll(referencedFiles(text));
        AIChatAttachment activeFile = activeFileAttachment();
        if (activeFile != null && attachments.stream().noneMatch(attachment -> attachment.content().equals(activeFile.content()))) {
            attachments.add(activeFile);
        }
        myAttachments.clear();
        updateChips();
        List<AIContent> attached = new ArrayList<>();
        for (AIChatAttachment attachment : attachments) {
            attached.addAll(attachment.content());
        }
        appendOwn(withAttachments(text, labels(attachments)), attached, session.getMessages().size(), text);
        clearUndo();
        myStopRequested = false;
        myAnswering = true;
        myStopButton.setVisible(true);
        myAnswer = newAnswer();

        int historySize = session.getMessages().size();
        AIStreamListener listener = new AIStreamListener() {
            @Override
            public void onTextDelta(String delta) {
                myUIAccess.give(() -> {
                    AIChatAnswer answer = answer();
                    answer.appendText(delta);
                    answer.getItem().setWritingStatus(LocalizeValue.empty());
                });
            }

            @Override
            public void onThinkingDelta(String delta) {
                myUIAccess.give(() -> {
                    AIChatAnswer answer = answer();
                    answer.appendThinking(delta);
                    answer.getItem().setWritingStatus(AILocalize.labelWritingThinking());
                });
            }

            @Override
            public void onToolUse(AIContent.ToolUse toolUse) {
                myUIAccess.give(() -> answer().getItem().setWritingStatus(AILocalize.labelWritingTool(toolUse.toolName())));
            }

            @Override
            public void onStatus(LocalizeValue status) {
                myUIAccess.give(() -> answer().getItem().setWritingStatus(status));
            }

            @Override
            public void onToolCall(AIContent.ToolCall toolCall) {
                if (toolCall.status() == AIToolCallStatus.COMPLETED || toolCall.status() == AIToolCallStatus.FAILED) {
                    AIChatFileRefresher.refresh(toolCall);
                }
                myUIAccess.give(() -> {
                    AIChatAnswer answer = answer();
                    answer.toolCall(toolCall);
                    answer.getItem().setWritingStatus(AILocalize.labelWritingTool(toolCall.title()));
                });
            }

            @Override
            public void onImage(AIContent.Image image) {
                myUIAccess.give(() -> answer().image(image));
            }

            @Override
            public void onPlan(List<AIPlanEntry> entries) {
                myUIAccess.give(() -> myPlan.update(entries));
            }

            @Override
            public void onPlanFile(Path file) {
                myUIAccess.give(() -> myPlanFile = file);
            }

            @Override
            public void onContextUsage(AIContextUsage usage) {
                myUIAccess.give(() -> {
                    myContextUsage = usage;
                    updateUsage();
                });
            }

            @Override
            public void onTitle(String title) {
                myUIAccess.give(() -> myListener.titleSuggested(title));
            }

            @Override
            public CompletableFuture<AIToolPermissionDecision> onToolPermission(AIToolPermissionRequest request) {
                // a question of the agent waits for its answer, whatever the chat allows
                if (!request.interactive() && (myRunEverything || myAllowedTools.contains(request.toolName()))) {
                    return CompletableFuture.completedFuture(AIToolPermissionDecision.allow());
                }
                CompletableFuture<AIToolPermissionDecision> decision = new CompletableFuture<>();
                myUIAccess.give(() -> ask(new AIChatPermissionPrompt(myProject, request, decision, () -> {
                    myRunEverything = true;
                    log(AILocalize.labelPermissionRunEverything().get());
                    decision.complete(AIToolPermissionDecision.allow());
                })));
                decision.whenCompleteAsync((value, error) -> {
                    if (value == null) {
                        return;
                    }
                    if (value.always()) {
                        myAllowedTools.add(request.toolName());
                    }
                    if (request.interactive()) {
                        String optionId = value.optionId();
                        request.options().stream()
                            .filter(option -> option.id().equals(optionId))
                            .findFirst()
                            .ifPresent(option -> log(AILocalize.labelPermissionAnswered(option.name()).get()));
                        return;
                    }
                    LocalizeValue tool = LocalizeValue.of(describe(request));
                    log((value.allowed() ? AILocalize.labelPermissionAllowed(tool) : AILocalize.labelPermissionDenied(tool)).get());
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

        List<AIContent> content = new ArrayList<>();
        // the rules of the user go first, hidden - the chat shows what the user wrote
        String rules = AIChatHiddenContext.userRules(myProject.getApplication().getInstance(AISettings.class).getActiveUserRules());
        if (rules != null) {
            content.add(AIContent.text(rules));
        }
        content.add(AIContent.text(text));
        for (AIChatAttachment attachment : attachments) {
            content.addAll(attachment.content());
        }
        // the turn starts once the files are marked - what the agent changes then can be undone
        Path directory = myWorkingDirectory != null ? myWorkingDirectory : projectDirectory();
        CompletableFuture<@Nullable String> instructions = modeInstructions(debugTurn, planTurn, directory);
        CompletableFuture<@Nullable AIChatCheckpoint> checkpoint = directory == null
            ? CompletableFuture.completedFuture(null)
            : AIChatCheckpointManager.getInstance(myProject).create(AILocalize.labelCheckpoint(summary(text)), directory)
                .<@Nullable AIChatCheckpoint>thenApply(value -> value)
                .exceptionally(error -> {
                    LOG.warn("Cannot mark the files before the turn", error);
                    return null;
                });
        checkpoint.thenCombine(instructions, (marked, mode) -> {
            myTurnCheckpoint = marked;
            return mode;
        }).thenComposeAsync(mode -> {
            if (myStopRequested) {
                return CompletableFuture.<AIResponse>failedFuture(new CancellationException());
            }
            if (mode != null) {
                content.add(rules == null ? 0 : 1, AIContent.text(mode));
            }
            return attachments.isEmpty() && rules == null && mode == null ? session.send(text, listener) : session.send(content, listener);
        }, myUIAccess)
            .whenComplete((response, throwable) -> myUIAccess.give(() -> finish(session, historySize, answer(), response, throwable)));
    }

    @RequiredUIAccess
    private void finish(AIChatSession session,
                        int historySize,
                        AIChatAnswer answer,
                        @Nullable AIResponse response,
                        @Nullable Throwable throwable) {
        myAnswer = null;
        myAnswering = false;
        myStopButton.setVisible(false);
        MessageListItem item = answer.getItem();
        item.setWriting(false);
        answer.finish();
        myListener.turnFinished(session.getMessages());
        AIChatFileRefresher.refreshDirectory(myWorkingDirectory != null ? myWorkingDirectory : projectDirectory());
        AIChatCheckpoint checkpoint = myTurnCheckpoint;
        myTurnCheckpoint = null;
        // an interrupted turn may have left the files half way - going back to before it is a decision of its own
        if (throwable == null && checkpoint != null && !answer.isEmpty()) {
            offerUndo(item, checkpoint);
        }
        // a plan turn which asked something waits for the answers - there is nothing to build yet
        List<AIQuestion> planQuestions = throwable == null && myPlanTurn && response != null
            ? AIChatPlanQuestionParser.parse(response.message().getText())
            : List.of();
        if (throwable == null && !answer.isEmpty() && (myDebugTurn || myPlanTurn && planQuestions.isEmpty())) {
            showFollowUps(myDebugTurn);
        }
        askPlanQuestions(planQuestions);
        myDebugTurn = false;
        myPlanTurn = false;

        if (throwable != null) {
            Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null ? throwable.getCause() : throwable;
            if (cause instanceof CancellationException) {
                if (answer.isEmpty()) {
                    myMessages.remove(item);
                }
                append(LocalizeValue.empty(), AILocalize.labelStopped().get(), MessageListItemStyle.SYSTEM);
            }
            else if (answer.isEmpty()) {
                item.setText(AILocalize.labelFailed(String.valueOf(cause.getMessage())).get());
                item.setStyle(MessageListItemStyle.ERROR);
            }
            else {
                append(LocalizeValue.empty(), AILocalize.labelFailed(String.valueOf(cause.getMessage())).get(), MessageListItemStyle.ERROR);
            }
        }
        else {
            // a non-streaming provider never fed the item, so fill it in from the finished answer
            if (response != null && answer.isEmpty()) {
                answer.show(response.message().content());
            }
            if (response != null) {
                myTurnUsage = response.usage();
                updateUsage();
            }

            // the tools the platform ran for the model this turn - the ones of an agent are in the answer already
            List<AIMessage> messages = session.getMessages();
            for (AIMessage message : messages.subList(Math.min(historySize, messages.size()), messages.size())) {
                for (AIContent.ToolUse toolUse : message.getToolUses()) {
                    append(LocalizeValue.empty(), AILocalize.labelToolCall(toolUse.toolName()).get(), MessageListItemStyle.SYSTEM);
                }
            }
        }

        String next = myQueue.poll();
        if (next != null) {
            send(next);
        }
    }

    /**
     * Stops the answer and drops the messages which waited for it.
     */
    @RequiredUIAccess
    private void stop() {
        myStopRequested = true;
        myQueue.clear();
        AIChatSession session = mySession;
        if (session != null) {
            session.cancel();
        }
    }

    /**
     * A queued message which is not to wait - the answer stops, and the message goes once it stopped.
     */
    @RequiredUIAccess
    private void sendNow(String text) {
        if (!myAnswering) {
            send(text);
            return;
        }
        myQueue.clear();
        myQueue.add(text);
        AIChatSession session = mySession;
        if (session != null) {
            session.cancel();
        }
    }

    /**
     * The ring of the usage - how full the context of the session is, the details in its tooltip and popup.
     */
    @RequiredUIAccess
    private void updateUsage() {
        List<String> parts = usageSummary();
        AIContextUsage context = myContextUsage;
        long percent = contextPercent(context);
        myUsage.setValue((int) Math.min(100, Math.max(0, percent)));
        myUsage.setToolTipText(LocalizeValue.of(String.join(" \u00b7 ", parts)));
        myUsage.setVisible(!parts.isEmpty());

        // the ring warns while the context fills up - the agent starts to forget, then cannot go on
        ProgressBarStyle tone = percent >= 95 ? ProgressBarStyle.ERROR : percent >= 80 ? ProgressBarStyle.WARNING : null;
        if (tone != myUsageTone) {
            if (myUsageTone != null) {
                myUsage.removeStyle(myUsageTone);
            }
            if (tone != null) {
                myUsage.addStyle(tone);
            }
            myUsageTone = tone;
        }
    }

    /**
     * The tokens of the last answer and how full the context of the session is, in a line each.
     */
    private List<String> usageSummary() {
        List<String> parts = new ArrayList<>();
        AIUsage usage = myTurnUsage;
        if (usage != null && usage.inputTokens() >= 0 && usage.outputTokens() >= 0) {
            parts.add(AILocalize.labelUsageTokens(tokens(usage.inputTokens()), tokens(usage.outputTokens())).get());
        }
        AIContextUsage context = myContextUsage;
        if (context != null && context.size() > 0 && context.used() >= 0) {
            parts.add(AILocalize.labelUsageContext(tokens(context.used()), tokens(context.size()), contextPercent(context)).get());
            String cost = cost(context);
            if (cost != null) {
                parts.add(cost);
            }
        }
        return parts;
    }

    private static long contextPercent(@Nullable AIContextUsage context) {
        return context == null || context.size() <= 0 || context.used() < 0 ? 0 : Math.round(context.used() * 100.0 / context.size());
    }

    private static @Nullable String cost(AIContextUsage context) {
        Double cost = context.cost();
        if (cost == null) {
            return null;
        }
        String currency = context.currency();
        return currency == null || currency.equals("USD")
            ? String.format(Locale.ROOT, "$%.2f", cost)
            : String.format(Locale.ROOT, "%.2f %s", cost, currency);
    }

    /**
     * The details of the usage, anchored to the ring - the context window, the last answer and what the session cost.
     */
    @RequiredUIAccess
    private void showUsagePopup() {
        AIContextUsage context = myContextUsage;
        AIUsage usage = myTurnUsage;
        if (context == null && usage == null) {
            return;
        }
        LightPopup popup = LightPopup.create(PopupOptions.builder().position(PopupPosition.TOP).arrow().build());
        VerticalLayout content = VerticalLayout.create(Space.SMALL);
        content.paddingBuilder().allSet(Space.LARGE).apply();

        if (context != null && context.size() > 0 && context.used() >= 0) {
            long percent = contextPercent(context);
            content.add(sectionLabel(AILocalize.labelUsageContextTitle()));
            content.add(Label.create(AILocalize.labelUsageContextDetail(tokens(context.used()), tokens(context.size()), percent)));
            ProgressBar bar = ProgressBar.create();
            bar.addStyle(ProgressBarStyle.HEIGHT_MEDIUM);
            if (myUsageTone != null) {
                bar.addStyle(myUsageTone);
            }
            bar.setMinimum(0);
            bar.setMaximum(100);
            bar.setValue((int) Math.min(100, percent));
            content.add(bar);
            Label left = Label.create(AILocalize.labelUsageContextLeft(tokens(Math.max(0, context.size() - context.used()))));
            left.addStyle(LabelStyle.FONT_SMALL);
            content.add(left);
        }
        if (usage != null && usage.inputTokens() >= 0 && usage.outputTokens() >= 0) {
            Label title = sectionLabel(AILocalize.labelUsageLastAnswer());
            title.paddingBuilder().topSet(Space.SMALL).apply();
            content.add(title);
            content.add(Label.create(AILocalize.labelUsageTokens(tokens(usage.inputTokens()), tokens(usage.outputTokens()))));
            if (usage.cachedInputTokens() > 0) {
                Label cached = Label.create(AILocalize.labelUsageCached(tokens(usage.cachedInputTokens())));
                cached.addStyle(LabelStyle.FONT_SMALL);
                content.add(cached);
            }
        }
        String cost = context == null ? null : cost(context);
        if (cost != null) {
            Label title = sectionLabel(AILocalize.labelUsageCostTitle());
            title.paddingBuilder().topSet(Space.SMALL).apply();
            content.add(title);
            content.add(Label.create(LocalizeValue.of(cost)));
        }
        popup.setContent(content);
        popup.showBy(myUsage);
    }

    private static String tokens(long count) {
        if (count >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", count / 1_000_000.0);
        }
        if (count >= 1_000) {
            return String.format(Locale.ROOT, "%.1fK", count / 1_000.0);
        }
        return String.valueOf(count);
    }

    @RequiredUIAccess
    public void focusInput() {
        myInput.focus();
    }

    @RequiredUIAccess
    public void showModelPicker() {
        showModelPopup();
    }

    /**
     * Switches to the next effort of the model, the default of the provider after the last one.
     *
     * @return false when the model has no efforts to switch between
     */
    @RequiredUIAccess
    public boolean cycleEffort() {
        AIModel model = myModel;
        if (model == null || model.getEffortLevels().isEmpty()) {
            return false;
        }
        List<AIEffortLevel> levels = new ArrayList<>();
        levels.add(DEFAULT_EFFORT);
        levels.addAll(model.getEffortLevels());
        int current = 0;
        for (int i = 0; i < levels.size(); i++) {
            if (levels.get(i).id().equals(myEffort)) {
                current = i;
            }
        }
        switchEffort(levels.get((current + 1) % levels.size()).id());
        return true;
    }

    /**
     * Attaches to the next message, and puts the cursor into the message box.
     */
    @RequiredUIAccess
    public void attach(AIChatAttachment attachment) {
        myAttachments.add(attachment);
        updateChips();
        myInput.focus();
    }

    /**
     * Images - screenshots - for the model to look at, chosen from the disk.
     */
    @RequiredUIAccess
    private void attachImages() {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor()
            .withTitle(AILocalize.dialogAttachImageTitle())
            .withExtensionFilter(AILocalize.labelImages(), IMAGE_TYPES.keySet().toArray(String[]::new));
        FileChooser.choosePaths(descriptor, myProject, null).whenCompleteAsync((paths, error) -> {
            if (error != null || paths == null) {
                return;
            }
            for (Path path : paths) {
                String name = path.getFileName().toString();
                String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
                String mediaType = IMAGE_TYPES.get(extension);
                try {
                    if (mediaType == null || Files.size(path) > MAX_IMAGE_BYTES) {
                        append(LocalizeValue.empty(), AILocalize.labelImageRefused(name).get(), MessageListItemStyle.ERROR);
                        continue;
                    }
                    attach(new AIChatAttachment(LocalizeValue.of(name), List.of(AIContent.image(mediaType, Files.readAllBytes(path)))));
                }
                catch (IOException e) {
                    append(LocalizeValue.empty(), AILocalize.labelFailed(String.valueOf(e.getMessage())).get(), MessageListItemStyle.ERROR);
                }
            }
        }, myUIAccess);
    }

    @RequiredUIAccess
    private void updateChips() {
        myChips.removeAll();
        for (AIChatAttachment attachment : List.copyOf(myAttachments)) {
            Button chip = Button.create(attachment.label(), event -> {
                myAttachments.remove(attachment);
                updateChips();
            });
            chip.setIcon(PlatformIconGroup.actionsClose());
            chip.setToolTipText(AILocalize.buttonRemoveAttachment());
            chip.addStyle(ButtonStyle.INPLACE);
            myChips.add(chip);
        }
        VirtualFile activeFile = myActiveFile;
        if (activeFile != null) {
            myActiveFileBox.setLabelText(LocalizeValue.of(activeFile.getName()));
            myActiveFileBox.setToolTipText(AILocalize.labelActiveFileTooltip());
            myChips.add(myActiveFileBox);
        }
        myChips.setVisible(!myAttachments.isEmpty() || activeFile != null);
    }

    /**
     * The file the user works on changed - it is offered to go with the next message.
     */
    @RequiredUIAccess
    public void setActiveFile(@Nullable VirtualFile file) {
        if (Objects.equals(file, myActiveFile)) {
            return;
        }
        myActiveFile = file;
        updateChips();
    }

    private @Nullable AIChatAttachment activeFileAttachment() {
        VirtualFile file = myActiveFile;
        if (file == null || !myActiveFileBox.getValueOrError() || !file.isValid()) {
            return null;
        }
        return new AIChatAttachment(LocalizeValue.of(file.getName()), List.of(AIContent.resourceLink("file://" + file.getPath(), file.getName(), null)));
    }

    /**
     * The files the text names as {@code @path} - completed from the files of the project - as links the agent reads.
     */
    private List<AIChatAttachment> referencedFiles(String text) {
        String basePath = myProject.getBasePath();
        if (basePath == null) {
            return List.of();
        }
        List<AIChatAttachment> files = new ArrayList<>();
        Matcher matcher = FILE_REFERENCE.matcher(text);
        while (matcher.find()) {
            Path path = Path.of(basePath).resolve(matcher.group(1)).normalize();
            if (path.startsWith(basePath) && Files.isRegularFile(path)) {
                String name = path.getFileName().toString();
                files.add(new AIChatAttachment(LocalizeValue.of(name), List.of(AIContent.resourceLink(path.toUri().toString(), name, null))));
            }
        }
        return files;
    }

    /**
     * The labels of what goes with a message - an image shows itself, it needs none.
     */
    private static List<String> labels(List<AIChatAttachment> attachments) {
        List<String> labels = new ArrayList<>();
        for (AIChatAttachment attachment : attachments) {
            if (!attachment.content().stream().allMatch(content -> content instanceof AIContent.Image)) {
                labels.add(attachment.label().get());
            }
        }
        return labels;
    }

    /**
     * The names of the files and selections a message of the history carried.
     */
    private static List<String> attachedNames(AIMessage message) {
        List<String> names = new ArrayList<>();
        for (AIContent content : message.content()) {
            if (content instanceof AIContent.ResourceLink link) {
                names.add(link.name());
            }
            else if (content instanceof AIContent.Resource resource) {
                String uri = resource.uri();
                names.add(uri.substring(uri.lastIndexOf('/') + 1));
            }
        }
        return names;
    }

    private static String withAttachments(String text, List<String> names) {
        if (names.isEmpty()) {
            return text;
        }
        StringBuilder result = new StringBuilder(text).append("\n\n");
        for (String name : names) {
            result.append("`").append(name).append("` ");
        }
        return result.toString().strip();
    }

    @RequiredUIAccess
    private void ask(AIChatPrompt<?> prompt) {
        myPrompts.addLast(prompt);
        if (myPrompts.size() == 1) {
            myPromptHolder.center(prompt.getComponent());
        }
        prompt.getResult().whenCompleteAsync((value, error) -> {
            boolean shown = myPrompts.peekFirst() == prompt;
            myPrompts.remove(prompt);
            if (!shown) {
                return;
            }
            myPromptHolder.remove(prompt.getComponent());
            AIChatPrompt<?> next = myPrompts.peekFirst();
            if (next != null) {
                myPromptHolder.center(next.getComponent());
            }
        }, myUIAccess);
    }

    /**
     * Shows what the chat said before it was opened again: the text of both sides, what the assistant thought and the
     * tools it ran.
     */
    @RequiredUIAccess
    private void showHistory(List<AIMessage> history) {
        List<AIMessage> shown = new ArrayList<>();
        for (AIMessage message : history) {
            // the answers to tools of the platform travel as messages of the user - they are not something the user said
            if (message.role() == AIRole.USER && message.getText().isEmpty()) {
                continue;
            }
            shown.add(message);
        }

        // a long conversation shows its end - rendering every message of it would hold the ui for long
        int hidden = Math.max(0, shown.size() - MAX_HISTORY_ENTRIES);
        if (hidden > 0) {
            append(LocalizeValue.empty(), AILocalize.labelHistoryHidden(hidden).get(), MessageListItemStyle.SYSTEM);
        }
        for (AIMessage message : shown.subList(hidden, shown.size())) {
            if (message.role() == AIRole.USER) {
                String said = AIChatHiddenContext.strip(message.getText()).strip();
                appendOwn(withAttachments(said, attachedNames(message)), message.content(), indexOf(history, message), said);
            }
            else {
                newAnswer().show(message.content());
                for (AIContent.ToolUse toolUse : message.getToolUses()) {
                    append(LocalizeValue.empty(), AILocalize.labelToolCall(toolUse.toolName()).get(), MessageListItemStyle.SYSTEM);
                }
            }
        }
        for (MessageListItem item : myMessages.getItems()) {
            item.setTime(null);
        }
    }

    @RequiredUIAccess
    private AIChatAnswer newAnswer() {
        MessageListItem item = append(LocalizeValue.of(myProvider.getName()), "", MessageListItemStyle.OTHER);
        if (myAnswering) {
            item.setWriting(true);
        }
        return new AIChatAnswer(myProject, myUIAccess, item);
    }

    @RequiredUIAccess
    private AIChatAnswer answer() {
        AIChatAnswer answer = myAnswer;
        if (answer == null) {
            answer = newAnswer();
            myAnswer = answer;
        }
        return answer;
    }

    @RequiredUIAccess
    private void log(String text) {
        AIChatAnswer answer = myAnswer;
        if (answer == null || !answer.isEmpty()) {
            append(LocalizeValue.empty(), text, MessageListItemStyle.SYSTEM);
            return;
        }
        myMessages.remove(answer.getItem());
        append(LocalizeValue.empty(), text, MessageListItemStyle.SYSTEM);
        myAnswer = newAnswer();
    }

    private static String describe(AIToolPermissionRequest request) {
        String description = request.description();
        return description == null || description.isBlank() ? request.displayName() : request.displayName() + ": " + description;
    }

    /**
     * A message of the user, with the images it carried shown below the text.
     */
    @RequiredUIAccess
    private void appendOwn(String text, List<AIContent> content, int index, String sentText) {
        MessageListItem item = append(AILocalize.labelYou(), text, MessageListItemStyle.OWN);
        if (index >= 0) {
            Button edit = Button.create(LocalizeValue.empty(), event -> edit(item, index, sentText));
            edit.setIcon(PlatformIconGroup.actionsEdit());
            edit.setToolTipText(AILocalize.buttonEditMessage());
            edit.addStyle(ButtonStyle.INPLACE);
            item.setActionsComponent(edit);
        }
        List<MessageListItemPart> images = new ArrayList<>();
        for (AIContent part : content) {
            if (part instanceof AIContent.Image image) {
                Component component = AIChatAnswer.imageComponent(image);
                if (component != null) {
                    images.add(MessageListItemPart.component(component));
                }
            }
        }
        if (images.isEmpty()) {
            return;
        }
        List<MessageListItemPart> parts = new ArrayList<>();
        if (!text.isEmpty()) {
            parts.add(MessageListItemPart.text(text));
        }
        parts.addAll(images);
        item.setParts(parts);
    }

    @RequiredUIAccess
    private void offerUndo(MessageListItem item, AIChatCheckpoint checkpoint) {
        Button undo = Button.create(LocalizeValue.empty(), event -> undo(item, checkpoint));
        undo.setIcon(PlatformIconGroup.actionsRollback());
        undo.setToolTipText(AILocalize.buttonUndoTurn());
        undo.addStyle(ButtonStyle.INPLACE);
        item.setActionsComponent(undo);
        myUndoItem = item;
    }

    @RequiredUIAccess
    private void clearUndo() {
        MessageListItem item = myUndoItem;
        if (item != null) {
            item.setActionsComponent(null);
            myUndoItem = null;
        }
    }

    /**
     * Puts the files back the way they were before the turn - the chat itself stays as it is.
     */
    @RequiredUIAccess
    private void undo(MessageListItem item, AIChatCheckpoint checkpoint) {
        if (myAnswering || item != myUndoItem) {
            return;
        }
        Path directory = myWorkingDirectory != null ? myWorkingDirectory : projectDirectory();
        if (directory == null) {
            return;
        }
        MessageBoxes.okCancel()
            .asQuestion()
            .title(AILocalize.dialogUndoTurnTitle())
            .text(AILocalize.dialogUndoTurnText(String.valueOf(directory.getFileName())))
            .showAsync()
            .whenCompleteAsync((confirmed, error) -> {
                if (!Boolean.TRUE.equals(confirmed) || myAnswering || item != myUndoItem) {
                    return;
                }
                clearUndo();
                checkpoint.revert(directory).whenCompleteAsync((ignored, failure) -> {
                    if (failure != null) {
                        append(LocalizeValue.empty(), AILocalize.labelUndoFailed(String.valueOf(failure.getMessage())).get(), MessageListItemStyle.ERROR);
                    }
                    else {
                        log(AILocalize.labelUndone().get());
                    }
                }, myUIAccess);
            }, myUIAccess);
    }

    private @Nullable Path projectDirectory() {
        String basePath = myProject.getBasePath();
        return basePath == null ? null : Path.of(basePath);
    }

    private static String summary(String text) {
        String compact = text.replaceAll("\\s+", " ").strip();
        return compact.length() > 40 ? compact.substring(0, 40) + "\u2026" : compact;
    }

    /**
     * Puts an earlier message back into the box - sent again, it replaces itself and what came after it.
     */
    @RequiredUIAccess
    private void edit(MessageListItem item, int index, String text) {
        if (myAnswering) {
            return;
        }
        myEditIndex = index;
        myEditItem = item;
        myEditBanner.setVisible(true);
        myInput.setText(text);
        myInput.focus();
    }

    @RequiredUIAccess
    private void cancelEdit() {
        myEditIndex = -1;
        myEditItem = null;
        myEditBanner.setVisible(false);
        myInput.setText("");
    }

    /**
     * The edited message and every message after it leave the chat - the provider starts over from what came before.
     */
    @RequiredUIAccess
    private void dropFromEdited(AIChatSession session) {
        List<AIMessage> messages = session.getMessages();
        session.startOver(List.copyOf(messages.subList(0, Math.min(myEditIndex, messages.size()))));

        MessageListItem edited = myEditItem;
        List<MessageListItem> items = myMessages.getItems();
        int from = edited == null ? -1 : items.indexOf(edited);
        if (from >= 0) {
            for (MessageListItem item : List.copyOf(items.subList(from, items.size()))) {
                myMessages.remove(item);
            }
        }
        myEditIndex = -1;
        myEditItem = null;
        myEditBanner.setVisible(false);
        // the plan was of the turns which are gone
        myPlan.update(List.of());
        log(AILocalize.labelEditedResent().get());
    }

    private static int indexOf(List<AIMessage> history, AIMessage message) {
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i) == message) {
                return i;
            }
        }
        return -1;
    }

    @RequiredUIAccess
    private MessageListItem append(LocalizeValue author, String text, MessageListItemStyle style) {
        MessageListItem item = myMessages.add(author, text);
        item.setTime(Instant.now());
        item.setStyle(style);
        return item;
    }
}

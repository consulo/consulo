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
package consulo.ide.impl.idea.find.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.dumb.DumbAware;
import consulo.application.internal.ProgressIndicatorBase;
import consulo.application.internal.ProgressIndicatorUtils;
import consulo.application.internal.ReadTask;
import consulo.application.progress.ProgressIndicator;
import consulo.application.ui.UISettings;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.EditorColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileEditor.UniqueVFilePathBuilder;
import consulo.fileEditor.VfsPresentationUtil;
import consulo.fileEditor.impl.internal.search.UnifiedSearchTextArea;
import consulo.find.*;
import consulo.find.localize.FindLocalize;
import consulo.ide.impl.idea.find.actions.ShowUsagesAction;
import consulo.ide.impl.idea.find.replaceInProject.ReplaceInProjectManager;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.usages.impl.UnifiedUsagePreviewPanel;
import consulo.ide.localize.IdeLocalize;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScopeUtil;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.action.*;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.font.Font;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.style.ComponentColors;
import consulo.ui.util.TextWithMnemonic;
import consulo.undoRedo.CommandProcessor;
import consulo.usage.*;
import consulo.usage.localize.UsageLocalize;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.Key;
import consulo.util.io.PathUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.PatternUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SoftReference;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author VISTALL
 * @since 2026-10-02
 */
public class UnifiedFindPopupPanel implements FindUI {
    private static final KeyStroke ENTER = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
    private static final KeyStroke ENTER_WITH_MODIFIERS =
        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Platform.current().os().isMac() ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK);
    private static final KeyStroke REPLACE_ALL =
        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK);

    private static final Size2D DEFAULT_SIZE = new Size2D(920, 660);

    private static final Key<AdvancedLabel> USAGE_TEXT_KEY = Key.create("UnifiedFindPopupPanel.usageText");
    private static final Key<AdvancedLabel> USAGE_FILE_KEY = Key.create("UnifiedFindPopupPanel.usageFile");

    record Validation(LocalizeValue message, @Nullable Component component) {
    }

    record FilePresentation(String path, @Nullable ColorValue background) {
    }

    private final FindUIHelper myHelper;
    private final Project myProject;
    private final Disposable myDisposable;
    private final UIAccess myUIAccess;

    private final AtomicBoolean myCanClose = new AtomicBoolean(true);
    private final AtomicBoolean myIsPinned = new AtomicBoolean(false);
    private final AtomicBoolean myCaseSensitiveState = new AtomicBoolean();
    private final AtomicBoolean myPreserveCaseState = new AtomicBoolean();
    private final AtomicBoolean myWholeWordsState = new AtomicBoolean();
    private final AtomicBoolean myRegexState = new AtomicBoolean();
    private final AtomicBoolean myNeedReset = new AtomicBoolean(true);

    private final DockLayout myRootLayout;
    private final UnifiedFindPopupScopeUI myScopeUI;

    private AdvancedLabel myTitleLabel;
    private Label myInfoLabel;
    private ImageBox myLoadingImage;
    private CheckBox myCbFileFilter;
    private TextBoxWithHistory myFileMaskField;
    private ActionToolbar myTopToolbar;
    private UnifiedSearchTextArea mySearchArea;
    private UnifiedSearchTextArea myReplaceArea;
    private DockLayout myReplaceRow;
    private ActionToolbar myScopeSelectionToolbar;
    private DockLayout myScopeDetailsLayout;
    private @Nullable Component myScopeDetailsComponent;
    private MutableFlatDataModel<UsageInfoAdapter> myResultsModel;
    private ListBox<UsageInfoAdapter> myResultsList;
    private Label myPreviewTitle;
    private UnifiedUsagePreviewPanel myUsagePreviewPanel;
    private Label myOKHintLabel;
    private Label myNavigationHintLabel;
    private Button myOKButton;
    private Button myReplaceAllButton;
    private Button myReplaceSelectedButton;

    private FindSearchContext mySelectedContext = FindSearchContext.ANY;
    private UnifiedFindPopupScopeUI.ScopeType mySelectedScope = UnifiedFindPopupScopeUI.PROJECT;

    private final List<UsageInfoAdapter> myResults = new ArrayList<>();
    private final Queue<UsageInfoAdapter> myPendingResults = new ConcurrentLinkedQueue<>();
    private final Map<VirtualFile, FilePresentation> myFilePresentations = new ConcurrentHashMap<>();
    private final AtomicBoolean myFlushScheduled = new AtomicBoolean();
    private @Nullable String myFirstResultPath;

    private @Nullable Window myWindow;
    private boolean myWindowWasActive;
    private boolean myIgnoreFocusRequests;
    private @Nullable ScheduledFuture<?> myScheduledSearch;
    private @Nullable ScheduledFuture<?> myScheduledPreview;
    private boolean myApplyingResults;
    private volatile @Nullable ProgressIndicatorBase myResultsPreviewSearchProgress;
    private volatile int myLoadingHash;
    private String myUsagesCount = "";
    private String myFilesCount = "";
    private final UsageViewPresentation myUsageViewPresentation = new UsageViewPresentation();

    @RequiredUIAccess
    UnifiedFindPopupPanel(FindUIHelper helper) {
        myHelper = helper;
        myProject = helper.getProject();
        myDisposable = Disposable.newDisposable();
        myUIAccess = UIAccess.current();

        myRootLayout = DockLayout.create(Space.NONE);
        myScopeUI = new UnifiedFindPopupScopeUI(this);

        Disposer.register(myDisposable, () -> {
            finishPreviousPreviewSearch();
            cancelScheduledSearch();
            cancelScheduledPreview();
            closeWindow();
        });

        initComponents();
        initByModel();
    }

    @Override
    @RequiredUIAccess
    public void showUI() {
        if (myWindow != null) {
            return;
        }

        WindowOptions.Builder options = WindowOptions.builder().disableModal();
        if (!Registry.is("ide.find.as.popup.decorated")) {
            options.undecorated();
        }

        Window window = Window.create(myHelper.getTitle(), options.build());
        window.setContent(myRootLayout);

        window.setSize(DEFAULT_SIZE);
        window.addCloseListener(event -> onWindowClosed());

        myWindow = window;
        myWindowWasActive = false;

        Disposer.register(myDisposable, FocusManager.get().addListener(this::onFocusChanged));

        CompletableFuture.allOf(
            myScopeSelectionToolbar.updateActionsAsync(),
            myTopToolbar.updateActionsAsync(),
            mySearchArea.updateAllAsync(),
            myReplaceArea.updateAllAsync()
        ).whenCompleteAsync((unused, throwable) -> {
            Window shownWindow = myWindow;
            if (shownWindow == null) {
                return;
            }

            shownWindow.show();

            focusSearchField();
            myUIAccess.getScheduler().schedule(this::focusSearchField, ModalityState.any(), 150, TimeUnit.MILLISECONDS);

            scheduleResultsUpdate();
        }, myUIAccess);
    }

    @RequiredUIAccess
    private void focusSearchField() {
        if (myWindow == null) {
            return;
        }

        TextArea searchField = mySearchArea.getTextArea();
        searchField.selectAll();
        searchField.focus();
    }

    @RequiredUIAccess
    private void onFocusChanged() {
        Window window = myWindow;
        if (window == null) {
            return;
        }

        if (window.isActive()) {
            myWindowWasActive = true;
            return;
        }

        if (myWindowWasActive && Window.getActiveWindow() != null && canBeClosed()) {
            closeImmediately();
        }
    }

    @RequiredUIAccess
    private void onWindowClosed() {
        if (myWindow == null) {
            return;
        }

        myWindow = null;

        saveSettings();

        Disposer.dispose(myDisposable);
    }

    @RequiredUIAccess
    private void closeWindow() {
        Window window = myWindow;
        myWindow = null;

        if (window != null) {
            window.close();
        }
    }

    @RequiredUIAccess
    private void closeImmediately() {
        Window window = myWindow;
        if (window != null) {
            myIsPinned.set(false);
            window.close();
        }
    }

    private boolean canBeClosed() {
        if (myProject.isDisposed()) {
            return true;
        }
        if (!myCanClose.get()) {
            return false;
        }
        return !myIsPinned.get();
    }

    @Override
    public void saveSettings() {
        FindSettings findSettings = FindSettings.getInstance();
        myScopeUI.applyTo(findSettings, mySelectedScope);
        myHelper.updateFindSettings();
        applyTo(FindManager.getInstance(myProject).getFindInProjectModel());
    }

    @Override
    public Disposable getDisposable() {
        return myDisposable;
    }

    @Override
    public Component getUIComponent() {
        return myRootLayout;
    }

    Project getProject() {
        return myProject;
    }

    FindUIHelper getHelper() {
        return myHelper;
    }

    AtomicBoolean getCanClose() {
        return myCanClose;
    }

    @RequiredUIAccess
    private void initComponents() {
        myTitleLabel = AdvancedLabel.create();
        myInfoLabel = Label.create(LocalizeValue.empty());
        myInfoLabel.setForegroundColor(ComponentColors.INFO_FOREGROUND);
        myLoadingImage = ImageBox.create(Image.busy());
        myLoadingImage.setVisible(false);

        myCbFileFilter = CheckBox.create(FindLocalize.findPopupFilemask());
        myFileMaskField = TextBoxWithHistory.create();
        myFileMaskField.setVisibleLength(16);
        myCbFileFilter.addValueListener(event -> {
            boolean checked = Boolean.TRUE.equals(myCbFileFilter.getValue());
            myFileMaskField.setEnabled(checked);
            if (!myIgnoreFocusRequests) {
                if (checked) {
                    myFileMaskField.focus();
                    myFileMaskField.selectAll();
                }
                else {
                    mySearchArea.getTextArea().focus();
                }
            }
            scheduleResultsUpdate();
        });
        myFileMaskField.addValueListener(event -> scheduleResultsUpdate());

        AnAction showFilterPopupAction = new MyShowFilterPopupAction();
        showFilterPopupAction.registerCustomShortcutSet(showFilterPopupAction.getShortcutSet(), myRootLayout);
        myTopToolbar = createToolbar(
            "FindInFilesTopMenu",
            ActionGroup.newImmutableBuilder().add(showFilterPopupAction).add(new MyPinAction()).build(),
            ActionToolbar.Style.HORIZONTAL
        );

        mySearchArea = new UnifiedSearchTextArea(myProject, true, myRootLayout);
        myReplaceArea = new UnifiedSearchTextArea(myProject, false, myRootLayout);
        mySearchArea.setMultilineEnabled(Registry.is("ide.find.as.popup.allow.multiline"));
        myReplaceArea.setMultilineEnabled(Registry.is("ide.find.as.popup.allow.multiline"));

        ToggleAction caseSensitiveAction = createAction(
            FindLocalize.findPopupCaseSensitive(),
            PlatformIconGroup.actionsMatchcase(),
            PlatformIconGroup.actionsMatchcasehovered(),
            PlatformIconGroup.actionsMatchcaseselected(),
            myCaseSensitiveState,
            () -> !myHelper.getModel().isReplaceState() || !myPreserveCaseState.get()
        );
        ToggleAction wholeWordsAction = createAction(
            FindLocalize.findWholeWords(),
            PlatformIconGroup.actionsWords(),
            PlatformIconGroup.actionsWordshovered(),
            PlatformIconGroup.actionsWordsselected(),
            myWholeWordsState,
            () -> !myRegexState.get()
        );
        ToggleAction regexAction = createAction(
            FindLocalize.findRegex(),
            PlatformIconGroup.actionsRegex(),
            PlatformIconGroup.actionsRegexhovered(),
            PlatformIconGroup.actionsRegexselected(),
            myRegexState,
            () -> !myHelper.getModel().isReplaceState() || !myPreserveCaseState.get()
        );
        mySearchArea.setSuffixActions(List.of(caseSensitiveAction, wholeWordsAction, regexAction));

        ToggleAction preserveCaseAction = createAction(
            FindLocalize.findOptionsReplacePreserveCase(),
            PlatformIconGroup.actionsPreservecase(),
            PlatformIconGroup.actionsPreservecasehover(),
            PlatformIconGroup.actionsPreservecaseselected(),
            myPreserveCaseState,
            () -> !myRegexState.get() && !myCaseSensitiveState.get()
        );
        myReplaceArea.setSuffixActions(List.of(preserveCaseAction));

        TextArea searchField = mySearchArea.getTextArea();
        TextArea replaceField = myReplaceArea.getTextArea();
        searchField.borderBuilder().topSet().bottomSet().apply();
        replaceField.borderBuilder().bottomSet().apply();

        searchField.addValueListener(event -> scheduleResultsUpdate());
        replaceField.addValueListener(event -> {
            applyTo(myHelper.getModel());
            updatePreview();
        });

        List<AnAction> scopeActions = new ArrayList<>();
        for (UnifiedFindPopupScopeUI.ScopeType scopeType : myScopeUI.getComponents().keySet()) {
            scopeActions.add(new MySelectScopeToggleAction(scopeType));
        }
        myScopeSelectionToolbar = createToolbar(
            ActionPlaces.EDITOR_TOOLBAR,
            ActionGroup.newImmutableBuilder().addAll(scopeActions).build(),
            ActionToolbar.Style.HORIZONTAL
        );
        myScopeDetailsLayout = DockLayout.create(Space.NONE);

        myResultsModel = FlatDataModel.of(new ArrayList<>());
        myResultsList = ListBox.create(myResultsModel);
        myResultsList.setRender(ComponentItemRender.reusable(this::createResultRow, this::bindResultRow));
        myResultsList.setPlaceholder(UILocalize.messageNothingtoshow());
        myResultsList.addValueListener(event -> {
            if (!myApplyingResults) {
                updatePreview();
            }
        });
        myResultsList.addDoubleClickListener(event -> navigateToSelectedUsage(null));

        myPreviewTitle = Label.create(LocalizeValue.empty());
        myUsagePreviewPanel = new UnifiedUsagePreviewPanel(myProject);
        Disposer.register(myDisposable, myUsagePreviewPanel);

        CheckBox openOnNewTabBox = CheckBox.create(FindLocalize.findOpenInNewTabAction());
        openOnNewTabBox.setValue(myHelper.getModel().isOpenInNewTab());
        openOnNewTabBox.addValueListener(event -> myHelper.getModel().setOpenInNewTab(Boolean.TRUE.equals(event.getValue())));

        myOKButton = Button.create(FindLocalize.findPopupFindButton());
        myOKButton.addStyle(ButtonStyle.BORDERLESS);
        myReplaceAllButton = Button.create(FindLocalize.findPopupReplaceAllButton());
        myReplaceAllButton.addStyle(ButtonStyle.BORDERLESS);
        myReplaceAllButton.setToolTipText(LocalizeValue.localizeTODO(KeymapUtil.getKeystrokeText(REPLACE_ALL)));
        myReplaceSelectedButton = Button.create(FindLocalize.findPopupReplaceSelectedButton(0));
        myReplaceSelectedButton.addStyle(ButtonStyle.BORDERLESS);

        myOKButton.addClickListener(event -> doOK(true));
        myReplaceAllButton.addClickListener(event -> doOK(false));
        myReplaceSelectedButton.addClickListener(event -> doReplaceSelected());

        myOKHintLabel = Label.create(LocalizeValue.empty());
        myOKHintLabel.setForegroundColor(ComponentColors.DISABLED_TEXT);
        myNavigationHintLabel = Label.create(LocalizeValue.empty());
        myNavigationHintLabel.setForegroundColor(ComponentColors.DISABLED_TEXT);

        installKeyActions(searchField, replaceField);

        myRootLayout.top(createTopLayout(searchField, replaceField));
        myRootLayout.center(createResultsLayout());
        myRootLayout.bottom(createBottomLayout(openOnNewTabBox));

        myIsPinned.set(UISettings.getInstance().getPinFindInPath());
    }

    @RequiredUIAccess
    private Component createTopLayout(TextArea searchField, TextArea replaceField) {
        HorizontalLayout titleLayout = HorizontalLayout.create(Space.SMALL);
        titleLayout.add(myTitleLabel);
        titleLayout.add(myInfoLabel);
        titleLayout.add(myLoadingImage);

        HorizontalLayout filterLayout = HorizontalLayout.create(Space.SMALL);
        filterLayout.add(myCbFileFilter);
        filterLayout.add(myFileMaskField);
        filterLayout.add(myTopToolbar.getUIComponent());

        DockLayout headerLayout = DockLayout.create(Space.NONE);
        headerLayout.left(titleLayout);
        headerLayout.right(filterLayout);
        headerLayout.paddingBuilder().leftSet(Space.SMALL).topSet(Space.SMALL).bottomSet(Space.SMALL).apply();

        DockLayout searchRow = DockLayout.create(Space.NONE);
        searchRow.center(searchField);

        myReplaceRow = DockLayout.create(Space.NONE);
        myReplaceRow.center(replaceField);

        DockLayout scopesLayout = DockLayout.create(Space.NONE);
        scopesLayout.left(myScopeSelectionToolbar.getUIComponent());
        scopesLayout.center(myScopeDetailsLayout);

        VerticalLayout topLayout = VerticalLayout.create(Space.NONE);
        topLayout.add(headerLayout);
        topLayout.add(searchRow);
        topLayout.add(myReplaceRow);
        topLayout.add(scopesLayout);
        return topLayout;
    }

    @RequiredUIAccess
    private Component createResultsLayout() {
        DockLayout previewLayout = DockLayout.create(Space.NONE);
        DockLayout previewTitleLayout = DockLayout.create(Space.NONE);
        previewTitleLayout.left(myPreviewTitle);
        previewTitleLayout.paddingBuilder().allSet(Space.SMALL).apply();
        previewTitleLayout.borderBuilder().topSet().bottomSet().apply();
        previewLayout.top(previewTitleLayout);
        previewLayout.center(myUsagePreviewPanel.getComponent());

        TwoComponentSplitLayout splitLayout = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);
        splitLayout.setFirstComponent(myResultsList);
        splitLayout.setSecondComponent(previewLayout);
        splitLayout.setProportion(33);
        return splitLayout;
    }

    @RequiredUIAccess
    private Component createBottomLayout(CheckBox openOnNewTabBox) {
        DockLayout bottomLayout = DockLayout.create();
        bottomLayout.paddingBuilder().allSet(Space.SMALL).apply();
        bottomLayout.left(openOnNewTabBox);

        HorizontalLayout rightBottomPanel = HorizontalLayout.create();
        rightBottomPanel.add(myNavigationHintLabel);
        rightBottomPanel.add(myOKHintLabel);
        rightBottomPanel.add(myOKButton);
        rightBottomPanel.add(myReplaceAllButton);
        rightBottomPanel.add(myReplaceSelectedButton);
        bottomLayout.right(rightBottomPanel);

        DockLayout borderWrapper = DockLayout.create();
        borderWrapper.center(bottomLayout);
        borderWrapper.borderBuilder().topSet().apply();
        return borderWrapper;
    }

    @RequiredUIAccess
    private void installKeyActions(TextArea searchField, TextArea replaceField) {
        new MyEnterAction().registerCustomShortcutSet(new CustomShortcutSet(ENTER), myRootLayout);
        DumbAwareAction.create(e -> doOK(true)).registerCustomShortcutSet(new CustomShortcutSet(ENTER_WITH_MODIFIERS), myRootLayout);
        DumbAwareAction.create(e -> doOK(false)).registerCustomShortcutSet(new CustomShortcutSet(REPLACE_ALL), myRootLayout);

        AnAction escape = ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_ESCAPE);
        DumbAwareAction.create(e -> closeImmediately())
            .registerCustomShortcutSet(escape == null ? CommonShortcuts.ESCAPE : escape.getShortcutSet(), myRootLayout);

        List<Shortcut> navigationShortcuts = new ArrayList<>();
        KeyStroke viewSourceKeyStroke = KeymapUtil.getKeyStroke(CommonShortcuts.getViewSource());
        if (viewSourceKeyStroke != null && !Comparing.equal(viewSourceKeyStroke, ENTER_WITH_MODIFIERS) && !Comparing.equal(viewSourceKeyStroke, ENTER)) {
            navigationShortcuts.add(new KeyboardShortcut(viewSourceKeyStroke, null));
        }
        KeyStroke editSourceKeyStroke = KeymapUtil.getKeyStroke(CommonShortcuts.getEditSource());
        if (editSourceKeyStroke != null && !Comparing.equal(editSourceKeyStroke, ENTER_WITH_MODIFIERS) && !Comparing.equal(editSourceKeyStroke, ENTER)) {
            navigationShortcuts.add(new KeyboardShortcut(editSourceKeyStroke, null));
        }
        if (!navigationShortcuts.isEmpty()) {
            DumbAwareAction.create(this::navigateToSelectedUsage)
                .registerCustomShortcutSet(new CustomShortcutSet(navigationShortcuts.toArray(Shortcut.EMPTY_ARRAY)), myRootLayout);
        }

        ShortcutSet up = new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));
        ShortcutSet down = new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
        DumbAwareAction previousUsageAction = DumbAwareAction.create(e -> myResultsList.moveSelection(-1));
        DumbAwareAction nextUsageAction = DumbAwareAction.create(e -> myResultsList.moveSelection(1));
        for (Component component : List.of(searchField, replaceField)) {
            previousUsageAction.registerCustomShortcutSet(up, component);
            nextUsageAction.registerCustomShortcutSet(down, component);
        }

        Keymap activeKeymap = KeymapManager.getInstance().getActiveKeymap();
        if (activeKeymap != null) {
            ShortcutSet findNextShortcutSet = new CustomShortcutSet(activeKeymap.getShortcuts("FindNext"));
            ShortcutSet findPreviousShortcutSet = new CustomShortcutSet(activeKeymap.getShortcuts("FindPrevious"));
            DumbAwareAction findNextAction = DumbAwareAction.create(e -> myResultsList.moveSelection(1));
            DumbAwareAction findPreviousAction = DumbAwareAction.create(e -> myResultsList.moveSelection(-1));
            for (Component component : List.of(searchField, replaceField)) {
                findNextAction.registerCustomShortcutSet(findNextShortcutSet, component);
                findPreviousAction.registerCustomShortcutSet(findPreviousShortcutSet, component);
            }
        }
    }

    private ActionToolbar createToolbar(String place, ActionGroup group, ActionToolbar.Style style) {
        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar(place, group, style);
        toolbar.setTargetUIComponent(myRootLayout);
        if (style == ActionToolbar.Style.HORIZONTAL) {
            toolbar.getUIComponent().paddingBuilder().topReset().bottomReset().apply();
        }
        return toolbar;
    }

    @RequiredUIAccess
    private Component createResultRow() {
        AdvancedLabel textLabel = AdvancedLabel.create();
        AdvancedLabel fileLabel = AdvancedLabel.create();

        DockLayout row = DockLayout.create(Space.NONE);
        row.center(textLabel);
        row.right(fileLabel);
        row.paddingBuilder().allSet(Space.X_SMALL).apply();

        row.putUserData(USAGE_TEXT_KEY, textLabel);
        row.putUserData(USAGE_FILE_KEY, fileLabel);
        return row;
    }

    @RequiredUIAccess
    private void bindResultRow(Component row, RenderItem<UsageInfoAdapter> item) {
        AdvancedLabel textLabel = Objects.requireNonNull(row.getUserData(USAGE_TEXT_KEY));
        AdvancedLabel fileLabel = Objects.requireNonNull(row.getUserData(USAGE_FILE_KEY));

        UsageInfoAdapter usage = item.getValue();
        if (!(usage instanceof UsageInfo2UsageAdapter usageAdapter)) {
            textLabel.updatePresentation(TextItemPresentation::clearText);
            fileLabel.updatePresentation(TextItemPresentation::clearText);
            row.setBackgroundColor(null);
            return;
        }

        TextChunk[] text = usageAdapter.getPresentation().getText();

        textLabel.updatePresentation(presentation -> {
            presentation.clearText();

            if (!usageAdapter.isValid()) {
                presentation.append(" " + UsageLocalize.nodeInvalid().get() + " ", TextAttribute.ERROR);
            }

            ColorValue searchMatchBackground = getSearchMatchBackground();
            for (int i = 1; i < text.length; ++i) {
                TextChunk textChunk = text[i];
                presentation.append(textChunk.getText(), toTextAttribute(textChunk, searchMatchBackground));
            }
        });

        VirtualFile file = usageAdapter.getFile();
        FilePresentation filePresentation = file == null ? null : getFilePresentation(file);
        UsageInfoAdapter previous = getPreviousResult(usageAdapter);
        boolean repeatedFile = previous instanceof UsageInfo2UsageAdapter previousAdapter && Comparing.equal(file, previousAdapter.getFile());

        fileLabel.updatePresentation(presentation -> {
            presentation.clearText();

            String path = filePresentation == null ? "" : filePresentation.path();
            presentation.append(path, repeatedFile ? new TextAttribute(Font.PLAIN, ComponentColors.DISABLED_TEXT) : TextAttribute.GRAYED);
            if (text.length > 0) {
                presentation.append(" " + text[0].getText(), TextAttribute.GRAYED);
            }
        });

        ColorValue background = null;
        if (!item.isSelected() && filePresentation != null) {
            background = filePresentation.background();
        }
        row.setBackgroundColor(background);
    }

    private void preparePresentation(UsageInfoAdapter usage) {
        if (!(usage instanceof UsageInfo2UsageAdapter usageAdapter)) {
            return;
        }

        ReadAction.run(() -> {
            if (!usageAdapter.isValid()) {
                return;
            }

            usageAdapter.getPresentation().getText();

            VirtualFile file = usageAdapter.getFile();
            if (file != null) {
                getFilePresentation(file);
            }
        });
    }

    private FilePresentation getFilePresentation(VirtualFile file) {
        FilePresentation presentation = myFilePresentations.get(file);
        if (presentation != null) {
            return presentation;
        }

        presentation = new FilePresentation(
            UniqueVFilePathBuilder.getInstance().getUniqueVirtualFilePath(myProject, file),
            VfsPresentationUtil.getFileBackgroundColor(myProject, file)
        );
        FilePresentation previous = myFilePresentations.putIfAbsent(file, presentation);
        return previous != null ? previous : presentation;
    }

    private @Nullable UsageInfoAdapter getPreviousResult(UsageInfoAdapter usage) {
        int index = myResultsModel.indexOf(usage);
        return index > 0 ? myResultsModel.get(index - 1) : null;
    }

    private static @Nullable ColorValue getSearchMatchBackground() {
        TextAttributes attributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
        return attributes == null ? null : attributes.getBackgroundColor();
    }

    private static TextAttribute toTextAttribute(TextChunk textChunk, @Nullable ColorValue searchMatchBackground) {
        TextAttributes attributes = textChunk.getAttributes();
        int fontType = attributes.getFontType();
        boolean highlighted = textChunk.getType() != null || (fontType & Font.BOLD) != 0;
        if (highlighted) {
            return new TextAttribute(fontType & ~Font.BOLD, attributes.getForegroundColor(), searchMatchBackground);
        }
        return new TextAttribute(fontType, attributes.getForegroundColor());
    }

    @RequiredUIAccess
    private void updatePreview() {
        UsageInfoAdapter selected = myResultsList.getValue();

        List<UsageInfo> selection = new ArrayList<>();
        String file = null;
        if (selected != null) {
            file = selected.getPath();
            if (selected.isValid()) {
                selection.addAll(Arrays.asList(selected.getMergedInfos()));
            }
        }

        myReplaceSelectedButton.setText(FindLocalize.findPopupReplaceSelectedButton(selection.size()));

        FindInProjectUtil.setupViewPresentation(myUsageViewPresentation, myHelper.getModel().clone());
        myUsagePreviewPanel.updateLayout(selection);

        LocalizeValue title = LocalizeValue.empty();
        if (file != null && UnifiedUsagePreviewPanel.cannotPreviewMessage(selection).isEmpty()) {
            title = LocalizeValue.of(PathUtil.getFileName(file));
        }
        myPreviewTitle.setText(title);
    }

    @RequiredUIAccess
    private void doOK(boolean openInFindWindow) {
        if (!canBeClosedImmediately()) {
            return;
        }

        FindModel validateModel = myHelper.getModel().clone();
        applyTo(validateModel);

        Validation validation = getValidationInfo(validateModel);
        if (validation != null) {
            MessageBoxes.okError(validation.message()).title(CommonLocalize.titleError()).showAsync();
            return;
        }

        if (validateModel.isReplaceState()
            && !openInFindWindow
            && myResultsModel.getSize() > 1
            && !ReplaceInProjectManager.getInstance(myProject)
            .showReplaceAllConfirmDialog(myUsagesCount, getStringToFind(), myFilesCount, getStringToReplace())) {
            return;
        }

        myHelper.getModel().copyFrom(validateModel);
        myHelper.getModel().setPromptOnReplace(openInFindWindow);
        myHelper.doOKAction();

        closeImmediately();
    }

    private boolean canBeClosedImmediately() {
        boolean state = myIsPinned.get();
        myIsPinned.set(false);
        try {
            return canBeClosed();
        }
        finally {
            myIsPinned.set(state);
        }
    }

    @RequiredUIAccess
    private void doReplaceSelected() {
        UsageInfoAdapter selected = myResultsList.getValue();
        if (selected == null) {
            return;
        }

        int index = myResultsModel.indexOf(selected);

        CommandProcessor.getInstance().newCommand()
            .project(myProject)
            .name(FindLocalize.findReplaceCommand())
            .run(() -> {
                try {
                    ReplaceInProjectManager.getInstance(myProject).replaceUsage(selected, myHelper.getModel(), Collections.emptySet(), false);

                    myResults.remove(selected);
                    myResultsModel.remove(selected);
                }
                catch (FindManager.MalformedReplacementStringException ex) {
                    MessageBoxes.okError(LocalizeValue.ofNullable(ex.getMessage()))
                        .title(FindLocalize.findReplaceInvalidReplacementStringTitle())
                        .showAsync();
                    return;
                }

                int size = myResultsModel.getSize();
                if (size > 0) {
                    myResultsList.setValueByIndex(Math.min(index, size - 1));
                }
            });
    }

    private ToggleAction createAction(
        LocalizeValue message,
        Image icon,
        Image hoveredIcon,
        Image selectedIcon,
        AtomicBoolean state,
        Supplier<Boolean> enableStateProvider
    ) {
        return new DumbAwareToggleAction(message, LocalizeValue.empty(), icon) {
            {
                getTemplatePresentation().setHoveredIcon(hoveredIcon);
                getTemplatePresentation().setSelectedIcon(selectedIcon);
                int mnemonic = KeyEvent.getExtendedKeyCodeForChar(
                    TextWithMnemonic.parse(getTemplatePresentation().getTextWithMnemonic()).getMnemonic()
                );
                if (mnemonic != KeyEvent.VK_UNDEFINED) {
                    setShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(
                        mnemonic,
                        Platform.current().os().isMac() ? InputEvent.ALT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK : InputEvent.ALT_DOWN_MASK
                    )));
                    registerCustomShortcutSet(getShortcutSet(), myRootLayout);
                }
            }

            @Override
            public boolean isSelected(AnActionEvent e) {
                return state.get();
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(enableStateProvider.get());
                Toggleable.setSelected(e.getPresentation(), state.get());
            }

            @Override
            public void setSelected(AnActionEvent e, boolean selected) {
                state.set(selected);
                scheduleResultsUpdate();
            }
        };
    }

    @Override
    @RequiredUIAccess
    public void initByModel() {
        FindModel model = myHelper.getModel();
        myCaseSensitiveState.set(model.isCaseSensitive());
        myWholeWordsState.set(model.isWholeWordsOnly());
        myRegexState.set(model.isRegularExpressions());

        mySelectedContext = model.getSearchContext();
        if (model.isReplaceState()) {
            myPreserveCaseState.set(model.isPreserveCase());
        }

        mySelectedScope = myScopeUI.initByModel(model);

        boolean isThereFileFilter = !StringUtil.isEmpty(model.getFileFilter());
        myIgnoreFocusRequests = true;
        try {
            myCbFileFilter.setValue(isThereFileFilter);
        }
        finally {
            myIgnoreFocusRequests = false;
        }

        List<String> variants = Arrays.asList(ArrayUtil.reverseArray(FindSettings.getInstance().getRecentFileMasks()));
        myFileMaskField.setHistory(variants);
        if (!variants.isEmpty()) {
            myFileMaskField.setValue(variants.get(0));
        }
        myFileMaskField.setEnabled(isThereFileFilter);

        FindInProjectSettings findInProjectSettings = FindInProjectSettings.getInstance(myProject);

        String toSearch = model.getStringToFind();
        if (StringUtil.isEmpty(toSearch)) {
            String[] history = findInProjectSettings.getRecentFindStrings();
            toSearch = history.length > 0 ? history[history.length - 1] : "";
        }
        mySearchArea.getTextArea().setValue(toSearch);

        String toReplace = model.getStringToReplace();
        if (StringUtil.isEmpty(toReplace)) {
            String[] history = findInProjectSettings.getRecentReplaceStrings();
            toReplace = history.length > 0 ? history[history.length - 1] : "";
        }
        myReplaceArea.getTextArea().setValue(toReplace);

        updateControls();
        updateScopeDetailsPanel();

        boolean isReplaceState = myHelper.isReplaceState();
        LocalizeValue title = LocalizeValue.of(myHelper.getTitle());
        myTitleLabel.updatePresentation(presentation -> {
            presentation.clearText();
            presentation.append(title, TextAttribute.REGULAR_BOLD);
        });

        Window window = myWindow;
        if (window != null) {
            window.setTitle(myHelper.getTitle());
        }

        myReplaceRow.setVisible(isReplaceState);
        myOKHintLabel.setText(LocalizeValue.localizeTODO(KeymapUtil.getKeystrokeText(ENTER_WITH_MODIFIERS)));
        myOKButton.setText(FindLocalize.findPopupFindButton());
        myReplaceAllButton.setVisible(isReplaceState);
        myReplaceSelectedButton.setVisible(isReplaceState);
    }

    @RequiredUIAccess
    private void updateControls() {
        boolean replaceState = myHelper.isReplaceState();
        myReplaceAllButton.setVisible(replaceState);
        myReplaceSelectedButton.setVisible(replaceState);

        boolean multiline = getStringToFind().contains("\n");
        myNavigationHintLabel.setVisible(multiline);
        mySearchArea.updateExtraActions();
        myReplaceArea.updateExtraActions();

        if (multiline) {
            LocalizeValue hint = LocalizeValue.empty();
            String findNextText = KeymapUtil.getFirstKeyboardShortcutText("FindNext");
            String findPreviousText = KeymapUtil.getFirstKeyboardShortcutText("FindPrevious");
            if (!StringUtil.isEmpty(findNextText) && !StringUtil.isEmpty(findPreviousText)) {
                hint = FindLocalize.labelUse0And1ToSelectUsages(findNextText, findPreviousText);
            }
            myNavigationHintLabel.setText(hint);
        }
    }

    @RequiredUIAccess
    private void updateScopeDetailsPanel() {
        Component component = myScopeUI.getComponents().get(mySelectedScope);

        Component previous = myScopeDetailsComponent;
        if (previous != component) {
            if (previous != null) {
                myScopeDetailsLayout.remove(previous);
            }

            if (component != null) {
                myScopeDetailsLayout.left(component);
            }
            myScopeDetailsComponent = component;
        }

        if (component instanceof HasFocus hasFocus && hasFocus.isFocusable() && myWindow != null) {
            hasFocus.focus();
        }
    }

    @RequiredUIAccess
    public void scheduleResultsUpdate() {
        if (myWindow == null || Disposer.isDisposed(myDisposable)) {
            return;
        }

        updateControls();

        cancelScheduledSearch();
        myScheduledSearch = myUIAccess.getScheduler().schedule(
            this::findSettingsChanged,
            ModalityState.any(),
            100,
            TimeUnit.MILLISECONDS
        );
    }

    private void schedulePreviewUpdate() {
        cancelScheduledPreview();
        myScheduledPreview = myUIAccess.getScheduler().schedule(
            () -> {
                myScheduledPreview = null;
                if (myWindow != null && !Disposer.isDisposed(myDisposable)) {
                    updatePreview();
                }
            },
            ModalityState.any(),
            150,
            TimeUnit.MILLISECONDS
        );
    }

    private void cancelScheduledPreview() {
        ScheduledFuture<?> scheduledPreview = myScheduledPreview;
        myScheduledPreview = null;
        if (scheduledPreview != null) {
            scheduledPreview.cancel(false);
        }
    }

    private void cancelScheduledSearch() {
        ScheduledFuture<?> scheduledSearch = myScheduledSearch;
        myScheduledSearch = null;
        if (scheduledSearch != null) {
            scheduledSearch.cancel(false);
        }
    }

    private void finishPreviousPreviewSearch() {
        ProgressIndicatorBase progress = myResultsPreviewSearchProgress;
        if (progress != null && !progress.isCanceled()) {
            progress.cancel();
        }
    }

    @RequiredUIAccess
    private void findSettingsChanged() {
        if (myWindow == null || Disposer.isDisposed(myDisposable)) {
            return;
        }

        Application application = Application.get();
        ModalityState modalityState = application.getCurrentModalityState();

        finishPreviousPreviewSearch();
        cancelScheduledPreview();
        myFilePresentations.clear();
        applyTo(myHelper.getModel());

        FindModel findModel = new FindModel();
        findModel.copyFrom(myHelper.getModel());
        if (findModel.getStringToFind().contains("\n") && Registry.is("ide.find.ignores.leading.whitespace.in.multiline.search")) {
            findModel.setMultiline(true);
        }

        Validation validation = getValidationInfo(myHelper.getModel());

        AtomicInteger resultsCount = new AtomicInteger();
        AtomicInteger resultsFilesCount = new AtomicInteger();

        ProgressIndicatorBase progressIndicatorWhenSearchStarted = new ProgressIndicatorBase() {
            @Override
            public void stop() {
                super.stop();

                int stoppedHash = System.identityHashCode(this);
                onStop(stoppedHash, LocalizeValue.empty());
                myUIAccess.give(() -> {
                    flushResults(stoppedHash, resultsCount.get(), resultsFilesCount.get());
                    if (myNeedReset.compareAndSet(true, false)) {
                        reset();
                    }
                });
            }
        };
        myResultsPreviewSearchProgress = progressIndicatorWhenSearchStarted;
        int hash = System.identityHashCode(progressIndicatorWhenSearchStarted);

        Set<VirtualFile> filesToScanInitially = new LinkedHashSet<>();
        if (myHelper.myPreviousModel != null
            && myHelper.myPreviousModel.getStringToFind().length() < myHelper.getModel().getStringToFind().length()) {
            for (UsageInfoAdapter usage : myResults) {
                if (usage instanceof UsageInfo2UsageAdapter usageAdapter) {
                    VirtualFile file = usageAdapter.getFile();
                    if (file != null) {
                        filesToScanInitially.add(file);
                    }
                }
            }
        }

        myHelper.myPreviousModel = myHelper.getModel().clone();

        myPendingResults.clear();

        myReplaceAllButton.setEnabled(false);
        myReplaceSelectedButton.setEnabled(false);
        myReplaceSelectedButton.setText(FindLocalize.findPopupReplaceSelectedButton(0));

        onStart(hash);
        if (validation != null && validation.component() != myReplaceArea.getTextArea()) {
            onStop(hash, validation.message());
            reset();
            return;
        }

        FindInProjectExecutor projectExecutor = FindInProjectExecutor.getInstance();
        GlobalSearchScope scope =
            GlobalSearchScopeUtil.toGlobalSearchScope(FindInProjectUtil.getScopeFromModel(myProject, myHelper.myPreviousModel), myProject);

        FindInProjectUtil.setupViewPresentation(myUsageViewPresentation, findModel);

        ProgressIndicatorUtils.scheduleWithWriteActionPriority(
            progressIndicatorWhenSearchStarted,
            new ReadTask() {
                @Override
                @RequiredReadAction
                public Continuation performInReadAction(ProgressIndicator indicator) {
                    FindUsagesProcessPresentation processPresentation =
                        FindInProjectUtil.setupProcessPresentation(myProject, myUsageViewPresentation);
                    ThreadLocal<String> lastUsageFileRef = new ThreadLocal<>();
                    ThreadLocal<Reference<Usage>> recentUsageRef = new ThreadLocal<>();
                    Map<Thread, UsageInfoAdapter> mergedUsages = new ConcurrentHashMap<>();

                    projectExecutor.findUsages(
                        myProject,
                        progressIndicatorWhenSearchStarted,
                        processPresentation,
                        findModel,
                        filesToScanInitially,
                        usage -> {
                            if (isCancelled()) {
                                onStop(hash, LocalizeValue.empty());
                                return false;
                            }

                            String file = lastUsageFileRef.get();
                            String usageFile = PathUtil.toSystemIndependentName(usage.getPath());
                            if (file == null || !file.equals(usageFile)) {
                                resultsFilesCount.incrementAndGet();
                                lastUsageFileRef.set(usageFile);
                            }

                            Usage recent = SoftReference.dereference(recentUsageRef.get());
                            UsageInfoAdapter recentAdapter = recent instanceof UsageInfoAdapter usageInfoAdapter ? usageInfoAdapter : null;
                            boolean merged = !myHelper.isReplaceState() && recentAdapter != null && recentAdapter.merge(usage);
                            if (merged) {
                                mergedUsages.put(Thread.currentThread(), recentAdapter);
                            }
                            else {
                                UsageInfoAdapter completedUsage = mergedUsages.remove(Thread.currentThread());
                                if (completedUsage != null) {
                                    preparePresentation(completedUsage);
                                }
                                if (usage instanceof UsageInfoAdapter usageInfoAdapter) {
                                    preparePresentation(usageInfoAdapter);
                                }
                                recentUsageRef.set(new WeakReference<>(usage));
                                myPendingResults.add(usage);
                            }

                            int occurrences = resultsCount.incrementAndGet();
                            scheduleResultsFlush(hash, resultsCount, resultsFilesCount);

                            boolean continueSearch = occurrences < ShowUsagesAction.getUsagesPageSize();
                            if (!continueSearch) {
                                onStop(hash, LocalizeValue.empty());
                            }
                            return continueSearch;
                        }
                    );

                    for (UsageInfoAdapter completedUsage : mergedUsages.values()) {
                        preparePresentation(completedUsage);
                    }
                    mergedUsages.clear();

                    return new Continuation(
                        () -> myUIAccess.give(() -> {
                            if (!isCancelled() && resultsCount.get() == 0) {
                                myResultsList.setPlaceholder(UILocalize.messageNothingtoshow());
                            }
                            onStop(hash, LocalizeValue.empty());
                        }),
                        modalityState
                    );
                }

                boolean isCancelled() {
                    return progressIndicatorWhenSearchStarted != myResultsPreviewSearchProgress || progressIndicatorWhenSearchStarted.isCanceled();
                }

                @Override
                public void onCanceled(ProgressIndicator indicator) {
                    if (myWindow != null && progressIndicatorWhenSearchStarted == myResultsPreviewSearchProgress) {
                        myUIAccess.give(() -> scheduleResultsUpdate());
                    }
                }
            }
        );
    }

    private void scheduleResultsFlush(int hash, AtomicInteger resultsCount, AtomicInteger resultsFilesCount) {
        if (!myFlushScheduled.compareAndSet(false, true)) {
            return;
        }

        myUIAccess.getScheduler().schedule(() -> {
            myFlushScheduled.set(false);
            flushResults(hash, resultsCount.get(), resultsFilesCount.get());
        }, ModalityState.any(), 50, TimeUnit.MILLISECONDS);
    }

    @RequiredUIAccess
    private void flushResults(int hash, int occurrences, int filesWithOccurrences) {
        if (hash != System.identityHashCode(myResultsPreviewSearchProgress) || Disposer.isDisposed(myDisposable)) {
            myPendingResults.clear();
            return;
        }

        if (myNeedReset.compareAndSet(true, false)) {
            myResults.clear();
            myFirstResultPath = null;
        }

        UsageInfoAdapter usage;
        while ((usage = myPendingResults.poll()) != null) {
            insertSorted(usage);
        }

        UsageInfoAdapter selected = myResultsList.getValue();
        myApplyingResults = true;
        try {
            myResultsModel.replaceAll(new ArrayList<>(myResults));

            if (selected != null && myResults.contains(selected)) {
                myResultsList.setValue(selected, false);
            }
            else if (!myResults.isEmpty()) {
                myResultsList.setValue(myResults.get(0), false);
            }
        }
        finally {
            myApplyingResults = false;
        }

        if (myResultsList.getValue() != selected || selected == null) {
            schedulePreviewUpdate();
        }

        myReplaceAllButton.setEnabled(occurrences > 0);
        myReplaceSelectedButton.setEnabled(occurrences > 0);

        StringBuilder builder = new StringBuilder();
        if (occurrences > 0) {
            builder.append(Math.min(ShowUsagesAction.getUsagesPageSize(), occurrences));
            boolean foundAllUsages = occurrences < ShowUsagesAction.getUsagesPageSize();
            myUsagesCount = String.valueOf(occurrences);
            if (!foundAllUsages) {
                builder.append("+");
                myUsagesCount += "+";
            }
            builder.append(UILocalize.messageMatches(occurrences).get());
            builder.append(" in ");
            builder.append(filesWithOccurrences);
            myFilesCount = String.valueOf(filesWithOccurrences);
            if (!foundAllUsages) {
                builder.append("+");
                myFilesCount += "+";
            }
            builder.append(UILocalize.messageFiles(filesWithOccurrences).get());
        }
        myInfoLabel.setText(LocalizeValue.of(builder.toString()));
    }

    private void insertSorted(UsageInfoAdapter usage) {
        if (myResults.isEmpty()) {
            myResults.add(usage);
            myFirstResultPath = usage.getPath();
            return;
        }

        int position = Collections.binarySearch(myResults, usage, this::compareResults);
        myResults.add(position < 0 ? -(position + 1) : position, usage);
    }

    private int compareResults(UsageInfoAdapter u1, UsageInfoAdapter u2) {
        String u1Path = u1.getPath();
        String u2Path = u2.getPath();
        if (u1Path.equals(myFirstResultPath) && !u2Path.equals(myFirstResultPath)) {
            return -1;
        }
        if (!u1Path.equals(myFirstResultPath) && u2Path.equals(myFirstResultPath)) {
            return 1;
        }

        int c = u1Path.compareTo(u2Path);
        if (c != 0) {
            return c;
        }

        c = Integer.compare(u1.getLine(), u2.getLine());
        if (c != 0) {
            return c;
        }
        return Integer.compare(u1.getNavigationOffset(), u2.getNavigationOffset());
    }

    @RequiredUIAccess
    private void reset() {
        myResults.clear();
        myPendingResults.clear();
        myFirstResultPath = null;
        myResultsModel.removeAll();
        myInfoLabel.setText(LocalizeValue.empty());
    }

    @RequiredUIAccess
    private void onStart(int hash) {
        myNeedReset.set(true);
        myLoadingHash = hash;
        myLoadingImage.setVisible(true);
        myResultsList.setPlaceholder(FindLocalize.emptyTextSearching());
    }

    private void onStop(int hash, LocalizeValue message) {
        if (hash != myLoadingHash) {
            return;
        }

        myUIAccess.giveIfNeed(() -> {
            myResultsList.setPlaceholder(
                message.isNotEmpty() ? UILocalize.messageNothingtoshowWithProblem(message) : UILocalize.messageNothingtoshow()
            );
            myLoadingImage.setVisible(false);
        });
    }

    @Override
    public @Nullable String getFileTypeMask() {
        if (myCbFileFilter != null && Boolean.TRUE.equals(myCbFileFilter.getValue())) {
            return myFileMaskField.getValue();
        }
        return null;
    }

    private @Nullable Validation getValidationInfo(FindModel model) {
        Validation scopeValidation = myScopeUI.validate(model, mySelectedScope);
        if (scopeValidation != null) {
            return scopeValidation;
        }

        TextArea searchField = mySearchArea.getTextArea();
        if (!myHelper.canSearchThisString()) {
            return new Validation(FindLocalize.findEmptySearchTextError(), searchField);
        }

        if (model.isRegularExpressions()) {
            String toFind = model.getStringToFind();
            Pattern pattern;
            try {
                pattern = Pattern.compile(toFind, model.isCaseSensitive() ? Pattern.MULTILINE : Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
                if (pattern.matcher("").matches() && !toFind.endsWith("$") && !toFind.startsWith("^")) {
                    return new Validation(FindLocalize.findEmptyMatchRegularExpressionError(), searchField);
                }
            }
            catch (PatternSyntaxException e) {
                return new Validation(FindLocalize.findInvalidRegularExpressionError(toFind, e.getDescription()), searchField);
            }

            if (model.isReplaceState()) {
                TextArea replaceField = myReplaceArea.getTextArea();
                if (!myResults.isEmpty()) {
                    try {
                        ReplaceInProjectManager.getInstance(myProject).replaceUsage(myResults.get(0), model, Collections.emptySet(), true);
                    }
                    catch (FindManager.MalformedReplacementStringException e) {
                        return new Validation(LocalizeValue.localizeTODO(e.getMessage()), replaceField);
                    }
                }

                try {
                    RegExReplacementBuilder.validate(pattern, getStringToReplace());
                }
                catch (IllegalArgumentException e) {
                    return new Validation(FindLocalize.findReplaceInvalidReplacementString(e.getMessage()), replaceField);
                }
            }
        }

        String mask = getFileTypeMask();
        if (mask != null) {
            if (mask.isEmpty()) {
                return new Validation(FindLocalize.findFilterEmptyFileMaskError(), myFileMaskField);
            }

            if (mask.contains(";")) {
                return new Validation(FindLocalize.messageFileMasksShouldBeCommaSeparated(), myFileMaskField);
            }

            try {
                createFileMaskRegExp(mask);
            }
            catch (PatternSyntaxException ex) {
                return new Validation(FindLocalize.findFilterInvalidFileMaskError(mask), myFileMaskField);
            }
        }
        return null;
    }

    private static void createFileMaskRegExp(String filter) throws PatternSyntaxException {
        String pattern;
        List<String> strings = StringUtil.split(filter, ",");
        if (strings.size() == 1) {
            pattern = PatternUtil.convertToRegex(filter.trim());
        }
        else {
            pattern = StringUtil.join(strings, s -> "(" + PatternUtil.convertToRegex(s.trim()) + ")", "|");
        }
        Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    }

    @Override
    public String getStringToFind() {
        return StringUtil.notNullize(mySearchArea.getTextArea().getValue());
    }

    private String getStringToReplace() {
        return StringUtil.notNullize(myReplaceArea.getTextArea().getValue());
    }

    private void applyTo(FindModel model) {
        model.setCaseSensitive(myCaseSensitiveState.get());
        if (model.isReplaceState()) {
            model.setPreserveCase(myPreserveCaseState.get());
        }
        model.setWholeWordsOnly(myWholeWordsState.get());

        model.setSearchContext(mySelectedContext);
        model.setRegularExpressions(myRegexState.get());
        model.setStringToFind(getStringToFind());

        if (model.isReplaceState()) {
            model.setStringToReplace(StringUtil.convertLineSeparators(getStringToReplace()));
        }

        model.setProjectScope(false);
        model.setDirectoryName(null);
        model.setModuleName(null);
        model.setCustomScopeName(null);
        model.setCustomScope(null);
        model.setCustomScope(false);
        myScopeUI.applyTo(model, mySelectedScope);

        model.setFindAll(false);

        model.setFileFilter(getFileTypeMask());
    }

    @RequiredUIAccess
    private void navigateToSelectedUsage(@Nullable AnActionEvent e) {
        Navigatable[] navigatables = e != null ? e.getData(Navigatable.KEY_OF_ARRAY) : null;
        if (navigatables != null) {
            if (canBeClosed()) {
                closeImmediately();
            }
            for (Navigatable navigatable : navigatables) {
                navigatable.navigate(true);
            }
            return;
        }

        UsageInfoAdapter usage = myResultsList.getValue();
        if (usage != null) {
            if (canBeClosed()) {
                closeImmediately();
            }
            usage.navigate(true);
        }
    }

    private class MySwitchContextToggleAction extends ToggleAction implements DumbAware {
        private final FindSearchContext myContext;

        MySwitchContextToggleAction(FindSearchContext context) {
            super(context.getName());
            myContext = context;
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return mySelectedContext == myContext;
        }

        @Override
        @RequiredUIAccess
        public void setSelected(AnActionEvent e, boolean state) {
            if (state) {
                mySelectedContext = myContext;
                scheduleResultsUpdate();
            }
        }
    }

    private class MySelectScopeToggleAction extends DumbAwareToggleAction {
        private final UnifiedFindPopupScopeUI.ScopeType myScope;

        MySelectScopeToggleAction(UnifiedFindPopupScopeUI.ScopeType scope) {
            super(scope.text, LocalizeValue.empty(), null);
            myScope = scope;
        }

        @Override
        public boolean displayTextInToolbar() {
            return true;
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return mySelectedScope == myScope;
        }

        @Override
        @RequiredUIAccess
        public void setSelected(AnActionEvent e, boolean state) {
            if (state) {
                mySelectedScope = myScope;
                myScopeSelectionToolbar.updateActionsAsync();
                updateScopeDetailsPanel();
                scheduleResultsUpdate();
            }
        }
    }

    private class MyShowFilterPopupAction extends DefaultActionGroup implements DumbAware, AnActionWithSyncUpdate {
        private final Image myPrimaryImage;

        MyShowFilterPopupAction() {
            super(FindLocalize.findPopupShowFilterPopup(), true);
            myPrimaryImage = PlatformIconGroup.generalFilter();
            getTemplatePresentation().setIcon(myPrimaryImage);

            KeyboardShortcut keyboardShortcut = ActionManager.getInstance().getKeyboardShortcut("ShowFilterPopup");
            if (keyboardShortcut != null) {
                setShortcutSet(new CustomShortcutSet(keyboardShortcut));
            }

            add(new MySwitchContextToggleAction(FindSearchContext.ANY));
            add(new MySwitchContextToggleAction(FindSearchContext.IN_COMMENTS));
            add(new MySwitchContextToggleAction(FindSearchContext.IN_STRING_LITERALS));
            add(new MySwitchContextToggleAction(FindSearchContext.EXCEPT_COMMENTS));
            add(new MySwitchContextToggleAction(FindSearchContext.EXCEPT_STRING_LITERALS));
            add(new MySwitchContextToggleAction(FindSearchContext.EXCEPT_COMMENTS_AND_STRING_LITERALS));
        }

        @Override
        public boolean showBelowArrow() {
            return false;
        }

        @Override
        public void update(AnActionEvent e) {
            if (!FindSearchContext.ANY.equals(mySelectedContext)) {
                e.getPresentation().setIcon(ImageEffects.layered(myPrimaryImage, PlatformIconGroup.greenbadge()));
            }
            else {
                e.getPresentation().setIcon(myPrimaryImage);
            }
        }
    }

    private class MyPinAction extends ToggleAction implements DumbAware {
        private MyPinAction() {
            super(IdeLocalize.actionToggleactionPinWindowText(), IdeLocalize.actionToggleactionPinWindowDescription(), PlatformIconGroup.generalPin_tab());
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return UISettings.getInstance().getPinFindInPath();
        }

        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            myIsPinned.set(state);
            UISettings.getInstance().setPinFindInPath(state);
        }
    }

    private class MyEnterAction extends DumbAwareAction {
        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            if (myHelper.isReplaceState()) {
                doReplaceSelected();
            }
            else {
                navigateToSelectedUsage(null);
            }
        }
    }
}

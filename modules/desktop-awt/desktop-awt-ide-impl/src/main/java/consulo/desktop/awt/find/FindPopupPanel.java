// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.find;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.HelpManager;
import consulo.application.dumb.DumbAware;
import consulo.application.impl.internal.progress.ProgressIndicatorBase;
import consulo.application.impl.internal.progress.ProgressIndicatorUtils;
import consulo.application.impl.internal.progress.ReadTask;
import consulo.application.progress.ProgressIndicator;
import consulo.application.ui.DimensionService;
import consulo.application.ui.UISettings;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.application.util.UserHomeFileUtil;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.event.DocumentListener;
import consulo.fileEditor.UniqueVFilePathBuilder;
import consulo.fileEditor.VfsPresentationUtil;
import consulo.find.*;
import consulo.find.localize.FindLocalize;
import consulo.ide.impl.idea.find.actions.ShowUsagesAction;
import consulo.ide.impl.idea.find.impl.*;
import consulo.ide.impl.idea.find.replaceInProject.ReplaceInProjectManager;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.openapi.ui.ComponentValidator;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.openapi.wm.impl.IdeGlassPaneImpl;
import consulo.ide.impl.idea.reference.SoftReference;
import consulo.ide.impl.idea.ui.ListFocusTraversalPolicy;
import consulo.ide.impl.idea.ui.PopupBorder;
import consulo.ide.impl.idea.ui.WindowMoveListener;
import consulo.ide.impl.idea.ui.WindowResizeListener;
import consulo.ide.impl.idea.ui.mac.TouchbarDataKeys;
import consulo.ide.impl.idea.usages.impl.UsagePreviewPanel;
import consulo.ide.impl.idea.util.PathUtil;
import consulo.ide.impl.idea.util.Producer;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.scratch.ScratchUtil;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScopeUtil;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.event.ProjectManagerListener;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.Button;
import consulo.ui.Label;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.ex.*;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.table.JBTable;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.style.ComponentColors;
import consulo.ui.util.TextWithMnemonic;
import consulo.undoRedo.CommandProcessor;
import consulo.usage.*;
import consulo.usage.localize.UsageLocalize;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.JBIterable;
import consulo.util.collection.SmartList;
import consulo.util.dataholder.Key;
import consulo.util.lang.*;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.Contract;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Window;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static consulo.ui.ex.SimpleTextAttributes.STYLE_PLAIN;
import static consulo.ui.ex.awt.FontUtil.spaceAndThinSpace;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;

public class FindPopupPanel extends JBPanel<FindPopupPanel> implements FindUI {
    private static final KeyStroke ENTER = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
    private static final KeyStroke ENTER_WITH_MODIFIERS =
        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Platform.current().os().isMac() ? InputEvent.META_DOWN_MASK : CTRL_DOWN_MASK);
    private static final KeyStroke REPLACE_ALL =
        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_MASK);

    private static final String FIND_TYPE = "FindInPath";
    private static final String SERVICE_KEY = "find.popup";
    private static final String SPLITTER_SERVICE_KEY = "find.popup.splitter";
    private static final Key<Boolean> DONT_REQUEST_FOCUS = Key.create("dontRequestFocus");
    @Nonnull
    private final FindUIHelper myHelper;
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final Disposable myDisposable;
    private final Alarm myPreviewUpdater;
    @Nonnull
    private final FindPopupScopeUI myScopeUI;
    private JComponent myCodePreviewComponent;
    private SearchTextArea mySearchTextArea;
    private SearchTextArea myReplaceTextArea;
    private final AtomicBoolean myCanClose = new AtomicBoolean(true);
    private final AtomicBoolean myIsPinned = new AtomicBoolean(false);
    private Label myOKHintLabel;
    private Label myNavigationHintLabel;
    private Alarm mySearchRescheduleOnCancellationsAlarm;
    private volatile ProgressIndicatorBase myResultsPreviewSearchProgress;

    private JLabel myTitleLabel;
    private JLabel myInfoLabel;
    private final AtomicBoolean myCaseSensitiveState = new AtomicBoolean();
    private final AtomicBoolean myPreserveCaseState = new AtomicBoolean();
    private final AtomicBoolean myWholeWordsState = new AtomicBoolean();
    private final AtomicBoolean myRegexState = new AtomicBoolean();
    private final List<AnAction> myExtraActions = new ArrayList<>();
    private StateRestoringCheckBoxWrapper myCbFileFilter;
    private ActionToolbar myScopeSelectionToolbar;
    private ComboBox<String> myFileMaskField;

    private Button myOKButton;
    private Button myReplaceAllButton;
    private Button myReplaceSelectedButton;
    private JTextArea mySearchComponent;
    private JTextArea myReplaceComponent;
    private FindSearchContext mySelectedContext = FindSearchContext.ANY;
    private FindPopupScopeUI.ScopeType mySelectedScope;
    private JPanel myScopeDetailsPanel;

    private JBTable myResultsPreviewTable;
    private DefaultTableModel myResultsPreviewTableModel;
    private SimpleColoredComponent myUsagePreviewTitle;
    private UsagePreviewPanel myUsagePreviewPanel;
    private DialogWrapper myDialog;
    private LoadingDecorator myLoadingDecorator;
    private int myLoadingHash;
    private final AtomicBoolean myNeedReset = new AtomicBoolean(true);
    private JPanel myTitlePanel;
    private String myUsagesCount;
    private String myFilesCount;
    private UsageViewPresentation myUsageViewPresentation;
    private final ComponentValidator myComponentValidator;

    @RequiredUIAccess
    FindPopupPanel(@Nonnull FindUIHelper helper) {
        myHelper = helper;
        myProject = myHelper.getProject();
        myDisposable = Disposable.newDisposable();
        myPreviewUpdater = new Alarm(myDisposable);
        myScopeUI = new FindPopupScopeUIImpl(this);
        myComponentValidator = new ComponentValidator(myDisposable) {
            @Override
            public void updateInfo(@Nullable ValidationInfo info) {
                if (info != null && info.component == mySearchComponent) {
                    super.updateInfo(null);
                }
                else {
                    super.updateInfo(info);
                }
            }
        };

        Disposer.register(myDisposable, () -> {
            finishPreviousPreviewSearch();
            if (mySearchRescheduleOnCancellationsAlarm != null) {
                Disposer.dispose(mySearchRescheduleOnCancellationsAlarm);
            }
            if (myUsagePreviewPanel != null) {
                Disposer.dispose(myUsagePreviewPanel);
            }
        });

        initComponents();
        initByModel();

        //FindUtil.triggerUsedOptionsStats(FIND_TYPE, myHelper.getModel());
    }

    @RequiredUIAccess
    @Override
    public void showUI() {
        if (myDialog != null && myDialog.isVisible()) {
            return;
        }
        if (myDialog != null && !Disposer.isDisposed(myDialog.getDisposable())) {
            myDialog.doCancelAction();
        }
        if (myDialog == null || Disposer.isDisposed(myDialog.getDisposable())) {
            myDialog = new DialogWrapper(myHelper.getProject(), null, true, DialogWrapper.IdeModalityType.MODELESS, false) {
                {
                    init();
                    getRootPane().setDefaultButton(null);
                }

                @Override
                protected void doOKAction() {
                    processCtrlEnter();
                }

                @Override
                protected void dispose() {
                    saveSettings();
                    super.dispose();
                }

                @Nullable
                @Override
                protected Border createContentPaneBorder() {
                    return null;
                }

                @Override
                protected JComponent createCenterPanel() {
                    return FindPopupPanel.this;
                }

                @Override
                protected String getDimensionServiceKey() {
                    return SERVICE_KEY;
                }
            };
            myDialog.setUndecorated(!Registry.is("ide.find.as.popup.decorated"));
            Application application = Application.get();
            application.getMessageBus()
                .connect(myDialog.getDisposable())
                .subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
                    @Override
                    public void projectClosed(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
                        closeImmediately();
                    }
                });
            Disposer.register(myDialog.getDisposable(), myDisposable);

            final Window window = TargetAWT.to(WindowManager.getInstance().suggestParentWindow(myProject));
            Component parent = UIUtil.findUltimateParent(window);
            RelativePoint showPoint = null;
            Coordinate2D location = DimensionService.getInstance().getLocation(SERVICE_KEY);
            Point screenPoint = location == null ? null : new Point(location.getX(), location.getY());
            if (screenPoint != null) {
                if (parent != null) {
                    SwingUtilities.convertPointFromScreen(screenPoint, parent);
                    showPoint = new RelativePoint(parent, screenPoint);
                }
                else {
                    showPoint = new RelativePoint(screenPoint);
                }
            }
            if (parent != null && showPoint == null) {
                int height = UISettings.getInstance().getShowNavigationBar() ? 135 : 115;
                if (parent instanceof Window parentWindow) {
                    consulo.ui.Window uiWindow = TargetAWT.from(parentWindow);
                    IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
                    if (ideFrame instanceof IdeFrameEx && ideFrame.isInFullScreen()) {
                        height -= 20;
                    }
                }
                showPoint = new RelativePoint(parent, new Point((parent.getSize().width - getPreferredSize().width) / 2, height));
            }
            application.invokeLater(() -> {
                if (mySearchComponent.getCaret() != null) {
                    mySearchComponent.selectAll();
                }
            });
            WindowMoveListener windowListener = new WindowMoveListener(this);
            myTitlePanel.addMouseListener(windowListener);
            myTitlePanel.addMouseMotionListener(windowListener);
            addMouseListener(windowListener);
            addMouseMotionListener(windowListener);
            Dimension panelSize = getPreferredSize();
            Size size = DimensionService.getInstance().getSize(SERVICE_KEY);
            Dimension prev = size == null ? null : new Dimension(size.getWidth(), size.getHeight());
            panelSize.width += JBUIScale.scale(24);//hidden 'loading' icon
            panelSize.height *= 2;
            if (prev != null && prev.height < panelSize.height) {
                prev.height = panelSize.height;
            }
            Window w = myDialog.getPeer().getWindow();
            final AnAction escape = ActionManager.getInstance().getAction("EditorEscape");
            JRootPane root = ((RootPaneContainer) w).getRootPane();

            IdeGlassPaneImpl glass = (IdeGlassPaneImpl) myDialog.getRootPane().getGlassPane();
            int i = Registry.intValue("ide.popup.resizable.border.sensitivity", 4);
            WindowResizeListener resizeListener = new WindowResizeListener(root, JBUI.insets(i), null) {
                private Cursor myCursor;

                @Override
                protected void setCursor(Component content, Cursor cursor) {
                    if (myCursor != cursor || myCursor != Cursor.getDefaultCursor()) {
                        glass.setCursor(cursor, this);
                        myCursor = cursor;

                        if (content instanceof JComponent component) {
                            IdeGlassPaneImpl.savePreProcessedCursor(component, content.getCursor());
                        }
                        super.setCursor(content, cursor);
                    }
                }
            };
            glass.addMousePreprocessor(resizeListener, myDisposable);
            glass.addMouseMotionPreprocessor(resizeListener, myDisposable);

            DumbAwareAction.create(e -> closeImmediately())
                .registerCustomShortcutSet(escape == null ? CommonShortcuts.ESCAPE : escape.getShortcutSet(), root, myDisposable);
            root.setWindowDecorationStyle(JRootPane.NONE);
            root.setBorder(PopupBorder.Factory.create(true, true));
            UIUtil.markAsPossibleOwner((Dialog) w);
            w.setBackground(UIUtil.getPanelBackground());
            w.setMinimumSize(panelSize);
            if (prev == null) {
                panelSize.height *= 1.5;
                panelSize.width *= 1.15;
            }
            w.setSize(prev != null ? prev : panelSize);

            IdeEventQueue.getInstance().getPopupManager().closeAllPopups(false);
            if (showPoint != null) {
                myDialog.setLocation(showPoint.getScreenPoint());
            }
            else {
                w.setLocationRelativeTo(parent);
            }

            w.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    // schedule first search session
                    scheduleResultsUpdate();

                    w.addWindowFocusListener(new WindowAdapter() {
                        @Override
                        public void windowLostFocus(WindowEvent e) {
                            Window oppositeWindow = e.getOppositeWindow();
                            if (oppositeWindow == w || oppositeWindow != null && oppositeWindow.getOwner() == w) {
                                return;
                            }
                            if (canBeClosed() || !myIsPinned.get() && oppositeWindow != null) {
                                //closeImmediately();
                                myDialog.doCancelAction();
                            }
                        }
                    });
                }
            });

            JRootPane rootPane = getRootPane();
            if (rootPane != null) {
                if (myHelper.isReplaceState()) {
                    rootPane.setDefaultButton((JButton) TargetAWT.to(myReplaceSelectedButton));
                }
                rootPane.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(ENTER_WITH_MODIFIERS, "openInFindWindow");
                rootPane.getActionMap().put("openInFindWindow", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        processCtrlEnter();
                    }
                });
            }
        }

        myDialog.showAsync();
    }

    protected boolean canBeClosed() {
        if (myProject.isDisposed()) {
            return true;
        }
        if (!myCanClose.get()) {
            return false;
        }
        if (myIsPinned.get()) {
            return false;
        }
        if (!Application.get().isActive()) {
            return false;
        }
        if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow() == null) {
            return false;
        }
        if (myFileMaskField.isPopupVisible()) {
            myFileMaskField.setPopupVisible(false);
            return false;
        }
        List<JBPopup> popups = ContainerUtil.filter(JBPopupFactory.getInstance().getChildPopups(this), popup -> !popup.isDisposed());
        if (!popups.isEmpty()) {
            for (JBPopup popup : popups) {
                popup.cancel();
            }
            return false;
        }
        return !myScopeUI.hideAllPopups();
    }

    @Override
    public void saveSettings() {
        DimensionService.getInstance().setSize(SERVICE_KEY, TargetAWT.from(myDialog.getSize()), myHelper.getProject());
        DimensionService.getInstance()
            .setLocation(SERVICE_KEY, TargetAWT.from(myDialog.getWindow().getLocationOnScreen()), myHelper.getProject());
        FindSettings findSettings = FindSettings.getInstance();
        myScopeUI.applyTo(findSettings, mySelectedScope);
        myHelper.updateFindSettings();
        applyTo(FindManager.getInstance(myProject).getFindInProjectModel());
    }

    @Nonnull
    @Override
    public Disposable getDisposable() {
        return myDisposable;
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Nonnull
    public Project getProject() {
        return myProject;
    }

    @Nonnull
    public FindUIHelper getHelper() {
        return myHelper;
    }

    @Nonnull
    public AtomicBoolean getCanClose() {
        return myCanClose;
    }

    @RequiredUIAccess
    private void initComponents() {
        myTitleLabel = new JBLabel(FindLocalize.findInPathDialogTitle().get(), UIUtil.ComponentStyle.REGULAR);
        RelativeFont.BOLD.install(myTitleLabel);
        myInfoLabel = new JBLabel("", UIUtil.ComponentStyle.SMALL);
        myLoadingDecorator =
            new LoadingDecorator(new JLabel(EmptyIcon.ICON_16), getDisposable(), 250, true, new AsyncProcessIcon("FindInPathLoading"));
        myLoadingDecorator.setLoadingText("");
        myCbFileFilter = new StateRestoringCheckBoxWrapper(FindLocalize.findPopupFilemask());
        myCbFileFilter.addValueListener(__ -> {
            boolean checked = myCbFileFilter.getValue();
            boolean requestFocus = myCbFileFilter.getComponent().getUserData(DONT_REQUEST_FOCUS) == null;

            myFileMaskField.setEnabled(checked);
            if (requestFocus) {
                IdeFocusManager focusManager = ProjectIdeFocusManager.getInstance(myProject);
                if (checked) {
                    focusManager.requestFocus(myFileMaskField, true);
                    myFileMaskField.getEditor().selectAll();
                }
                else {
                    focusManager.requestFocus(mySearchComponent, true);
                }
            }
        });
        myCbFileFilter.addValueListener(__ -> scheduleResultsUpdate());
        myFileMaskField = new ComboBox<>() {
            @Override
            public Dimension getPreferredSize() {
                int width = 0;
                int buttonWidth = 0;
                Component[] components = getComponents();
                for (Component component : components) {
                    Dimension size = component.getPreferredSize();
                    int w = size != null ? size.width : 0;
                    if (component instanceof JButton) {
                        buttonWidth = w;
                    }
                    width += w;
                }
                ComboBoxEditor editor = getEditor();
                if (editor != null) {
                    Component editorComponent = editor.getEditorComponent();
                    if (editorComponent != null) {
                        FontMetrics fontMetrics = editorComponent.getFontMetrics(editorComponent.getFont());
                        width = Math.max(width, fontMetrics.stringWidth(String.valueOf(getSelectedItem())) + buttonWidth);
                        //Let's reserve some extra space for just one 'the next' letter
                        width += fontMetrics.stringWidth("m");
                    }
                }
                Dimension size = super.getPreferredSize();
                Insets insets = getInsets();
                width += insets.left + insets.right;
                size.width = MathUtil.clamp(width, JBUIScale.scale(80), JBUIScale.scale(500));
                return size;
            }
        };
        myFileMaskField.setEditable(true);
        myFileMaskField.setMaximumRowCount(8);
        myFileMaskField.addActionListener(__ -> scheduleResultsUpdate());
        Component editorComponent = myFileMaskField.getEditor().getEditorComponent();
        if (editorComponent instanceof EditorTextField etf) {
            etf.addDocumentListener(new DocumentListener() {
                @Override
                public void documentChanged(@Nonnull consulo.document.event.DocumentEvent event) {
                    onFileMaskChanged();
                }
            });
        }
        else {
            if (editorComponent instanceof JTextComponent textComponent) {
                textComponent.getDocument().addDocumentListener(new DocumentAdapter() {
                    @Override
                    protected void textChanged(@Nonnull DocumentEvent e) {
                        onFileMaskChanged();
                    }
                });
            }
            else {
                assert false;
            }
        }

        ActionGroup.Builder topActionGroup = ActionGroup.newImmutableBuilder();
        AnAction showFilterPopupAction = new MyShowFilterPopupAction();
        showFilterPopupAction.registerCustomShortcutSet(showFilterPopupAction.getShortcutSet(), this);
        ToggleAction pinAction = new MyPinAction();

        topActionGroup.add(showFilterPopupAction);
        topActionGroup.add(pinAction);

        CheckBox openOnNewTabBox = CheckBox.create(FindLocalize.findOpenInNewTabAction());
        openOnNewTabBox.setValue(myHelper.getModel().isOpenInNewTab());
        openOnNewTabBox.addValueListener(event -> {
            myHelper.getModel().setOpenInNewTab(event.getValue());
        });

        myOKButton = Button.create(FindLocalize.findPopupFindButton());
        myOKButton.addStyle(ButtonStyle.BORDERLESS);

        myReplaceAllButton = Button.create(FindLocalize.findPopupReplaceAllButton());
        myReplaceAllButton.addStyle(ButtonStyle.BORDERLESS);
        myReplaceSelectedButton = Button.create(FindLocalize.findPopupReplaceSelectedButton(0));
        myReplaceSelectedButton.addStyle(ButtonStyle.BORDERLESS);

        myOKButton.addClickListener(event -> doOK(true));
        myReplaceAllButton.addClickListener(__ -> doOK(false));
        myReplaceSelectedButton.addClickListener(e -> doReplaceSelected());

        TouchbarDataKeys.putDialogButtonDescriptor((JComponent) TargetAWT.to(myOKButton), 0, true);

        new MyEnterAction(false).registerCustomShortcutSet(new CustomShortcutSet(ENTER), this);
        DumbAwareAction.create(__ -> processCtrlEnter()).registerCustomShortcutSet(new CustomShortcutSet(ENTER_WITH_MODIFIERS), this);
        DumbAwareAction.create(__ -> doOK(false)).registerCustomShortcutSet(new CustomShortcutSet(REPLACE_ALL), this);

        myReplaceAllButton.setToolTipText(LocalizeValue.localizeTODO(KeymapUtil.getKeystrokeText(REPLACE_ALL)));

        List<Shortcut> navigationKeyStrokes = new ArrayList<>();
        KeyStroke viewSourceKeyStroke = KeymapUtil.getKeyStroke(CommonShortcuts.getViewSource());
        if (viewSourceKeyStroke != null
            && !Comparing.equal(viewSourceKeyStroke, ENTER_WITH_MODIFIERS)
            && !Comparing.equal(viewSourceKeyStroke, ENTER)) {
            navigationKeyStrokes.add(new KeyboardShortcut(viewSourceKeyStroke, null));
        }
        KeyStroke editSourceKeyStroke = KeymapUtil.getKeyStroke(CommonShortcuts.getEditSource());
        if (editSourceKeyStroke != null && !Comparing.equal(editSourceKeyStroke, ENTER_WITH_MODIFIERS)
            && !Comparing.equal(editSourceKeyStroke, ENTER)) {
            navigationKeyStrokes.add(new KeyboardShortcut(editSourceKeyStroke, null));
        }
        if (!navigationKeyStrokes.isEmpty()) {
            DumbAwareAction.create(this::navigateToSelectedUsage)
                .registerCustomShortcutSet(new CustomShortcutSet(navigationKeyStrokes.toArray(Shortcut.EMPTY_ARRAY)), this);
        }

        mySearchComponent = new JBTextArea();
        mySearchComponent.setColumns(25);
        mySearchComponent.setRows(1);
        mySearchComponent.getAccessibleContext().setAccessibleName(FindLocalize.findSearchAccessibleName().get());
        myReplaceComponent = new JBTextArea();
        myReplaceComponent.setColumns(25);
        myReplaceComponent.setRows(1);
        myReplaceComponent.getAccessibleContext().setAccessibleName(FindLocalize.findReplaceAccessibleName().get());
        mySearchTextArea = new SearchTextArea(mySearchComponent, true);
        myReplaceTextArea = new SearchTextArea(myReplaceComponent, false);
        mySearchTextArea.setMultilineEnabled(Registry.is("ide.find.as.popup.allow.multiline"));
        myReplaceTextArea.setMultilineEnabled(Registry.is("ide.find.as.popup.allow.multiline"));
        ToggleAction caseSensitiveAction = createAction(
            FindLocalize.findPopupCaseSensitive(),
            "CaseSensitive",
            AllIcons.Actions.MatchCase,
            AllIcons.Actions.MatchCaseHovered,
            AllIcons.Actions.MatchCaseSelected,
            myCaseSensitiveState,
            () -> !myHelper.getModel().isReplaceState() || !myPreserveCaseState.get()
        );
        ToggleAction wholeWordsAction = createAction(
            FindLocalize.findWholeWords(),
            "WholeWords",
            AllIcons.Actions.Words,
            AllIcons.Actions.WordsHovered,
            AllIcons.Actions.WordsSelected,
            myWholeWordsState,
            () -> !myRegexState.get()
        );
        ToggleAction regexAction = createAction(
            FindLocalize.findRegex(),
            "Regex",
            AllIcons.Actions.Regex,
            AllIcons.Actions.RegexHovered,
            AllIcons.Actions.RegexSelected,
            myRegexState,
            () -> !myHelper.getModel().isReplaceState() || !myPreserveCaseState.get()
        );
        mySearchTextArea.setSuffixActions(List.of(caseSensitiveAction, wholeWordsAction, regexAction));
        ToggleAction preserveCaseAction = createAction(
            FindLocalize.findOptionsReplacePreserveCase(),
            "PreserveCase",
            AllIcons.Actions.PreserveCase,
            AllIcons.Actions.PreserveCaseHover,
            AllIcons.Actions.PreserveCaseSelected,
            myPreserveCaseState,
            () -> !myRegexState.get() && !myCaseSensitiveState.get()
        );
        myReplaceTextArea.setSuffixActions(List.of(preserveCaseAction));
        myExtraActions.addAll(Arrays.asList(caseSensitiveAction, wholeWordsAction, regexAction, preserveCaseAction));
        List<Pair<FindPopupScopeUI.ScopeType, JComponent>> scopeComponents = myScopeUI.getComponents();

        myScopeDetailsPanel = new JPanel(new CardLayout());
        myScopeDetailsPanel.setBorder(JBUI.Borders.customLine(0, 1, 0, 0));

        List<AnAction> scopeActions = new ArrayList<>(scopeComponents.size());
        for (Pair<FindPopupScopeUI.ScopeType, JComponent> scopeComponent : scopeComponents) {
            FindPopupScopeUI.ScopeType scopeType = scopeComponent.first;
            scopeActions.add(new MySelectScopeToggleAction(scopeType));
            myScopeDetailsPanel.add(scopeType.name, scopeComponent.second);
        }
        myScopeSelectionToolbar = createToolbar(scopeActions.toArray(AnAction.EMPTY_ARRAY));
        myScopeSelectionToolbar.setTargetComponent(mySearchComponent);
        mySelectedScope = scopeComponents.get(0).first;

        myResultsPreviewTableModel = createTableModel();
        myResultsPreviewTable = new JBTable(myResultsPreviewTableModel) {
            @Override
            public Dimension getPreferredScrollableViewportSize() {
                return new Dimension(getWidth(), 1 + getRowHeight() * 4);
            }
        };
        myResultsPreviewTable.setFocusable(false);
        myResultsPreviewTable.getEmptyText().setShowAboveCenter(false);
        myResultsPreviewTable.setShowColumns(false);
        myResultsPreviewTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        myResultsPreviewTable.setShowGrid(false);
        myResultsPreviewTable.setIntercellSpacing(JBUI.emptySize());
        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(@Nonnull MouseEvent event) {
                if (event.getSource() != myResultsPreviewTable) {
                    return false;
                }
                navigateToSelectedUsage(null);
                return true;
            }
        }.installOn(myResultsPreviewTable);
        myResultsPreviewTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                myResultsPreviewTable.transferFocus();
            }
        });
        applyFont(JBFont.label(), myResultsPreviewTable);
        JComponent[] tableAware = {mySearchComponent, myReplaceComponent, (JComponent) TargetAWT.to(myReplaceSelectedButton)};
        for (JComponent component : tableAware) {
            ScrollingUtil.installActions(myResultsPreviewTable, false, component);
        }

        ActionListener helpAction = __ -> HelpManager.getInstance().invokeHelp("reference.dialogs.findinpath");
        registerKeyboardAction(helpAction, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        registerKeyboardAction(helpAction, KeyStroke.getKeyStroke(KeyEvent.VK_HELP, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        KeymapManager keymapManager = KeymapManager.getInstance();
        Keymap activeKeymap = keymapManager.getActiveKeymap();
        if (activeKeymap != null) {
            ShortcutSet findNextShortcutSet = new CustomShortcutSet(activeKeymap.getShortcuts("FindNext"));
            ShortcutSet findPreviousShortcutSet = new CustomShortcutSet(activeKeymap.getShortcuts("FindPrevious"));
            DumbAwareAction findNextAction = DumbAwareAction.create(event -> {
                int selectedRow = myResultsPreviewTable.getSelectedRow();
                if (selectedRow >= 0 && selectedRow < myResultsPreviewTable.getRowCount() - 1) {
                    myResultsPreviewTable.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
                    ScrollingUtil.ensureIndexIsVisible(myResultsPreviewTable, selectedRow + 1, 1);
                }
            });
            DumbAwareAction findPreviousAction = DumbAwareAction.create(event -> {
                int selectedRow = myResultsPreviewTable.getSelectedRow();
                if (selectedRow > 0 && selectedRow <= myResultsPreviewTable.getRowCount() - 1) {
                    myResultsPreviewTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
                    ScrollingUtil.ensureIndexIsVisible(myResultsPreviewTable, selectedRow - 1, 1);
                }
            });
            for (JComponent component : tableAware) {
                findNextAction.registerCustomShortcutSet(findNextShortcutSet, component);
                findPreviousAction.registerCustomShortcutSet(findPreviousShortcutSet, component);
            }
        }
        myUsagePreviewTitle = new SimpleColoredComponent();
        myUsagePreviewTitle.setBorder(JBUI.Borders.empty(3, 8, 4, 8));
        myUsageViewPresentation = new UsageViewPresentation();
        myUsagePreviewPanel = new UsagePreviewPanel(myProject, myUsageViewPresentation, Registry.is("ide.find.as.popup.editable.code")) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(myResultsPreviewTable.getWidth(), Math.max(getHeight(), getLineHeight() * 15));
            }
        };
        Disposer.register(myDisposable, myUsagePreviewPanel);
        final Runnable updatePreviewRunnable = () -> {
            if (Disposer.isDisposed(myDisposable)) {
                return;
            }
            int[] selectedRows = myResultsPreviewTable.getSelectedRows();
            final List<UsageInfo> selection = new SmartList<>();
            String file = null;
            for (int row : selectedRows) {
                Object value = myResultsPreviewTable.getModel().getValueAt(row, 0);
                UsageInfoAdapter adapter = (UsageInfoAdapter) value;
                file = adapter.getPath();
                if (adapter.isValid()) {
                    selection.addAll(Arrays.asList(adapter.getMergedInfos()));
                }
            }
            myReplaceSelectedButton.setText(FindLocalize.findPopupReplaceSelectedButton(selection.size()));
            FindInProjectUtil.setupViewPresentation(myUsageViewPresentation, myHelper.getModel().clone());
            myUsagePreviewPanel.updateLayout(selection);
            myUsagePreviewTitle.clear();
            if (myUsagePreviewPanel.getCannotPreviewMessage(selection) == null && file != null) {
                myUsagePreviewTitle.append(PathUtil.getFileName(file), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                VirtualFile virtualFile = VfsUtil.findFileByIoFile(new File(file), true);
                String locationPath = virtualFile == null ? null : getPresentablePath(myProject, virtualFile.getParent(), 120);
                if (locationPath != null) {
                    myUsagePreviewTitle.append(
                        spaceAndThinSpace() + locationPath,
                        new SimpleTextAttributes(STYLE_PLAIN, UIUtil.getContextHelpForeground())
                    );
                }
            }
        };
        myResultsPreviewTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || Disposer.isDisposed(myPreviewUpdater)) {
                return;
            }
            //todo[vasya]: remove this dirty hack of updating preview panel after clicking on Replace button
            myPreviewUpdater.addRequest(
                updatePreviewRunnable,
                50
            );
        });
        DocumentAdapter documentAdapter = new DocumentAdapter() {
            @Override
            protected void textChanged(@Nonnull DocumentEvent e) {
                if (myDialog == null) {
                    return;
                }
                if (e.getDocument() == mySearchComponent.getDocument()) {
                    scheduleResultsUpdate();
                }
                if (e.getDocument() == myReplaceComponent.getDocument()) {
                    applyTo(myHelper.getModel());
                    if (myHelper.getModel().isRegularExpressions()) {
                        myComponentValidator.updateInfo(getValidationInfo(myHelper.getModel()));
                    }
                    Application.get().invokeLater(updatePreviewRunnable);
                }
            }
        };
        mySearchComponent.getDocument().addDocumentListener(documentAdapter);
        myReplaceComponent.getDocument().addDocumentListener(documentAdapter);

        mySearchRescheduleOnCancellationsAlarm = new Alarm();

        JBSplitter splitter = new OnePixelSplitter(true, .33f);
        splitter.setSplitterProportionKey(SPLITTER_SERVICE_KEY);
        splitter.getDivider().setBackground(OnePixelDivider.BACKGROUND);
        JBScrollPane scrollPane = new JBScrollPane(myResultsPreviewTable) {
            @Override
            public Dimension getMinimumSize() {
                Dimension size = super.getMinimumSize();
                size.height = myResultsPreviewTable.getPreferredScrollableViewportSize().height;
                return size;
            }
        };
        scrollPane.setBorder(JBUI.Borders.customLine(1, 0, 0, 0));
        
        splitter.setFirstComponent(scrollPane);

        myOKHintLabel = Label.create(LocalizeValue.empty());
        myOKHintLabel.setEnabled(false);
        myNavigationHintLabel = Label.create(LocalizeValue.empty());
        myNavigationHintLabel.setEnabled(false);

        DockLayout bottomLayout = DockLayout.create();
        bottomLayout.addBorders(BorderStyle.EMPTY, null, 4);
        bottomLayout.left(openOnNewTabBox);

        HorizontalLayout rightBottomPanel = HorizontalLayout.create();
        bottomLayout.right(rightBottomPanel);

        rightBottomPanel.add(myNavigationHintLabel);
        rightBottomPanel.add(myOKHintLabel);
        rightBottomPanel.add(myOKButton);
        rightBottomPanel.add(myReplaceAllButton);
        rightBottomPanel.add(myReplaceSelectedButton);

        myCodePreviewComponent = myUsagePreviewPanel.createComponent();
        JPanel previewPanel = new JPanel(new BorderLayout());
        Wrapper usagePreviewBorderWrapper = new Wrapper(myUsagePreviewTitle);
        usagePreviewBorderWrapper.setBorder(JBUI.Borders.customLine(0, 0, 1, 0));
        previewPanel.add(usagePreviewBorderWrapper, BorderLayout.NORTH);
        previewPanel.add(myCodePreviewComponent, BorderLayout.CENTER);
        splitter.setSecondComponent(previewPanel);
        JPanel scopesPanel = new JPanel(new MigLayout("flowx, gap 26, ins 0"));
        scopesPanel.setBorder(JBUI.Borders.empty());
        
        scopesPanel.add(myScopeSelectionToolbar.getComponent());
        scopesPanel.add(myScopeDetailsPanel, "growx, pushx");
        setLayout(new MigLayout("flowx, ins 0, gap 0, fillx, hidemode 3"));
        myTitlePanel = new JPanel(new MigLayout("flowx, ins 0, gap 0, fillx, filly"));
        myTitlePanel.add(myTitleLabel, "gapright 4");
        myTitlePanel.add(myInfoLabel);
        myTitlePanel.add(myLoadingDecorator.getComponent(), "w 24, wmin 24");
        myTitlePanel.add(Box.createHorizontalGlue(), "growx, pushx");
        myTitlePanel.setBorder(JBUI.Borders.empty(8, 4, 4, 4));

        add(myTitlePanel, "sx 2, growx, growx, growy");
        add(TargetAWT.to(myCbFileFilter.getComponent()));
        add(myFileMaskField, "gapleft 4, gapright 16, gaptop 4");
        myIsPinned.set(UISettings.getInstance().getPinFindInPath());

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("FindInFilesTopMenu", topActionGroup.build(), true);
        toolbar.setTargetComponent(this);

        JComponent component = toolbar.getComponent();
        component.setBorder(JBUI.Borders.emptyRight(4));
        component.setOpaque(false);

        add(component, "gaptop 4, wrap");

        add(mySearchTextArea, "pushx, growx, sx 10, pad 0 0 0 4, gaptop 4, wrap");
        mySearchTextArea.setBorder(JBUI.Borders.customLine(1, 0, 1, 0));

        myReplaceTextArea.setBorder(JBUI.Borders.customLine(0, 0, 1, 0));

        add(myReplaceTextArea, "pushx, growx, sx 10, pad 0 0 0 0, wrap");
        add(scopesPanel, "sx 10, pushx, growx, ax left, wrap, gaptop 0, gapbottom 0");
        add(splitter, "pushx, growx, growy, pushy, sx 10, wrap, pad 0");

        DockLayout borderWrapper = DockLayout.create();
        borderWrapper.center(bottomLayout);
        borderWrapper.addBorder(BorderPosition.TOP, BorderStyle.LINE, ComponentColors.BORDER);

        add(TargetAWT.to(borderWrapper), "pushx, growx, dock south, sx 10");

        MnemonicHelper.init(this);

        List<Component> focusOrder = new ArrayList<>();
        focusOrder.add(mySearchComponent);
        focusOrder.add(myReplaceComponent);
        focusOrder.add(TargetAWT.to(myCbFileFilter.getComponent()));
        ContainerUtil.addAll(focusOrder, focusableComponents(myScopeDetailsPanel));
        focusOrder.add(editorComponent);
        ContainerUtil.addAll(focusOrder, focusableComponents(TargetAWT.to(bottomLayout)));
        setFocusCycleRoot(true);
        setFocusTraversalPolicy(new ListFocusTraversalPolicy(focusOrder));
    }

    @Contract("_,!null,_->!null")
    private static String getPresentablePath(@Nonnull Project project, @Nullable VirtualFile virtualFile, int maxChars) {
        if (virtualFile == null) {
            return null;
        }
        String path = ScratchUtil.isScratch(virtualFile)
            ? ScratchUtil.getRelativePath(project, virtualFile)
            : VfsUtilCore.isAncestor(project.getBaseDir(), virtualFile, true)
            ? VfsUtilCore.getRelativeLocation(virtualFile, project.getBaseDir())
            : UserHomeFileUtil.getLocationRelativeToUserHome(virtualFile.getPath());
        return path == null ? null : maxChars < 0 ? path : StringUtil.trimMiddle(path, maxChars);
    }

    private DefaultTableModel createTableModel() {
        final DefaultTableModel model = new DefaultTableModel() {
            private String firstResultPath;

            private final Comparator<Vector<UsageInfoAdapter>> COMPARATOR = (v1, v2) -> {
                UsageInfoAdapter u1 = v1.get(0);
                UsageInfoAdapter u2 = v2.get(0);
                String u2Path = u2.getPath();
                final String u1Path = u1.getPath();
                if (u1Path.equals(firstResultPath) && !u2Path.equals(firstResultPath)) {
                    return -1; // first result is always sorted first
                }
                if (!u1Path.equals(firstResultPath) && u2Path.equals(firstResultPath)) {
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
            };

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            @SuppressWarnings({"UseOfObsoleteCollectionType", "unchecked", "rawtypes"})
            //Inserts search results in sorted order
            public void addRow(Object[] rowData) {
                Vector<Vector<UsageInfoAdapter>> dataVector = (Vector<Vector<UsageInfoAdapter>>) (Vector) this.dataVector;
                if (myNeedReset.compareAndSet(true, false)) {
                    dataVector.clear();
                    fireTableDataChanged();
                }
                final Vector<UsageInfoAdapter> v = (Vector) convertToVector(rowData);
                if (dataVector.isEmpty()) {
                    addRow(v);
                    myResultsPreviewTable.getSelectionModel().setSelectionInterval(0, 0);
                    firstResultPath = v.get(0).getPath();
                }
                else {
                    final int p = Collections.binarySearch(dataVector, v, COMPARATOR);
                    assert p < 0 : "duplicate result found";
                    int row = -(p + 1);
                    insertRow(row, v);
                }
            }
        };

        model.addColumn("Usages");
        return model;
    }

    private void processCtrlEnter() {
        doOK(true);
    }

    private void onFileMaskChanged() {
        Object item = myFileMaskField.getEditor().getItem();
        if (item != null && !item.equals(myFileMaskField.getSelectedItem())) {
            myFileMaskField.setSelectedItem(item);
        }
        scheduleResultsUpdate();
    }

    private void closeImmediately() {
        if (canBeClosedImmediately() && myDialog != null && myDialog.isVisible()) {
            myIsPinned.set(false);
            myDialog.doCancelAction();
        }
    }

    //Some popups shown above may prevent panel closing, first of all we should close them
    private boolean canBeClosedImmediately() {
        boolean state = myIsPinned.get();
        myIsPinned.set(false);
        try {
            //Here we actually close popups
            return myDialog != null && canBeClosed();
        }
        finally {
            myIsPinned.set(state);
        }
    }

    @RequiredUIAccess
    private void doOK(boolean openInFindWindow) {
        if (!canBeClosedImmediately()) {
            return;
        }

        FindModel validateModel = myHelper.getModel().clone();
        applyTo(validateModel);

        ValidationInfo validationInfo = getValidationInfo(validateModel);

        if (validationInfo == null) {
            if (validateModel.isReplaceState() &&
                !openInFindWindow &&
                myResultsPreviewTable.getRowCount() > 1 &&
                !ReplaceInProjectManager.getInstance(myProject)
                    .showReplaceAllConfirmDialog(myUsagesCount, getStringToFind(), myFilesCount, getStringToReplace())) {
                return;
            }
            myHelper.getModel().copyFrom(validateModel);
            myHelper.getModel().setPromptOnReplace(openInFindWindow);
            myHelper.doOKAction();
        }
        else {
            LocalizeValue message = validationInfo.message;
            Messages.showMessageDialog(this, message.get(), CommonLocalize.titleError().get(), UIUtil.getErrorIcon());
            return;
        }
        myIsPinned.set(false);
        myDialog.doCancelAction();
    }

    private void doReplaceSelected() {
        int rowToSelect = myResultsPreviewTable.getSelectionModel().getMinSelectionIndex();
        Map<Integer, Usage> usages = getSelectedUsages();
        if (usages == null) {
            return;
        }

        CommandProcessor.getInstance().newCommand()
            .project(myProject)
            .name(FindLocalize.findReplaceCommand())
            .run(() -> {
                for (Map.Entry<Integer, Usage> entry : usages.entrySet()) {
                    try {
                        ReplaceInProjectManager.getInstance(myProject)
                            .replaceUsage(entry.getValue(), myHelper.getModel(), Collections.emptySet(), false);
                        ((DefaultTableModel) myResultsPreviewTable.getModel()).removeRow(entry.getKey());
                    }
                    catch (FindManager.MalformedReplacementStringException ex) {
                        if (!Application.get().isUnitTestMode()) {
                            Messages.showErrorDialog(
                                this,
                                ex.getMessage(),
                                FindLocalize.findReplaceInvalidReplacementStringTitle().get()
                            );
                        }
                        break;
                    }
                }

                Application.get().invokeLater(() -> {
                    if (myResultsPreviewTable.getRowCount() > rowToSelect) {
                        myResultsPreviewTable.getSelectionModel().setSelectionInterval(rowToSelect, rowToSelect);
                    }
                    ScrollingUtil.ensureSelectionExists(myResultsPreviewTable);
                });
            });
    }

    private ToggleAction createAction(
        @Nonnull LocalizeValue message,
        String optionName,
        Image icon,
        Image hoveredIcon,
        Image selectedIcon,
        AtomicBoolean state,
        Producer<Boolean> enableStateProvider
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
                        Platform.current().os().isMac() ? ALT_DOWN_MASK | CTRL_DOWN_MASK : ALT_DOWN_MASK
                    )));
                    registerCustomShortcutSet(getShortcutSet(), FindPopupPanel.this);
                }
            }

            @Override
            public boolean isSelected(@Nonnull AnActionEvent e) {
                return state.get();
            }

            @Override
            @RequiredUIAccess
            public void update(@Nonnull AnActionEvent e) {
                e.getPresentation().setEnabled(enableStateProvider.produce());
                Toggleable.setSelected(e.getPresentation(), state.get());
            }

            @Override
            public void setSelected(@Nonnull AnActionEvent e, boolean selected) {
                //FUCounterUsageLogger.getInstance().logEvent("find", "check.box.toggled", new FeatureUsageData().
                //        addData("type", FIND_TYPE).
                //        addData("option_name", optionName).
                //        addData("option_value", selected));
                state.set(selected);
                scheduleResultsUpdate();
            }
        };
    }

    @Override
    @RequiredUIAccess
    public void addNotify() {
        super.addNotify();
        Application application = Application.get();
        application.invokeLater(() -> ScrollingUtil.ensureSelectionExists(myResultsPreviewTable), application.getAnyModalityState());
        myScopeSelectionToolbar.updateActionsImmediately();
    }

    @Override
    @RequiredUIAccess
    public void initByModel() {
        FindModel myModel = myHelper.getModel();
        myCaseSensitiveState.set(myModel.isCaseSensitive());
        myWholeWordsState.set(myModel.isWholeWordsOnly());
        myRegexState.set(myModel.isRegularExpressions());

        mySelectedContext = myModel.getSearchContext();
        if (myModel.isReplaceState()) {
            myPreserveCaseState.set(myModel.isPreserveCase());
        }

        mySelectedScope = myScopeUI.initByModel(myModel);

        boolean isThereFileFilter = myModel.getFileFilter() != null && !myModel.getFileFilter().isEmpty();
        try {
            myCbFileFilter.getComponent().putUserData(DONT_REQUEST_FOCUS, Boolean.TRUE);
            myCbFileFilter.setValue(isThereFileFilter);
        }
        finally {
            myCbFileFilter.getComponent().putUserData(DONT_REQUEST_FOCUS, null);
        }
        myFileMaskField.removeAllItems();
        List<String> variants = Arrays.asList(ArrayUtil.reverseArray(FindSettings.getInstance().getRecentFileMasks()));
        for (String variant : variants) {
            myFileMaskField.addItem(variant);
        }
        if (!variants.isEmpty()) {
            myFileMaskField.setSelectedItem(variants.get(0));
        }
        myFileMaskField.setEnabled(isThereFileFilter);
        String toSearch = myModel.getStringToFind();
        FindInProjectSettings findInProjectSettings = FindInProjectSettings.getInstance(myProject);

        if (StringUtil.isEmpty(toSearch)) {
            String[] history = findInProjectSettings.getRecentFindStrings();
            toSearch = history.length > 0 ? history[history.length - 1] : "";
        }

        mySearchComponent.setText(toSearch);
        String toReplace = myModel.getStringToReplace();

        if (StringUtil.isEmpty(toReplace)) {
            String[] history = findInProjectSettings.getRecentReplaceStrings();
            toReplace = history.length > 0 ? history[history.length - 1] : "";
        }
        myReplaceComponent.setText(toReplace);
        updateControls();
        updateScopeDetailsPanel();

        boolean isReplaceState = myHelper.isReplaceState();
        myTitleLabel.setText(myHelper.getTitle());
        myReplaceTextArea.setVisible(isReplaceState);
        myOKHintLabel.setText(KeymapUtil.getKeystrokeText(ENTER_WITH_MODIFIERS));

        myOKButton.setText(FindLocalize.findPopupFindButton());
        myReplaceAllButton.setVisible(isReplaceState);
        myReplaceSelectedButton.setVisible(isReplaceState);
    }

    private void updateControls() {
        myReplaceAllButton.setVisible(myHelper.isReplaceState());
        myReplaceSelectedButton.setVisible(myHelper.isReplaceState());
        myNavigationHintLabel.setVisible(mySearchComponent.getText().contains("\n"));
        mySearchTextArea.updateExtraActions();
        myReplaceTextArea.updateExtraActions();
        if (myNavigationHintLabel.isVisible()) {
            myNavigationHintLabel.setText("");
            KeymapManager keymapManager = KeymapManager.getInstance();
            Keymap activeKeymap = keymapManager.getActiveKeymap();
            if (activeKeymap != null) {
                String findNextText = KeymapUtil.getFirstKeyboardShortcutText("FindNext");
                String findPreviousText = KeymapUtil.getFirstKeyboardShortcutText("FindPrevious");
                if (!StringUtil.isEmpty(findNextText) && !StringUtil.isEmpty(findPreviousText)) {
                    myNavigationHintLabel.setText(FindLocalize.labelUse0And1ToSelectUsages(findNextText, findPreviousText).get());
                }
            }
        }
    }

    private void updateScopeDetailsPanel() {
        ((CardLayout) myScopeDetailsPanel.getLayout()).show(myScopeDetailsPanel, mySelectedScope.name);
        Component firstFocusableComponent =
            focusableComponents(myScopeDetailsPanel).find(c -> c.isFocusable() && c.isEnabled() && c.isShowing());
        myScopeDetailsPanel.revalidate();
        myScopeDetailsPanel.repaint();
        if (firstFocusableComponent != null) {
            Application.get().invokeLater(() -> ProjectIdeFocusManager.getInstance(myProject).requestFocus(firstFocusableComponent, true));
        }
        if (firstFocusableComponent == null && !mySearchComponent.isFocusOwner() && !myReplaceComponent.isFocusOwner()) {
            Application.get().invokeLater(() -> ProjectIdeFocusManager.getInstance(myProject).requestFocus(mySearchComponent, true));
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void scheduleResultsUpdate() {
        if (myDialog == null || !myDialog.isVisible()) {
            return;
        }
        if (mySearchRescheduleOnCancellationsAlarm == null || mySearchRescheduleOnCancellationsAlarm.isDisposed()) {
            return;
        }
        updateControls();
        mySearchRescheduleOnCancellationsAlarm.cancelAllRequests();
        mySearchRescheduleOnCancellationsAlarm.addRequest(this::findSettingsChanged, 100);
    }

    private void finishPreviousPreviewSearch() {
        if (myResultsPreviewSearchProgress != null && !myResultsPreviewSearchProgress.isCanceled()) {
            myResultsPreviewSearchProgress.cancel();
        }
    }

    @RequiredUIAccess
    private void findSettingsChanged() {
        if (isShowing()) {
            ScrollingUtil.ensureSelectionExists(myResultsPreviewTable);
        }
        final Application application = Application.get();
        final ModalityState state = application.getCurrentModalityState();
        finishPreviousPreviewSearch();
        mySearchRescheduleOnCancellationsAlarm.cancelAllRequests();
        applyTo(myHelper.getModel());
        FindModel findModel = new FindModel();
        findModel.copyFrom(myHelper.getModel());
        if (findModel.getStringToFind().contains("\n") && Registry.is("ide.find.ignores.leading.whitespace.in.multiline.search")) {
            findModel.setMultiline(true);
        }

        ValidationInfo result = getValidationInfo(myHelper.getModel());
        myComponentValidator.updateInfo(result);

        final ProgressIndicatorBase progressIndicatorWhenSearchStarted = new ProgressIndicatorBase() {
            @Override
            public void stop() {
                super.stop();
                onStop(System.identityHashCode(this));
                application.invokeLater(() -> {
                    if (myNeedReset.compareAndSet(true, false)) { //nothing is found, let's clear previous results
                        reset();
                    }
                });
            }
        };
        myResultsPreviewSearchProgress = progressIndicatorWhenSearchStarted;
        final int hash = System.identityHashCode(myResultsPreviewSearchProgress);

        // Use previously shown usage files as hint for faster search and better usage preview performance if pattern length increased
        Set<VirtualFile> filesToScanInitially = new LinkedHashSet<>();

        if (myHelper.myPreviousModel != null
            && myHelper.myPreviousModel.getStringToFind().length() < myHelper.getModel().getStringToFind().length()) {
            final DefaultTableModel previousModel = (DefaultTableModel) myResultsPreviewTable.getModel();
            for (int i = 0, len = previousModel.getRowCount(); i < len; ++i) {
                final Object value = previousModel.getValueAt(i, 0);
                if (value instanceof UsageInfo2UsageAdapter usage) {
                    final VirtualFile file = usage.getFile();
                    if (file != null) {
                        filesToScanInitially.add(file);
                    }
                }
            }
        }

        myHelper.myPreviousModel = myHelper.getModel().clone();

        myReplaceAllButton.setEnabled(false);
        myReplaceSelectedButton.setEnabled(false);
        myReplaceSelectedButton.setText(FindLocalize.findPopupReplaceSelectedButton(0));

        onStart(hash);
        if (result != null && result.component != myReplaceComponent) {
            onStop(hash, result.message);
            reset();
            return;
        }

        FindInProjectExecutor projectExecutor = FindInProjectExecutor.getInstance();
        GlobalSearchScope scope =
            GlobalSearchScopeUtil.toGlobalSearchScope(FindInProjectUtil.getScopeFromModel(myProject, myHelper.myPreviousModel), myProject);
        TableCellRenderer renderer = projectExecutor.createTableCellRenderer();
        if (renderer == null) {
            renderer = new UsageTableCellRenderer(scope);
        }
        myResultsPreviewTable.getColumnModel().getColumn(0).setCellRenderer(renderer);

        final AtomicInteger resultsCount = new AtomicInteger();
        final AtomicInteger resultsFilesCount = new AtomicInteger();
        FindInProjectUtil.setupViewPresentation(myUsageViewPresentation, findModel);

        ProgressIndicatorUtils.scheduleWithWriteActionPriority(
            myResultsPreviewSearchProgress,
            new ReadTask() {
                @Override
                @RequiredReadAction
                public Continuation performInReadAction(@Nonnull ProgressIndicator indicator) {
                    final FindUsagesProcessPresentation processPresentation =
                        FindInProjectUtil.setupProcessPresentation(myProject, myUsageViewPresentation);
                    ThreadLocal<String> lastUsageFileRef = new ThreadLocal<>();
                    ThreadLocal<Reference<Usage>> recentUsageRef = new ThreadLocal<>();

                    projectExecutor.findUsages(
                        myProject,
                        myResultsPreviewSearchProgress,
                        processPresentation,
                        findModel,
                        filesToScanInitially,
                        usage -> {
                            if (isCancelled()) {
                                onStop(hash);
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
                            final boolean merged = !myHelper.isReplaceState() && recentAdapter != null && recentAdapter.merge(usage);
                            if (!merged) {
                                recentUsageRef.set(new WeakReference<>(usage));
                            }

                            application.invokeLater(
                                () -> {
                                    if (isCancelled()) {
                                        onStop(hash);
                                        return;
                                    }
                                    DefaultTableModel model = (DefaultTableModel) myResultsPreviewTable.getModel();
                                    if (!merged) {
                                        model.addRow(new Object[]{usage});
                                    }
                                    else {
                                        model.fireTableRowsUpdated(model.getRowCount() - 1, model.getRowCount() - 1);
                                    }
                                    myCodePreviewComponent.setVisible(true);
                                    if (model.getRowCount() == 1) {
                                        myResultsPreviewTable.setRowSelectionInterval(0, 0);
                                    }
                                    int occurrences = resultsCount.get();
                                    int filesWithOccurrences = resultsFilesCount.get();
                                    myCodePreviewComponent.setVisible(occurrences > 0);
                                    myReplaceAllButton.setEnabled(occurrences > 0);
                                    myReplaceSelectedButton.setEnabled(occurrences > 0);

                                    StringBuilder stringBuilder = new StringBuilder();
                                    if (occurrences > 0) {
                                        stringBuilder.append(Math.min(ShowUsagesAction.getUsagesPageSize(), occurrences));
                                        boolean foundAllUsages = occurrences < ShowUsagesAction.getUsagesPageSize();
                                        myUsagesCount = String.valueOf(occurrences);
                                        if (!foundAllUsages) {
                                            stringBuilder.append("+");
                                            myUsagesCount += "+";
                                        }
                                        stringBuilder.append(UILocalize.messageMatches(occurrences).get());
                                        stringBuilder.append(" in ");
                                        stringBuilder.append(filesWithOccurrences);
                                        myFilesCount = String.valueOf(filesWithOccurrences);
                                        if (!foundAllUsages) {
                                            stringBuilder.append("+");
                                            myFilesCount += "+";
                                        }
                                        stringBuilder.append(UILocalize.messageFiles(filesWithOccurrences));
                                    }
                                    myInfoLabel.setText(stringBuilder.toString());
                                },
                                state
                            );

                            boolean continueSearch = resultsCount.incrementAndGet() < ShowUsagesAction.getUsagesPageSize();
                            if (!continueSearch) {
                                onStop(hash);
                            }
                            return continueSearch;
                        }
                    );

                    return new Continuation(
                        () -> {
                            if (!isCancelled() && resultsCount.get() == 0) {
                                showEmptyText(LocalizeValue.empty());
                            }
                            onStop(hash);
                        },
                        state
                    );
                }

                boolean isCancelled() {
                    return progressIndicatorWhenSearchStarted != myResultsPreviewSearchProgress || progressIndicatorWhenSearchStarted.isCanceled();
                }

                @Override
                public void onCanceled(@Nonnull ProgressIndicator indicator) {
                    if (isShowing() && progressIndicatorWhenSearchStarted == myResultsPreviewSearchProgress) {
                        scheduleResultsUpdate();
                    }
                }
            }
        );
    }

    private void reset() {
        ((DefaultTableModel) myResultsPreviewTable.getModel()).getDataVector().clear();
        ((DefaultTableModel) myResultsPreviewTable.getModel()).fireTableDataChanged();
        myResultsPreviewTable.getSelectionModel().clearSelection();
        myInfoLabel.setText(null);
    }

    private void showEmptyText(@Nonnull LocalizeValue message) {
        StatusText emptyText = myResultsPreviewTable.getEmptyText();
        emptyText.clear();
        emptyText.setText(
            message != LocalizeValue.empty()
                ? UILocalize.messageNothingtoshowWithProblem(message).get()
                : UILocalize.messageNothingtoshow().get()
        );
        if (mySelectedScope == FindPopupScopeUIImpl.DIRECTORY && !myHelper.getModel().isWithSubdirectories()) {
            emptyText.appendSecondaryText(
                FindLocalize.findRecursivelyHint().get(),
                SimpleTextAttributes.LINK_ATTRIBUTES,
                e -> {
                    myHelper.getModel().setWithSubdirectories(true);
                    scheduleResultsUpdate();
                }
            );
        }
    }

    private void onStart(int hash) {
        myNeedReset.set(true);
        myLoadingHash = hash;
        myLoadingDecorator.startLoading(false);
        myResultsPreviewTable.getEmptyText().setText(FindLocalize.emptyTextSearching().get());
    }

    private void onStop(int hash) {
        onStop(hash, LocalizeValue.empty());
    }

    private void onStop(int hash, LocalizeValue message) {
        if (hash != myLoadingHash) {
            return;
        }
        UIUtil.invokeLaterIfNeeded(() -> {
            showEmptyText(message);
            myLoadingDecorator.stopLoading();
        });
    }

    @Override
    @Nullable
    public String getFileTypeMask() {
        String mask = null;
        if (myCbFileFilter != null && myCbFileFilter.getValue()) {
            mask = (String) myFileMaskField.getSelectedItem();
        }
        return mask;
    }

    // "null means OK"
    @Nullable()
    private ValidationInfo getValidationInfo(@Nonnull FindModel model) {
        ValidationInfo scopeValidationInfo = myScopeUI.validate(model, mySelectedScope);
        if (scopeValidationInfo != null) {
            return scopeValidationInfo;
        }

        if (!myHelper.canSearchThisString()) {
            return new ValidationInfo(FindLocalize.findEmptySearchTextError(), mySearchComponent);
        }

        if (model.isRegularExpressions()) {
            String toFind = model.getStringToFind();
            Pattern pattern;
            try {
                pattern =
                    Pattern.compile(toFind, model.isCaseSensitive() ? Pattern.MULTILINE : Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
                if (pattern.matcher("").matches() && !toFind.endsWith("$") && !toFind.startsWith("^")) {
                    return new ValidationInfo(FindLocalize.findEmptyMatchRegularExpressionError(), mySearchComponent);
                }
            }
            catch (PatternSyntaxException e) {
                return new ValidationInfo(
                    FindLocalize.findInvalidRegularExpressionError(toFind, e.getDescription()),
                    mySearchComponent
                );
            }
            if (model.isReplaceState()) {
                if (myResultsPreviewTable.getRowCount() > 0) {
                    Object value = myResultsPreviewTable.getModel().getValueAt(0, 0);
                    if (value instanceof Usage usage) {
                        try {
                            // Just check
                            ReplaceInProjectManager.getInstance(myProject).replaceUsage(usage, model, Collections.emptySet(), true);
                        }
                        catch (FindManager.MalformedReplacementStringException e) {
                            return new ValidationInfo(LocalizeValue.localizeTODO(e.getMessage()), myReplaceComponent);
                        }
                    }
                }

                try {
                    RegExReplacementBuilder.validate(pattern, getStringToReplace());
                }
                catch (IllegalArgumentException e) {
                    return new ValidationInfo(
                        FindLocalize.findReplaceInvalidReplacementString(e.getMessage()),
                        myReplaceComponent
                    );
                }
            }
        }

        final String mask = getFileTypeMask();

        if (mask != null) {
            if (mask.isEmpty()) {
                return new ValidationInfo(FindLocalize.findFilterEmptyFileMaskError(), myFileMaskField);
            }

            if (mask.contains(";")) {
                return new ValidationInfo(FindLocalize.messageFileMasksShouldBeCommaSeparated(), myFileMaskField);
            }
            try {
                createFileMaskRegExp(mask);   // verify that the regexp compiles
            }
            catch (PatternSyntaxException ex) {
                return new ValidationInfo(FindLocalize.findFilterInvalidFileMaskError(mask), myFileMaskField);
            }
        }
        return null;
    }

    @Override
    @Nonnull
    public String getStringToFind() {
        return mySearchComponent.getText();
    }

    @Nonnull
    private String getStringToReplace() {
        return myReplaceComponent.getText();
    }

    private void applyTo(@Nonnull FindModel model) {
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

        String mask = getFileTypeMask();
        model.setFileFilter(mask);
    }

    private void navigateToSelectedUsage(@Nullable AnActionEvent e) {
        Navigatable[] navigatables = e != null ? e.getData(Navigatable.KEY_OF_ARRAY) : null;
        if (navigatables != null) {
            if (canBeClosed()) {
                myDialog.doCancelAction();
            }
            OpenSourceUtil.navigate(navigatables);
            return;
        }

        Map<Integer, Usage> usages = getSelectedUsages();
        if (usages != null) {
            if (canBeClosed()) {
                myDialog.doCancelAction();
            }
            boolean first = true;
            for (Usage usage : usages.values()) {
                if (first) {
                    usage.navigate(true);
                }
                else {
                    usage.highlightInEditor();
                }
                first = false;
            }
        }
    }

    @Nullable
    private Map<Integer, Usage> getSelectedUsages() {
        int[] rows = myResultsPreviewTable.getSelectedRows();
        Map<Integer, Usage> result = null;
        for (int i = rows.length - 1; i >= 0; i--) {
            int row = rows[i];
            Object valueAt = myResultsPreviewTable.getModel().getValueAt(row, 0);
            if (valueAt instanceof Usage usage) {
                if (result == null) {
                    result = new LinkedHashMap<>();
                }
                result.put(row, usage);
            }
        }
        return result;
    }

    public static ActionToolbar createToolbar(AnAction... actions) {
        ActionToolbar toolbar =
            ActionManager.getInstance().createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, new DefaultActionGroup(actions), true);
        toolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
        return toolbar;
    }

    private static void applyFont(JBFont font, Component... components) {
        for (Component component : components) {
            component.setFont(font);
        }
    }

    private static void createFileMaskRegExp(@Nullable String filter) throws PatternSyntaxException {
        if (filter == null) {
            return;
        }
        String pattern;
        final List<String> strings = StringUtil.split(filter, ",");
        if (strings.size() == 1) {
            pattern = PatternUtil.convertToRegex(filter.trim());
        }
        else {
            pattern = StringUtil.join(strings, s -> "(" + PatternUtil.convertToRegex(s.trim()) + ")", "|");
        }
        // just check validity
        //noinspection ResultOfMethodCallIgnored
        Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    }

    @Nonnull
    private static JBIterable<Component> focusableComponents(@Nonnull Component component) {
        return UIUtil.uiTraverser(component)
            .bfsTraversal()
            .filter(c -> c instanceof JComboBox || c instanceof AbstractButton || c instanceof JTextComponent);
    }

    private class MySwitchContextToggleAction extends ToggleAction implements DumbAware {
        FindSearchContext myContext;

        MySwitchContextToggleAction(FindSearchContext context) {
            super(context.getName());
            myContext = context;
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return mySelectedContext == myContext;
        }

        @Override
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            if (state) {
                mySelectedContext = myContext;
                scheduleResultsUpdate();
            }
        }
    }

    private class MySelectScopeToggleAction extends DumbAwareToggleAction {
        private final FindPopupScopeUI.ScopeType myScope;

        MySelectScopeToggleAction(FindPopupScopeUI.ScopeType scope) {
            super(scope.text, LocalizeValue.empty(), null);
            myScope = scope;
        }

        @Override
        public boolean displayTextInToolbar() {
            return true;
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return mySelectedScope == myScope;
        }

        @Override
        @RequiredUIAccess
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            if (state) {
                mySelectedScope = myScope;
                myScopeSelectionToolbar.updateActionsImmediately();
                updateScopeDetailsPanel();
                scheduleResultsUpdate();
            }
        }
    }

    private class MyShowFilterPopupAction extends DefaultActionGroup implements DumbAware {
        private final Image myPrimaryImage;

        MyShowFilterPopupAction() {
            super(FindLocalize.findPopupShowFilterPopup(), true);
            myPrimaryImage = PlatformIconGroup.generalFilter();

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

        @RequiredUIAccess
        @Override
        public void update(@Nonnull AnActionEvent e) {
            super.update(e);

            if (!FindSearchContext.ANY.equals(mySelectedContext)) {
                e.getPresentation().setIcon(ImageEffects.layered(myPrimaryImage, PlatformIconGroup.greenbadge()));
            }
            else {
                e.getPresentation().setIcon(myPrimaryImage);
            }
        }
    }

    static class UsageTableCellRenderer extends JPanel implements TableCellRenderer {
        private final ColoredTableCellRenderer myUsageRenderer = new ColoredTableCellRenderer() {
            @Override
            protected void customizeCellRenderer(JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
                if (value instanceof UsageInfo2UsageAdapter usageAdapter) {
                    if (!usageAdapter.isValid()) {
                        myUsageRenderer.append(" " + UsageLocalize.nodeInvalid().get() + " ", SimpleTextAttributes.ERROR_ATTRIBUTES);
                    }
                    TextChunk[] text = ((UsageInfo2UsageAdapter) value).getPresentation().getText();

                    // skip line number / file info
                    for (int i = 1; i < text.length; ++i) {
                        TextChunk textChunk = text[i];
                        SimpleTextAttributes attributes = getAttributes(textChunk);
                        myUsageRenderer.append(textChunk.getText(), attributes);
                    }
                }
                setBorder(null);
            }

            @Nonnull
            private SimpleTextAttributes getAttributes(@Nonnull TextChunk textChunk) {
                SimpleTextAttributes at = textChunk.getSimpleAttributesIgnoreBackground();
                boolean highlighted = textChunk.getType() != null || at.getFontStyle() == Font.BOLD;
                return highlighted ? new SimpleTextAttributes(
                    null,
                    at.getFgColor(),
                    at.getWaveColor(),
                    at.getStyle() & ~SimpleTextAttributes.STYLE_BOLD | SimpleTextAttributes.STYLE_SEARCH_MATCH
                ) : at;
            }
        };

        private final ColoredTableCellRenderer myFileAndLineNumber = new ColoredTableCellRenderer() {
            private final SimpleTextAttributes REPEATED_FILE_ATTRIBUTES =
                new SimpleTextAttributes(STYLE_PLAIN, new JBColor(0xCCCCCC, 0x5E5E5E));
            private final SimpleTextAttributes ORDINAL_ATTRIBUTES = new SimpleTextAttributes(STYLE_PLAIN, new JBColor(0x999999, 0x999999));

            @Override
            protected void customizeCellRenderer(JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
                if (value instanceof UsageInfo2UsageAdapter usageAdapter) {
                    TextChunk[] text = usageAdapter.getPresentation().getText();
                    // line number / file info
                    VirtualFile file = usageAdapter.getFile();
                    String uniqueVirtualFilePath = getFilePath(usageAdapter);
                    VirtualFile prevFile = findPrevFile(table, row, column);
                    SimpleTextAttributes attributes = Comparing.equal(file, prevFile) ? REPEATED_FILE_ATTRIBUTES : ORDINAL_ATTRIBUTES;
                    append(uniqueVirtualFilePath, attributes);
                    if (text.length > 0) {
                        append(" " + text[0].getText(), ORDINAL_ATTRIBUTES);
                    }
                }
                setBorder(null);
            }

            @Nonnull
            private String getFilePath(@Nonnull UsageInfo2UsageAdapter ua) {
                VirtualFile file = ua.getFile();
                if (ScratchUtil.isScratch(file)) {
                    return StringUtil.notNullize(getPresentablePath(ua.getUsageInfo().getProject(), ua.getFile(), 60));
                }
                return UniqueVFilePathBuilder.getInstance().getUniqueVirtualFilePath(ua.getUsageInfo().getProject(), file, myScope);
            }

            @Nullable
            private VirtualFile findPrevFile(@Nonnull JTable table, int row, int column) {
                if (row <= 0) {
                    return null;
                }
                Object prev = table.getValueAt(row - 1, column);
                return prev instanceof UsageInfo2UsageAdapter usageAdapter ? usageAdapter.getFile() : null;
            }
        };

        private static final int MARGIN = 2;
        private final GlobalSearchScope myScope;

        UsageTableCellRenderer(GlobalSearchScope scope) {
            myScope = scope;
            setLayout(new BorderLayout());
            add(myUsageRenderer, BorderLayout.CENTER);
            add(myFileAndLineNumber, BorderLayout.EAST);
            setBorder(JBUI.Borders.empty(MARGIN, MARGIN, MARGIN, 0));
        }

        @Override
        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
        ) {
            myUsageRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            myFileAndLineNumber.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBackground(myUsageRenderer.getBackground());
            if (!isSelected && value instanceof UsageInfo2UsageAdapter usageAdapter) {
                Project project = usageAdapter.getUsageInfo().getProject();
                Color color = TargetAWT.to(VfsPresentationUtil.getFileBackgroundColor(project, usageAdapter.getFile()));
                setBackground(color);
                myUsageRenderer.setBackground(color);
                myFileAndLineNumber.setBackground(color);
            }
            getAccessibleContext().setAccessibleName(FindLocalize.findPopupFoundElementAccesibleName(
                Objects.requireNonNullElse(myUsageRenderer.getAccessibleContext().getAccessibleName(), ""),
                Objects.requireNonNullElse(myFileAndLineNumber.getAccessibleContext().getAccessibleName(), "")
            ).get());
            return this;
        }
    }

    private class MyPinAction extends ToggleAction implements DumbAware {
        private MyPinAction() {
            super(IdeLocalize.actionToggleactionPinWindowText(), IdeLocalize.actionToggleactionPinWindowDescription(), AllIcons.General.Pin_tab);
        }

        @Override
        public boolean isDumbAware() {
            return true;
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return UISettings.getInstance().getPinFindInPath();
        }

        @Override
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            myIsPinned.set(state);
            UISettings.getInstance().setPinFindInPath(state);
            //FUCounterUsageLogger.getInstance().logEvent("find", "pin.toggled", new FeatureUsageData().addData("option_value", state));
        }
    }


    private class MyEnterAction extends DumbAwareAction {
        private final boolean myEnterAsOK;

        private MyEnterAction(boolean enterAsOK) {
            myEnterAsOK = enterAsOK;
        }

        @Override
        @RequiredUIAccess
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setEnabled(
                e.getData(Editor.KEY) == null
                    || SwingUtilities.isDescendingFrom(e.getData(UIExAWTDataKey.CONTEXT_COMPONENT), myFileMaskField)
            );
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            if (SwingUtilities.isDescendingFrom(e.getData(UIExAWTDataKey.CONTEXT_COMPONENT), myFileMaskField)
                && myFileMaskField.isPopupVisible()) {
                myFileMaskField.hidePopup();
                return;
            }
            if (myScopeUI.hideAllPopups()) {
                return;
            }
            if (myEnterAsOK) {
                doOK(true);
            }
            else if (myHelper.isReplaceState()) {
                doReplaceSelected();
            }
            else {
                navigateToSelectedUsage(null);
            }
        }
    }
}

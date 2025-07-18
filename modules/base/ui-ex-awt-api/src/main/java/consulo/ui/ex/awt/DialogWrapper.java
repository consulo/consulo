/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.annotation.DeprecationInfo;
import consulo.application.*;
import consulo.application.ui.UISettings;
import consulo.application.ui.wm.ApplicationIdeFocusManager;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.application.util.registry.Registry;
import consulo.awt.hacking.JComponentHacking;
import consulo.component.ComponentManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.platform.base.localize.ActionLocalize;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.Alerts;
import consulo.ui.Button;
import consulo.ui.CheckBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.IdeGlassPane;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.PositionTracker;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.touchBar.TouchBarController;
import consulo.ui.ex.action.util.ActionUtil;
import consulo.ui.ex.action.util.ShortcutUtil;
import consulo.ui.ex.awt.internal.*;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awt.util.IdeGlassPaneUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.BalloonBuilder;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.util.collection.ArrayUtil;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.TimeoutUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.MagicConstant;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The standard base class for modal dialog boxes. The dialog wrapper could be used only on event dispatch thread.
 * In case when the dialog must be created from other threads use
 * {@link EventQueue#invokeLater(Runnable)} or {@link EventQueue#invokeAndWait(Runnable)}.
 * <p>
 * See also http://confluence.jetbrains.net/display/IDEADEV/IntelliJ+IDEA+DialogWrapper.
 */
@SuppressWarnings({"SSBasedInspection", "MethodMayBeStatic", "UnusedDeclaration"})
public abstract class DialogWrapper {
    public static enum IdeModalityType {
        IDE,
        PROJECT,
        MODELESS;

        public Dialog.ModalityType toAwtModality() {
            return switch (this) {
                case IDE -> Dialog.ModalityType.APPLICATION_MODAL;
                case PROJECT -> Dialog.ModalityType.DOCUMENT_MODAL;
                case MODELESS -> Dialog.ModalityType.MODELESS;
                default -> null;
            };
        }
    }

    private static final Logger LOGGER = Logger.getInstance(DialogWrapper.class);

    /**
     * The default exit code for "OK" action.
     */
    public static final int OK_EXIT_CODE = 0;
    /**
     * The default exit code for "Cancel" action.
     */
    public static final int CANCEL_EXIT_CODE = 1;
    /**
     * The default exit code for "Close" action. Equal to cancel.
     */
    public static final int CLOSE_EXIT_CODE = CANCEL_EXIT_CODE;
    /**
     * If you use your own custom exit codes you have to start them with
     * this constant.
     */
    public static final int NEXT_USER_EXIT_CODE = 2;

    /**
     * If your action returned by <code>createActions</code> method has non
     * <code>null</code> value for this key, then the button that corresponds to the action will be the
     * default button for the dialog. It's true if you don't change this behaviour
     * of <code>createJButtonForAction(Action)</code> method.
     */
    public static final String DEFAULT_ACTION = "DefaultAction";

    public static final String FOCUSED_ACTION = "FocusedAction";

    private static final String NO_AUTORESIZE = "NoAutoResizeAndFit";

    private static final KeyStroke SHOW_OPTION_KEYSTROKE =
        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK);

    private final DialogWrapperPeer myPeer;
    private int myExitCode = CANCEL_EXIT_CODE;

    /**
     * The shared instance of default border for dialog's content pane.
     */
    public static final Insets ourDefaultBorderInsets = UIUtil.PANEL_SMALL_INSETS;
    @Deprecated
    public static final Border ourDefaultBorder = new EmptyBorder(UIUtil.PANEL_SMALL_INSETS);

    private float myHorizontalStretch = 1.0f;
    private float myVerticalStretch = 1.0f;
    /**
     * Defines horizontal alignment of buttons.
     */
    private int myButtonAlignment = SwingConstants.RIGHT;
    private final boolean myCreateSouthSection;
    private boolean myCrossClosesWindow = true;

    protected LocalizeAction myOKAction;
    protected LocalizeAction myCancelAction;
    protected LocalizeAction myHelpAction;
    private final Map<Action, JButton> myButtonMap = new LinkedHashMap<>();

    private boolean myClosed = false;

    protected boolean myPerformAction = false;

    private Action myYesAction = null;
    private Action myNoAction = null;

    protected CheckBox myCheckBoxDoNotShowDialog;
    @Nullable
    private DoNotAskOption myDoNotAsk;

    private JComponent myPreferredFocusedComponent;
    private Supplier<Point> myInitialLocationCallback;

    private Dimension myActualSize = null;
    private List<ValidationInfo> myInfo = Collections.emptyList();

    protected final Disposable myDisposable = new Disposable() {
        @Override
        public String toString() {
            return DialogWrapper.this.toString();
        }

        @Override
        public void dispose() {
            DialogWrapper.this.dispose();
        }
    };
    private final List<JBOptionButton> myOptionsButtons = new ArrayList<>();
    private int myCurrentOptionsButtonIndex = -1;
    private boolean myResizeInProgress = false;
    private ComponentAdapter myResizeListener;

    protected LocalizeValue getDoNotShowMessage() {
        return CommonLocalize.dialogOptionsDoNotShow();
    }

    public void setDoNotAskOption(@Nullable DoNotAskOption doNotAsk) {
        myDoNotAsk = doNotAsk;
    }

    protected ErrorText myErrorText;
    private int myMaxErrorTextLength;

    private final Alarm myErrorTextAlarm = new Alarm();

    private static final Color BALLOON_BORDER = new JBColor(new Color(0xe0a8a9), new Color(0x73454b));
    private static final Color BALLOON_BACKGROUND = new JBColor(new Color(0xf5e6e7), new Color(0x593d41));

    /**
     * Creates modal {@code DialogWrapper}. The currently active window will be the dialog's parent.
     *
     * @param project     parent window for the dialog will be calculated based on focused window for the
     *                    specified {@code project}. This parameter can be {@code null}. In this case parent window
     *                    will be suggested based on current focused window.
     * @param canBeParent specifies whether the dialog can be parent for other windows. This parameter is used
     *                    by {@code WindowManager}.
     * @throws IllegalStateException if the dialog is invoked not on the event dispatch thread
     */
    protected DialogWrapper(@Nullable ComponentManager project, boolean canBeParent) {
        this(project, canBeParent, IdeModalityType.IDE);
    }

    protected DialogWrapper(@Nullable ComponentManager project, boolean canBeParent, @Nonnull IdeModalityType ideModalityType) {
        this(project, null, canBeParent, ideModalityType);
    }

    protected DialogWrapper(
        @Nullable ComponentManager project,
        @Nullable Component parentComponent,
        boolean canBeParent,
        @Nonnull IdeModalityType ideModalityType
    ) {
        this(project, parentComponent, canBeParent, ideModalityType, true);
    }

    protected DialogWrapper(
        @Nullable ComponentManager project,
        @Nullable Component parentComponent,
        boolean canBeParent,
        @Nonnull IdeModalityType ideModalityType,
        boolean createSouth
    ) {
        myPeer = parentComponent == null
            ? createPeer(project, canBeParent, project == null ? IdeModalityType.IDE : ideModalityType)
            : createPeer(parentComponent, canBeParent);
        myCreateSouthSection = createSouth;
        Window window = myPeer.getWindow();
        if (window != null) {
            myResizeListener = new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    if (!myResizeInProgress) {
                        myActualSize = myPeer.getSize();
                        if (myErrorText != null && myErrorText.isVisible()) {
                            myActualSize.height -= myErrorText.getMinimumSize().height;
                        }
                    }
                }
            };
            window.addComponentListener(myResizeListener);
        }
        createDefaultActions();
    }

    /**
     * Creates modal {@code DialogWrapper} that can be parent for other windows.
     * The currently active window will be the dialog's parent.
     *
     * @param project parent window for the dialog will be calculated based on focused window for the
     *                specified {@code project}. This parameter can be {@code null}. In this case parent window
     *                will be suggested based on current focused window.
     * @throws IllegalStateException if the dialog is invoked not on the event dispatch thread
     * @see DialogWrapper#DialogWrapper(Project, boolean)
     */
    protected DialogWrapper(@Nullable ComponentManager project) {
        this(project, true);
    }

    /**
     * Creates modal {@code DialogWrapper}. The currently active window will be the dialog's parent.
     *
     * @param canBeParent specifies whether the dialog can be parent for other windows. This parameter is used
     *                    by {@code WindowManager}.
     * @throws IllegalStateException if the dialog is invoked not on the event dispatch thread
     */
    protected DialogWrapper(boolean canBeParent) {
        this((ComponentManager)null, canBeParent);
    }

    /**
     * Typically, we should set a parent explicitly. Use {@link WindowManager#suggestParentWindow}
     * method to find out the best parent for your dialog. Exceptions are cases
     * when we do not have a project to figure out which window
     * is more suitable as an owner for the dialog.
     * <p/>
     *
     * @deprecated use {@link DialogWrapper#DialogWrapper(Project, boolean, boolean)}
     */
    @Deprecated
    protected DialogWrapper(boolean canBeParent, boolean applicationModalIfPossible) {
        this(null, canBeParent, applicationModalIfPossible);
    }

    protected DialogWrapper(ComponentManager project, boolean canBeParent, boolean applicationModalIfPossible) {
        ensureEventDispatchThread();
        consulo.ui.Window owner = null;
        myPeer = createPeer(project, canBeParent, applicationModalIfPossible);
        myCreateSouthSection = true;
        createDefaultActions();
    }

    /**
     * @param parent      parent component which is used to calculate heavy weight window ancestor.
     *                    {@code parent} cannot be {@code null} and must be showing.
     * @param canBeParent can be parent
     * @throws IllegalStateException if the dialog is invoked not on the event dispatch thread
     */
    protected DialogWrapper(@Nonnull Component parent, boolean canBeParent) {
        ensureEventDispatchThread();
        myCreateSouthSection = true;
        myPeer = createPeer(parent, canBeParent);
        createDefaultActions();
    }

    //validation
    private final Alarm myValidationAlarm = new Alarm(getValidationThreadToUse(), myDisposable);

    @Nonnull
    protected Alarm.ThreadToUse getValidationThreadToUse() {
        return Alarm.ThreadToUse.SWING_THREAD;
    }

    private int myValidationDelay = 300;
    private boolean myDisposed = false;
    private boolean myValidationStarted = false;
    private final ErrorPainter myErrorPainter = new ErrorPainter();
    protected JComponent myErrorPane;
    private boolean myErrorPainterInstalled = false;

    /**
     * Allows to postpone first start of validation
     *
     * @return <code>false</code> if start validation in <code>init()</code> method
     */
    protected boolean postponeValidation() {
        return true;
    }

    /**
     * Validates user input and returns <code>null</code> if everything is fine
     * or validation description with component where problem has been found.
     *
     * @return <code>null</code> if everything is OK or validation descriptor
     */
    @Nullable
    @RequiredUIAccess
    protected ValidationInfo doValidate() {
        return null;
    }

    /**
     * Validates user input and returns <code>List&lt;ValidationInfo&gt;</code>.
     * If everything is fine the returned list is empty otherwise
     * the list contains all invalid fields with error messages.
     * This method should preferably be used when validating forms with multiply
     * fields that require validation.
     *
     * @return <code>List&lt;ValidationInfo&gt;</code> of invalid fields. List
     * is empty if no errors found.
     */
    @Nonnull
    @RequiredUIAccess
    protected List<ValidationInfo> doValidateAll() {
        ValidationInfo vi = doValidate();
        return vi != null ? Collections.singletonList(vi) : Collections.EMPTY_LIST;
    }

    public void setValidationDelay(int delay) {
        myValidationDelay = delay;
    }

    private void installErrorPainter() {
        if (myErrorPainterInstalled) {
            return;
        }
        myErrorPainterInstalled = true;
        UIUtil.invokeLaterIfNeeded(() -> IdeGlassPaneUtil.installPainter(getContentPanel(), myErrorPainter, myDisposable));
    }

    protected void updateErrorInfo(@Nonnull List<ValidationInfo> info) {
        boolean updateNeeded = Registry.is("ide.inplace.errors.balloon") ? !myInfo.equals(info) : !myErrorText.isTextSet(info);

        if (updateNeeded) {
            SwingUtilities.invokeLater(() -> {
                if (myDisposed) {
                    return;
                }
                setErrorInfoAll(info);
                myPeer.getRootPane().getGlassPane().repaint();
                getOKAction().setEnabled(info.isEmpty());
            });
        }
    }

    protected void createDefaultActions() {
        myOKAction = new OkAction();
        myCancelAction = new CancelAction();
        myHelpAction = new HelpAction();
    }

    public void setUndecorated(boolean undecorated) {
        myPeer.setUndecorated(undecorated);
    }

    public final void addMouseListener(@Nonnull MouseListener listener) {
        myPeer.addMouseListener(listener);
    }

    public final void addMouseListener(@Nonnull MouseMotionListener listener) {
        myPeer.addMouseListener(listener);
    }

    public final void addKeyListener(@Nonnull KeyListener listener) {
        myPeer.addKeyListener(listener);
    }

    /**
     * Closes and disposes the dialog and sets the specified exit code.
     *
     * @param exitCode exit code
     * @param isOk     is OK
     * @throws IllegalStateException if the dialog is invoked not on the event dispatch thread
     */
    public final void close(int exitCode, boolean isOk) {
        ensureEventDispatchThread();
        if (myClosed) {
            return;
        }
        myClosed = true;
        myExitCode = exitCode;
        Window window = getWindow();
        if (window != null && myResizeListener != null) {
            window.removeComponentListener(myResizeListener);
            myResizeListener = null;
        }

        if (isOk) {
            processDoNotAskOnOk(exitCode);
        }
        else {
            processDoNotAskOnCancel();
        }

        Disposer.dispose(myDisposable);
    }

    public final void close(int exitCode) {
        close(exitCode, exitCode != CANCEL_EXIT_CODE);
    }

    /**
     * Creates border for dialog's content pane. By default content
     * pane has has empty border with <code>(8,12,8,12)</code> insets. Subclasses can
     * return <code>null</code> for no border.
     *
     * @return content pane border
     */
    @Nullable
    protected Border createContentPaneBorder() {
        return JBUI.Borders.empty(ourDefaultBorderInsets);
    }

    @Nullable
    protected JComponent createDoNotAskCheckbox() {
        return myCheckBoxDoNotShowDialog != null && myCheckBoxDoNotShowDialog.isVisible()
            ? (JComponent) TargetAWT.to(myCheckBoxDoNotShowDialog)
            : null;
    }

    /**
     * Creates panel located at the south of the content pane. By default that
     * panel contains dialog's buttons. This default implementation uses <code>createActions()</code>
     * and <code>createJButtonForAction(Action)</code> methods to construct the panel.
     *
     * @return south panel
     */
    @Nullable
    @RequiredUIAccess
    protected JComponent createSouthPanel() {
        Action[] actions = createActions();
        Action[] leftSideActions = createLeftSideActions();
        Map<Action, JButton> buttonMap = new LinkedHashMap<>();

        boolean hasHelpToMoveToLeftSide = false;
        if (Arrays.asList(actions).contains(getHelpAction())) {
            hasHelpToMoveToLeftSide = true;
            actions = ArrayUtil.remove(actions, getHelpAction());
        }

        boolean mac = Platform.current().os().isMac();
        if (mac) {
            for (Action action : actions) {
                if (action instanceof MacOtherAction) {
                    leftSideActions = ArrayUtil.append(leftSideActions, action);
                    actions = ArrayUtil.remove(actions, action);
                    break;
                }
            }
        }

        JPanel panel = new JPanel(new BorderLayout());

        JPanel buttonsPanel = null;

        if (actions.length > 0 || leftSideActions.length > 0) {
            int gridX = 0;
            if (leftSideActions.length > 0) {
                buttonsPanel = createButtons(leftSideActions, buttonMap);
            }

            if (actions.length > 0) {
                if (mac) {
                    // move ok action to the right
                    int okNdx = ArrayUtil.indexOf(actions, getOKAction());
                    if (okNdx >= 0 && okNdx != actions.length - 1) {
                        actions = ArrayUtil.append(ArrayUtil.remove(actions, getOKAction()), getOKAction());
                    }

                    // move cancel action to the left
                    int cancelNdx = ArrayUtil.indexOf(actions, getCancelAction());
                    if (cancelNdx > 0) {
                        actions = ArrayUtil.mergeArrays(new Action[]{getCancelAction()}, ArrayUtil.remove(actions, getCancelAction()));
                    }

                    /*if (!hasFocusedAction(actions)) {
                        int ndx = ArrayUtil.find(actions, getCancelAction());
                        if (ndx >= 0) {
                            actions[ndx].putValue(FOCUSED_ACTION, Boolean.TRUE);
                        }
                    }*/
                }

                buttonsPanel = createButtons(actions, buttonMap);
            }

            myButtonMap.clear();
            myButtonMap.putAll(buttonMap);
        }

        JButton helpButton = null;
        if (hasHelpToMoveToLeftSide) {
            helpButton = new JButton(getHelpAction());
            helpButton.putClientProperty("JButton.buttonType", "help");
            helpButton.setText("");
            helpButton.setToolTipText(ActionLocalize.actionHelptopicsDescription().get());
            panel.add(helpButton, BorderLayout.WEST);
        }

        String targetButtonPosition = BorderLayout.EAST;
        switch (myButtonAlignment) {
            case SwingConstants.CENTER:
                targetButtonPosition = BorderLayout.CENTER;
                break;
            case SwingConstants.RIGHT:
                targetButtonPosition = BorderLayout.EAST;
                break;
        }

        if (buttonsPanel != null) {
            panel.add(buttonsPanel, targetButtonPosition);
        }

        DoNotAskOption askOption = myDoNotAsk;
        if (askOption != null) {
            myCheckBoxDoNotShowDialog = CheckBox.create(askOption.getDoNotShowMessage());
            myCheckBoxDoNotShowDialog.setVisible(askOption.canBeHidden());
            myCheckBoxDoNotShowDialog.setValue(!askOption.isToBeShown());

            JComponent southPanel = panel;

            if (!askOption.canBeHidden()) {
                return southPanel;
            }

            JComponent doNotAskCheckbox = createDoNotAskCheckbox();
            if (helpButton != null) {
                panel.remove(helpButton);

                panel.add(new BorderLayoutPanel().addToLeft(helpButton).addToCenter(doNotAskCheckbox), BorderLayout.WEST);
            } else {
                JPanel withCB = addDoNotShowCheckBox(southPanel, doNotAskCheckbox);
                panel = withCB;
            }
        }

        panel.setBorder(IdeBorderFactory.createEmptyBorder(JBUI.insetsTop(8)));

        TouchBarController.getInstance().setButtonActions(panel, buttonMap.values(), List.of(), null);

        return panel;
    }

    @Nonnull
    private Action[] filter(@Nonnull Action[] actions) {
        ArrayList<Action> answer = new ArrayList<>();
        for (Action action : actions) {
            if (action != null && (action != getHelpAction())) {
                answer.add(action);
            }
        }
        return answer.toArray(new Action[answer.size()]);
    }

    protected boolean shouldAddErrorNearButtons() {
        return false;
    }

    protected boolean toBeShown() {
        return !myCheckBoxDoNotShowDialog.getValueOrError();
    }

    @Nonnull
    public static JPanel addDoNotShowCheckBox(JComponent southPanel, @Nonnull JComponent checkBox) {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.add(checkBox);

        panel.add(wrapper, BorderLayout.WEST);
        panel.add(southPanel, BorderLayout.EAST);
        checkBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));

        return panel;
    }

    private boolean hasFocusedAction(@Nonnull Action[] actions) {
        for (Action action : actions) {
            if (action.getValue(FOCUSED_ACTION) != null && (Boolean)action.getValue(FOCUSED_ACTION)) {
                return true;
            }
        }

        return false;
    }

    @Nonnull
    private JPanel createButtons(@Nonnull Action[] actions, @Nonnull Map<Action, JButton> buttons) {
        if (!UISettings.getShadowInstance().ALLOW_MERGE_BUTTONS) {
            List<Action> actionList = new ArrayList<>();
            for (Action action : actions) {
                actionList.add(action);
                if (action instanceof OptionAction optionAction) {
                    Action[] options = optionAction.getOptions();
                    actionList.addAll(Arrays.asList(options));
                }
            }
            if (actionList.size() != actions.length) {
                actions = actionList.toArray(actionList.toArray(new Action[actionList.size()]));
            }
        }

        JPanel buttonsPanel = new JPanel(new HorizontalLayout(8, SwingConstants.CENTER));
        for (Action action : actions) {
            JButton button = createJButtonForAction(action);
            Object value = action.getValue(Action.MNEMONIC_KEY);
            if (value instanceof Integer integer) {
                int mnemonic = integer;
                Object name = action.getValue(Action.NAME);
                if (mnemonic == 'Y' && "Yes".equals(name)) {
                    myYesAction = action;
                }
                else if (mnemonic == 'N' && "No".equals(name)) {
                    myNoAction = action;
                }
                button.setMnemonic(mnemonic);
            }

            if (action.getValue(FOCUSED_ACTION) != null) {
                myPreferredFocusedComponent = button;
            }

            buttons.put(action, button);
            buttonsPanel.add(button, HorizontalLayout.CENTER);
        }
        return buttonsPanel;
    }

    /**
     * @param action should be registered to find corresponding JButton
     * @return button for specified action or null if it's not found
     */
    @Nullable
    protected JButton getButton(@Nonnull Action action) {
        return myButtonMap.get(action);
    }

    /**
     * Creates <code>JButton</code> for the specified action. If the button has not <code>null</code>
     * value for <code>DialogWrapper.DEFAULT_ACTION</code> key then the created button will be the
     * default one for the dialog.
     *
     * @param action action for the button
     * @return button with action specified
     * @see DialogWrapper#DEFAULT_ACTION
     */
    protected JButton createJButtonForAction(Action action) {
        JButton button;
        if (action instanceof OptionAction optionAction && UISettings.getShadowInstance().ALLOW_MERGE_BUTTONS) {
            Action[] options = optionAction.getOptions();
            button = new JBOptionButton(action, options);
            JBOptionButton eachOptionsButton = (JBOptionButton)button;
            eachOptionsButton.setOkToProcessDefaultMnemonics(false);
            eachOptionsButton.setOptionTooltipText(LocalizeValue.localizeTODO(
                "Press " + ShortcutUtil.getKeystrokeText(SHOW_OPTION_KEYSTROKE) +
                    " to expand or use a mnemonic of a contained action"
            ).get());
            myOptionsButtons.add(eachOptionsButton);

            Set<JBOptionButton.OptionInfo> infos = eachOptionsButton.getOptionInfos();
            for (final JBOptionButton.OptionInfo eachInfo : infos) {
                if (eachInfo.getMnemonic() >= 0) {
                    CustomShortcutSet sc = new CustomShortcutSet(KeyStroke.getKeyStroke(
                        "alt pressed " + (char)eachInfo.getMnemonic()
                    ));

                    new DumbAwareAction() {
                        @RequiredUIAccess
                        @Override
                        public void actionPerformed(@Nonnull AnActionEvent e) {
                            JBOptionButton buttonToActivate = eachInfo.getButton();
                            buttonToActivate.showPopup(eachInfo.getAction(), true);
                        }
                    }.registerCustomShortcutSet(sc, getPeer().getRootPane());
                }
            }

        }
        else {
            button = new JButton(action) {
                @Override
                public void updateUI() {
                    super.updateUI();

                    Action a = getAction();
                    if (a instanceof LocalizeAction localizeAction) {
                        localizeAction.updateName();

                        updateMnemonic(this, this.getText(), a);
                    }
                }

                @Override
                protected void configurePropertiesFromAction(Action a) {
                    super.configurePropertiesFromAction(a);

                    setVisibleFromAction(a);
                }

                @Override
                protected void actionPropertyChanged(Action action, String propertyName) {
                    if (DialogWrapperAction.VISIBLE.equals(propertyName)) {
                        setVisibleFromAction(action);
                    }
                    else {
                        super.actionPropertyChanged(action, propertyName);
                    }
                }

                private void setVisibleFromAction(@Nullable Action a) {
                    setVisible(!(a instanceof DialogWrapperAction visibleFromAction) || visibleFromAction.myVisible);
                }
            };
        }

        String text = button.getText();

        updateMnemonic(button, text, action);

        if (action.getValue(DEFAULT_ACTION) != null && myPeer != null && !myPeer.isHeadless()) {
            getRootPane().setDefaultButton(button);
        }
        return button;
    }

    private void updateMnemonic(JButton button, String text, Action action) {
        if (text == null) {
            return;
        }

        int mnemonic = 0;
        StringBuilder plainText = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '_' || ch == '&') {
                i++;
                if (i >= text.length()) {
                    break;
                }
                ch = text.charAt(i);
                if (ch != '_' && ch != '&') {
                    // Mnemonic is case insensitive.
                    int vk = ch;
                    if (vk >= 'a' && vk <= 'z') {
                        vk -= 'a' - 'A';
                    }
                    mnemonic = vk;
                }
            }
            plainText.append(ch);
        }
        button.setText(plainText.toString());
        Object name = action.getValue(Action.NAME);
        if (mnemonic == KeyEvent.VK_Y && "Yes".equals(name)) {
            myYesAction = action;
        }
        else if (mnemonic == KeyEvent.VK_N && "No".equals(name)) {
            myNoAction = action;
        }

        button.setMnemonic(mnemonic);
    }

    protected DialogWrapperPeer createPeer(@Nonnull Component parent, boolean canBeParent) {
        return DialogWrapperPeerFactory.getInstance().createPeer(this, parent, canBeParent);
    }

    /**
     * Dialogs with no parents are discouraged.
     * Instead, use e.g. {@link DialogWrapper#createPeer(Window, boolean, boolean)}
     */
    @Deprecated
    protected DialogWrapperPeer createPeer(boolean canBeParent, boolean applicationModalIfPossible) {
        return createPeer(null, canBeParent, applicationModalIfPossible);
    }

    protected DialogWrapperPeer createPeer(
        consulo.ui.Window owner,
        boolean canBeParent,
        IdeModalityType ideModalityType
    ) {
        return DialogWrapperPeerFactory.getInstance().createPeer(this, owner, canBeParent, ideModalityType);
    }

    @Deprecated
    protected DialogWrapperPeer createPeer(
        ComponentManager project,
        boolean canBeParent,
        boolean applicationModalIfPossible
    ) {
        return DialogWrapperPeerFactory.getInstance().createPeer(
            this,
            project,
            canBeParent,
            applicationModalIfPossible ? IdeModalityType.IDE : IdeModalityType.PROJECT
        );
    }

    protected DialogWrapperPeer createPeer(
        @Nullable ComponentManager project,
        boolean canBeParent,
        IdeModalityType ideModalityType
    ) {
        return DialogWrapperPeerFactory.getInstance().createPeer(this, project, canBeParent, ideModalityType);
    }

    protected DialogWrapperPeer createPeer(@Nullable ComponentManager project, boolean canBeParent) {
        return DialogWrapperPeerFactory.getInstance().createPeer(this, project, canBeParent);
    }

    @Nullable
    protected JComponent createTitlePane() {
        return null;
    }

    /**
     * Factory method. It creates the panel located at the
     * north of the dialog's content pane. The implementation can return <code>null</code>
     * value. In this case there will be no input panel.
     *
     * @return north panel
     */
    @Nullable
    protected JComponent createNorthPanel() {
        return null;
    }

    /**
     * Factory method. It creates panel with dialog options. Options panel is located at the
     * center of the dialog's content pane. The implementation can return <code>null</code>
     * value. In this case there will be no options panel.
     *
     * @return center panel
     */
    @Nullable
    protected abstract JComponent createCenterPanel();

    /**
     * @see Window#toFront()
     */
    public void toFront() {
        myPeer.toFront();
    }

    /**
     * @see Window#toBack()
     */
    public void toBack() {
        myPeer.toBack();
    }

    protected boolean setAutoAdjustable(boolean autoAdjustable) {
        JRootPane rootPane = getRootPane();
        if (rootPane == null) {
            return false;
        }
        rootPane.putClientProperty(NO_AUTORESIZE, autoAdjustable ? null : Boolean.TRUE);
        return true;
    }

    //true by default
    public boolean isAutoAdjustable() {
        JRootPane rootPane = getRootPane();
        return rootPane == null || rootPane.getClientProperty(NO_AUTORESIZE) == null;
    }

    /**
     * Dispose the wrapped and releases all resources allocated be the wrapper to help
     * more effecient garbage collection. You should never invoke this method twice or
     * invoke any method of the wrapper after invocation of <code>dispose</code>.
     *
     * @throws IllegalStateException if the dialog is disposed not on the event dispatch thread
     */
    protected void dispose() {
        ensureEventDispatchThread();
        myErrorTextAlarm.cancelAllRequests();
        myValidationAlarm.cancelAllRequests();
        myDisposed = true;
        if (myButtonMap != null) {
            for (JButton button : myButtonMap.values()) {
                button.setAction(null); // avoid memory leak via KeyboardManager
            }
            myButtonMap.clear();
        }

        JRootPane rootPane = getRootPane();
        // if rootPane = null, dialog has already been disposed
        if (rootPane != null) {
            unregisterKeyboardActions(rootPane);
            if (myActualSize != null && isAutoAdjustable()) {
                setSize(myActualSize.width, myActualSize.height);
            }
            myPeer.dispose();
        }
    }

    public static void cleanupRootPane(@Nullable JRootPane rootPane) {
        if (rootPane == null) {
            return;
        }
        // Must be preserved:
        //   Component#appContext, Component#appContext, Container#component
        //   JRootPane#contentPane due to popup recycling & our border styling
        // Must be cleared:
        //   JComponent#clientProperties, contentPane children
        RepaintManager.currentManager(rootPane).removeInvalidComponent(rootPane);
        unregisterKeyboardActions(rootPane);
        Container contentPane = rootPane.getContentPane();
        if (contentPane != null) {
            contentPane.removeAll();
        }
        if (rootPane.getGlassPane() instanceof IdeGlassPane && rootPane.getClass() == JRootPane.class) {
            rootPane.setGlassPane(new JPanel()); // resizeable AbstractPopup but not DialogWrapperPeer
        }

        JComponentHacking.setClientProperties(rootPane, null);
    }

    public static void unregisterKeyboardActions(Component rootPane) {
        int[] flags = {
            JComponent.WHEN_FOCUSED,
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
            JComponent.WHEN_IN_FOCUSED_WINDOW
        };
        for (JComponent eachComp : UIUtil.uiTraverser(rootPane).traverse().filter(JComponent.class)) {
            ActionMap actionMap = eachComp.getActionMap();
            if (actionMap == null) {
                continue;
            }
            for (KeyStroke eachStroke : eachComp.getRegisteredKeyStrokes()) {
                boolean remove = true;
                for (int i : flags) {
                    Object key = eachComp.getInputMap(i).get(eachStroke);
                    Action action = key == null ? null : actionMap.get(key);
                    if (action instanceof UIResource) {
                        remove = false;
                    }
                }
                if (remove) {
                    eachComp.unregisterKeyboardAction(eachStroke);
                }
            }
        }
    }

    public static void cleanupWindowListeners(@Nullable Window window) {
        if (window == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            for (WindowListener listener : window.getWindowListeners()) {
                if (listener.getClass().getName().startsWith("com.intellij.")) {
                    //LOGGER.warn("Stale listener: " + listener);
                    window.removeWindowListener(listener);
                }
            }
        });
    }

    /**
     * This method is invoked by default implementation of "Cancel" action. It just closes dialog
     * with <code>CANCEL_EXIT_CODE</code>. This is convenient place to override functionality of "Cancel" action.
     * Note that the method does nothing if "Cancel" action isn't enabled.
     */
    public void doCancelAction() {
        processDoNotAskOnCancel();

        if (getCancelAction().isEnabled()) {
            close(CANCEL_EXIT_CODE);
        }
    }

    private void processDoNotAskOnCancel() {
        if (myDoNotAsk != null && myDoNotAsk.shouldSaveOptionsOnCancel() && myDoNotAsk.canBeHidden()) {
            myDoNotAsk.setToBeShown(toBeShown(), CANCEL_EXIT_CODE);
        }
    }

    /**
     * You can use this method if you want to know by which event this actions got triggered. It is called only if
     * the cancel action was triggered by some input event, <code>doCancelAction</code> is called otherwise.
     *
     * @param source AWT event
     * @see #doCancelAction
     */
    public void doCancelAction(AWTEvent source) {
        doCancelAction();
    }

    /**
     * Programmatically perform a "click" of default dialog's button. The method does
     * nothing if the dialog has no default button.
     */
    public void clickDefaultButton() {
        JButton button = getRootPane().getDefaultButton();
        if (button != null) {
            button.doClick();
        }
    }

    public void doOKActionPublic() {
        doOKAction();
    }

    /**
     * This method is invoked by default implementation of "OK" action. It just closes dialog
     * with <code>OK_EXIT_CODE</code>. This is convenient place to override functionality of "OK" action.
     * Note that the method does nothing if "OK" action isn't enabled.
     */
    protected void doOKAction() {
        processDoNotAskOnOk(OK_EXIT_CODE);

        if (getOKAction().isEnabled()) {
            close(OK_EXIT_CODE);
        }
    }

    protected void processDoNotAskOnOk(int exitCode) {
        if (myDoNotAsk != null && myDoNotAsk.canBeHidden()) {
            myDoNotAsk.setToBeShown(toBeShown(), exitCode);
        }
    }

    /**
     * @return whether the native window cross button closes the window or not.
     * <code>true</code> means that cross performs hide or dispose of the dialog.
     */
    public boolean shouldCloseOnCross() {
        return myCrossClosesWindow;
    }

    /**
     * Creates actions for dialog.
     * <p>
     * By default "OK" and "Cancel" actions are returned. The "Help" action is automatically added if
     * {@link #getHelpId()} returns non-null value.
     * <p>
     * Each action is represented by <code>JButton</code> created by {@link #createJButtonForAction(Action)}.
     * These buttons are then placed into {@link #createSouthPanel() south panel} of dialog.
     *
     * @return dialog actions
     * @see #createSouthPanel
     * @see #createJButtonForAction
     */
    @Nonnull
    protected Action[] createActions() {
        if (getHelpId() == null) {
            if (Platform.current().os().isMac()) {
                return new Action[]{getCancelAction(), getOKAction()};
            }

            return new Action[]{getOKAction(), getCancelAction()};
        }
        else {
            if (Platform.current().os().isMac()) {
                return new Action[]{getHelpAction(), getCancelAction(), getOKAction()};
            }
            return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
        }
    }

    @Nonnull
    protected Action[] createLeftSideActions() {
        return new Action[0];
    }

    /**
     * @return default implementation of "OK" action. This action just invokes
     * <code>doOKAction()</code> method.
     * @see #doOKAction
     */
    @Nonnull
    protected LocalizeAction getOKAction() {
        return myOKAction;
    }

    /**
     * @return default implementation of "Cancel" action. This action just invokes
     * <code>doCancelAction()</code> method.
     * @see #doCancelAction
     */
    @Nonnull
    protected LocalizeAction getCancelAction() {
        return myCancelAction;
    }

    /**
     * @return default implementation of "Help" action. This action just invokes
     * <code>doHelpAction()</code> method.
     * @see #doHelpAction
     */
    @Nonnull
    protected LocalizeAction getHelpAction() {
        return myHelpAction;
    }

    protected boolean isProgressDialog() {
        return false;
    }

    public final boolean isModalProgress() {
        return isProgressDialog();
    }

    /**
     * Returns content pane
     *
     * @return content pane
     * @see JDialog#getContentPane
     */
    public Container getContentPane() {
        assert myPeer != null;
        return myPeer.getContentPane();
    }

    /**
     * @see JDialog#validate
     */
    public void validate() {
        myPeer.validate();
    }

    /**
     * @see JDialog#repaint
     */
    public void repaint() {
        myPeer.repaint();
    }

    /**
     * Returns key for persisting dialog dimensions.
     * <p>
     * Default implementation returns <code>null</code> (no persisting).
     *
     * @return dimension service key
     */
    @Nullable
    protected String getDimensionServiceKey() {
        return null;
    }

    @Nullable
    public final String getDimensionKey() {
        return getDimensionServiceKey();
    }

    public int getExitCode() {
        return myExitCode;
    }

    /**
     * @return component which should be focused when the dialog appears
     * on the screen.
     */
    @Nullable
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return Platform.current().os().isMac() ? myPreferredFocusedComponent : null;
    }

    /**
     * @return horizontal stretch of the dialog. It means that the dialog's horizontal size is
     * the product of horizontal stretch by horizontal size of packed dialog. The default value
     * is <code>1.0f</code>
     */
    public final float getHorizontalStretch() {
        return myHorizontalStretch;
    }

    /**
     * @return vertical stretch of the dialog. It means that the dialog's vertical size is
     * the product of vertical stretch by vertical size of packed dialog. The default value
     * is <code>1.0f</code>
     */
    public final float getVerticalStretch() {
        return myVerticalStretch;
    }

    protected final void setHorizontalStretch(float hStretch) {
        myHorizontalStretch = hStretch;
    }

    protected final void setVerticalStretch(float vStretch) {
        myVerticalStretch = vStretch;
    }

    /**
     * @return window owner
     * @see Window#getOwner
     */
    public Window getOwner() {
        return myPeer.getOwner();
    }

    public Window getWindow() {
        return myPeer.getWindow();
    }

    public JComponent getContentPanel() {
        return (JComponent)myPeer.getContentPane();
    }

    /**
     * @return root pane
     * @see JDialog#getRootPane
     */
    public JRootPane getRootPane() {
        return myPeer.getRootPane();
    }

    /**
     * @return dialog size
     * @see Window#getSize
     */
    public Dimension getSize() {
        return myPeer.getSize();
    }

    /**
     * @return dialog title
     * @see Dialog#getTitle
     */
    public String getTitle() {
        return myPeer.getTitle();
    }

    @Deprecated(forRemoval = true)
    @DeprecationInfo("Hack. will be removed later")
    public void initPublic() {
        init();
    }

    protected void init() {
        myErrorText = new ErrorText(getErrorTextAlignment());
        myErrorText.setVisible(false);
        ComponentAdapter resizeListener = new ComponentAdapter() {
            private int myHeight;

            @Override
            public void componentResized(ComponentEvent event) {
                int height = !myErrorText.isVisible() ? 0 : event.getComponent().getHeight();
                if (height != myHeight) {
                    myHeight = height;
                    myResizeInProgress = true;
                    myErrorText.setMinimumSize(new Dimension(0, height));
                    JRootPane root = myPeer.getRootPane();
                    if (root != null) {
                        root.validate();
                    }
                    if (myActualSize != null && !shouldAddErrorNearButtons()) {
                        myPeer.setSize(myActualSize.width, myActualSize.height + height);
                    }
                    myErrorText.revalidate();
                    myResizeInProgress = false;
                }
            }
        };
        myErrorText.myLabel.addComponentListener(resizeListener);
        Disposer.register(myDisposable, () -> myErrorText.myLabel.removeComponentListener(resizeListener));

        JPanel root = new JPanel(createRootLayout());
        myPeer.setContentPane(root);

        CustomShortcutSet sc = new CustomShortcutSet(SHOW_OPTION_KEYSTROKE);
        AnAction toggleShowOptions = new DumbAwareAction() {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                expandNextOptionButton();
            }
        };
        toggleShowOptions.registerCustomShortcutSet(sc, root);

        initRootPanel(root);

        if (myErrorPane == null) {
            myErrorPane = root;
        }

        MnemonicHelper.init(root);
        if (!postponeValidation()) {
            startTrackingValidation();
        }
        if (Platform.current().os().isWindows()) {
            installEnterHook(root);
        }
    }

    protected void initRootPanel(@Nonnull JPanel root) {
        JPanel northSection = new JPanel(new BorderLayout());
        root.add(northSection, BorderLayout.NORTH);

        JComponent titlePane = createTitlePane();
        if (titlePane != null) {
            northSection.add(titlePane, BorderLayout.CENTER);
        }

        JComponent centerSection = new JPanel(new BorderLayout());
        root.add(centerSection, BorderLayout.CENTER);

        root.setBorder(createContentPaneBorder());

        JComponent n = createNorthPanel();
        if (n != null) {
            centerSection.add(n, BorderLayout.NORTH);
        }

        JComponent c = createCenterPanel();
        if (c != null) {
            centerSection.add(c, BorderLayout.CENTER);
            myErrorPane = c;
        }

        if (myCreateSouthSection) {
            JPanel southSection = new JPanel(new BorderLayout());
            root.add(southSection, BorderLayout.SOUTH);

            southSection.add(myErrorText, BorderLayout.CENTER);
            JComponent south = createSouthPanel();
            if (south != null) {
                southSection.add(south, BorderLayout.SOUTH);
            }
        }

        myErrorTextAlarm.setActivationComponent(root);
    }

    protected int getErrorTextAlignment() {
        return SwingConstants.LEADING;
    }

    protected LayoutManager createRootLayout() {
        return new BorderLayout();
    }

    protected static void installEnterHook(JComponent root) {
        new DumbAwareAction() {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (owner instanceof JButton button && owner.isEnabled()) {
                    try {
                        consulo.ui.Component uiComponent = TargetAWT.from(button);

                        if (uiComponent instanceof Button uiButton) {
                            uiButton.invoke(Objects.requireNonNull(e.getInputDetails()));
                            return;
                        }
                    }
                    catch (Exception ignored) {
                    }

                    button.doClick();
                }
            }

            @Override
            public void update(@Nonnull AnActionEvent e) {
                Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                e.getPresentation().setEnabled(owner instanceof JButton && owner.isEnabled());
            }
        }.registerCustomShortcutSet(CustomShortcutSet.fromString("ENTER"), root);
    }

    protected void expandNextOptionButton() {
        if (myCurrentOptionsButtonIndex > 0) {
            myOptionsButtons.get(myCurrentOptionsButtonIndex).closePopup();
            myCurrentOptionsButtonIndex++;
        }
        else if (!myOptionsButtons.isEmpty()) {
            myCurrentOptionsButtonIndex = 0;
        }

        if (myCurrentOptionsButtonIndex >= 0 && myCurrentOptionsButtonIndex < myOptionsButtons.size()) {
            myOptionsButtons.get(myCurrentOptionsButtonIndex).showPopup(null, true);
        }
    }

    void startTrackingValidation() {
        SwingUtilities.invokeLater(() -> {
            if (!myValidationStarted && !myDisposed) {
                myValidationStarted = true;
                initValidation();
            }
        });
    }

    protected final void initValidation() {
        myValidationAlarm.cancelAllRequests();
        Runnable validateRequest = () -> {
            if (myDisposed) {
                return;
            }
            List<ValidationInfo> result = doValidateAll();
            if (!result.isEmpty()) {
                installErrorPainter();
            }
            myErrorPainter.setValidationInfo(result);
            updateErrorInfo(result);

            if (!myDisposed) {
                initValidation();
            }
        };

        if (getValidationThreadToUse() == Alarm.ThreadToUse.SWING_THREAD) {
            // null if headless
            JRootPane rootPane = getRootPane();
            //noinspection deprecation
            myValidationAlarm.addRequest(
                validateRequest,
                myValidationDelay,
                ApplicationManager.getApplication() == null
                    ? null
                    : rootPane == null
                    ? Application.get().getCurrentModalityState()
                    : Application.get().getModalityStateForComponent(rootPane)
            );
        }
        else {
            myValidationAlarm.addRequest(validateRequest, myValidationDelay);
        }
    }

    protected boolean isNorthStrictedToPreferredSize() {
        return true;
    }

    protected boolean isCenterStrictedToPreferredSize() {
        return false;
    }

    protected boolean isSouthStrictedToPreferredSize() {
        return true;
    }

    protected JComponent createContentPane() {
        return new JPanel();
    }

    /**
     * @see Window#pack
     */
    public void pack() {
        myPeer.pack();
    }

    public Dimension getPreferredSize() {
        return myPeer.getPreferredSize();
    }

    /**
     * Sets horizontal alignment of dialog's buttons.
     *
     * @param alignment alignment of the buttons. Acceptable values are
     *                  <code>SwingConstants.CENTER</code> and <code>SwingConstants.RIGHT</code>.
     *                  The <code>SwingConstants.RIGHT</code> is the default value.
     * @throws IllegalArgumentException if <code>alignment</code> isn't acceptable
     */
    public final void setButtonsAlignment(@MagicConstant(intValues = {SwingConstants.CENTER, SwingConstants.RIGHT}) int alignment) {
        if (SwingConstants.CENTER != alignment && SwingConstants.RIGHT != alignment) {
            throw new IllegalArgumentException("unknown alignment: " + alignment);
        }
        myButtonAlignment = alignment;
    }

    public final void setCrossClosesWindow(boolean crossClosesWindow) {
        myCrossClosesWindow = crossClosesWindow;
    }

    protected final void setCancelButtonIcon(Icon icon) {
        // Setting icons causes buttons be 'square' style instead of
        // 'rounded', which is expected by apple users.
        if (!Platform.current().os().isMac()) {
            myCancelAction.putValue(Action.SMALL_ICON, icon);
        }
    }

    @Deprecated
    @DeprecationInfo("setCancelButtonText(LocalizeValue)")
    protected final void setCancelButtonText(String text) {
        myCancelAction.putValue(Action.NAME, text);
    }

    protected final void setCancelButtonText(@Nonnull LocalizeValue actionText) {
        myCancelAction.setText(actionText);
    }

    public void setModal(boolean modal) {
        myPeer.setModal(modal);
    }

    public boolean isModal() {
        return myPeer.isModal();
    }

    public void setOKActionEnabled(boolean isEnabled) {
        myOKAction.setEnabled(isEnabled);
    }

    protected final void setOKButtonIcon(Icon icon) {
        // Setting icons causes buttons be 'square' style instead of
        // 'rounded', which is expected by apple users.
        if (!Platform.current().os().isMac()) {
            myOKAction.putValue(Action.SMALL_ICON, icon);
        }
    }

    /**
     * @param text action without mnemonic. If mnemonic is set, presentation would be shifted by one to the left
     *             {@link AbstractButton#setText(String)}
     *             {@link AbstractButton#updateDisplayedMnemonicIndex(String, int)}
     */
    @Deprecated
    @DeprecationInfo("setOKButtonText(LocalizeValue)")
    protected final void setOKButtonText(String text) {
        myOKAction.putValue(Action.NAME, text);
    }

    @Deprecated
    @DeprecationInfo("setOKButtonText(LocalizeValue)")
    protected final void setOKButtonMnemonic(int c) {
        myOKAction.putValue(Action.MNEMONIC_KEY, c);
    }

    protected final void setOKButtonText(LocalizeValue actionText) {
        myOKAction.setText(actionText);
    }

    /**
     * @return the help identifier or null if no help is available.
     */
    @Nullable
    protected String getHelpId() {
        return null;
    }

    /**
     * Invoked by default implementation of "Help" action.
     * Note that the method does nothing if "Help" action isn't enabled.
     * <p>
     * The default implementation shows the help page with id returned
     * by {@link #getHelpId()}. If that method returns null,
     * a message box with message "no help available" is shown.
     */
    @RequiredUIAccess
    protected void doHelpAction() {
        if (myHelpAction.isEnabled()) {
            String helpId = getHelpId();
            if (helpId != null) {
                HelpManager.getInstance().invokeHelp(helpId);
            }
            else {
                Alerts.okInfo(UILocalize.thereIsNoHelpForThisDialogErrorMessage())
                    .title(UILocalize.noHelpAvailableDialogTitle())
                    .showAsync();
            }
        }
    }

    public boolean isOK() {
        return getExitCode() == OK_EXIT_CODE;
    }

    public boolean isOKActionEnabled() {
        return myOKAction.isEnabled();
    }

    /**
     * @return <code>true</code> if and only if visible
     * @see Component#isVisible
     */
    public boolean isVisible() {
        return myPeer.isVisible();
    }

    /**
     * @return <code>true</code> if and only if showing
     * @see Window#isShowing
     */
    public boolean isShowing() {
        return myPeer.isShowing();
    }

    public void setScalableSize(int width, int height) {
        setSize(JBUI.scale(width), JBUI.scale(height));
    }

    /**
     * @param width  width
     * @param height height
     * @see JDialog#setSize
     */
    public void setSize(int width, int height) {
        myPeer.setSize(width, height);
    }

    /**
     * @param title title
     * @see JDialog#setTitle
     */
    @Deprecated
    public void setTitle(String title) {
        myPeer.setTitle(title);
    }

    /**
     * @param title title
     * @see JDialog#setTitle
     */
    public void setTitle(LocalizeValue title) {
        myPeer.setTitle(title.get());
    }

    /**
     * @see JDialog#isResizable
     */
    public void isResizable() {
        myPeer.isResizable();
    }

    /**
     * @param resizable is resizable
     * @see JDialog#setResizable
     */
    public void setResizable(boolean resizable) {
        myPeer.setResizable(resizable);
    }

    /**
     * @return dialog location
     * @see JDialog#getLocation
     */
    public Point getLocation() {
        return myPeer.getLocation();
    }

    /**
     * @param p new dialog location
     * @see JDialog#setLocation(Point)
     */
    public void setLocation(Point p) {
        myPeer.setLocation(p);
    }

    /**
     * @param x x
     * @param y y
     * @see JDialog#setLocation(int, int)
     */
    public void setLocation(int x, int y) {
        myPeer.setLocation(x, y);
    }

    public void centerRelativeToParent() {
        myPeer.centerInParent();
    }

    /**
     * Show the dialog
     *
     * @throws IllegalStateException if the dialog is invoked not on the event dispatch thread
     */
    @Deprecated
    @DeprecationInfo("Use async version")
    @RequiredUIAccess
    public void show() {
        showAndGetOk();
    }

    @Deprecated
    @DeprecationInfo("Use async version")
    @RequiredUIAccess
    public boolean showAndGet() {
        show();
        return isOK();
    }

    /**
     * You need this method ONLY for NON-MODAL dialogs. Otherwise, use {@link #show()} or {@link #showAndGet()}.
     *
     * @return result callback
     */
    @Nonnull
    @RequiredUIAccess
    public AsyncResult<Boolean> showAndGetOk() {
        AsyncResult<Boolean> result = new AsyncResult<>();

        ensureEventDispatchThread();
        registerKeyboardShortcuts();


        Disposable uiParent = Disposer.get("ui");
        if (uiParent != null) { // may be null if no app yet (license agreement)
            Disposer.register(uiParent, myDisposable); // ensure everything is disposed on app quit
        }

        Disposer.register(myDisposable, () -> result.setDone(isOK()));

        myPeer.show();

        return result;
    }

    @Nonnull
    @RequiredUIAccess
    public AsyncResult<Void> showAsync() {
        UIAccess uiAccess = UIAccess.current();

        AsyncResult<Void> result = AsyncResult.undefined();
        showInternal().doWhenProcessed(() -> {
            if (isOK()) {
                result.setDone();
            }
            else {
                result.setRejected();
            }
        });
        return result;
    }

    private AsyncResult<Void> showInternal() {
        ensureEventDispatchThread();

        registerKeyboardShortcuts();

        Disposable uiParent = Disposer.get("ui");
        if (uiParent != null) {
            Disposer.register(uiParent, myDisposable); // ensure everything is disposed on app quit
        }
        return myPeer.showAsync();
    }

    /**
     * @return Location in absolute coordinates which is used when dialog has no dimension service key
     * or no position was stored yet.
     * Can return null. In that case dialog will be centered relative to its owner.
     */
    @Nullable
    public Point getInitialLocation() {
        return myInitialLocationCallback == null ? null : myInitialLocationCallback.get();
    }

    public void setInitialLocationCallback(Supplier<Point> callback) {
        myInitialLocationCallback = callback;
    }

    private void registerKeyboardShortcuts() {
        JRootPane rootPane = getRootPane();

        if (rootPane == null) {
            return;
        }

        ActionListener cancelKeyboardAction = createCancelAction();
        if (cancelKeyboardAction != null) {
            rootPane.registerKeyboardAction(
                cancelKeyboardAction,
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
            );
            ActionUtil.registerForEveryKeyboardShortcut(
                getRootPane(),
                cancelKeyboardAction,
                CommonShortcuts.getCloseActiveWindow()
            );
        }
        registerForEveryKeyboardShortcut(cancelKeyboardAction, CommonShortcuts.getCloseActiveWindow());

        ActionListener helpAction = e -> doHelpAction();

        registerForEveryKeyboardShortcut(helpAction, CommonShortcuts.getContextHelp());
        rootPane.registerKeyboardAction(
            helpAction,
            KeyStroke.getKeyStroke(KeyEvent.VK_HELP, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        if (myButtonMap != null) {
            rootPane.registerKeyboardAction(
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        focusPreviousButton();
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
            );
            rootPane.registerKeyboardAction(
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        focusNextButton();
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
            );
        }

        if (myYesAction != null) {
            rootPane.registerKeyboardAction(
                myYesAction,
                KeyStroke.getKeyStroke(KeyEvent.VK_Y, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
            );
        }

        if (myNoAction != null) {
            rootPane.registerKeyboardAction(
                myNoAction,
                KeyStroke.getKeyStroke(KeyEvent.VK_N, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
            );
        }
    }

    private void registerForEveryKeyboardShortcut(ActionListener action, @Nonnull ShortcutSet shortcuts) {
        for (Shortcut shortcut : shortcuts.getShortcuts()) {
            if (shortcut instanceof KeyboardShortcut ks) {
                KeyStroke first = ks.getFirstKeyStroke();
                KeyStroke second = ks.getSecondKeyStroke();
                if (second == null) {
                    getRootPane().registerKeyboardAction(action, first, JComponent.WHEN_IN_FOCUSED_WINDOW);
                }
            }
        }
    }

    /**
     * @return null if we should ignore <Esc> for window closing
     */
    @Nullable
    protected ActionListener createCancelAction() {
        return e -> {
            if (!InternalPopupUtil.handleEscKeyEvent()) {
                doCancelAction(e);
            }
        };
    }

    private void focusPreviousButton() {
        JButton[] myButtons = new ArrayList<>(myButtonMap.values()).toArray(new JButton[0]);
        for (int i = 0; i < myButtons.length; i++) {
            if (myButtons[i].hasFocus()) {
                if (i == 0) {
                    myButtons[myButtons.length - 1].requestFocus();
                    return;
                }
                myButtons[i - 1].requestFocus();
                return;
            }
        }
    }

    private void focusNextButton() {
        JButton[] myButtons = new ArrayList<>(myButtonMap.values()).toArray(new JButton[0]);
        for (int i = 0; i < myButtons.length; i++) {
            if (myButtons[i].hasFocus()) {
                if (i == myButtons.length - 1) {
                    IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myButtons[0]);
                    return;
                }
                IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myButtons[i + 1]);
                return;
            }
        }
    }

    public long getTypeAheadTimeoutMs() {
        return 0l;
    }

    public boolean isToDispatchTypeAhead() {
        return isOK();
    }

    public static boolean isMultipleModalDialogs() {
        Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (c != null) {
            DialogWrapper wrapper = findInstance(c);
            return wrapper != null && wrapper.getPeer().getCurrentModalEntities().length > 1;
        }
        return false;
    }

    /**
     * Base class for dialog wrapper actions that need to ensure that only
     * one action for the dialog is running.
     */
    protected abstract class DialogWrapperAction extends LocalizeAction {
        public static final String VISIBLE = "actionVisible";

        private boolean myVisible = true;

        @Deprecated
        @DeprecationInfo("Use constructor with LocalizeValue")
        protected DialogWrapperAction(String name) {
            super(LocalizeValue.of(name));
        }

        /**
         * The constructor
         *
         * @param name the action name (see {@link Action#NAME})
         */
        protected DialogWrapperAction(LocalizeValue localizeValue) {
            super(localizeValue);
        }

        public void setVisible(boolean visible) {
            if (myVisible == visible) {
                return;
            }

            myVisible = visible;

            firePropertyChange(VISIBLE, !myVisible, myVisible);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            if (myClosed || myPerformAction) {
                return;
            }

            try {
                myPerformAction = true;
                doAction(e);
            }
            finally {
                myPerformAction = false;
            }
        }

        /**
         * Do actual work for the action. This method is called only if no other action
         * is performed in parallel (checked using {@link DialogWrapper#myPerformAction}),
         * and dialog is active (checked using {@link DialogWrapper#myClosed})
         *
         * @param e action
         */
        protected abstract void doAction(ActionEvent e);
    }

    protected class OkAction extends DialogWrapperAction {
        public OkAction() {
            super(CommonLocalize.buttonOk());
            putValue(DEFAULT_ACTION, Boolean.TRUE);
        }

        @Override
        @RequiredUIAccess
        protected void doAction(ActionEvent e) {
            ValidationInfo info = doValidate();
            if (info != null) {
                if (info.component != null && info.component.isVisible()) {
                    ApplicationIdeFocusManager.getInstance().requestFocus(info.component, true);
                }
                DialogEarthquakeShaker.shake((JDialog)getPeer().getWindow());
                startTrackingValidation();
                return;
            }
            doOKAction();
        }
    }

    protected class CancelAction extends DialogWrapperAction {
        private CancelAction() {
            super(CommonLocalize.buttonCancel());
        }

        @Override
        protected void doAction(ActionEvent e) {
            doCancelAction();
        }
    }

    /**
     * The action that just closes dialog with the specified exit code
     * (like the default behavior of the actions "Ok" and "Cancel").
     */
    protected class DialogWrapperExitAction extends DialogWrapperAction {
        /**
         * The exit code for the action
         */
        protected final int myExitCode;

        /**
         * The constructor
         *
         * @param name     the action name
         * @param exitCode the exit code for dialog
         */
        public DialogWrapperExitAction(String name, int exitCode) {
            super(name);
            myExitCode = exitCode;
        }

        @Override
        protected void doAction(ActionEvent e) {
            if (isEnabled()) {
                close(myExitCode);
            }
        }
    }

    private class HelpAction extends LocalizeAction {
        private HelpAction() {
            super(CommonLocalize.buttonHelp());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(ActionEvent e) {
            doHelpAction();
        }
    }

    public void clearErrorText() {
        setErrorInfoAll(List.of());
    }

    /**
     * Don't override this method. It is not final for the API compatibility.
     * It will not be called by the DialogWrapper's validator.
     * Use this method only in circumstances when the exact invalid component is hard to
     * detect or the valid status is based on several fields. In other cases use
     * <code>{@link #setErrorText(String, JComponent)}</code> method.
     *
     * @param text the error text to display
     */
    public void setErrorText(@Nonnull LocalizeValue text) {
        setErrorText(text, null);
    }

    /**
     * Don't override this method. It is not final for the API compatibility.
     * It will not be called by the DialogWrapper's validator.
     * Use this method only in circumstances when the exact invalid component is hard to
     * detect or the valid status is based on several fields. In other cases use
     * <code>{@link #setErrorText(String, JComponent)}</code> method.
     *
     * @param text the error text to display
     */
    @Deprecated
    @DeprecationInfo("Use setErrorText(LocalizeValue)")
    public void setErrorText(@Nullable String text) {
        setErrorText(text, null);
    }

    public void setErrorText(@Nonnull LocalizeValue text, @Nullable JComponent component) {
        setErrorInfoAll(
            text == LocalizeValue.empty()
                ? Collections.EMPTY_LIST
                : Collections.singletonList(new ValidationInfo(text, component))
        );
    }

    @Deprecated
    @DeprecationInfo("Use setErrorText(LocalizeValue, JComponent)")
    public void setErrorText(@Nullable String text, @Nullable JComponent component) {
        setErrorText(LocalizeValue.ofNullable(text), component);
    }

    protected void setErrorInfoAll(@Nonnull List<ValidationInfo> info) {
        if (myInfo.equals(info)) {
            return;
        }

        myErrorTextAlarm.cancelAllRequests();
        SwingUtilities.invokeLater(() -> myErrorText.clearError());

        List<ValidationInfo> corrected = myInfo.stream().filter((vi) -> !info.contains(vi)).collect(Collectors.toList());
        if (Registry.is("ide.inplace.errors.outline")) {
            corrected.stream()
                .filter(vi -> (vi.component != null && vi.component.getBorder() instanceof ErrorBorderCapable))
                .forEach(vi -> vi.component.putClientProperty("JComponent.error.outline", false));
        }

        if (Registry.is("ide.inplace.errors.balloon")) {
            corrected.stream().filter(vi -> vi.component != null).forEach(vi -> {
                vi.component.putClientProperty("JComponent.error.balloon.builder", null);

                Balloon balloon = (Balloon)vi.component.getClientProperty("JComponent.error.balloon");
                if (balloon != null && !balloon.isDisposed()) {
                    balloon.hide();
                }

                Component fc = getFocusable(vi.component);
                if (fc != null) {
                    for (FocusListener fl : fc.getFocusListeners()) {
                        if (fl instanceof ErrorFocusListener) {
                            fc.removeFocusListener(fl);
                        }
                    }
                }
            });
        }

        myInfo = info;

        if (Registry.is("ide.inplace.errors.outline")) {
            myInfo.stream()
                .filter(vi -> (vi.component != null && vi.component.getBorder() instanceof ErrorBorderCapable))
                .forEach(vi -> vi.component.putClientProperty("JComponent.error.outline", true));
        }

        if (Registry.is("ide.inplace.errors.balloon") && !myInfo.isEmpty()) {
            for (ValidationInfo vi : myInfo) {
                Component fc = getFocusable(vi.component);
                if (fc != null && fc.isFocusable()) {
                    if (vi.component.getClientProperty("JComponent.error.balloon.builder") == null) {
                        JLabel label = new JLabel();
                        label.setHorizontalAlignment(SwingConstants.LEADING);
                        setErrorTipText(vi.component, label, vi.message);

                        BalloonBuilder balloonBuilder = JBPopupFactory.getInstance()
                            .createBalloonBuilder(label)
                            .setDisposable(getDisposable())
                            .setBorderInsets(UIManager.getInsets("Balloon.error.textInsets"))
                            .setPointerSize(new JBDimension(17, 6))
                            .setCornerToPointerDistance(JBUI.scale(30))
                            .setHideOnKeyOutside(false)
                            .setHideOnClickOutside(false)
                            .setHideOnAction(false)
                            .setBorderColor(BALLOON_BORDER)
                            .setFillColor(BALLOON_BACKGROUND)
                            .setHideOnFrameResize(false)
                            .setRequestFocus(false)
                            .setAnimationCycle(100)
                            .setShadow(true);

                        vi.component.putClientProperty("JComponent.error.balloon.builder", balloonBuilder);

                        ErrorFocusListener fl = new ErrorFocusListener(label, vi.message, vi.component);
                        if (fc.hasFocus()) {
                            fl.showErrorTip();
                        }

                        fc.addFocusListener(fl);
                        Disposer.register(getDisposable(), () -> fc.removeFocusListener(fl));
                    }
                }
                else {
                    SwingUtilities.invokeLater(() -> myErrorText.appendError(vi.message));
                }
            }
        }
        else if (!myInfo.isEmpty()) {
            myErrorTextAlarm.addRequest(
                () -> {
                    for (ValidationInfo vi : myInfo) {
                        myErrorText.appendError(vi.message);
                    }
                },
                300,
                null
            );
        }
    }

    private void setErrorTipText(JComponent component, JLabel label, LocalizeValue text) {
        Insets insets = UIManager.getInsets("Balloon.error.textInsets");
        float oneLineWidth = BasicGraphicsUtils.getStringWidth(label, label.getFontMetrics(label.getFont()), text.get());
        float textWidth = getRootPane().getWidth() - component.getX() - insets.left - insets.right - JBUI.scale(30);
        if (textWidth < JBUI.scale(90)) {
            textWidth = JBUI.scale(90);
        }
        if (textWidth > oneLineWidth) {
            textWidth = oneLineWidth;
        }

        String htmlText = String.format("<html><div width=%d>%s</div></html>", (int)textWidth, text);
        label.setText(htmlText);
    }

    private Component getFocusable(Component source) {
        if (source == null) {
            return null;
        }
        else if (source instanceof JScrollPane scrollPane) {
            return scrollPane.getViewport().getView();
        }
        else if (source instanceof JComboBox comboBox && comboBox.isEditable()) {
            return comboBox.getEditor().getEditorComponent();
        }
        else if (source instanceof JSpinner spinner) {
            Container c = spinner.getEditor();
            synchronized (c.getTreeLock()) {
                return c.getComponent(0);
            }
        }
        else if (source instanceof Container container) {
            List<Component> cl;
            synchronized (container.getTreeLock()) {
                cl = Arrays.asList(container.getComponents());
            }
            return cl.stream().filter(Component::isFocusable).count() > 1 ? null : source;
        }
        else {
            return source;
        }
    }


    private void updateSize() {
        if (myActualSize == null && !myErrorText.isVisible()) {
            myActualSize = getSize();
        }
    }

    @Nullable
    public static DialogWrapper findInstance(Component c) {
        while (c != null) {
            if (c instanceof DialogWrapperDialog dialog) {
                return dialog.getDialogWrapper();
            }
            c = c.getParent();
        }
        return null;
    }

    private void resizeWithAnimation(@Nonnull final Dimension size) {
        //todo[kb]: fix this PITA
        myResizeInProgress = true;
        if (!Registry.is("enable.animation.on.dialogs")) {
            setSize(size.width, size.height);
            myResizeInProgress = false;
            return;
        }

        new Thread("DialogWrapper resizer") {
            int time = 200;
            int steps = 7;

            @Override
            public void run() {
                int step = 0;
                Dimension cur = getSize();
                int h = (size.height - cur.height) / steps;
                int w = (size.width - cur.width) / steps;
                while (step++ < steps) {
                    setSize(cur.width + w * step, cur.height + h * step);
                    TimeoutUtil.sleep(time / steps);
                }
                setSize(size.width, size.height);
                //repaint();
                if (myErrorText.shouldBeVisible()) {
                    myErrorText.setVisible(true);
                }
                myResizeInProgress = false;
            }
        }.start();
    }

    private class ErrorText extends JPanel {
        private final JLabel myLabel = new JLabel();
        private List<LocalizeValue> errors = new ArrayList<>();

        private ErrorText(int horizontalAlignment) {
            setLayout(new BorderLayout());
            myLabel.setBorder(JBUI.Borders.empty(16, 13, 16, 13));
            myLabel.setHorizontalAlignment(horizontalAlignment);
            JBScrollPane pane = new JBScrollPane(
                myLabel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            );
            pane.setBorder(JBUI.Borders.empty());
            pane.setBackground(null);
            pane.getViewport().setBackground(null);
            pane.setOpaque(false);
            add(pane, BorderLayout.CENTER);
        }

        private void clearError() {
            errors.clear();
            myLabel.setBounds(0, 0, 0, 0);
            myLabel.setText("");
            setVisible(false);
            updateSize();
        }

        private void appendError(LocalizeValue text) {
            errors.add(text);
            myLabel.setBounds(0, 0, 0, 0);
            StringBuilder sb = new StringBuilder("<html><font color='#" + ColorUtil.toHex(JBColor.RED) + "'>");
            errors.forEach(error -> sb.append("<left>").append(error).append("</left><br/>"));
            sb.append("</font></html>");
            myLabel.setText(sb.toString());
            setVisible(true);
            updateSize();
        }

        private boolean shouldBeVisible() {
            return !errors.isEmpty();
        }

        private boolean isTextSet(@Nonnull List<ValidationInfo> info) {
            if (info.isEmpty()) {
                return errors.isEmpty();
            }
            else {
                return errors.size() == info.size() && errors.equals(info.stream().map(i -> i.message).collect(Collectors.toList()));
            }
        }
    }

    private static class ErrorTipTracker extends PositionTracker<Balloon> {
        private final int y;

        private ErrorTipTracker(JComponent component, int y) {
            super(component);
            this.y = y;
        }

        @Override
        public RelativePoint recalculateLocation(Balloon balloon) {
            int width = getComponent().getWidth();
            int delta = width < JBUI.scale(120) ? width / 2 : JBUI.scale(60);
            return new RelativePoint(getComponent(), new Point(delta, y));
        }
    }

    private class ErrorFocusListener implements FocusListener {
        private final JLabel label;
        private final LocalizeValue text;
        private final JComponent component;

        private ErrorFocusListener(JLabel label, LocalizeValue text, JComponent component) {
            this.label = label;
            this.text = text;
            this.component = component;
        }

        @Override
        public void focusGained(FocusEvent e) {
            Balloon b = (Balloon)component.getClientProperty("JComponent.error.balloon");
            if (b == null || b.isDisposed()) {
                showErrorTip();
            }
        }

        @Override
        public void focusLost(FocusEvent e) {
            Balloon b = (Balloon)component.getClientProperty("JComponent.error.balloon");
            if (b != null && !b.isDisposed()) {
                b.hide();
            }
        }

        private void showErrorTip() {
            BalloonBuilder balloonBuilder =
                (BalloonBuilder)component.getClientProperty("JComponent.error.balloon.builder");
            if (balloonBuilder == null) {
                return;
            }

            Balloon balloon = balloonBuilder.createBalloon();

            ComponentListener rl = new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    if (!balloon.isDisposed()) {
                        setErrorTipText(component, label, text);
                        balloon.revalidate();
                    }
                }
            };

            balloon.addListener(new JBPopupListener() {
                @Override
                public void onClosed(@Nonnull LightweightWindowEvent event) {
                    JRootPane rootPane = getRootPane();
                    if (rootPane != null) {
                        rootPane.removeComponentListener(rl);
                    }

                    if (component.getClientProperty("JComponent.error.balloon") == event.asBalloon()) {
                        component.putClientProperty("JComponent.error.balloon", null);
                    }
                }
            });

            getRootPane().addComponentListener(rl);

            Point componentPos = SwingUtilities.convertPoint(component, 0, 0, getRootPane().getLayeredPane());
            Dimension bSize = balloon.getPreferredSize();

            Insets cInsets = component.getInsets();
            int top = cInsets != null ? cInsets.top : 0;
            if (componentPos.y >= bSize.height + top) {
                balloon.show(new ErrorTipTracker(component, 0), Balloon.Position.above);
            }
            else {
                balloon.show(new ErrorTipTracker(component, component.getHeight()), Balloon.Position.below);
            }
            component.putClientProperty("JComponent.error.balloon", balloon);
        }
    }

    public final DialogWrapperPeer getPeer() {
        return myPeer;
    }

    /**
     * Ensure that dialog is used from even dispatch thread.
     *
     * @throws IllegalStateException if the dialog is invoked not on the event dispatch thread
     */
    private static void ensureEventDispatchThread() {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException("The DialogWrapper can be used only on event dispatch thread.");
        }
    }

    public boolean isDisposed() {
        return myDisposed;
    }

    public final Disposable getDisposable() {
        return myDisposable;
    }

    /**
     * @see PropertyDoNotAskOption
     */
    public interface DoNotAskOption {
        abstract class Adapter implements DoNotAskOption {
            /**
             * Save the state of the checkbox in the settings, or perform some other related action.
             * This method is called right after the dialog is {@link #close(int) closed}.
             * <br/>
             * Note that this method won't be called in the case when the dialog is closed by {@link #CANCEL_EXIT_CODE Cancel}
             * if {@link #shouldSaveOptionsOnCancel() saving the choice on cancel is disabled} (which is by default).
             *
             * @param isSelected true if user selected "don't show again".
             * @param exitCode   the {@link #getExitCode() exit code} of the dialog.
             * @see #shouldSaveOptionsOnCancel()
             */
            public abstract void rememberChoice(boolean isSelected, int exitCode);

            /**
             * Tells whether the checkbox should be selected by default or not.
             *
             * @return true if the checkbox should be selected by default.
             */
            public boolean isSelectedByDefault() {
                return false;
            }

            @Override
            public boolean shouldSaveOptionsOnCancel() {
                return false;
            }

            @Nonnull
            @Override
            public LocalizeValue getDoNotShowMessage() {
                return CommonLocalize.dialogOptionsDoNotAsk();
            }

            @Override
            public final boolean isToBeShown() {
                return !isSelectedByDefault();
            }

            @Override
            public final void setToBeShown(boolean toBeShown, int exitCode) {
                rememberChoice(!toBeShown, exitCode);
            }

            @Override
            public final boolean canBeHidden() {
                return true;
            }
        }

        boolean isToBeShown();

        void setToBeShown(boolean value, int exitCode);

        /**
         * Should be 'true' for checkbox to be visible.
         */
        boolean canBeHidden();

        boolean shouldSaveOptionsOnCancel();

        @Nonnull
        default LocalizeValue getDoNotShowMessage() {
            return CommonLocalize.dialogOptionsDoNotShow();
        }
    }

    public static class PropertyDoNotAskOption implements DoNotAskOption {

        private final String myProperty;

        public PropertyDoNotAskOption(String property) {
            myProperty = property;
        }

        @Override
        public boolean isToBeShown() {
            return ApplicationPropertiesComponent.getInstance().getBoolean(myProperty, false);
        }

        @Override
        public void setToBeShown(boolean value, int exitCode) {
            ApplicationPropertiesComponent.getInstance().setValue(myProperty, Boolean.toString(value));
        }

        @Override
        public boolean canBeHidden() {
            return false;
        }

        @Override
        public boolean shouldSaveOptionsOnCancel() {
            return false;
        }

        @Nonnull
        @Override
        public LocalizeValue getDoNotShowMessage() {
            return CommonLocalize.dialogOptionsDoNotAsk();
        }
    }

    private ErrorPaintingType getErrorPaintingType() {
        return ErrorPaintingType.SIGN;
    }

    private class ErrorPainter extends AbstractPainter {
        private List<ValidationInfo> info;

        @Override
        public void executePaint(Component component, Graphics2D g) {
            for (ValidationInfo i : info) {
                if (i.component != null && !(Registry.is("ide.inplace.errors.outline"))) {
                    int w = i.component.getWidth();
                    int h = i.component.getHeight();
                    Point p;
                    switch (getErrorPaintingType()) {
                        case DOT:
                            p = SwingUtilities.convertPoint(i.component, 2, h / 2, component);
                            TargetAWT.to(AllIcons.Ide.ErrorPoint).paintIcon(component, g, p.x, p.y);
                            break;
                        case SIGN:
                            p = SwingUtilities.convertPoint(i.component, w, 0, component);
                            TargetAWT.to(AllIcons.General.Error).paintIcon(component, g, p.x - 8, p.y - 8);
                            break;
                        case LINE:
                            p = SwingUtilities.convertPoint(i.component, 0, h, component);
                            Graphics g2 = g.create();
                            try {
                                //noinspection UseJBColor
                                g2.setColor(new Color(255, 0, 0, 100));
                                g2.fillRoundRect(p.x, p.y - 2, w, 4, 2, 2);
                            }
                            finally {
                                g2.dispose();
                            }
                            break;
                    }
                }
            }
        }

        @Override
        public boolean needsRepaint() {
            return true;
        }

        private void setValidationInfo(@Nonnull List<ValidationInfo> info) {
            this.info = info;
        }
    }

    private static enum ErrorPaintingType {
        DOT,
        SIGN,
        LINE
    }
}

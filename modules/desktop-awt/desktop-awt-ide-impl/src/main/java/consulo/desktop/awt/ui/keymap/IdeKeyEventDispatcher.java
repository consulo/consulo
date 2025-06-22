/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.desktop.awt.ui.keymap;

import consulo.application.AccessToken;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.ModalityStateImpl;
import consulo.application.util.registry.Registry;
import consulo.awt.hacking.AWTKeyStrokeHacking;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.desktop.awt.ui.ProhibitAWTEvents;
import consulo.desktop.awt.ui.keymap.keyGesture.KeyboardGestureProcessor;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.dataContext.BaseDataManager;
import consulo.ide.impl.idea.openapi.actionSystem.ActionPromoter;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionImplUtil;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.openapi.keymap.impl.ActionProcessor;
import consulo.ide.impl.idea.openapi.keymap.impl.KeyState;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.ShortcutTextField;
import consulo.ide.impl.idea.openapi.wm.impl.IdeGlassPaneEx;
import consulo.ide.impl.idea.ui.ComponentWithMnemonics;
import consulo.ide.impl.idea.ui.KeyStrokeAdapter;
import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import consulo.platform.Platform;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ui.internal.StatusBarEx;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.IdeFrameUtil;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.KeyboardLayoutUtil;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.ex.awt.util.MacUIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.keymap.localize.KeyMapLocalize;
import consulo.ui.ex.popup.*;
import consulo.ui.ex.toolWindow.ToolWindowFloatingDecorator;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.im.InputContext;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * This class is automaton with finite number of state.
 *
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public final class IdeKeyEventDispatcher implements Disposable {
    private KeyStroke myFirstKeyStroke;
    /**
     * When we "dispatch" key event via keymap, i.e. when registered action has been executed
     * instead of event dispatching, then we have to consume all following KEY_RELEASED and
     * KEY_TYPED event because they are not valid.
     */
    private boolean myPressedWasProcessed;
    private KeyState myState = KeyState.STATE_INIT;

    private final BasePresentationFactory myPresentationFactory = new BasePresentationFactory();
    private boolean myDisposed = false;
    private boolean myLeftCtrlPressed = false;
    private boolean myRightAltPressed = false;

    private final KeyboardGestureProcessor myKeyGestureProcessor = new KeyboardGestureProcessor(this);

    private final KeyProcessorContext myContext = new KeyProcessorContext();
    private final IdeEventQueue myQueue;

    private Future<?> mySecondStrokeTimeout = CompletableFuture.completedFuture(null);
    private final Runnable mySecondStrokeTimeoutRunnable = () -> {
        if (myState == KeyState.STATE_WAIT_FOR_SECOND_KEYSTROKE) {
            resetState();
            DataContext dataContext = myContext.getDataContext();
            StatusBar.Info.set(null, dataContext == null ? null : dataContext.getData(Project.KEY));
        }
    };

    private Future<?> mySecondKeystrokePopupTimeout = CompletableFuture.completedFuture(null);

    public IdeKeyEventDispatcher(IdeEventQueue queue) {
        myQueue = queue;
        Application parent = ApplicationManager.getApplication();  // Application is null on early start when e.g. license dialog is shown
        if (parent != null) {
            Disposer.register(parent, this);
        }
    }

    public boolean isWaitingForSecondKeyStroke() {
        return getState() == KeyState.STATE_WAIT_FOR_SECOND_KEYSTROKE || isPressedWasProcessed();
    }

    /**
     * @return <code>true</code> if and only if the passed event is already dispatched by the
     * <code>IdeKeyEventDispatcher</code> and there is no need for any other processing of the event.
     */
    @RequiredUIAccess
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (myDisposed) {
            return false;
        }
        KeyboardLayoutUtil.storeAsciiForChar(e);

        if (e.isConsumed()) {
            return false;
        }

        if (isSpeedSearchEditing(e)) {
            return false;
        }

        // http://www.jetbrains.net/jira/browse/IDEADEV-12372
        if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                myLeftCtrlPressed = e.getKeyLocation() == KeyEvent.KEY_LOCATION_LEFT;
            }
            else if (e.getID() == KeyEvent.KEY_RELEASED) {
                myLeftCtrlPressed = false;
            }
        }
        else if (e.getKeyCode() == KeyEvent.VK_ALT) {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                myRightAltPressed = e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT;
            }
            else if (e.getID() == KeyEvent.KEY_RELEASED) {
                myRightAltPressed = false;
            }
        }

        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        Component focusOwner = focusManager.getFocusOwner();

        // shortcuts should not work in shortcut setup fields
        if (focusOwner instanceof ShortcutTextField) {
            return false;
        }
        if (focusOwner instanceof JTextComponent textComponent && textComponent.isEditable()
            && e.getKeyChar() != KeyEvent.CHAR_UNDEFINED && e.getKeyCode() != KeyEvent.VK_ESCAPE) {
            MacUIUtil.hideCursor();
        }

        MenuSelectionManager menuSelectionManager = MenuSelectionManager.defaultManager();
        MenuElement[] selectedPath = menuSelectionManager.getSelectedPath();
        if (selectedPath.length > 0) {
            if (!(selectedPath[0] instanceof ComboPopup)) {
                // The following couple of lines of code is a PATCH!!!
                // It is needed to ignore ENTER KEY_TYPED events which sometimes can reach editor when an action
                // is invoked from main menu via Enter key.
                setState(KeyState.STATE_PROCESSED);
                setPressedWasProcessed(true);
                return false;
            }
        }

        // Keymap shortcuts (i.e. not local shortcuts) should work only in:
        // - main frame
        // - floating focusedWindow
        // - when there's an editor in contexts
        Window focusedWindow = focusManager.getFocusedWindow();
        boolean isModalContext = focusedWindow != null && isModalContext(focusedWindow);

        if (ApplicationManager.getApplication() == null) {
            return false; //EA-39114
        }

        DataManager dataManager = DataManager.getInstance();
        if (dataManager == null) {
            return false;
        }

        DataContext dataContext = dataManager.getDataContext();

        myContext.setDataContext(dataContext);
        myContext.setFocusOwner(focusOwner);
        myContext.setModalContext(isModalContext);
        myContext.setInputEvent(e);

        try {
            if (getState() == KeyState.STATE_INIT) {
                return inInitState();
            }
            else if (getState() == KeyState.STATE_PROCESSED) {
                return inProcessedState();
            }
            else if (getState() == KeyState.STATE_WAIT_FOR_SECOND_KEYSTROKE) {
                return inWaitForSecondStrokeState();
            }
            else if (getState() == KeyState.STATE_SECOND_STROKE_IN_PROGRESS) {
                return inSecondStrokeInProgressState();
            }
            else if (getState() == KeyState.STATE_KEY_GESTURE_PROCESSOR) {
                return myKeyGestureProcessor.process();
            }
            else {
                throw new IllegalStateException("state = " + getState());
            }
        }
        finally {
            myContext.clear();
        }
    }

    private static boolean isSpeedSearchEditing(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_BACK_SPACE) {
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JComponent owner) {
                SpeedSearchSupply supply = SpeedSearchSupply.getSupply(owner);
                return supply != null && supply.isPopupActive();
            }
        }
        return false;
    }

    /**
     * @return <code>true</code> if and only if the <code>component</code> represents
     * modal context.
     * @throws IllegalArgumentException if <code>component</code> is <code>null</code>.
     */
    public static boolean isModalContext(@Nonnull Component component) {
        Window awtWindow = UIUtil.getWindow(component);

        consulo.ui.Window uiWindow = TargetAWT.from(awtWindow);

        IdeFrame ideFrame = uiWindow == null ? null : uiWindow.getUserData(IdeFrame.KEY);
        if (IdeFrameUtil.isRootFrame(ideFrame)) {
            RootPaneContainer rootPaneContainer = (RootPaneContainer)awtWindow;

            Component glassPane = rootPaneContainer.getGlassPane();
            if (glassPane instanceof IdeGlassPaneEx ideGlassPaneEx) {
                return ideGlassPaneEx.isInModalContext();
            }
        }

        if (awtWindow instanceof JDialog dialog && !dialog.isModal()) {
            Window owner = dialog.getOwner();
            return owner != null && isModalContext(owner);
        }

        if (awtWindow instanceof JFrame) {
            return false;
        }

        boolean isFloatingDecorator = awtWindow instanceof ToolWindowFloatingDecorator;

        boolean isPopup = !(component instanceof JFrame) && !(component instanceof JDialog);
        if (isPopup && component instanceof JWindow window) {
            JBPopup popup = (JBPopup)window.getRootPane().getClientProperty(JBPopup.KEY);
            if (popup != null) {
                return popup.isModalContext();
            }
        }

        return !isFloatingDecorator;
    }

    @RequiredUIAccess
    private boolean inWaitForSecondStrokeState() {
        // a key pressed means that the user starts to enter the second stroke...
        if (KeyEvent.KEY_PRESSED == myContext.getInputEvent().getID()) {
            setState(KeyState.STATE_SECOND_STROKE_IN_PROGRESS);
            return inSecondStrokeInProgressState();
        }
        // looks like RELEASEs (from the first stroke) go here...  skip them
        return true;
    }

    /**
     * This is hack. AWT doesn't allow to create KeyStroke with specified key code and key char
     * simultaneously. Therefore we are using reflection.
     */
    private static KeyStroke getKeyStrokeWithoutMouseModifiers(KeyStroke originalKeyStroke) {
        int modifier = originalKeyStroke.getModifiers()
            & ~InputEvent.BUTTON1_DOWN_MASK
            & ~InputEvent.BUTTON1_MASK
            & ~InputEvent.BUTTON2_DOWN_MASK
            & ~InputEvent.BUTTON2_MASK
            & ~InputEvent.BUTTON3_DOWN_MASK
            & ~InputEvent.BUTTON3_MASK;
        return (KeyStroke)AWTKeyStrokeHacking.getCachedStroke(
            originalKeyStroke.getKeyChar(),
            originalKeyStroke.getKeyCode(),
            modifier,
            originalKeyStroke.isOnKeyRelease()
        );
    }

    @RequiredUIAccess
    private boolean inSecondStrokeInProgressState() {
        KeyEvent e = myContext.getInputEvent();

        // when any key is released, we stop waiting for the second stroke
        if (KeyEvent.KEY_RELEASED == e.getID()) {
            myFirstKeyStroke = null;
            setState(KeyState.STATE_INIT);
            Project project = myContext.getDataContext().getData(Project.KEY);
            StatusBar.Info.set(null, project);
            return false;
        }

        KeyStroke originalKeyStroke = KeyStrokeAdapter.getDefaultKeyStroke(e);
        if (originalKeyStroke == null) {
            return false;
        }
        KeyStroke keyStroke = getKeyStrokeWithoutMouseModifiers(originalKeyStroke);

        updateCurrentContext(myContext.getFoundComponent(), new KeyboardShortcut(myFirstKeyStroke, keyStroke), myContext.isModalContext());

        // consume the wrong second stroke and keep on waiting
        if (myContext.getActions().isEmpty()) {
            return true;
        }

        // finally user had managed to enter the second keystroke, so let it be processed
        Project project = myContext.getDataContext().getData(Project.KEY);
        StatusBarEx statusBar = (StatusBarEx)WindowManager.getInstance().getStatusBar(project);
        if (processAction(e, myActionProcessor)) {
            if (statusBar != null) {
                statusBar.setInfo(null);
            }
            return true;
        }
        else {
            return false;
        }
    }

    @RequiredUIAccess
    private boolean inProcessedState() {
        KeyEvent e = myContext.getInputEvent();

        // ignore typed events which come after processed pressed event
        if (KeyEvent.KEY_TYPED == e.getID() && isPressedWasProcessed()) {
            return true;
        }
        if (KeyEvent.KEY_RELEASED == e.getID() && KeyEvent.VK_ALT == e.getKeyCode() && isPressedWasProcessed()) {
            //see IDEADEV-8615
            return true;
        }
        setState(KeyState.STATE_INIT);
        setPressedWasProcessed(false);
        return inInitState();
    }

    private static final Set<String> ALT_GR_LAYOUTS =
        new HashSet<>(Arrays.asList("pl", "de", "fi", "fr", "no", "da", "se", "pt", "nl", "tr", "sl", "hu", "bs", "hr", "sr", "sk", "lv"));

    @RequiredUIAccess
    private boolean inInitState() {
        Component focusOwner = myContext.getFocusOwner();
        boolean isModalContext = myContext.isModalContext();
        DataContext dataContext = myContext.getDataContext();
        KeyEvent e = myContext.getInputEvent();

        // http://www.jetbrains.net/jira/browse/IDEADEV-12372
        if (myLeftCtrlPressed && myRightAltPressed && focusOwner != null && e.getModifiers() == (InputEvent.CTRL_MASK | InputEvent.ALT_MASK)) {
            if (Registry.is("actionSystem.force.alt.gr")) {
                return false;
            }
            InputContext inputContext = focusOwner.getInputContext();
            if (inputContext != null) {
                Locale locale = inputContext.getLocale();
                if (locale != null) {
                    String language = locale.getLanguage();
                    if (ALT_GR_LAYOUTS.contains(language)) {
                        // don't search for shortcuts
                        return false;
                    }
                }
            }
        }

        KeyStroke originalKeyStroke = KeyStrokeAdapter.getDefaultKeyStroke(e);
        if (originalKeyStroke == null) {
            return false;
        }
        KeyStroke keyStroke = getKeyStrokeWithoutMouseModifiers(originalKeyStroke);

        if (myKeyGestureProcessor.processInitState()) {
            return true;
        }

        if (Platform.current().os().isMac()) {
            boolean keyTyped = e.getID() == KeyEvent.KEY_TYPED;
            boolean hasMnemonicsInWindow = e.getID() == KeyEvent.KEY_PRESSED && hasMnemonicInWindow(focusOwner, e.getKeyCode())
                || keyTyped && hasMnemonicInWindow(focusOwner, e.getKeyChar());
            boolean imEnabled = IdeEventQueue.getInstance().isInputMethodEnabled();

            if (e.getModifiersEx() == InputEvent.ALT_DOWN_MASK && (hasMnemonicsInWindow || !imEnabled && keyTyped)) {
                setPressedWasProcessed(true);
                setState(KeyState.STATE_PROCESSED);
                return false;
            }
        }

        updateCurrentContext(focusOwner, new KeyboardShortcut(keyStroke, null), isModalContext);
        if (myContext.getActions().isEmpty()) {
            // there's nothing mapped for this stroke
            return false;
        }

        if (myContext.isHasSecondStroke()) {
            myFirstKeyStroke = keyStroke;
            ArrayList<Pair<AnAction, KeyStroke>> secondKeyStrokes = getSecondKeystrokeActions();

            Project project = dataContext.getData(Project.KEY);
            StringBuilder message = new StringBuilder();
            message.append(KeyMapLocalize.prefixKeyPressedMessage());
            message.append(' ');
            for (int i = 0; i < secondKeyStrokes.size(); i++) {
                Pair<AnAction, KeyStroke> pair = secondKeyStrokes.get(i);
                if (i > 0) {
                    message.append(", ");
                }
                message.append(pair.getFirst().getTemplatePresentation().getText());
                message.append(" (");
                message.append(KeymapUtil.getKeystrokeText(pair.getSecond()));
                message.append(")");
            }

            StatusBar.Info.set(message.toString(), project);

            mySecondStrokeTimeout.cancel(false);
            mySecondStrokeTimeout = Application.get()
                .getLastUIAccess()
                .getScheduler()
                .schedule(
                    mySecondStrokeTimeoutRunnable,
                    Registry.intValue("actionSystem.secondKeystrokeTimeout"),
                    TimeUnit.MILLISECONDS
                );

            if (Registry.is("actionSystem.secondKeystrokeAutoPopupEnabled")) {
                mySecondKeystrokePopupTimeout.cancel(false);
                if (secondKeyStrokes.size() > 1) {
                    DataContext oldContext = myContext.getDataContext();
                    mySecondKeystrokePopupTimeout = Application.get().getLastUIAccess().getScheduler().schedule(
                        () -> {
                            if (myState == KeyState.STATE_WAIT_FOR_SECOND_KEYSTROKE) {
                                StatusBar.Info.set(null, oldContext.getData(Project.KEY));
                                new SecondaryKeystrokePopup(
                                    myFirstKeyStroke,
                                    secondKeyStrokes,
                                    oldContext
                                ).showInBestPositionFor(oldContext);
                            }
                        },
                        Registry.intValue("actionSystem.secondKeystrokePopupTimeout"),
                        TimeUnit.MILLISECONDS
                    );
                }
            }

            setState(KeyState.STATE_WAIT_FOR_SECOND_KEYSTROKE);
            return true;
        }
        else {
            return processAction(e, myActionProcessor);
        }
    }

    private ArrayList<Pair<AnAction, KeyStroke>> getSecondKeystrokeActions() {
        ArrayList<Pair<AnAction, KeyStroke>> secondKeyStrokes = new ArrayList<>();
        for (AnAction action : myContext.getActions()) {
            Shortcut[] shortcuts = action.getShortcutSet().getShortcuts();
            for (Shortcut shortcut : shortcuts) {
                if (shortcut instanceof KeyboardShortcut) {
                    KeyboardShortcut keyShortcut = (KeyboardShortcut)shortcut;
                    if (keyShortcut.getFirstKeyStroke().equals(myFirstKeyStroke)) {
                        secondKeyStrokes.add(Pair.create(action, keyShortcut.getSecondKeyStroke()));
                    }
                }
            }
        }
        return secondKeyStrokes;
    }

    public static boolean hasMnemonicInWindow(Component focusOwner, KeyEvent event) {
        return KeyEvent.KEY_TYPED == event.getID() && hasMnemonicInWindow(focusOwner, event.getKeyChar())
            || KeyEvent.KEY_PRESSED == event.getID() && hasMnemonicInWindow(focusOwner, event.getKeyCode());
    }

    private static boolean hasMnemonicInWindow(Component focusOwner, int keyCode) {
        if (keyCode == KeyEvent.VK_ALT || keyCode == 0) {
            return false; // Optimization
        }
        Container container = getContainer(focusOwner);
        return hasMnemonic(container, keyCode) || hasMnemonicInBalloons(container, keyCode);
    }

    @Nullable
    private static Container getContainer(@Nullable Component focusOwner) {
        if (focusOwner == null) {
            return null;
        }
        if (focusOwner.isLightweight()) {
            Container container = focusOwner.getParent();
            while (container != null) {
                Container parent = container.getParent();
                if (parent instanceof JLayeredPane) {
                    break;
                }
                if (parent != null && parent.isLightweight()) {
                    container = parent;
                }
                else {
                    break;
                }
            }
            return container;
        }

        return SwingUtilities.windowForComponent(focusOwner);
    }

    private static boolean hasMnemonic(Container container, int keyCode) {
        if (container == null) {
            return false;
        }

        Component[] components = container.getComponents();
        for (Component component : components) {
            if (component instanceof AbstractButton button) {
                if (button instanceof JBOptionButton optionButton) {
                    if (optionButton.isOkToProcessDefaultMnemonics()) {
                        return true;
                    }
                }
                else if (button.getMnemonic() == keyCode) {
                    return true;
                }
            }
            if (component instanceof JLabel label && label.getDisplayedMnemonic() == keyCode) {
                return true;
            }
            if (component instanceof Container subContainer && hasMnemonic(subContainer, keyCode)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasMnemonicInBalloons(Container container, int code) {
        if (UIUtil.findUltimateParent(container) instanceof RootPaneContainer rootPaneContainer) {
            for (Component component : rootPaneContainer.getLayeredPane().getComponents()) {
                if (component instanceof ComponentWithMnemonics
                    && component instanceof Container subContainer && hasMnemonic(subContainer, code)) {
                    return true;
                }
            }
        }
        return false;
    }

    private final ActionProcessor myActionProcessor = new ActionProcessor() {
        @Nonnull
        @Override
        public AnActionEvent createEvent(
            InputEvent inputEvent,
            @Nonnull DataContext context,
            @Nonnull String place,
            @Nonnull Presentation presentation,
            ActionManager manager
        ) {
            return new AnActionEvent(inputEvent, context, place, presentation, manager, 0);
        }

        @Override
        public void onUpdatePassed(InputEvent inputEvent, @Nonnull AnAction action, @Nonnull AnActionEvent actionEvent) {
            setState(KeyState.STATE_PROCESSED);
            setPressedWasProcessed(inputEvent.getID() == KeyEvent.KEY_PRESSED);
        }

        @Override
        public void performAction(@Nonnull InputEvent e, @Nonnull AnAction action, @Nonnull AnActionEvent actionEvent) {
            e.consume();

            DataContext ctx = actionEvent.getDataContext();
            if (action instanceof ActionGroup group && !group.canBePerformed(ctx)) {
                JBPopupFactory.getInstance().createActionGroupPopup(
                        group.getTemplatePresentation().getText(),
                        group,
                        ctx,
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                        false
                    )
                    .showInBestPositionFor(ctx);
            }
            else {
                ActionImplUtil.performActionDumbAware(action, actionEvent);
            }

            if (Registry.is("actionSystem.fixLostTyping")) {
                IdeEventQueue.getInstance().doWhenReady(() -> IdeEventQueue.getInstance().getKeyEventDispatcher().resetState());
            }
        }
    };

    @RequiredUIAccess
    public boolean processAction(InputEvent e, @Nonnull ActionProcessor processor) {
        ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
        Project project = myContext.getDataContext().getData(Project.KEY);
        boolean dumb = project != null && DumbService.getInstance(project).isDumb();
        List<AnActionEvent> nonDumbAwareAction = new ArrayList<>();
        List<AnAction> actions = myContext.getActions();
        for (AnAction action : actions.toArray(new AnAction[actions.size()])) {
            Presentation presentation = myPresentationFactory.getPresentation(action);

            // Mouse modifiers are 0 because they have no any sense when action is invoked via keyboard
            AnActionEvent actionEvent =
                processor.createEvent(e, myContext.getDataContext(), ActionPlaces.MAIN_MENU, presentation, ActionManager.getInstance());

            try (AccessToken ignored = ProhibitAWTEvents.start("update")) {
                ActionImplUtil.performDumbAwareUpdate(action, actionEvent, true);
            }

            if (dumb && !action.isDumbAware()) {
                if (!Boolean.FALSE.equals(presentation.getClientProperty(ActionImplUtil.WOULD_BE_ENABLED_IF_NOT_DUMB_MODE))) {
                    nonDumbAwareAction.add(actionEvent);
                }
                continue;
            }

            if (!presentation.isEnabled()) {
                continue;
            }

            processor.onUpdatePassed(e, action, actionEvent);

            if (myContext.getDataContext() instanceof BaseDataManager.DataContextWithEventCount dataContextWithEventCount) {
                // this is not true for test data contexts
                dataContextWithEventCount.setEventCount(IdeEventQueue.getInstance().getEventCount(), this);
            }
            actionManager.fireBeforeActionPerformed(action, actionEvent.getDataContext(), actionEvent);
            Component component = actionEvent.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
            if (component != null && !component.isShowing()) {
                return true;
            }

            processor.performAction(e, action, actionEvent);
            actionManager.fireAfterActionPerformed(action, actionEvent.getDataContext(), actionEvent);
            return true;
        }

        if (!nonDumbAwareAction.isEmpty()) {
            showDumbModeWarningLaterIfNobodyConsumesEvent(e, nonDumbAwareAction.toArray(new AnActionEvent[nonDumbAwareAction.size()]));
        }

        return false;
    }

    private static void showDumbModeWarningLaterIfNobodyConsumesEvent(InputEvent e, AnActionEvent... actionEvents) {
        Application application = Application.get();
        if (application.getCurrentModalityState() == ModalityStateImpl.NON_MODAL) {
            application.invokeLater(() -> {
                if (e.isConsumed()) {
                    return;
                }

                ActionImplUtil.showDumbModeWarning(actionEvents);
            });
        }
    }

    /**
     * This method fills <code>myActions</code> list.
     *
     * @return true if there is a shortcut with second stroke found.
     */
    public KeyProcessorContext updateCurrentContext(Component component, Shortcut sc, boolean isModalContext) {
        myContext.setFoundComponent(null);
        myContext.getActions().clear();

        if (isControlEnterOnDialog(component, sc)) {
            return myContext;
        }

        boolean hasSecondStroke = false;

        // here we try to find "local" shortcuts

        for (; component != null; component = component.getParent()) {
            if (!(component instanceof JComponent jComponent)) {
                continue;
            }
            List<AnAction> listOfActions = ActionImplUtil.getActions(jComponent);
            if (listOfActions.isEmpty()) {
                continue;
            }
            for (Object listOfAction : listOfActions) {
                if (listOfAction instanceof AnAction action) {
                    hasSecondStroke |= addAction(action, sc);
                }
            }
            // once we've found a proper local shortcut(s), we continue with non-local shortcuts
            if (!myContext.getActions().isEmpty()) {
                myContext.setFoundComponent(jComponent);
                break;
            }
        }

        // search in main keymap

        Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
        String[] actionIds = keymap.getActionIds(sc);

        ActionManager actionManager = ActionManager.getInstance();
        for (String actionId : actionIds) {
            AnAction action = actionManager.getAction(actionId);
            if (action != null) {
                if (isModalContext && !action.isEnabledInModalContext()) {
                    continue;
                }
                hasSecondStroke |= addAction(action, sc);
            }
        }

        if (!hasSecondStroke && sc instanceof KeyboardShortcut keyboardShortcut) {
            // little trick to invoke action which second stroke is a key w/o modifiers, but user still
            // holds the modifier key(s) of the first stroke

            KeyStroke firstKeyStroke = keyboardShortcut.getFirstKeyStroke();
            KeyStroke secondKeyStroke = keyboardShortcut.getSecondKeyStroke();

            if (secondKeyStroke != null && secondKeyStroke.getModifiers() != 0 && firstKeyStroke.getModifiers() != 0) {
                KeyboardShortcut altShortCut =
                    new KeyboardShortcut(firstKeyStroke, KeyStroke.getKeyStroke(secondKeyStroke.getKeyCode(), 0));
                String[] additionalActions = keymap.getActionIds(altShortCut);

                for (String actionId : additionalActions) {
                    AnAction action = actionManager.getAction(actionId);
                    if (action != null) {
                        if (isModalContext && !action.isEnabledInModalContext()) {
                            continue;
                        }
                        hasSecondStroke |= addAction(action, altShortCut);
                    }
                }
            }
        }

        myContext.setHasSecondStroke(hasSecondStroke);
        List<AnAction> actions = myContext.getActions();

        if (actions.size() > 1) {
            List<AnAction> readOnlyActions = Collections.unmodifiableList(actions);
            Application.get().getExtensionPoint(ActionPromoter.class).forEach(promoter -> {
                List<AnAction> promoted = promoter.promote(readOnlyActions, myContext.getDataContext());
                if (promoted == null || promoted.isEmpty()) {
                    return;
                }

                actions.removeAll(promoted);
                actions.addAll(0, promoted);
            });
        }

        return myContext;
    }

    private static KeyboardShortcut CONTROL_ENTER = KeyboardShortcut.fromString("control ENTER");

    private static boolean isControlEnterOnDialog(Component component, Shortcut sc) {
        return CONTROL_ENTER.equals(sc) && !IdeEventQueue.getInstance().isPopupActive() //avoid Control+Enter in completion
            && DialogWrapper.findInstance(component) != null;
    }

    /**
     * @return true if action is added and has second stroke
     */
    private boolean addAction(AnAction action, Shortcut sc) {
        boolean hasSecondStroke = false;

        Shortcut[] shortcuts = action.getShortcutSet().getShortcuts();
        for (Shortcut each : shortcuts) {
            if (!each.isKeyboard()) {
                continue;
            }

            if (each.startsWith(sc)) {
                if (!myContext.getActions().contains(action)) {
                    myContext.getActions().add(action);
                }

                if (each instanceof KeyboardShortcut) {
                    hasSecondStroke |= ((KeyboardShortcut)each).getSecondKeyStroke() != null;
                }
            }
        }

        return hasSecondStroke;
    }

    public KeyProcessorContext getContext() {
        return myContext;
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }

    public KeyState getState() {
        return myState;
    }

    public void setState(KeyState state) {
        myState = state;
        if (myQueue != null) {
            myQueue.maybeReady();
        }
    }

    public void resetState() {
        setState(KeyState.STATE_INIT);
        setPressedWasProcessed(false);
    }

    public boolean isPressedWasProcessed() {
        return myPressedWasProcessed;
    }

    public void setPressedWasProcessed(boolean pressedWasProcessed) {
        myPressedWasProcessed = pressedWasProcessed;
    }

    public boolean isReady() {
        return myState == KeyState.STATE_INIT || myState == KeyState.STATE_PROCESSED;
    }

    private static class SecondaryKeystrokePopup extends ListPopupImpl {
        private SecondaryKeystrokePopup(
            @Nonnull KeyStroke firstKeystroke,
            @Nonnull List<Pair<AnAction, KeyStroke>> actions,
            DataContext context
        ) {
            super(buildStep(actions, context));
            registerActions(firstKeystroke, actions, context);
        }

        private void registerActions(
            @Nonnull KeyStroke firstKeyStroke,
            @Nonnull List<Pair<AnAction, KeyStroke>> actions,
            DataContext ctx
        ) {
            ContainerUtil.process(
                actions,
                pair -> {
                    String actionText = pair.getFirst().getTemplatePresentation().getText();
                    AbstractAction a = new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            cancel();
                            invokeAction(pair.getFirst(), ctx);
                        }
                    };

                    KeyStroke keyStroke = pair.getSecond();
                    if (keyStroke != null) {
                        registerAction(actionText, keyStroke, a);

                        if (keyStroke.getModifiers() == 0) {
                            // do a little trick here, so if I will press Command+R and the second keystroke is just 'R',
                            // I want to be able to hold the Command while pressing 'R'

                            KeyStroke additionalKeyStroke = KeyStroke.getKeyStroke(keyStroke.getKeyCode(), firstKeyStroke.getModifiers());
                            String _existing = getActionForKeyStroke(additionalKeyStroke);
                            if (_existing == null) {
                                registerAction("__additional__" + actionText, additionalKeyStroke, a);
                            }
                        }
                    }

                    return true;
                }
            );
        }

        private static void invokeAction(@Nonnull AnAction action, DataContext ctx) {
            AnActionEvent event = new AnActionEvent(
                null,
                ctx,
                ActionPlaces.UNKNOWN,
                action.getTemplatePresentation().clone(),
                ActionManager.getInstance(),
                0
            );
            if (ActionImplUtil.lastUpdateAndCheckDumb(action, event, true)) {
                ActionImplUtil.performActionDumbAware(action, event);
            }
        }

        @Override
        protected ListCellRenderer getListElementRenderer() {
            return new ActionListCellRenderer();
        }

        private static ListPopupStep buildStep(@Nonnull List<Pair<AnAction, KeyStroke>> actions, DataContext ctx) {
            return new BaseListPopupStep<Pair<AnAction, KeyStroke>>(
                "Choose an action",
                ContainerUtil.findAll(
                    actions,
                    pair -> {
                        AnAction action = pair.getFirst();
                        Presentation presentation = action.getTemplatePresentation().clone();
                        AnActionEvent event =
                            new AnActionEvent(null, ctx, ActionPlaces.UNKNOWN, presentation, ActionManager.getInstance(), 0);

                        ActionImplUtil.performDumbAwareUpdate(action, event, true);
                        return presentation.isEnabled() && presentation.isVisible();
                    }
                )
            ) {
                @Override
                public PopupStep onChosen(Pair<AnAction, KeyStroke> selectedValue, boolean finalChoice) {
                    invokeAction(selectedValue.getFirst(), ctx);
                    return FINAL_CHOICE;
                }
            };
        }

        private static class ActionListCellRenderer extends ColoredListCellRenderer {
            @Override
            protected void customizeCellRenderer(@Nonnull JList list, Object value, int index, boolean selected, boolean hasFocus) {
                if (value instanceof Pair) {
                    //noinspection unchecked
                    Pair<AnAction, KeyStroke> pair = (Pair<AnAction, KeyStroke>)value;
                    append(KeymapUtil.getShortcutText(new KeyboardShortcut(pair.getSecond(), null)), SimpleTextAttributes.GRAY_ATTRIBUTES);
                    appendTextPadding(30);
                    String text = pair.getFirst().getTemplatePresentation().getText();
                    if (text != null) {
                        append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    }
                }
            }
        }
    }
}

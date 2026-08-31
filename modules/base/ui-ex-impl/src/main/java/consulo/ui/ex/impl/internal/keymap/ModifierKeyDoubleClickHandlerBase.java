/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ui.ex.impl.internal.keymap;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.ModifiedInputDetails.Modifier;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.internal.ActionUpdateInvoker;
import consulo.ui.ex.internal.KeyMapSetting;
import consulo.ui.ex.keymap.internal.ModifierKeyDoubleClickHandler;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.util.lang.Clock;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Everything of the modifier double click but the keys themselves - a frontend feeds
 * {@link #processKey(KeyCode, Set, boolean)} from wherever it gets them and says which data context an action
 * runs against.
 * <p/>
 * The timings that detect the double click were tuned for search everywhere, so if you need to change them,
 * please make sure search everywhere behaviour remains intact.
 *
 * @author VISTALL
 */
public abstract class ModifierKeyDoubleClickHandlerBase implements ModifierKeyDoubleClickHandler {
    /** past this, the second press is a new first one rather than the other half of a double click */
    private static final long DOUBLE_CLICK_MS = 300;

    /** a bare modifier double click is dropped when something else was typed this recently before it */
    private static final long OTHER_KEY_MS = 500;

    private static final Map<Modifier, KeyCode> MODIFIER_KEYS = Map.of(
        Modifier.ALT, KeyCode.ALT,
        Modifier.CTRL, KeyCode.CTRL,
        Modifier.META, KeyCode.META,
        Modifier.SHIFT, KeyCode.SHIFT
    );

    private final ActionManager myActionManager;
    private final KeyMapSetting myKeyMapSetting;
    private final ConcurrentMap<String, Registration> myRegistrations = new ConcurrentHashMap<>();

    private volatile boolean myIsRunningAction;
    private volatile @Nullable Supplier<DataContext> myDataContextSupplier;

    protected ModifierKeyDoubleClickHandlerBase(ActionManager actionManager, KeyMapSetting keyMapSetting) {
        myActionManager = actionManager;
        myKeyMapSetting = keyMapSetting;

        // one listener for all of them - any other action running means the user moved on, and whatever half
        // of a double click was recorded is stale
        myActionManager.addAnActionListener(new AnActionListener() {
            @Override
            public void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
                if (!myIsRunningAction) {
                    resetAll();
                }
            }
        });

        Modifier modifier = getMultiCaretActionModifier();
        registerAction(IdeActions.ACTION_EDITOR_CLONE_CARET_ABOVE, modifier, KeyCode.UP);
        registerAction(IdeActions.ACTION_EDITOR_CLONE_CARET_BELOW, modifier, KeyCode.DOWN);
        registerAction(IdeActions.ACTION_EDITOR_MOVE_CARET_LEFT_WITH_SELECTION, modifier, KeyCode.LEFT);
        registerAction(IdeActions.ACTION_EDITOR_MOVE_CARET_RIGHT_WITH_SELECTION, modifier, KeyCode.RIGHT);
        registerAction(IdeActions.ACTION_EDITOR_MOVE_LINE_START_WITH_SELECTION, modifier, KeyCode.HOME);
        registerAction(IdeActions.ACTION_EDITOR_MOVE_LINE_END_WITH_SELECTION, modifier, KeyCode.END);
    }

    /**
     * Which modifier the multi caret actions hang off. Only the frontend knows the keyboard the user is on,
     * and on a mac control belongs to the system.
     */
    protected Modifier getMultiCaretActionModifier() {
        return Modifier.CTRL;
    }

    /**
     * Context an action triggered this way runs against - the awt focus owner, the scope the browser reported.
     */
    protected DataContext createDataContext() {
        Supplier<DataContext> supplier = myDataContextSupplier;

        return supplier == null ? DataManager.getInstance().getDataContext() : supplier.get();
    }

    /**
     * Installed by the frontend that feeds the keys, since whatever knows where a key came from is also what
     * knows what it was aimed at.
     */
    public void setDataContextSupplier(@Nullable Supplier<DataContext> dataContextSupplier) {
        myDataContextSupplier = dataContextSupplier;
    }

    @Override
    public void registerAction(String actionId, Modifier modifier, @Nullable KeyCode actionKey, boolean skipIfActionHasShortcut) {
        myRegistrations.put(actionId, new Registration(actionId, modifier, actionKey, skipIfActionHasShortcut));
    }

    @Override
    public void unregisterAction(String actionId) {
        myRegistrations.remove(actionId);
    }

    @Override
    public boolean isRunningAction() {
        return myIsRunningAction;
    }

    public boolean processKey(KeyCode keyCode, Set<Modifier> modifiers, boolean pressed) {
        return processKey(keyCode, modifiers, pressed, Clock.getTime());
    }

    /**
     * @param timeMillis when the key was struck. A frontend whose keys travel to get here must pass the time
     *                   they were read at - measuring the gaps on arrival measures the network instead of the
     *                   user. Only compared against other times of the same frontend, so any epoch will do.
     * @return true when the key was consumed and must not reach anything else
     */
    public boolean processKey(KeyCode keyCode, Set<Modifier> modifiers, boolean pressed, long timeMillis) {
        boolean consumed = false;
        for (Registration registration : myRegistrations.values()) {
            consumed |= registration.processKey(keyCode, modifiers, pressed, timeMillis);
        }
        return consumed;
    }

    private void resetAll() {
        for (Registration registration : myRegistrations.values()) {
            registration.resetState();
        }
    }

    private final class Registration {
        private final String myActionId;
        private final Modifier myModifier;
        private final @Nullable KeyCode myActionKey;
        private final boolean mySkipIfActionHasShortcut;

        private boolean myPressedFirst;
        private boolean myPressedSecond;
        private boolean myReleasedFirst;
        private boolean myOtherKeyWasPressed;
        private long myLastTimePressed;

        private Registration(String actionId, Modifier modifier, @Nullable KeyCode actionKey, boolean skipIfActionHasShortcut) {
            myActionId = actionId;
            myModifier = modifier;
            myActionKey = actionKey;
            mySkipIfActionHasShortcut = skipIfActionHasShortcut;
        }

        private synchronized boolean processKey(KeyCode keyCode, Set<Modifier> modifiers, boolean pressed, long timeMillis) {
            if (keyCode == MODIFIER_KEYS.get(myModifier)) {
                if (hasOtherModifiers(modifiers)) {
                    resetState();
                    return false;
                }
                if (myActionKey == null && myOtherKeyWasPressed && timeMillis - myLastTimePressed < OTHER_KEY_MS) {
                    resetState();
                    return false;
                }
                myOtherKeyWasPressed = false;
                if (myPressedFirst && timeMillis - myLastTimePressed > OTHER_KEY_MS) {
                    resetState();
                }
                handleModifier(pressed, timeMillis);
                return false;
            }

            if (myPressedFirst && myReleasedFirst && myPressedSecond && myActionKey != null) {
                if (keyCode == myActionKey && !hasOtherModifiers(modifiers)) {
                    return !pressed || run();
                }
                return false;
            }

            myLastTimePressed = timeMillis;
            myOtherKeyWasPressed = true;
            if (keyCode == KeyCode.ESCAPE || keyCode == KeyCode.TAB) {
                myLastTimePressed = 0;
            }

            resetState();
            return false;
        }

        /** the modifier being tracked is the only one that may be down */
        private boolean hasOtherModifiers(Set<Modifier> modifiers) {
            for (Modifier modifier : modifiers) {
                if (modifier != myModifier) {
                    return true;
                }
            }
            return false;
        }

        private void handleModifier(boolean pressed, long timeMillis) {
            if (myPressedFirst && timeMillis - myLastTimePressed > DOUBLE_CLICK_MS) {
                resetState();
                return;
            }

            if (pressed) {
                if (!myPressedFirst) {
                    resetState();
                    myPressedFirst = true;
                    myLastTimePressed = timeMillis;
                    return;
                }
                if (myReleasedFirst) {
                    myPressedSecond = true;
                    myLastTimePressed = timeMillis;
                    return;
                }
            }
            else {
                if (myPressedFirst && !myReleasedFirst) {
                    myReleasedFirst = true;
                    myLastTimePressed = timeMillis;
                    return;
                }
                if (myPressedFirst && myReleasedFirst && myPressedSecond) {
                    resetState();
                    if (myActionKey == null && !shouldSkipIfActionHasShortcut()) {
                        run();
                    }
                    return;
                }
            }

            resetState();
        }

        private synchronized void resetState() {
            myPressedFirst = false;
            myPressedSecond = false;
            myReleasedFirst = false;
        }

        private boolean run() {
            // the setting turns the double press off without unregistering anything, so the action itself never
            // has to work out how it was invoked
            if (!myKeyMapSetting.isEnabledDoublePressShortcuts()) {
                return false;
            }

            myIsRunningAction = true;
            try {
                AnAction action = myActionManager.getAction(myActionId);
                if (action == null) {
                    return false;
                }

                DataContext context = createDataContext();
                AnActionEvent event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.MAIN_MENU, context);
                ActionUpdateInvoker.updateSync(action, event);
                if (!event.getPresentation().isEnabled()) {
                    return false;
                }

                ActionManagerEx actionManager = (ActionManagerEx)myActionManager;
                actionManager.fireBeforeActionPerformed(action, event.getDataContext(), event);
                action.actionPerformed(event);
                actionManager.fireAfterActionPerformed(action, event.getDataContext(), event);
                return true;
            }
            finally {
                myIsRunningAction = false;
            }
        }

        private boolean shouldSkipIfActionHasShortcut() {
            return mySkipIfActionHasShortcut && KeymapUtil.getActiveKeymapShortcuts(myActionId).getShortcuts().length > 0;
        }

        @Override
        public String toString() {
            return "modifier double-click [modifier=" + myModifier + ",actionKey=" + myActionKey + ",actionId=" + myActionId + "]";
        }
    }
}

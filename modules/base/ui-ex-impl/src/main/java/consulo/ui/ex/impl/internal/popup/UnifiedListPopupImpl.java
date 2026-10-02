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
package consulo.ui.ex.impl.internal.popup;

import consulo.component.ComponentManager;
import consulo.logging.Logger;
import consulo.ui.HeavyPopup;
import consulo.ui.Popup;
import consulo.ui.LightPopup;
import consulo.ui.PopupOptions;
import consulo.ui.PopupPosition;
import consulo.ui.Point2D;
import consulo.ui.ListBox;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemRender;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.action.ShortcutProvider;
import consulo.ui.ex.action.ShortcutSet;
import consulo.ui.ex.awt.popup.ListPopupStepEx;
import consulo.ui.ex.impl.internal.action.ActionImplUtil;
import consulo.ui.ex.impl.internal.popup.action.ActionPopupItem;
import consulo.ui.ex.impl.internal.popup.action.ActionPopupStep;
import consulo.ui.ex.impl.internal.popup.action.PopupInlineActions;
import consulo.ui.ex.internal.InlineButton;
import consulo.ui.ex.internal.InlineButtonsList;
import consulo.ui.ex.internal.UIInternalEx;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyboardInputDetails;
import consulo.ui.event.details.MouseInputDetails;
import consulo.ui.UIAccess;
import consulo.ui.model.FlatDataModel;
import consulo.ui.ex.popup.AsyncPopupStep;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.ex.popup.event.ListPopupKeyListener;
import consulo.ui.util.TextWithMnemonic;
import org.jspecify.annotations.Nullable;

import javax.swing.event.ListSelectionListener;
import java.awt.event.InputEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The counterpart of {@code ListPopupImpl} for the frontends which have no swing - each {@link ListPopupStep} is a
 * {@link LightPopup} of its own, and a step reached through a substep is stacked beside the one which owns it, the
 * way a submenu is.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public class UnifiedListPopupImpl extends UnifiedPopupImpl implements ListPopup {
    private static final Logger LOG = Logger.getInstance(UnifiedListPopupImpl.class);

    private final class Level {
        private final ListPopupStep<Object> myStep;
        private final PopupInlineActions myInlineActions;
        private final ListBox<Object> myList;
        private final Popup myPopup;

        private int myActiveButton = -1;

        @RequiredUIAccess
        private Level(ListPopupStep<Object> step, boolean nested) {
            myStep = step;
            myInlineActions = new PopupInlineActions(step, () -> false);
            myList = buildList(this);
            myPopup = buildPopup(this, nested);
        }
    }

    private final @Nullable ComponentManager myProject;
    private final CompletableFuture<? extends ListPopupStep> myRootStep;

    private final List<Consumer<Object>> mySelectionListeners = new ArrayList<>();
    private final List<ListPopupKeyListener> myKeyListeners = new ArrayList<>();

    /**
     * Innermost step first, so the top of the stack is the one the user is looking at.
     */
    private final Deque<Level> myLevels = new ArrayDeque<>();

    private @Nullable TextItemRender<Object> myRender;

    /** kept until the unified popups grow title chrome to put the pin button on */
    private @Nullable Predicate<? super JBPopup> myCouldPin;
    private int myMinimumWidth = -1;
    private boolean myResizable;

    private consulo.ui.@Nullable Component myAnchor;
    private @Nullable InputDetails myAnchorDetails;
    private @Nullable Point2D myAnchorPoint;
    private int myAnchorHeight;

    private boolean myAutoHandleBeforeShow;
    // closing a level from here fires its close listener as well, and that listener is the one which unwinds the
    // stack - without this it would unwind the stack it is already unwinding
    private boolean myUnwinding;

    public UnifiedListPopupImpl(@Nullable ComponentManager project, ListPopupStep step) {
        this(project, CompletableFuture.completedFuture(step));
    }

    public UnifiedListPopupImpl(@Nullable ComponentManager project, CompletableFuture<? extends ListPopupStep> step) {
        myProject = project;
        myRootStep = step;
    }

    @Override
    public ListPopupStep getListStep() {
        Level top = myLevels.peek();
        return top == null ? myRootStep.join() : top.myStep;
    }

    @Override
    public void setCouldPin(@Nullable Predicate<? super JBPopup> couldPin) {
        myCouldPin = couldPin;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setRender(TextItemRender<?> render) {
        myRender = (TextItemRender<Object>) render;

        Level top = myLevels.peek();
        if (top != null) {
            top.myList.setRender(myRender);
        }
    }

    @Override
    public void setMinimumWidth(int width) {
        myMinimumWidth = width;
    }

    @Override
    public void setResizable(boolean resizable) {
        myResizable = resizable;
    }

    /**
     * A step reached through another one is stacked beside the one which owns it, the way a submenu is. The first
     * step hangs under whatever raised it, and is placed instead when there is nothing to hang off.
     */
    @RequiredUIAccess
    private Popup buildPopup(Level level, boolean nested) {
        PopupOptions.Builder options = PopupOptions.builder();
        if (myResizable) {
            options.resizable();
        }
        if (nested) {
            options.position(PopupPosition.END);
        }

        PopupOptions built = options.build();

        // a step which has something to hang off is a light popup, whether that is the step which owns it or
        // whatever raised the popup - only a popup with no target at all is placed
        Popup popup = nested || myAnchor != null ? LightPopup.create(built) : HeavyPopup.create(built);

        popup.setTitle(level.myStep.getTitle());
        popup.setContent(level.myList);
        popup.setMinimumWidth(myMinimumWidth);

        popup.addCloseListener(event -> unwindTo(popup));

        return popup;
    }

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    private ListBox<Object> buildList(Level level) {
        ListPopupStep<Object> step = level.myStep;

        FlatDataModel<Object> model = step.getModel();
        ListBox<Object> list = UIInternalEx.get()._Components_popupListBox(model != null ? model : FlatDataModel.of(step.getValues()));

        list.setRender(renderFor(step));

        list.isSeparator(step::isSeparator);

        // the pointer is what a popup is chosen with, so the row under it is the one the popup is offering - the
        // awt list does the same from its mouse motion listener
        list.setSelectOnHover(true);

        if (list instanceof InlineButtonsList<?> buttonsList) {
            ((InlineButtonsList<Object>) buttonsList).setInlineButtons(value -> createInlineButtons(level, value));
        }

        int defaultIndex = step.getDefaultOptionIndex();
        if (defaultIndex >= 0 && defaultIndex < step.getValues().size()) {
            list.setValueByIndex(defaultIndex);
        }

        // moving over the rows only previews them - what a listener does with that is its own, the breakpoint
        // chooser marks the range each variant would cover
        list.addValueListener(event -> {
            setActiveButton(level, -1);

            fireSelectionChanged(event.getValue());
        });

        // the choice is the click, not the move onto the row: a step preselects its default, and choosing from a
        // change of value would refuse the row the selection already sits on
        list.addClickListener(event -> {
            if (!(event.getInputDetails() instanceof KeyboardInputDetails)) {
                setActiveButton(level, -1);
            }

            handleSelect(level, true, event.getInputDetails());
        });

        list.addKeyPressedListener(event -> onKeyPressed(level, event.getInputDetails()));

        if (!myKeyListeners.isEmpty()) {
            list.addKeyReleasedListener(event -> onKeyReleased(level, event.getInputDetails()));
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    private TextItemRender<Object> renderFor(ListPopupStep<Object> step) {
        TextItemRender<Object> render = myRender;
        if (render != null) {
            return render;
        }

        return (presentation, item) -> {
            Object value = item.getValue();
            if (value == null) {
                return;
            }

            presentation.withIcon(step.getIconFor(value));
            presentation.append(TextWithMnemonic.parse(step.getTextFor(value)).getText());

            if (value instanceof ShortcutProvider shortcutProvider) {
                ShortcutSet shortcutSet = shortcutProvider.getShortcut();
                Shortcut shortcut = shortcutSet == null ? null : ArrayUtil.getFirstElement(shortcutSet.getShortcuts());
                if (shortcut != null) {
                    presentation.append("  " + KeymapUtil.getShortcutText(shortcut), TextAttribute.GRAYED);
                }
            }

            String secondary = step instanceof ListPopupStepEx<?> stepEx ? ((ListPopupStepEx<Object>) stepEx).getValueFor(value) : null;
            if (secondary != null) {
                presentation.append("  " + secondary, TextAttribute.GRAYED);
            }
        };
    }

    private List<InlineButton> createInlineButtons(Level level, Object value) {
        List<PopupInlineActions.Button> buttons = level.myInlineActions.getButtons(value);
        if (buttons.isEmpty()) {
            return List.of();
        }

        List<InlineButton> inlineButtons = new ArrayList<>(buttons.size());
        for (int i = 0; i < buttons.size(); i++) {
            PopupInlineActions.Button button = buttons.get(i);

            Image icon = level.myInlineActions.getIcon(value, button, false);
            int index = i;

            inlineButtons.add(new InlineButton(
                icon == null ? Image.empty(Image.DEFAULT_ICON_SIZE) : icon,
                level.myInlineActions.getToolTip(value, i),
                button.alwaysVisible(),
                details -> performButton(level, value, index, details)
            ));
        }
        return inlineButtons;
    }

    @RequiredUIAccess
    private void setActiveButton(Level level, int index) {
        if (level.myActiveButton == index) {
            return;
        }

        level.myActiveButton = index;

        if (level.myList instanceof InlineButtonsList<?> buttonsList) {
            buttonsList.setActiveInlineButton(index);
        }
    }

    @RequiredUIAccess
    private void onKeyPressed(Level level, KeyboardInputDetails details) {
        if (isDisposed() || !myLevels.contains(level)) {
            return;
        }

        for (ListPopupKeyListener listener : new ArrayList<>(myKeyListeners)) {
            if (listener.keyPressed(this, details)) {
                return;
            }
        }

        KeyCode keyCode = details.getKeyCode();
        Object value = level.myList.getValue();

        if (KeyCode.RIGHT.equals(keyCode)) {
            if (value != null && level.myInlineActions.hasButtons(value) && nextButton(level, value)) {
                return;
            }

            handleSelect(level, false);
        }
        else if (KeyCode.LEFT.equals(keyCode)) {
            if (value != null && level.myInlineActions.hasButtons(value) && previousButton(level)) {
                return;
            }

            if (myLevels.size() > 1 && myLevels.peek() == level) {
                level.myPopup.close();
            }
        }
    }

    @RequiredUIAccess
    private void onKeyReleased(Level level, KeyboardInputDetails details) {
        if (isDisposed() || !myLevels.contains(level)) {
            return;
        }

        for (ListPopupKeyListener listener : new ArrayList<>(myKeyListeners)) {
            if (listener.keyReleased(this, details)) {
                return;
            }
        }
    }

    @RequiredUIAccess
    private boolean nextButton(Level level, Object value) {
        int count = level.myInlineActions.getButtonsCount(value);
        int current = level.myActiveButton;
        if (current >= count - 1) {
            return false;
        }

        int next = current + 1;
        setActiveButton(level, next);

        if (count == 1 && level.myInlineActions.isMoreButton(value, next)) {
            performButton(level, value, next);
        }
        return true;
    }

    @RequiredUIAccess
    private boolean previousButton(Level level) {
        int current = level.myActiveButton;
        if (current < 0) {
            return false;
        }

        setActiveButton(level, current - 1);
        return true;
    }

    @RequiredUIAccess
    private void handleSelect(Level level, boolean handleFinalChoices) {
        handleSelect(level, handleFinalChoices, null);
    }

    @RequiredUIAccess
    private void handleSelect(Level level, boolean handleFinalChoices, @Nullable InputDetails details) {
        Object value = level.myList.getValue();
        if (isDisposed() || value == null || !level.myStep.isSelectable(value)) {
            return;
        }

        if (!handleFinalChoices && !level.myStep.hasSubstep(value)) {
            return;
        }

        if (level.myActiveButton >= 0) {
            performButton(level, value, level.myActiveButton, details);
            return;
        }

        choose(level, level.myStep, value, handleFinalChoices, details);
    }

    @RequiredUIAccess
    private void performButton(Level level, Object value, int index) {
        performButton(level, value, index, null);
    }

    @RequiredUIAccess
    private void performButton(Level level, Object value, int index, @Nullable InputDetails details) {
        if (isDisposed() || !myLevels.contains(level)) {
            return;
        }

        PopupInlineActions inlineActions = level.myInlineActions;
        if (inlineActions.isMoreButton(value, index)) {
            choose(level, level.myStep, value, false, details);
            return;
        }

        ActionPopupItem item = inlineActions.getInlineItem(value, index);
        ListPopupStep<?> listStep = level.myStep;
        if (item == null || !(listStep instanceof ActionPopupStep step)) {
            return;
        }

        if (!ActionImplUtil.isKeepPopupOpen(inlineActions.getKeepPopupOnPerform(value, index), details)) {
            markOk();
            unwindTo(null);

            step.performActionItem(item, null);
            return;
        }

        step.performActionItem(item, null);

        updateStepItems(level, step);
    }

    @RequiredUIAccess
    private void updateStepItems(Level level, ActionPopupStep step) {
        step.updateStepItems().whenComplete((ignored, throwable) -> {
            if (throwable == null && !isDisposed() && myLevels.contains(level)) {
                level.myList.setRender(renderFor(level.myStep));
            }
        });
    }

    private void fireSelectionChanged(@Nullable Object value) {
        for (Consumer<Object> listener : new ArrayList<>(mySelectionListeners)) {
            // a listener previews the choice, and a preview which fails is no reason to refuse the choice itself
            try {
                listener.accept(value);
            }
            catch (Throwable e) {
                LOG.error("Popup selection listener failed", e);
            }
        }
    }

    @RequiredUIAccess
    private void choose(@Nullable Level level, ListPopupStep<Object> step, Object value, boolean finalChoice) {
        choose(level, step, value, finalChoice, null);
    }

    @RequiredUIAccess
    private void choose(
        @Nullable Level level,
        ListPopupStep<Object> step,
        Object value,
        boolean finalChoice,
        @Nullable InputDetails details
    ) {
        PopupStep<?> next;
        try {
            next = step.onChosen(value, finalChoice);
        }
        catch (Throwable e) {
            // the step failed to carry the choice out, but a choice was still made - there is nothing further to
            // show, and leaving the popup up would only offer the same choice again
            LOG.error("Popup step failed to handle the chosen value", e);
            next = PopupStep.FINAL_CHOICE;
        }

        if (next instanceof AsyncPopupStep<?> asyncStep) {
            UIAccess uiAccess = UIAccess.current();

            CompletableFuture.supplyAsync(() -> {
                try {
                    return asyncStep.call();
                }
                catch (Exception e) {
                    throw new CompletionException(e);
                }
            }).whenCompleteAsync((resolved, throwable) -> {
                if (isDisposed()) {
                    return;
                }

                if (throwable != null) {
                    LOG.error("Popup step failed to build its substep", throwable);
                    unwindTo(null);
                }
                else if (resolved instanceof ListPopupStep<?> resolvedListStep && closeAbove(level)) {
                    pushLevel(resolvedListStep);
                }
            }, uiAccess);
            return;
        }

        ListPopupStep<?> listStep = step;
        if (next == PopupStep.FINAL_CHOICE
            && level != null
            && myLevels.contains(level)
            && value instanceof ActionPopupItem item
            && listStep instanceof ActionPopupStep actionStep
            && ActionImplUtil.isKeepPopupOpen(item.getKeepPopupOnPerform(), details)
            && actionStep.isSelectable(item)) {
            if (!ActionImplUtil.isKeepPopupOpen(item.getKeepPopupOnPerform(), (InputDetails) null)) {
                actionStep.performActionItem(item, null);
            }
            updateStepItems(level, actionStep);
            return;
        }

        if (next == PopupStep.FINAL_CHOICE || !(next instanceof ListPopupStep<?> nextListStep)) {
            markOk();

            if (getFinalRunnable() == null) {
                setFinalRunnable(step.getFinalRunnable());
            }

            unwindTo(null);
            return;
        }

        if (closeAbove(level)) {
            pushLevel(nextListStep);
        }
    }

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    private void pushLevel(ListPopupStep<?> step) {
        Level parent = myLevels.peek();

        Level level = new Level((ListPopupStep<Object>) step, parent != null);

        myLevels.push(level);

        Popup popup = level.myPopup;
        if (parent != null && popup instanceof LightPopup light) {
            // anchored to the popup which owns the row rather than the row itself - how many components a list
            // makes for its rows is the frontend's business, so a row is not something to hold on to
            light.showBy(parent.myPopup);
        }
        else if (myAnchor != null && myAnchorPoint != null) {
            popup.showAt(myAnchor, myAnchorPoint.x(), myAnchorPoint.y(), myAnchorHeight);
        }
        else if (myAnchor != null && myAnchorDetails instanceof MouseInputDetails mouse) {
            // a menu asked for by pointing at something opens where it was pointed at, not at the corner of
            // whatever was pointed at
            popup.showAt(myAnchor, mouse.getX(), mouse.getY(), 0);
        }
        else if (myAnchor != null && popup instanceof LightPopup light) {
            light.showBy(myAnchor);
        }
        else if (popup instanceof HeavyPopup heavy) {
            heavy.showInCenterOf(null);
        }
    }

    @Override
    @RequiredUIAccess
    public void showCenteredInCurrentWindow(ComponentManager project) {
        myAnchor = null;
        myAnchorDetails = null;
        show();
    }

    @Override
    @RequiredUIAccess
    public void showBy(consulo.ui.Component component, @Nullable InputDetails inputDetails) {
        myAnchor = component;
        myAnchorDetails = inputDetails;
        show();
    }

    @Override
    @RequiredUIAccess
    public void showAtPoint(consulo.ui.Component target, int x, int y, int anchorHeight) {
        myAnchor = target;
        myAnchorDetails = null;
        myAnchorPoint = new Point2D(x, y);
        myAnchorHeight = anchorHeight;
        show();
    }

    @RequiredUIAccess
    private void show() {
        UIAccess uiAccess = UIAccess.current();

        myRootStep.whenCompleteAsync((step, throwable) -> {
            if (isDisposed()) {
                return;
            }

            if (throwable != null) {
                // the action runner drops whatever this throws into a future nobody reads, so an unlogged failure
                // here looks exactly like a popup which silently did not open
                LOG.error("Failed to build popup step", throwable);
                return;
            }

            try {
                if (handleAutoSelection(step)) {
                    return;
                }

                fireBeforeShown();

                pushLevel(step);
            }
            catch (Throwable e) {
                LOG.error("Failed to show popup", e);
            }
        }, uiAccess);
    }

    /**
     * Mirrors {@code ListPopupImpl#beforeShow} - a step which asked for it and offers a single way forward is taken
     * without ever putting a list on screen.
     */
    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    private boolean handleAutoSelection(ListPopupStep step) {
        if (!myAutoHandleBeforeShow) {
            return false;
        }

        int selectable = 0;
        Object single = null;
        for (Object value : step.getValues()) {
            if (step.isSelectable(value)) {
                selectable++;
                single = value;
            }
        }

        if (selectable != 1) {
            return false;
        }

        choose(null, (ListPopupStep<Object>) step, single, true);
        return true;
    }

    @RequiredUIAccess
    private boolean closeAbove(@Nullable Level level) {
        if (level == null) {
            return true;
        }

        if (myUnwinding || !myLevels.contains(level)) {
            return false;
        }

        myUnwinding = true;
        try {
            while (myLevels.peek() != level) {
                Level top = myLevels.pop();

                if (!isOk()) {
                    top.myStep.canceled();
                }

                if (top.myPopup.isVisible()) {
                    top.myPopup.close();
                }
            }
        }
        finally {
            myUnwinding = false;
        }
        return true;
    }

    /**
     * Closes every level down to and including {@code level}, or the whole stack when it is {@code null}. Emptying
     * the stack is what ends the popup, so dismissing a submenu leaves the popup which owns it standing.
     */
    @RequiredUIAccess
    private void unwindTo(@Nullable Popup level) {
        if (isDisposed() || myUnwinding) {
            return;
        }

        myUnwinding = true;
        try {
            while (!myLevels.isEmpty()) {
                Level top = myLevels.pop();

                if (!isOk()) {
                    top.myStep.canceled();
                }

                if (top.myPopup != level && top.myPopup.isVisible()) {
                    top.myPopup.close();
                }

                if (top.myPopup == level) {
                    break;
                }
            }
        }
        finally {
            myUnwinding = false;
        }

        if (myLevels.isEmpty()) {
            finish();
        }
    }

    @Override
    @RequiredUIAccess
    public void cancel(@Nullable InputEvent e) {
        unwindTo(null);
    }

    @Override
    @RequiredUIAccess
    public void handleSelect(boolean handleFinalChoices) {
        Level top = myLevels.peek();
        if (top != null) {
            handleSelect(top, handleFinalChoices);
        }
    }

    @Override
    @RequiredUIAccess
    public void handleSelect(boolean handleFinalChoices, InputEvent e) {
        handleSelect(handleFinalChoices);
    }

    @Override
    public void setHandleAutoSelectionBeforeShow(boolean autoHandle) {
        myAutoHandleBeforeShow = autoHandle;
    }

    @Override
    public void addSelectionListener(Consumer<Object> selectionListener) {
        mySelectionListeners.add(selectionListener);
    }

    @Override
    public void addKeyListener(ListPopupKeyListener listener) {
        myKeyListeners.add(listener);
    }

    @Override
    public @Nullable Object getSelectedValue() {
        Level top = myLevels.peek();
        return top == null ? null : top.myList.getValue();
    }

    @Override
    @RequiredUIAccess
    public void setSelectedValue(Object value) {
        Level top = myLevels.peek();
        if (top != null && top.myStep.isSelectable(value)) {
            top.myList.setValue(value);
        }
    }

    @Override
    public @Nullable String getSpeedSearchText() {
        Level top = myLevels.peek();
        return top == null ? null : top.myList.getSpeedSearchText();
    }

    @Override
    public boolean isVisible() {
        Level top = myLevels.peek();
        return top != null && top.myPopup.isVisible();
    }

    @Override
    public @Nullable UIAccess getUIAccess() {
        Level top = myLevels.peek();
        return top == null ? null : top.myPopup.getUIAccess();
    }

    @Override
    public void setCaption(String title) {
        Level root = myLevels.peekLast();
        if (root != null) {
            root.myPopup.setTitle(title);
        }
    }

    @Override
    public void addListSelectionListener(ListSelectionListener listSelectionListener) {
        throw new UnsupportedOperationException();
    }
}

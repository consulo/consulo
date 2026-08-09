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
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.util.collection.ArrayUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.UIAccess;
import consulo.ui.ex.popup.AsyncPopupStep;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.ex.popup.PopupStep;
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

    private record Level(ListPopupStep step, Popup popup) {
    }

    private final @Nullable ComponentManager myProject;
    private final CompletableFuture<? extends ListPopupStep> myRootStep;

    private final List<Consumer<Object>> mySelectionListeners = new ArrayList<>();

    /**
     * Innermost step first, so the top of the stack is the one the user is looking at.
     */
    private final Deque<Level> myLevels = new ArrayDeque<>();

    private @Nullable ListBox<Object> myTopList;
    private @Nullable TextItemRender<Object> myRender;
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
        return top == null ? myRootStep.join() : top.step();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setRender(TextItemRender<?> render) {
        myRender = (TextItemRender<Object>) render;

        ListBox<Object> list = myTopList;
        if (list != null) {
            list.setRender(myRender);
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
    private Popup buildPopup(ListPopupStep step, boolean nested) {
        PopupOptions.Builder options = PopupOptions.builder();
        if (myResizable) {
            options.resizable();
        }
        if (nested) {
            options.position(PopupPosition.END);
        }

        PopupOptions built = options.build();

        Popup popup = nested ? LightPopup.create(built) : HeavyPopup.create(built);

        popup.setTitle(step.getTitle());
        popup.setContent(buildList(step));
        popup.setMinimumWidth(myMinimumWidth);

        popup.addCloseListener(event -> unwindTo(popup));

        return popup;
    }

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    private ListBox<Object> buildList(ListPopupStep step) {
        ListBox<Object> list = ListBox.create(step.getValues());

        TextItemRender<Object> render = myRender;
        list.setRender(render != null ? render : (presentation, item) -> {
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
        });

        list.isSeparator(step::isSeparator);

        // the pointer is what a popup is chosen with, so the row under it is the one the popup is offering - the
        // awt list does the same from its mouse motion listener
        list.setSelectOnHover(true);

        int defaultIndex = step.getDefaultOptionIndex();
        if (defaultIndex >= 0 && defaultIndex < step.getValues().size()) {
            list.setValueByIndex(defaultIndex);
        }

        // moving over the rows only previews them - what a listener does with that is its own, the breakpoint
        // chooser marks the range each variant would cover
        list.addValueListener(event -> fireSelectionChanged(event.getValue()));

        // the choice is the click, not the move onto the row: a step preselects its default, and choosing from a
        // change of value would refuse the row the selection already sits on
        list.addClickListener(event -> onValueChosen(step, list.getValue()));

        myTopList = list;
        return list;
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
    private void onValueChosen(ListPopupStep step, @Nullable Object value) {
        if (isDisposed() || value == null || !step.isSelectable(value)) {
            return;
        }

        PopupStep next;
        try {
            next = step.onChosen(value, true);
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
                else if (resolved instanceof ListPopupStep resolvedListStep) {
                    pushLevel(resolvedListStep);
                }
            }, uiAccess);
            return;
        }

        if (next == PopupStep.FINAL_CHOICE || !(next instanceof ListPopupStep nextListStep)) {
            markOk();

            if (getFinalRunnable() == null) {
                setFinalRunnable(step.getFinalRunnable());
            }

            unwindTo(null);
            return;
        }

        pushLevel(nextListStep);
    }

    @RequiredUIAccess
    private void pushLevel(ListPopupStep step) {
        Level parent = myLevels.peek();

        Popup popup = buildPopup(step, parent != null);

        myLevels.push(new Level(step, popup));

        if (parent != null && popup instanceof LightPopup light) {
            // anchored to the popup which owns the row rather than the row itself - how many components a list
            // makes for its rows is the frontend's business, so a row is not something to hold on to
            light.showBy(parent.popup());
        }
        else if (myAnchor != null && myAnchorPoint != null) {
            popup.showAt(myAnchor, myAnchorPoint.x(), myAnchorPoint.y(), myAnchorHeight);
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

            if (handleAutoSelection(step)) {
                return;
            }

            fireBeforeShown();

            try {
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

        onValueChosen(step, single);
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
                    top.step().canceled();
                }

                if (top.popup() != level && top.popup().isVisible()) {
                    top.popup().close();
                }

                if (top.popup() == level) {
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
        ListBox<Object> list = myTopList;
        if (list != null) {
            onValueChosen(getListStep(), list.getValue());
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
    public boolean isVisible() {
        Level top = myLevels.peek();
        return top != null && top.popup().isVisible();
    }

    @Override
    public void setCaption(String title) {
        Level top = myLevels.peek();
        if (top != null) {
            top.popup().setTitle(title);
        }
    }

    @Override
    public void addListSelectionListener(ListSelectionListener listSelectionListener) {
        throw new UnsupportedOperationException();
    }
}

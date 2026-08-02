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
import consulo.ui.LightPopup;
import consulo.ui.LightPopupOptions;
import consulo.ui.LightPopupPosition;
import consulo.ui.ListBox;
import consulo.ui.TextItemRender;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import org.jspecify.annotations.Nullable;

import javax.swing.event.ListSelectionListener;
import java.awt.event.InputEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
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

    private record Level(ListPopupStep step, LightPopup popup) {
    }

    private final @Nullable ComponentManager myProject;
    private final ListPopupStep myRootStep;

    private final List<Consumer<Object>> mySelectionListeners = new ArrayList<>();

    /**
     * Innermost step first, so the top of the stack is the one the user is looking at.
     */
    private final Deque<Level> myLevels = new ArrayDeque<>();

    private @Nullable ListBox<Object> myTopList;

    private boolean myAutoHandleBeforeShow;
    // closing a level from here fires its close listener as well, and that listener is the one which unwinds the
    // stack - without this it would unwind the stack it is already unwinding
    private boolean myUnwinding;

    public UnifiedListPopupImpl(@Nullable ComponentManager project, ListPopupStep step) {
        myProject = project;
        myRootStep = step;
    }

    @Override
    public ListPopupStep getListStep() {
        Level top = myLevels.peek();
        return top == null ? myRootStep : top.step();
    }

    @RequiredUIAccess
    private LightPopup buildPopup(ListPopupStep step, boolean nested) {
        LightPopupOptions.Builder options = LightPopupOptions.builder();
        if (nested) {
            options.position(LightPopupPosition.END);
        }

        LightPopup popup = LightPopup.create(options.build());

        popup.setTitle(step.getTitle());
        popup.setContent(buildList(step));

        popup.addCloseListener(event -> unwindTo(popup));

        return popup;
    }

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    private ListBox<Object> buildList(ListPopupStep step) {
        ListBox<Object> list = ListBox.create(step.getValues());

        list.setRender((TextItemRender<Object>) (presentation, item) -> {
            Object value = item.getValue();
            if (value == null) {
                return;
            }

            presentation.withIcon(step.getIconFor(value));
            presentation.append(step.getTextFor(value));
        });

        int defaultIndex = step.getDefaultOptionIndex();
        if (defaultIndex >= 0 && defaultIndex < step.getValues().size()) {
            list.setValueByIndex(defaultIndex);
        }

        list.addValueListener(event -> onValueChosen(step, event.getValue()));

        myTopList = list;
        return list;
    }

    @RequiredUIAccess
    private void onValueChosen(ListPopupStep step, @Nullable Object value) {
        if (isDisposed() || value == null || !step.isSelectable(value)) {
            return;
        }

        for (Consumer<Object> listener : new ArrayList<>(mySelectionListeners)) {
            // a listener previews the choice, and a preview which cannot be applied is still a choice - letting it
            // throw here would leave the popup open over a selection it already reported
            try {
                listener.accept(value);
            }
            catch (Throwable e) {
                LOG.error("Popup selection listener failed", e);
            }
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

        LightPopup popup = buildPopup(step, parent != null);

        myLevels.push(new Level(step, popup));

        if (parent == null) {
            popup.showInCenterOf(null);
        }
        else {
            // anchored to the popup which owns the row rather than the row itself - how many components a list
            // makes for its rows is the frontend's business, so a row is not something to hold on to
            popup.showBy(parent.popup());
        }
    }

    @Override
    @RequiredUIAccess
    public void showCenteredInCurrentWindow(ComponentManager project) {
        show();
    }

    @Override
    @RequiredUIAccess
    public void showBy(consulo.ui.Component component, InputDetails inputDetails) {
        show();
    }

    @RequiredUIAccess
    private void show() {
        if (handleAutoSelection()) {
            return;
        }

        fireBeforeShown();

        try {
            pushLevel(myRootStep);
        }
        catch (Throwable e) {
            // the action runner drops whatever this throws into a future nobody reads, so an unlogged failure
            // here looks exactly like a popup which silently did not open
            consulo.logging.Logger.getInstance(UnifiedListPopupImpl.class).error("Failed to show popup", e);
            throw e;
        }
    }

    /**
     * Mirrors {@code ListPopupImpl#beforeShow} - a step which asked for it and offers a single way forward is taken
     * without ever putting a list on screen.
     */
    @RequiredUIAccess
    private boolean handleAutoSelection() {
        if (!myAutoHandleBeforeShow) {
            return false;
        }

        int selectable = 0;
        Object single = null;
        for (Object value : myRootStep.getValues()) {
            if (myRootStep.isSelectable(value)) {
                selectable++;
                single = value;
            }
        }

        if (selectable != 1) {
            return false;
        }

        onValueChosen(myRootStep, single);
        return true;
    }

    /**
     * Closes every level down to and including {@code level}, or the whole stack when it is {@code null}. Emptying
     * the stack is what ends the popup, so dismissing a submenu leaves the popup which owns it standing.
     */
    @RequiredUIAccess
    private void unwindTo(@Nullable LightPopup level) {
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

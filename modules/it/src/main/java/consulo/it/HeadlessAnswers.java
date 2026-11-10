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
package consulo.it;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.it.internal.HeadlessUIAccess;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.MessageButtonRole;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Answers the message boxes, input boxes, color pickers and dialogs of a test by their id, as a user would. A box or
 * dialog shown without a registered answer - or while no answers are installed - is logged as an error, which fails
 * the test: a box fails its result, a dialog is cancelled.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public final class HeadlessAnswers {
    private static final Logger LOG = Logger.getInstance(HeadlessAnswers.class);

    private final Map<String, MessageButtonRole> myMessageBoxes = new LinkedHashMap<>();
    private final Map<String, ValueAnswer> myInputBoxes = new LinkedHashMap<>();
    private final Map<String, ValueAnswer> myColorPickers = new LinkedHashMap<>();
    private final Map<String, Consumer<HeadlessDialog>> myDialogs = new LinkedHashMap<>();
    private final Map<Class<? extends DialogDescriptor>, Consumer<HeadlessDialog>> myDialogTypes = new LinkedHashMap<>();
    private final Set<Object> myUsed = new LinkedHashSet<>();
    private final List<String> myShown = new ArrayList<>();
    private final List<String> myFailures = new ArrayList<>();
    private int myRunning;
    private volatile boolean myChecked;

    private HeadlessAnswers() {
    }

    /**
     * Installs the answers on the headless UI for the lifetime of the parent; only one set can be installed at a time.
     * An answer that was never used, or a failed handler, is logged as an error on disposal unless
     * {@link #assertAllAnswered()} already checked it.
     */
    public static HeadlessAnswers of(Disposable parent) {
        HeadlessAnswers answers = new HeadlessAnswers();
        HeadlessUIAccess.INSTANCE.installAnswers(answers);
        Disposer.register(parent, () -> {
            HeadlessUIAccess.INSTANCE.removeAnswers(answers);
            if (!answers.myChecked) {
                List<String> problems = answers.problems();
                if (!problems.isEmpty()) {
                    LOG.error(String.join("\n", problems));
                }
            }
        });
        return answers;
    }

    public synchronized HeadlessAnswers messageBox(String id, MessageButtonRole role) {
        myMessageBoxes.put(id, role);
        return this;
    }

    /**
     * Confirms the input box with the value; null dismisses it.
     */
    public synchronized HeadlessAnswers inputBox(String id, @Nullable Object value) {
        myInputBoxes.put(id, new ValueAnswer(value));
        return this;
    }

    /**
     * Picks the color; null dismisses the picker.
     */
    public synchronized HeadlessAnswers colorPicker(String id, @Nullable ColorValue color) {
        myColorPickers.put(id, new ValueAnswer(color));
        return this;
    }

    /**
     * Answers the dialog whose descriptor has the id.
     */
    public synchronized HeadlessAnswers dialog(String id, Consumer<HeadlessDialog> handler) {
        myDialogs.put(id, handler);
        return this;
    }

    /**
     * Answers every dialog whose descriptor is an instance of the class, when no answer is registered for its id.
     */
    public synchronized HeadlessAnswers dialog(Class<? extends DialogDescriptor> descriptorClass, Consumer<HeadlessDialog> handler) {
        myDialogTypes.put(descriptorClass, handler);
        return this;
    }

    /**
     * @return the ids of the message boxes and dialogs shown so far, in order
     */
    public synchronized List<String> shown() {
        return List.copyOf(myShown);
    }

    /**
     * Fails when a registered message box or dialog was never shown - the flow went on without asking - or when a dialog
     * was unexpected or its handler failed.
     */
    public void assertAllAnswered() {
        myChecked = true;
        List<String> problems = problems();
        if (!problems.isEmpty()) {
            throw new AssertionError(String.join("\n", problems));
        }
    }

    /**
     * Waits until every registered answer was used and its handler returned, then {@link #assertAllAnswered() asserts}.
     */
    public void awaitAllAnswered() throws InterruptedException {
        awaitAllAnswered(Duration.ofSeconds(60));
    }

    public void awaitAllAnswered(Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        synchronized (this) {
            while (myRunning > 0 || !unusedAnswers().isEmpty()) {
                long left = deadline - System.nanoTime();
                if (left <= 0) {
                    break;
                }
                wait(Math.max(1, left / 1_000_000));
            }
        }
        assertAllAnswered();
    }

    public synchronized @Nullable MessageButtonRole messageBoxAnswer(@Nullable String id) {
        MessageButtonRole role = id == null ? null : myMessageBoxes.get(id);
        if (role != null) {
            use(id);
        }
        return role;
    }

    public synchronized @Nullable ValueAnswer inputBoxAnswer(@Nullable String id) {
        ValueAnswer answer = id == null ? null : myInputBoxes.get(id);
        if (answer != null) {
            use(id);
        }
        return answer;
    }

    public synchronized @Nullable ValueAnswer colorPickerAnswer(@Nullable String id) {
        ValueAnswer answer = id == null ? null : myColorPickers.get(id);
        if (answer != null) {
            use(id);
        }
        return answer;
    }

    /**
     * Reports a box nobody answered, whether or not answers are installed, and returns the failure for its result.
     */
    public static IllegalStateException unexpected(@Nullable HeadlessAnswers answers, String kind, @Nullable String id, LocalizeValue text) {
        String message = "unexpected " + kind + ": " + (id != null ? id : "<no id> " + text.get());
        if (answers != null) {
            synchronized (answers) {
                answers.myShown.add(id != null ? id : text.get());
            }
            answers.fail(message, null);
        }
        else {
            LOG.error(message);
        }
        return new IllegalStateException(message);
    }

    public void dialogShown(Dialog dialog) {
        DialogDescriptor descriptor = dialog.getDescriptor();
        String id = descriptor.getId();
        Consumer<HeadlessDialog> handler = findHandler(id, descriptor);

        HeadlessDialog headlessDialog = new HeadlessDialog(dialog);
        try {
            if (handler == null) {
                fail("unexpected dialog: " + id, null);
                headlessDialog.pressCancel();
                return;
            }
            handler.accept(headlessDialog);
        }
        catch (Throwable e) {
            fail("answer of " + id + " failed: " + e, e);
            headlessDialog.pressCancel();
        }
        finally {
            synchronized (this) {
                myRunning--;
                notifyAll();
            }
        }
    }

    /**
     * Cancels a dialog shown while no answers are installed, and logs it as an error.
     */
    public static void dialogWithoutAnswers(Dialog dialog) {
        LOG.error("dialog shown while no answers are installed: " + dialog.getDescriptor().getId());
        new HeadlessDialog(dialog).pressCancel();
    }

    private void use(String id) {
        myShown.add(id);
        myUsed.add(id);
        notifyAll();
    }

    private synchronized @Nullable Consumer<HeadlessDialog> findHandler(String id, DialogDescriptor descriptor) {
        myShown.add(id);

        Consumer<HeadlessDialog> handler = myDialogs.get(id);
        if (handler != null) {
            myUsed.add(id);
            myRunning++;
            return handler;
        }
        for (Map.Entry<Class<? extends DialogDescriptor>, Consumer<HeadlessDialog>> entry : myDialogTypes.entrySet()) {
            if (entry.getKey().isInstance(descriptor)) {
                myUsed.add(entry.getKey());
                myRunning++;
                return entry.getValue();
            }
        }
        myRunning++;
        return null;
    }

    private synchronized List<String> problems() {
        List<String> problems = new ArrayList<>(myFailures);
        problems.addAll(unusedAnswers());
        return problems;
    }

    private synchronized List<String> unusedAnswers() {
        List<String> unused = new ArrayList<>();
        for (String id : myMessageBoxes.keySet()) {
            if (!myUsed.contains(id)) {
                unused.add("message box was never shown: " + id);
            }
        }
        for (String id : myInputBoxes.keySet()) {
            if (!myUsed.contains(id)) {
                unused.add("input box was never shown: " + id);
            }
        }
        for (String id : myColorPickers.keySet()) {
            if (!myUsed.contains(id)) {
                unused.add("color picker was never shown: " + id);
            }
        }
        for (String id : myDialogs.keySet()) {
            if (!myUsed.contains(id)) {
                unused.add("dialog was never shown: " + id);
            }
        }
        for (Class<? extends DialogDescriptor> type : myDialogTypes.keySet()) {
            if (!myUsed.contains(type)) {
                unused.add("dialog was never shown: " + type.getName());
            }
        }
        return unused;
    }

    private void fail(String message, @Nullable Throwable e) {
        synchronized (this) {
            myFailures.add(message);
        }
        LOG.error(message, e);
    }

    /**
     * A confirmed value, or null for a dismissal.
     */
    public record ValueAnswer(@Nullable Object value) {
    }
}

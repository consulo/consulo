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
package consulo.ui.impl;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.MessageButtonRole;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Answers message and input boxes from a script, for the frontends which have no display.
 * <p>
 * A box nobody scripted fails its result rather than guessing or hanging - an unexpected dialog is
 * a test failure, and a result which never arrives is the worst way to discover one.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public final class ScriptedDialogs {
    public enum Unexpected {
        FAIL,
        DISMISS
    }

    private static @Nullable ScriptedDialogs ourInstalled;

    private final Unexpected myUnexpected;
    private final Queue<Object> myAnswers = new LinkedList<>();
    private final List<Object> mySeen = new ArrayList<>();

    private ScriptedDialogs(Unexpected unexpected) {
        myUnexpected = unexpected;
    }

    /**
     * Installs a script for as long as the parent lives.
     */
    public static ScriptedDialogs install(Disposable parent, Unexpected unexpected) {
        ScriptedDialogs dialogs = new ScriptedDialogs(unexpected);
        ourInstalled = dialogs;
        Disposer.register(parent, () -> {
            if (ourInstalled == dialogs) {
                ourInstalled = null;
            }
        });
        return dialogs;
    }

    public static @Nullable ScriptedDialogs installed() {
        return ourInstalled;
    }

    /**
     * Answers the next message box by pressing the button carrying this role.
     */
    public ScriptedDialogs expect(MessageButtonRole role) {
        myAnswers.add(role);
        return this;
    }

    /**
     * Answers the next input box with this value. Null dismisses it.
     */
    public ScriptedDialogs expectInput(@Nullable Object value) {
        myAnswers.add(new InputAnswer(value));
        return this;
    }

    /**
     * Every box which was shown, in the order it was shown.
     */
    public List<Object> seen() {
        return List.copyOf(mySeen);
    }

    public Unexpected unexpected() {
        return myUnexpected;
    }

    public void record(Object request) {
        mySeen.add(request);
    }

    public @Nullable Object nextAnswer() {
        return myAnswers.poll();
    }

    public record InputAnswer(@Nullable Object value) {
    }
}

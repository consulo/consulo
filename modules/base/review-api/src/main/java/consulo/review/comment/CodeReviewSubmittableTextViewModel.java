// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.comment;

import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.review.list.UserPresentation;
import consulo.review.util.ComputedResult;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public interface CodeReviewSubmittableTextViewModel {
    Project getProject();

    /**
     * Input text state
     */
    MutableObservableValue<String> getText();

    /**
     * State of submission progress
     * <p>
     * null means that submission wasn't started yet
     */
    ObservableValue<@Nullable ComputedResult<?>> getState();

    void addFocusRequestListener(Disposable parentDisposable, Runnable listener);

    void requestFocus();

    /**
     * Who writes the text - the picture shown before the input - or {@code null} to show none.
     */
    default @Nullable UserPresentation getCurrentUserPresentation() {
        return null;
    }

    /**
     * Prepares the editor the text is written in, or {@code null} to write it in a message input. With a setup the
     * input is a markdown editor sent with {@code Ctrl+Enter}, so completion contributors can offer what they read from
     * the editor the setup marked.
     */
    default @Nullable Consumer<Editor> getEditorSetup() {
        return null;
    }
}

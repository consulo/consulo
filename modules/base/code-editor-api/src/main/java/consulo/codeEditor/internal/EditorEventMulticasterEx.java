// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.internal;

import consulo.codeEditor.event.EditorEventMulticaster;
import consulo.codeEditor.event.FocusChangeListener;
import consulo.disposer.Disposable;
import consulo.document.internal.EditReadOnlyListener;
import jakarta.annotation.Nonnull;
import kava.beans.PropertyChangeListener;

public interface EditorEventMulticasterEx extends EditorEventMulticaster {
    void addErrorStripeListener(@Nonnull ErrorStripeListener listener, @Nonnull Disposable parentDisposable);

    void addEditReadOnlyListener(@Nonnull EditReadOnlyListener listener, @Nonnull Disposable parentDisposable);

    void addPropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull Disposable parentDisposable);

    void addFocusChangeListener(@Nonnull FocusChangeListener listener, @Nonnull Disposable parentDisposable);
}

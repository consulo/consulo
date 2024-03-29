// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.event;

import consulo.disposer.Disposable;
import consulo.codeEditor.CaretAction;
import consulo.codeEditor.CaretModel;

import java.util.EventListener;

/**
 * A listener which will be notified when {@link CaretModel#runForEachCaret(CaretAction)} or
 * {@link CaretModel#runForEachCaret(CaretAction, boolean)} is invoked.
 *
 * @see CaretModel#addCaretActionListener(CaretActionListener, Disposable)
 */
public interface CaretActionListener extends EventListener {
  default void beforeAllCaretsAction() {
  }

  default void afterAllCaretsAction() {
  }
}

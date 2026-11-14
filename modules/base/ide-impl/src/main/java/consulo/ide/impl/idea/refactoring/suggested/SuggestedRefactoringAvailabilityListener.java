// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.refactoring.suggested;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringState;

@TopicAPI(ComponentScope.PROJECT)
public interface SuggestedRefactoringAvailabilityListener {
    default void cleared() {
    }

    default void disabled() {
    }

    default void updated(SuggestedRefactoringState state) {
    }

    default void reset() {
    }
}

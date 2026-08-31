// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

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
package consulo.language.editor.ui.navigation;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiElement;
import consulo.navigation.TargetPresentation;

import java.util.Comparator;

/**
 * Extracts everything a target popup needs to draw one row. Implementations are the replacement for
 * subclasses of the removed {@code PsiElementListCellRenderer}.
 * <p>
 * Called off the UI thread under a read lock. A renderer never calls this - by the time a row is
 * painted its {@link TargetPresentation} is already built.
 */
@FunctionalInterface
public interface TargetPresentationProvider<T extends PsiElement> {
    @RequiredReadAction
    TargetPresentation getPresentation(T element);

    /**
     * Orders elements the way this provider would show them. Builds a presentation per comparison, so it
     * is meant for the short lists that feed tooltips and prefetches, not for the popup model.
     */
    @RequiredReadAction
    default Comparator<T> comparator() {
        return Comparator.comparing(element -> {
            TargetPresentation presentation = getPresentation(element);
            String text = presentation.getPresentableText().get();
            return presentation.getContainerText().isEmpty() ? text : text + " " + presentation.getContainerText().get();
        });
    }
}

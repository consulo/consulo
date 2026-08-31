/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.editor.gutter;

import consulo.language.psi.PsiElement;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEvent;

/**
 * @author max
 */
public interface GutterIconNavigationHandler<T extends PsiElement> {
    /**
     * @param e the event the icon was activated with - where it happened comes from the event's input
     *          details, so a handler works the same on every frontend
     */
    @RequiredUIAccess
    void navigate(ComponentEvent<?> e, T elt);
}

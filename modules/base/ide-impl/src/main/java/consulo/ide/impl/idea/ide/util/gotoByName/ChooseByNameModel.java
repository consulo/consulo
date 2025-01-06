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
package consulo.ide.impl.idea.ide.util.gotoByName;

import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

public interface ChooseByNameModel {
    String getPromptText();

    String getNotInMessage();

    String getNotFoundMessage();

    /**
     * @return consulo.localize.LocalizeValue#of() for disable
     */
    @Nonnull
    LocalizeValue getCheckBoxName();

    boolean loadInitialCheckBoxState();

    void saveInitialCheckBoxState(boolean state);

    ListCellRenderer getListCellRenderer();

    /**
     * Returns the list of names to show in the chooser.
     *
     * @param checkBoxState the current state of the chooser checkbox (for example, [x] Include non-project classes for Ctrl-N)
     * @return the names to show. All items in the returned array must be non-null.
     */
    @Nonnull
    String[] getNames(boolean checkBoxState);

    @Nonnull
    Object[] getElementsByName(String name, boolean checkBoxState, final String pattern);

    @Nullable
    String getElementName(Object element);

    @Nonnull
    String[] getSeparators();

    @Nullable
    String getFullName(Object element);

    @Nullable
    String getHelpId();

    boolean willOpenEditor();

    boolean useMiddleMatching();
}
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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareToggleAction;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
public class SetChangesGroupingAction extends DumbAwareToggleAction {
    private final String myGroupingKey;
    private final Supplier<Set<String>> myGroupingKeys;
    private final BiConsumer<String, Boolean> mySetGrouping;

    public SetChangesGroupingAction(LocalizeValue text,
                                    String groupingKey,
                                    Supplier<Set<String>> groupingKeys,
                                    BiConsumer<String, Boolean> setGrouping) {
        super(text);
        myGroupingKey = groupingKey;
        myGroupingKeys = groupingKeys;
        mySetGrouping = setGrouping;
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        return myGroupingKeys.get().contains(myGroupingKey);
    }

    @RequiredUIAccess
    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        mySetGrouping.accept(myGroupingKey, state);
    }
}

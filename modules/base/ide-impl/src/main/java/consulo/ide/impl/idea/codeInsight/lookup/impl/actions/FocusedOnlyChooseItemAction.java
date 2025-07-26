/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.idea.codeInsight.lookup.impl.actions;

import consulo.annotation.component.ActionImpl;
import consulo.language.editor.completion.lookup.Lookup;

/**
 * @author VISTALL
 * @since 2025-07-26
 */
@ActionImpl(id = "EditorChooseLookupItem")
public class FocusedOnlyChooseItemAction extends ChooseItemAction {
    public FocusedOnlyChooseItemAction() {
        super(new Handler(true, Lookup.NORMAL_SELECT_CHAR));
    }

    @Override
    public int getExecuteWeight() {
        // must be executed first before EnterAction
        return Integer.MAX_VALUE;
    }
}

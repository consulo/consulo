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
package consulo.fileEditor.impl.internal.largeFileEditor.action;

import consulo.annotation.component.ExtensionImpl;
import consulo.ui.ex.action.IdeActions;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-09-20
 */
@ExtensionImpl
public class LfeEditorActionHandlerFindWordAtCaret extends LfeEditorActionHandlerDisabled {
    @Nonnull
    @Override
    public String getActionId() {
        return IdeActions.ACTION_FIND_WORD_AT_CARET;
    }
}

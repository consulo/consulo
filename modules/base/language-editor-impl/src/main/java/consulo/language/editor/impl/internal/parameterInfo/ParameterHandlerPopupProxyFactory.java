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
package consulo.language.editor.impl.internal.parameterInfo;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.codeEditor.Editor;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ParameterHandlerPopupProxyFactory {
    ParameterHandlerPopupProxy create(Editor editor);

    /**
     * Parameter info of the item selected in an open completion list - gone on any key or when the selection moves.
     */
    @RequiredUIAccess
    void showLookupHint(Editor editor, ParameterInfoModel model, boolean requestFocus);

    /**
     * @return hides what was shown
     */
    @RequiredUIAccess
    Runnable showLoading(Editor editor, String title, Runnable cancel);
}

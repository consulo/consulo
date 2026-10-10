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

import consulo.language.editor.completion.lookup.Lookup;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * The popup a parameter info controller shows its model in. Each frontend draws and places it its own way; the
 * controller only says what to show and when.
 *
 * @author VISTALL
 * @since 2026-10-10
 */
public interface ParameterHandlerPopupProxy {
    boolean isVisible();

    @RequiredUIAccess
    void show(ParameterInfoModel model, @Nullable ParameterInfoAnchor anchor, boolean requestFocus, boolean hideByTextChange);

    @RequiredUIAccess
    void update(ParameterInfoModel model, @Nullable ParameterInfoAnchor anchor);

    @RequiredUIAccess
    void adjustForLookup(Lookup lookup);

    @RequiredUIAccess
    void hide();
}

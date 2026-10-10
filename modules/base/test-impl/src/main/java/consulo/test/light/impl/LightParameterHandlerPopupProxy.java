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
package consulo.test.light.impl;

import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.internal.parameterInfo.ParameterHandlerPopupProxy;
import consulo.language.editor.internal.parameterInfo.ParameterInfoAnchor;
import consulo.language.editor.internal.parameterInfo.ParameterInfoModel;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 */
public class LightParameterHandlerPopupProxy implements ParameterHandlerPopupProxy {
    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    @RequiredUIAccess
    public void show(ParameterInfoModel model, @Nullable ParameterInfoAnchor anchor, boolean requestFocus, boolean hideByTextChange) {
    }

    @Override
    @RequiredUIAccess
    public void update(ParameterInfoModel model, @Nullable ParameterInfoAnchor anchor) {
    }

    @Override
    @RequiredUIAccess
    public void adjustForLookup(Lookup lookup) {
    }

    @Override
    @RequiredUIAccess
    public void hide() {
    }
}

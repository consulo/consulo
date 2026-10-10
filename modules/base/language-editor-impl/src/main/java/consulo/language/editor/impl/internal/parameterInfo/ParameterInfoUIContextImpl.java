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

import consulo.language.editor.parameterInfo.ParameterInfoUIContext;
import consulo.language.editor.parameterInfo.SignatureBuilder;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
final class ParameterInfoUIContextImpl implements ParameterInfoUIContext {
    private final ParameterInfoState myState;
    private final boolean mySingleParameterInfo;

    private int myIndex;
    private @Nullable SignatureBuilderImpl myResult;

    ParameterInfoUIContextImpl(ParameterInfoState state, boolean singleParameterInfo) {
        myState = state;
        mySingleParameterInfo = singleParameterInfo;
    }

    void reset(int index) {
        myIndex = index;
        myResult = null;
    }

    @Nullable SignatureBuilderImpl getResult() {
        return myResult;
    }

    @Override
    public SignatureBuilder signature() {
        return new SignatureBuilderImpl(builder -> myResult = builder);
    }

    @Override
    public boolean isUIComponentEnabled() {
        return myState.isEnabled(myIndex);
    }

    @Override
    public void setUIComponentEnabled(boolean enabled) {
        myState.setEnabled(myIndex, enabled);
    }

    @Override
    public int getCurrentParameterIndex() {
        return myState.getCurrentParameterIndex();
    }

    @Override
    public PsiElement getParameterOwner() {
        return myState.getParameterOwner();
    }

    @Override
    public boolean isSingleOverload() {
        return myState.getObjects().length == 1;
    }

    @Override
    public boolean isSingleParameterInfo() {
        return mySingleParameterInfo;
    }
}

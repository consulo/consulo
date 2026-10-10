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

import consulo.language.editor.parameterInfo.ParameterInfoHandler;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import consulo.language.editor.internal.parameterInfo.ParameterInfoModel;
import consulo.language.editor.internal.parameterInfo.ParameterInfoSignature;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public final class ParameterInfoState {
    private Object[] myObjects;
    private boolean[] myEnabled;
    private int myCurrentParameterIndex = -1;
    private @Nullable PsiElement myParameterOwner;
    private @Nullable Object myHighlighted;

    public ParameterInfoState(Object[] objects) {
        setObjects(objects);
    }

    public void setObjects(Object[] objects) {
        myObjects = objects;
        myEnabled = new boolean[objects.length];
        Arrays.fill(myEnabled, true);
    }

    public Object[] getObjects() {
        return myObjects;
    }

    public boolean isEnabled(int index) {
        return myEnabled[index];
    }

    public void setEnabled(int index, boolean enabled) {
        myEnabled[index] = enabled;
    }

    public int getCurrentParameterIndex() {
        return myCurrentParameterIndex;
    }

    public void setCurrentParameterIndex(int currentParameterIndex) {
        myCurrentParameterIndex = currentParameterIndex;
    }

    public @Nullable PsiElement getParameterOwner() {
        return myParameterOwner;
    }

    public void setParameterOwner(@Nullable PsiElement parameterOwner) {
        myParameterOwner = parameterOwner;
    }

    public @Nullable Object getHighlighted() {
        return myHighlighted;
    }

    public void setHighlighted(@Nullable Object highlighted) {
        myHighlighted = highlighted;
    }

    @SuppressWarnings("unchecked")
    public ParameterInfoModel buildModel(ParameterInfoHandler handler, boolean singleParameterInfo, LocalizeValue switchHint) {
        ParameterInfoUIContextImpl context = new ParameterInfoUIContextImpl(this, singleParameterInfo);

        List<ParameterInfoSignature> signatures = new ArrayList<>(myObjects.length);
        for (int i = 0; i < myObjects.length; i++) {
            boolean highlighted = myObjects[i].equals(myHighlighted);
            if (singleParameterInfo && myObjects.length > 1 && !highlighted) {
                continue;
            }

            context.reset(i);
            handler.updateUI(myObjects[i], context);

            SignatureBuilderImpl result = context.getResult();
            if (result == null) {
                continue;
            }

            signatures.add(new ParameterInfoSignature(
                result.getSegments(),
                result.isDisabled(),
                result.isDeprecated(),
                highlighted && !singleParameterInfo,
                !singleParameterInfo
            ));
        }

        if (!signatures.isEmpty()) {
            ParameterInfoSignature last = signatures.get(signatures.size() - 1);
            signatures.set(
                signatures.size() - 1,
                new ParameterInfoSignature(last.segments(), last.disabled(), last.deprecated(), last.highlighted(), false)
            );
        }

        return new ParameterInfoModel(List.copyOf(signatures), singleParameterInfo ? LocalizeValue.empty() : switchHint);
    }
}

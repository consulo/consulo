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
package consulo.versionControlSystem.internal;

import consulo.localize.LocalizeValue;
import consulo.versionControlSystem.VcsShowConfirmationOption;

public class VcsShowConfirmationOptionImpl extends VcsAbstractSetting implements VcsShowConfirmationOption {
    private Value myValue = Value.SHOW_CONFIRMATION;

    private final LocalizeValue myCaption;
    private final LocalizeValue myDoNothingCaption;
    private final LocalizeValue myShowConfirmationCaption;
    private final LocalizeValue myDoActionSilentlyCaption;

    public VcsShowConfirmationOptionImpl(
        String displayName,
        LocalizeValue caption,
        LocalizeValue doNothingCaption,
        LocalizeValue showConfirmationCaption,
        LocalizeValue doActionSilentlyCaption
    ) {
        super(displayName);
        myCaption = caption;
        myDoNothingCaption = doNothingCaption;
        myShowConfirmationCaption = showConfirmationCaption;
        myDoActionSilentlyCaption = doActionSilentlyCaption;
    }

    @Override
    public Value getValue() {
        return myValue;
    }

    @Override
    public void setValue(Value value) {
        myValue = value;
    }

    @Override
    public boolean isPersistent() {
        return true;
    }
}

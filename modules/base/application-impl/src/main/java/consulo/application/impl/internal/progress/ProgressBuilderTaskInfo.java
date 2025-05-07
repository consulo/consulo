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
package consulo.application.impl.internal.progress;

import consulo.application.localize.ApplicationLocalize;
import consulo.application.progress.TaskInfo;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-05-06
 */
public class ProgressBuilderTaskInfo implements TaskInfo {
    private final LocalizeValue myTitleText;
    private final boolean myCancelable;

    public ProgressBuilderTaskInfo(LocalizeValue titleText, boolean cancelable) {
        myTitleText = titleText;
        myCancelable = cancelable;
    }

    @Nonnull
    @Override
    public String getTitle() {
        return myTitleText.get();
    }

    @Nonnull
    @Override
    public LocalizeValue getCancelTextValue() {
        return ApplicationLocalize.taskButtonCancel();
    }

    @Nonnull
    @Override
    public LocalizeValue getCancelTooltipTextValue() {
        return ApplicationLocalize.taskButtonCancel();
    }

    @Override
    public boolean isCancellable() {
        return myCancelable;
    }
}

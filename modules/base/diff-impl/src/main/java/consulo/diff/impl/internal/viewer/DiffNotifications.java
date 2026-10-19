/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.viewer;

import consulo.diff.comparison.DiffTooBigException;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.util.TextDiffType;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Toast;
import consulo.ui.ToastStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import org.jspecify.annotations.Nullable;

public class DiffNotifications {
    @RequiredUIAccess
    public static Component createInsertedContent() {
        return createNotification(LocalizeValue.localizeTODO("Content added"), TextDiffType.INSERTED.getColor(null));
    }

    @RequiredUIAccess
    public static Component createRemovedContent() {
        return createNotification(LocalizeValue.localizeTODO("Content removed"), TextDiffType.DELETED.getColor(null));
    }

    @RequiredUIAccess
    public static Component createEqualContents() {
        return createEqualContents(true, true);
    }

    @RequiredUIAccess
    public static Component createEqualContents(boolean equalCharsets, boolean equalSeparators) {
        if (!equalCharsets && !equalSeparators) {
            return createNotification(DiffLocalize.diffContentsHaveDifferencesOnlyInCharsetAndLineSeparatorsMessageText());
        }
        if (!equalSeparators) {
            return createNotification(DiffLocalize.diffContentsHaveDifferencesOnlyInLineSeparatorsMessageText());
        }
        if (!equalCharsets) {
            return createNotification(DiffLocalize.diffContentsHaveDifferencesOnlyInCharsetMessageText());
        }
        return createNotification(DiffLocalize.diffContentsAreIdenticalMessageText());
    }

    @RequiredUIAccess
    public static Component createError() {
        return createNotification(LocalizeValue.localizeTODO("Can not calculate diff"));
    }

    @RequiredUIAccess
    public static Component createOperationCanceled() {
        return createNotification(LocalizeValue.localizeTODO("Can not calculate diff. Operation canceled."));
    }

    @RequiredUIAccess
    public static Component createDiffTooBig() {
        return createNotification(LocalizeValue.localizeTODO("Can not calculate diff. " + DiffTooBigException.MESSAGE));
    }

    //
    // Impl
    //

    @RequiredUIAccess
    public static Component createNotification(LocalizeValue text) {
        return createNotification(text, null);
    }

    @RequiredUIAccess
    public static Component createNotification(LocalizeValue text, @Nullable ColorValue background) {
        return createNotification(text, background, true);
    }

    @RequiredUIAccess
    public static Component createNotification(LocalizeValue text, @Nullable ColorValue background, boolean showHideAction) {
        Toast toast = Toast.create();
        toast.setText(text);
        if (background == null) {
            toast.addStyle(ToastStyle.INFO);
        }
        else {
            toast.setBackgroundColor(background);
        }
        if (showHideAction) {
            toast.addAction(LocalizeValue.localizeTODO("Hide"), () -> toast.setVisible(false));
        }
        return toast;
    }
}

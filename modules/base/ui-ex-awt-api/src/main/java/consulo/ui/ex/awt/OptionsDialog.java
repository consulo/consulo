/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;

/**
 * author: lesya
 */
public abstract class OptionsDialog extends DialogWrapper {
    protected final Project myProject;

    private class MyDoNotAsk implements DoNotAskOption {
        @Override
        public boolean isToBeShown() {
            return OptionsDialog.this.isToBeShown();
        }

        @Override
        public void setToBeShown(boolean value, int exitCode) {
            OptionsDialog.this.setToBeShown(value, DialogWrapper.CANCEL_EXIT_CODE != exitCode);
        }

        @Override
        public boolean canBeHidden() {
            return OptionsDialog.this.canBeHidden();
        }

        @Override
        public boolean shouldSaveOptionsOnCancel() {
            return OptionsDialog.this.shouldSaveOptionsOnCancel();
        }

        @Nonnull
        @Override
        public LocalizeValue getDoNotShowMessage() {
            return OptionsDialog.this.getDoNotShowMessage();
        }
    }

    protected OptionsDialog(@Nullable Project project) {
        super(project, true);
        myProject = project;
        setDoNotAskOption(new MyDoNotAsk());
    }

    protected OptionsDialog(Project project, boolean canBeParent) {
        super(project, canBeParent);
        myProject = project;
        setDoNotAskOption(new MyDoNotAsk());
    }

    protected OptionsDialog(boolean canBeParent) {
        super(canBeParent);
        myProject = null;
        setDoNotAskOption(new MyDoNotAsk());
    }

    protected OptionsDialog(Component parent, boolean canBeParent) {
        super(parent, canBeParent);
        myProject = null;
        setDoNotAskOption(new MyDoNotAsk());
    }

    @Override
    protected LocalizeValue getDoNotShowMessage() {
        return CommonLocalize.dialogOptionsDoNotShowShort();
    }

    public static boolean shiftIsPressed(int inputEventModifiers) {
        return (inputEventModifiers & Event.SHIFT_MASK) != 0;
    }

    protected abstract boolean isToBeShown();

    protected abstract void setToBeShown(boolean value, boolean onOk);

    protected boolean canBeHidden() {
        return true;
    }

    protected abstract boolean shouldSaveOptionsOnCancel();
}

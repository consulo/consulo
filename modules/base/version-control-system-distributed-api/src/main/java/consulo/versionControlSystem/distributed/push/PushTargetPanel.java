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
package consulo.versionControlSystem.distributed.push;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.TextItemPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

public abstract class PushTargetPanel<T extends PushTarget> implements Disposable {
    /**
     * @param isActive true if appropriate repository changes will be pushed, a.e. if repository checked
     */
    public abstract void render(TextItemPresentation presentation,
                                boolean isSelected,
                                boolean isActive,
                                @Nullable String forceRenderedText);

    @RequiredUIAccess
    public abstract Component getComponent();

    public abstract @Nullable T getValue();

    public boolean isEditable() {
        return getValue() != null;
    }

    @RequiredUIAccess
    public void editingStarted() {
    }

    @RequiredUIAccess
    public abstract void fireOnCancel();

    @RequiredUIAccess
    public abstract void fireOnChange();

    @RequiredUIAccess
    public abstract LocalizeValue verify();

    public abstract void setFireOnChangeAction(Runnable action);

    /**
     * Add an ability to track edit field process
     */
    public abstract void addTargetEditorListener(PushTargetEditorListener listener);

    @RequiredUIAccess
    public void forceUpdateEditableUiModel(String forcedText) {
    }

    public boolean showSourceWhenEditing() {
        return true;
    }

    @Override
    public void dispose() {
    }
}

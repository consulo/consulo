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
package consulo.ui.ex.action;

import consulo.application.dumb.DumbAware;
import consulo.component.util.ModificationTracker;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.util.lang.lazy.LazyValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Supplier;

public abstract class ComputableActionGroup extends ActionGroup implements DumbAware {
    private Supplier<AnAction[]> myActions;

    protected ComputableActionGroup() {
    }

    protected ComputableActionGroup(boolean popup) {
        setPopup(popup);
    }

    protected ComputableActionGroup(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
        super(text, description);
    }

    protected ComputableActionGroup(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }

    protected ComputableActionGroup(@Nonnull LocalizeValue text, boolean popup) {
        super(text, popup);
    }

    @Override
    public boolean hideIfNoVisibleChildren() {
        return true;
    }

    @Nonnull
    protected ModificationTracker getModificationTracker() {
        return ModificationTracker.NEVER_CHANGED;
    }

    @Nonnull
    protected abstract AnAction[] computeChildren(@Nonnull ActionManager manager);

    @Override
    @Nonnull
    public final AnAction[] getChildren(@Nullable AnActionEvent e) {
        if (e == null) {
            return EMPTY_ARRAY;
        }

        if (myActions == null) {
            myActions = LazyValue.notNullWithModCount(() -> computeChildren(e.getActionManager()), () -> getModificationTracker().getModificationCount());
        }
        return myActions.get();
    }
}
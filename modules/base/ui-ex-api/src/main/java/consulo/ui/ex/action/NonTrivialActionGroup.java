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
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.util.ActionGroupUtil;
import jakarta.annotation.Nonnull;

/**
 * This group hides itself when there's no enabled and visible child.
 *
 * @author gregsh
 * @see SmartPopupActionGroup
 * @see NonEmptyActionGroup
 */
public class NonTrivialActionGroup extends DefaultActionGroup implements DumbAware {
    public NonTrivialActionGroup() {
        super();
    }

    public NonTrivialActionGroup(@Nonnull LocalizeValue text, boolean popup) {
        super(text, popup);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setVisible(!ActionGroupUtil.isGroupEmpty(this, e));
    }
}

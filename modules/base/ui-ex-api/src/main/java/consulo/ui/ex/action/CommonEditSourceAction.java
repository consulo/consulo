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
package consulo.ui.ex.action;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2025-09-04
 */
public class CommonEditSourceAction extends BaseNavigateToSourceAction {
    public CommonEditSourceAction() {
        super(
            ActionLocalize.actionEditsourceText(),
            ActionLocalize.actionEditsourceDescription(),
            PlatformIconGroup.actionsEditsource(),
            true
        );
    }

    public CommonEditSourceAction(boolean focusEditor) {
        super(focusEditor);
    }

    public CommonEditSourceAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, boolean focusEditor) {
        super(text, description, focusEditor);
    }

    public CommonEditSourceAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon, boolean focusEditor) {
        super(text, description, icon, focusEditor);
    }
}
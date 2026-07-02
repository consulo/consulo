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
package consulo.ui.ex.action;

import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 */
@Deprecated
@SuppressWarnings("deprecation")
public abstract class LegacyAnAction extends AnAction implements AnActionWithSyncUpdate {
    public LegacyAnAction() {
        super();
    }

    public LegacyAnAction(@Nullable Image icon) {
        super(icon);
    }

    public LegacyAnAction(@Nullable String text) {
        super(text);
    }

    public LegacyAnAction(@Nullable String text, @Nullable String description, @Nullable Image icon) {
        super(text, description, icon);
    }

    public LegacyAnAction(LocalizeValue text) {
        super(text);
    }

    public LegacyAnAction(LocalizeValue text, LocalizeValue description) {
        super(text, description);
    }

    public LegacyAnAction(LocalizeValue text, LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }

    @Override
    public void update(AnActionEvent e) {
    }
}

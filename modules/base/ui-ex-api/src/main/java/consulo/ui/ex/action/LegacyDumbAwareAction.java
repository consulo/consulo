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
public abstract class LegacyDumbAwareAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public LegacyDumbAwareAction() {
        super();
    }

    public LegacyDumbAwareAction(Image icon) {
        super(icon);
    }

    public LegacyDumbAwareAction(@Nullable String text) {
        super(text);
    }

    public LegacyDumbAwareAction(@Nullable String text, @Nullable String description, @Nullable Image icon) {
        super(text, description, icon);
    }

    public LegacyDumbAwareAction(LocalizeValue text) {
        super(text);
    }

    public LegacyDumbAwareAction(LocalizeValue text, LocalizeValue description) {
        super(text, description);
    }

    public LegacyDumbAwareAction(LocalizeValue text, LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }
}

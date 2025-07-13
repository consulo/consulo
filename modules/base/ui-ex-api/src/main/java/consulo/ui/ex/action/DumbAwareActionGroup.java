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

import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

/**
 * @author VISTALL
 * @since 2025-06-24
 */
public abstract class DumbAwareActionGroup extends ActionGroup implements DumbAware {
    protected DumbAwareActionGroup() {
    }

    @Deprecated
    protected DumbAwareActionGroup(@Nls(capitalization = Nls.Capitalization.Title) String shortName, boolean popup) {
        super(shortName, popup);
    }

    @Deprecated
    protected DumbAwareActionGroup(@Nls(capitalization = Nls.Capitalization.Title) String text, @Nls(capitalization = Nls.Capitalization.Sentence) String description, Image icon) {
        super(text, description, icon);
    }

    protected DumbAwareActionGroup(@Nonnull LocalizeValue text) {
        super(text);
    }

    protected DumbAwareActionGroup(@Nonnull LocalizeValue text, boolean popup) {
        super(text, popup);
    }

    protected DumbAwareActionGroup(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
        super(text, description);
    }

    protected DumbAwareActionGroup(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }
}

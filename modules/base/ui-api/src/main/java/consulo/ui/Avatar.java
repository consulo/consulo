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
package consulo.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

/**
 * The round picture of a person: their image, or the initials of their name on the colour of that name when there is
 * no image. The name is also the tooltip.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public interface Avatar extends Component, HasComponentStyle<AvatarStyle> {
    static Avatar create(LocalizeValue name) {
        return create(name, null);
    }

    static Avatar create(LocalizeValue name, @Nullable Image image) {
        return UIInternal.get()._Components_avatar(name, image);
    }

    LocalizeValue getName();

    @RequiredUIAccess
    void setName(LocalizeValue name);

    @Nullable
    Image getImage();

    @RequiredUIAccess
    void setImage(@Nullable Image image);
}

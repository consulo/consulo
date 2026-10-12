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
import consulo.ui.internal.UIInternal;
import consulo.ui.layout.Layout;
import consulo.ui.layout.LayoutConstraint;
import org.jspecify.annotations.Nullable;

/**
 * A box grouping what belongs to one subject: a media on top, a header with a title and a subtitle, the content and a
 * footer, each of them optional.
 * <p/>
 * A card does not react to the user on its own. A card which stands for a choice carries the control making it - a
 * button of a {@link RadioGroup} or a {@link CheckBox} in its header prefix - and passes a click on its body to that
 * control.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
public interface Card extends Layout<LayoutConstraint>, HasComponentStyle<CardStyle> {
    static Card create() {
        return UIInternal.get()._Components_card();
    }

    @RequiredUIAccess
    Card setMedia(@Nullable Component media);

    @RequiredUIAccess
    Card setTitle(LocalizeValue title);

    @RequiredUIAccess
    Card setSubtitle(LocalizeValue subtitle);

    /**
     * Replaces the title and the subtitle with a component of the caller's.
     */
    @RequiredUIAccess
    Card setHeader(@Nullable Component header);

    @RequiredUIAccess
    Card setHeaderPrefix(@Nullable Component prefix);

    @RequiredUIAccess
    Card setHeaderSuffix(@Nullable Component suffix);

    @RequiredUIAccess
    default Card add(PseudoComponent component) {
        return add(component.getComponent());
    }

    /**
     * Adds to the content, below the header.
     */
    @RequiredUIAccess
    default Card add(Component component) {
        return (Card) add(component, LayoutConstraint.NONE);
    }

    @RequiredUIAccess
    Card addToFooter(Component component);
}

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
package consulo.it.internal.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.Card;
import consulo.ui.CardStyle;
import consulo.ui.Component;
import consulo.ui.layout.LayoutConstraint;
import org.jspecify.annotations.Nullable;

/**
 * Headless {@link Card}: every part it is given is a child, so a walk over the hierarchy reaches it.
 *
 * @author VISTALL
 */
public class HeadlessCard extends HeadlessLayoutBase<LayoutConstraint> implements Card {
    private @Nullable Component myMedia;
    private @Nullable Component myHeader;
    private @Nullable Component myHeaderPrefix;
    private @Nullable Component myHeaderSuffix;

    private LocalizeValue myTitle = LocalizeValue.empty();
    private LocalizeValue mySubtitle = LocalizeValue.empty();

    @Override
    public Card setMedia(@Nullable Component media) {
        myMedia = replace(myMedia, media);
        return this;
    }

    @Override
    public Card setTitle(LocalizeValue title) {
        myTitle = title;
        return this;
    }

    public LocalizeValue getTitle() {
        return myTitle;
    }

    @Override
    public Card setSubtitle(LocalizeValue subtitle) {
        mySubtitle = subtitle;
        return this;
    }

    public LocalizeValue getSubtitle() {
        return mySubtitle;
    }

    @Override
    public Card setHeader(@Nullable Component header) {
        myHeader = replace(myHeader, header);
        return this;
    }

    @Override
    public Card setHeaderPrefix(@Nullable Component prefix) {
        myHeaderPrefix = replace(myHeaderPrefix, prefix);
        return this;
    }

    @Override
    public Card setHeaderSuffix(@Nullable Component suffix) {
        myHeaderSuffix = replace(myHeaderSuffix, suffix);
        return this;
    }

    @Override
    public Card addToFooter(Component component) {
        addChild(component);
        return this;
    }

    @Override
    public void addStyle(CardStyle style) {
    }

    private @Nullable Component replace(@Nullable Component old, @Nullable Component component) {
        if (old != null) {
            remove(old);
        }
        if (component != null) {
            addChild(component);
        }
        return component;
    }
}

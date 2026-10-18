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
package consulo.sandboxPlugin.ui.tab;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Avatar;
import consulo.ui.AvatarStyle;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.image.Image;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@ExtensionImpl(id = "avatar", order = "after messageList")
public class AvatarUITesterTab implements UITesterTab {
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Avatar");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        VerticalLayout root = VerticalLayout.create(Space.LARGE);
        root.add(row(LocalizeValue.localizeTODO("Initials"), LocalizeValue.of("Valery Semenchuk"), null));
        root.add(row(LocalizeValue.localizeTODO("Other name"), LocalizeValue.of("Sand Reviewer"), null));
        root.add(row(LocalizeValue.localizeTODO("Image"), LocalizeValue.of("Login Avatar"), PlatformIconGroup.actionsLoginavatar()));
        root.add(row(LocalizeValue.localizeTODO("No name"), LocalizeValue.empty(), null));
        return root;
    }

    @RequiredUIAccess
    private static Component row(LocalizeValue title, LocalizeValue name, @Nullable Image image) {
        HorizontalLayout row = HorizontalLayout.create(Space.MEDIUM);
        row.add(Label.create(title));
        row.add(avatar(name, image, AvatarStyle.X_SMALL));
        row.add(avatar(name, image, AvatarStyle.SMALL));
        row.add(avatar(name, image, null));
        row.add(avatar(name, image, AvatarStyle.LARGE));
        row.add(avatar(name, image, AvatarStyle.X_LARGE));
        return row;
    }

    @RequiredUIAccess
    private static Avatar avatar(LocalizeValue name, @Nullable Image image, @Nullable AvatarStyle style) {
        Avatar avatar = Avatar.create(name, image);
        if (style != null) {
            avatar.addStyle(style);
        }
        return avatar;
    }
}

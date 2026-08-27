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
package consulo.language.editor.ui.navigation;

import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.navigation.TargetPresentation;
import consulo.ui.RenderItem;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.TextItemRender;

/**
 * Draws a row of a target popup. Reads nothing but the {@link TargetPresentation} it is handed, which
 * is what lets it run on any frontend and never touch the model.
 *
 * @author VISTALL
 * @since 2026-08-27
 */
public class TargetPresentationRender<T extends PsiElement> implements TextItemRender<ItemWithPresentation<T>> {
    @Override
    public void render(TextItemPresentation presentation, RenderItem<ItemWithPresentation<T>> item) {
        ItemWithPresentation<T> value = item.getValue();
        if (value == null) {
            return;
        }

        TargetPresentation target = value.getPresentation();

        presentation.withIcon(target.getIcon());
        presentation.withBackgroundColor(target.getBackgroundColor());

        TextAttribute presentableAttribute = target.getPresentableTextAttribute();
        presentation.append(target.getPresentableText(), presentableAttribute == null ? TextAttribute.REGULAR : presentableAttribute);

        LocalizeValue containerText = target.getContainerText();
        if (containerText.isNotEmpty()) {
            TextAttribute containerAttribute = target.getContainerTextAttribute();
            presentation.append(LocalizeValue.space());
            presentation.append(containerText, containerAttribute == null ? TextAttribute.GRAYED : containerAttribute);
        }

        LocalizeValue locationText = target.getLocationText();
        if (locationText.isNotEmpty()) {
            presentation.withSuffix(locationText, target.getLocationIcon());
        }
    }
}

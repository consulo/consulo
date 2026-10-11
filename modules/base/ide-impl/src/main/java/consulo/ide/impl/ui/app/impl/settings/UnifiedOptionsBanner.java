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
package consulo.ide.impl.ui.app.impl.settings;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.AdvancedLabel;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.TextAttribute;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class UnifiedOptionsBanner {
    private static final LocalizeValue SEPARATOR = LocalizeValue.of(" › ");

    private final DockLayout myComponent;
    private final AdvancedLabel myText;
    private final Label myProjectIcon;

    @RequiredUIAccess
    public UnifiedOptionsBanner() {
        myText = AdvancedLabel.create();

        myProjectIcon = Label.create();
        myProjectIcon.setImage(PlatformIconGroup.generalProjectconfigurable());
        myProjectIcon.setVisible(false);

        HorizontalLayout row = HorizontalLayout.create(Space.SMALL);
        row.add(myText);
        row.add(myProjectIcon);

        myComponent = DockLayout.create(Space.NONE);
        myComponent.left(row);
        myComponent.paddingBuilder().topSet(Space.MEDIUM).bottomSet(Space.SMALL).horizontalSet(Space.LARGE).apply();
    }

    public Component getComponent() {
        return myComponent;
    }

    @RequiredUIAccess
    public void update(List<LocalizeValue> path, @Nullable LocalizeValue projectTooltip) {
        myText.updatePresentation(presentation -> {
            for (int i = 0; i < path.size(); i++) {
                if (i > 0) {
                    presentation.append(SEPARATOR, TextAttribute.REGULAR);
                }
                presentation.append(path.get(i), TextAttribute.REGULAR_BOLD);
            }
        });

        if (projectTooltip == null) {
            myProjectIcon.setVisible(false);
        }
        else {
            myProjectIcon.setToolTipText(projectTooltip);
            myProjectIcon.setVisible(true);
        }
    }
}

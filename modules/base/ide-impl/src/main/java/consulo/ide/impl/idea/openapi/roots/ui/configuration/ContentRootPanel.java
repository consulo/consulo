/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ide.impl.idea.openapi.roots.ui.configuration;

import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ContentEntry;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.AdvancedLabel;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.TextAttribute;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.virtualFileSystem.VirtualFileManager;

import java.io.File;
import java.util.Set;

/**
 * @author Eugene Zhuravlev
 * @since 2004-01-19
 */
public class ContentRootPanel {
    protected final ContentRootPanelCallback myCallback;
    private final ContentEntry myContentEntry;

    public ContentRootPanel(ContentRootPanelCallback callback, ContentEntry contentEntry) {
        myCallback = callback;
        myContentEntry = contentEntry;
    }

    protected ContentEntry getContentEntry() {
        return myContentEntry;
    }

    @RequiredUIAccess
    public Component createComponent(Set<String> missingUrls) {
        VerticalLayout layout = VerticalLayout.create(Space.SMALL);
        layout.add(createHeader(missingUrls));
        addBottomComponents(layout, missingUrls);
        layout.paddingBuilder().allSet(Space.SMALL).bottomSet(Space.LARGE).apply();
        return layout;
    }

    @RequiredUIAccess
    protected void addBottomComponents(VerticalLayout layout, Set<String> missingUrls) {
    }

    @RequiredUIAccess
    private Component createHeader(Set<String> missingUrls) {
        String url = getContentEntry().getUrl();
        String path = toDisplayPath(url);
        int separator = path.lastIndexOf(File.separatorChar);
        boolean hasLocation = separator > 0 && separator < path.length() - 1;
        String name = hasLocation ? path.substring(separator + 1) : path;
        String location = hasLocation ? path.substring(0, separator) : "";
        TextAttribute attribute = missingUrls.contains(url) ? TextAttribute.ERROR_BOLD : TextAttribute.REGULAR_BOLD;

        AdvancedLabel headerLabel = AdvancedLabel.create();
        headerLabel.updatePresentation(presentation -> {
            presentation.append(LocalizeValue.of(name), attribute);
            if (hasLocation) {
                presentation.append(LocalizeValue.of("  " + location), TextAttribute.GRAYED);
            }
        });

        Button button = Button.create(LocalizeValue.empty());
        button.setToolTipText(ProjectLocalize.modulePathsRemoveContentTooltip());
        button.addStyle(ButtonStyle.INPLACE);
        button.setIcon(PlatformIconGroup.actionsClose());
        button.addClickListener(event -> myCallback.deleteContentEntry(event.getComponent()));

        DockLayout header = DockLayout.create(Space.NONE);
        header.center(headerLabel);
        header.right(button);
        return header;
    }

    protected static String toDisplayPath(String url) {
        return VirtualFileManager.extractPath(url).replace('/', File.separatorChar);
    }
}

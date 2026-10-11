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
package consulo.ide.impl.idea.openapi.roots.ui.configuration;

import consulo.application.Application;
import consulo.content.ContentFolderPropertyProvider;
import consulo.content.ContentFolderTypeProvider;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ContentFolder;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.AdvancedLabel;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Space;
import consulo.ui.TextAttribute;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.font.Font;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.lang.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2025-01-07
 */
public class FolderContentRootPanel extends ContentRootPanel {
    public FolderContentRootPanel(ContentRootPanelCallback callback, ContentEntry contentEntry) {
        super(callback, contentEntry);
    }

    @Override
    @RequiredUIAccess
    protected void addBottomComponents(VerticalLayout layout, Set<String> missingUrls) {
        Map<ContentFolderTypeProvider, List<ContentFolder>> folderByType = new LinkedHashMap<>();
        for (ContentFolder folder : getContentEntry().getFolders(t -> true)) {
            if (folder.isSynthetic()) {
                continue;
            }

            folderByType.computeIfAbsent(folder.getType(), t -> new ArrayList<>()).add(folder);
        }

        Application application = Application.get();
        boolean hasProperties = application.getExtensionPoint(ContentFolderPropertyProvider.class).hasAnyExtensions();

        List<ContentFolderTypeProvider> providers = new ArrayList<>();
        application.getExtensionPoint(ContentFolderTypeProvider.class).forEachExtensionSafe(provider -> {
            if (folderByType.containsKey(provider)) {
                providers.add(provider);
            }
        });
        for (ContentFolderTypeProvider provider : folderByType.keySet()) {
            if (!providers.contains(provider)) {
                providers.add(provider);
            }
        }

        for (ContentFolderTypeProvider provider : providers) {
            List<ContentFolder> folders = folderByType.get(provider);
            if (folders != null) {
                layout.add(createFolderGroupComponent(provider, folders, missingUrls, hasProperties));
            }
        }
    }

    @RequiredUIAccess
    protected Component createFolderGroupComponent(
        ContentFolderTypeProvider provider,
        List<ContentFolder> folders,
        Set<String> missingUrls,
        boolean hasProperties
    ) {
        ColorValue color = provider.getGroupColor();

        AdvancedLabel titleLabel = AdvancedLabel.create();
        titleLabel.updatePresentation(presentation -> presentation.append(provider.getName(), new TextAttribute(Font.BOLD, color)));

        VerticalLayout groupLayout = VerticalLayout.create(Space.X_SMALL);
        groupLayout.add(titleLabel);

        for (ContentFolder folder : folders) {
            boolean valid = !missingUrls.contains(folder.getUrl()) && !missingUrls.contains(getContentEntry().getUrl());

            HorizontalLayout buttonsLayout = HorizontalLayout.create(Space.NONE);
            if (hasProperties || !folder.getProperties().isEmpty()) {
                buttonsLayout.add(createChangeOptionsButton(folder));
            }
            buttonsLayout.add(createFolderDeleteButton(folder, provider, valid));

            DockLayout rowLayout = DockLayout.create(Space.NONE);
            rowLayout.center(createFolderComponent(folder, valid));
            rowLayout.right(buttonsLayout);
            rowLayout.paddingBuilder().leftSet(Space.LARGE).apply();

            groupLayout.add(rowLayout);
        }
        return groupLayout;
    }

    @RequiredUIAccess
    private Component createFolderComponent(ContentFolder folder, boolean valid) {
        LocalizeValue path = LocalizeValue.of(toRelativeDisplayPath(folder.getUrl(), getContentEntry().getUrl()));

        HorizontalLayout layout = HorizontalLayout.create(Space.NONE);
        if (valid) {
            layout.add(Hyperlink.create(path, event -> myCallback.navigateFolder(folder)));
        }
        else {
            AdvancedLabel label = AdvancedLabel.create();
            label.updatePresentation(presentation -> presentation.append(path, TextAttribute.ERROR));
            layout.add(label);
        }
        return layout;
    }

    protected static String toRelativeDisplayPath(String url, String ancestorUrl) {
        if (url.equals(ancestorUrl)) {
            return ".";
        }

        String prefix = StringUtil.endsWithChar(ancestorUrl, '/') ? ancestorUrl : ancestorUrl + "/";
        if (url.startsWith(prefix)) {
            return url.substring(prefix.length()).replace('/', File.separatorChar);
        }
        return toDisplayPath(url);
    }

    @RequiredUIAccess
    private Button createChangeOptionsButton(ContentFolder folder) {
        Button button = Button.create(LocalizeValue.empty());
        button.setToolTipText(ProjectLocalize.modulePathsPropertiesTooltip());
        button.addStyle(ButtonStyle.INPLACE);
        button.setIcon(PlatformIconGroup.generalInline_edit());
        button.addClickListener(event -> myCallback.showChangeOptionsDialog(folder, event.getComponent()));
        return button;
    }

    @RequiredUIAccess
    private Button createFolderDeleteButton(ContentFolder folder, ContentFolderTypeProvider provider, boolean valid) {
        Button button = Button.create(LocalizeValue.empty());
        button.setToolTipText(valid ? ProjectLocalize.modulePathsUnmark0Tooltip(provider.getName()) : ProjectLocalize.modulePathsRemoveTooltip());
        button.addStyle(ButtonStyle.INPLACE);
        button.setIcon(PlatformIconGroup.actionsClose());
        button.addClickListener(event -> myCallback.deleteContentFolder(folder));
        return button;
    }
}

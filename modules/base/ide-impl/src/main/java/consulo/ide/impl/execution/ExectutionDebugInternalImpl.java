/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.execution;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.execution.debug.internal.ExectutionDebugInternal;
import consulo.ide.setting.module.OrderEntryTypeEditor;
import consulo.language.content.ContentFoldersSupportUtil;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.layer.ContentFolder;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.ColoredStringBuilder;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2024-12-06
 */
@Singleton
@ServiceImpl
public class ExectutionDebugInternalImpl implements ExectutionDebugInternal {
    @Nonnull
    @RequiredReadAction
    @Override
    @SuppressWarnings("unchecked")
    public Image getContentRootIcon(@Nonnull Project project, @Nonnull VirtualFile file) {
        ProjectFileIndex index = ProjectFileIndex.getInstance(project);
        if (index.isInLibrary(file)) {
            List<OrderEntry> entries = index.getOrderEntriesForFile(file);
            for (OrderEntry entry : entries) {
                OrderEntryTypeEditor<OrderEntry> editor = OrderEntryTypeEditor.getEditor(entry.getType().getId());

                Consumer<ColoredTextContainer> render = editor.getRender(entry);
                ColoredStringBuilder builder = new ColoredStringBuilder();
                render.accept(builder);
                Image icon = builder.getIcon();
                if (icon != null) {
                    return icon;
                }
            }
        }
        else {
            ContentFolder contentFolder = index.getContentFolder(file);
            if (contentFolder != null) {
                return ContentFoldersSupportUtil.getContentFolderIcon(contentFolder.getType(), contentFolder.getProperties());
            }
        }

        return PlatformIconGroup.nodesFolder();
    }
}

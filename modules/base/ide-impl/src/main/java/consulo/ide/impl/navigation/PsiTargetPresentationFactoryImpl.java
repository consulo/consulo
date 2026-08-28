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
package consulo.ide.impl.navigation;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.fileEditor.VfsPresentationUtil;
import consulo.language.editor.ui.navigation.PsiTargetPresentationFactory;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.SymbolPresentationUtil;
import consulo.language.util.ModuleUtilCore;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.navigation.NavigationService;
import consulo.navigation.TargetPresentationBuilder;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.File;

/**
 * @author VISTALL
 * @since 2026-08-27
 */
@Singleton
@ServiceImpl
public class PsiTargetPresentationFactoryImpl implements PsiTargetPresentationFactory {
    private final NavigationService myNavigationService;

    @Inject
    public PsiTargetPresentationFactoryImpl(NavigationService navigationService) {
        myNavigationService = navigationService;
    }

    @Override
    @RequiredReadAction
    public TargetPresentationBuilder presentationBuilder(PsiElement element) {
        String text = SymbolPresentationUtil.getSymbolPresentableText(element);

        TargetPresentationBuilder builder = myNavigationService.presentationBuilder(LocalizeValue.of(text == null ? String.valueOf(element) : text));
        builder = builder.withIcon(IconDescriptorUpdaters.getIcon(element, 0));

        String containerText = SymbolPresentationUtil.getSymbolContainerText(element);
        if (containerText != null) {
            builder = builder.withContainerText(LocalizeValue.of(containerText));
        }

        if (!element.isValid()) {
            return builder;
        }

        PsiFile file = element.getContainingFile();
        VirtualFile virtualFile = file == null ? null : file.getVirtualFile();

        if (virtualFile != null) {
            ColorValue fileColor = VfsPresentationUtil.getFileBackgroundColor(element.getProject(), virtualFile);
            if (fileColor != null) {
                builder = builder.withBackgroundColor(fileColor);
            }
        }

        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(element.getProject()).getFileIndex();

        if (virtualFile != null && (fileIndex.isInLibrarySource(virtualFile) || fileIndex.isInLibraryClasses(virtualFile))) {
            return builder.withLocationText(LocalizeValue.of(libraryLocation(fileIndex, virtualFile)), PlatformIconGroup.nodesPplibfolder());
        }

        Module module = ModuleUtilCore.findModuleForPsiElement(element);
        if (module != null) {
            Image icon = virtualFile != null && fileIndex.isInTestSourceContent(virtualFile)
                ? PlatformIconGroup.modulesTestroot()
                : PlatformIconGroup.nodesModule();
            builder = builder.withLocationText(LocalizeValue.of(module.getName()), icon);
        }

        return builder;
    }

    private static String libraryLocation(ProjectFileIndex fileIndex, VirtualFile virtualFile) {
        String location = "";
        for (OrderEntry order : fileIndex.getOrderEntriesForFile(virtualFile)) {
            if (order instanceof LibraryOrderEntry || order instanceof ModuleExtensionWithSdkOrderEntry) {
                location = order.getPresentableName();
                break;
            }
        }

        location = location.substring(location.lastIndexOf(File.separatorChar) + 1);

        VirtualFile archiveFile = ArchiveVfsUtil.getVirtualFileForArchive(virtualFile);
        if (archiveFile != null && !location.equals(archiveFile.getName())) {
            location += " (" + archiveFile.getName() + ")";
        }
        return location;
    }
}

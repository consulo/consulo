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
package consulo.ide.impl.idea.openapi.roots.ui;

import consulo.annotation.component.ServiceImpl;
import consulo.application.AllIcons;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkType;
import consulo.content.bundle.SdkUtil;
import consulo.content.library.Library;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryPresentationManager;
import consulo.ide.impl.idea.openapi.roots.ui.util.CompositeAppearance;
import consulo.ide.impl.idea.openapi.roots.ui.util.SimpleTextCellAppearance;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.util.PathUtil;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.OrderEntryTypeEditor;
import consulo.ide.ui.CellAppearanceEx;
import consulo.ide.ui.FileAppearanceService;
import consulo.ide.ui.OrderEntryAppearanceService;
import consulo.module.Module;
import consulo.module.content.layer.ContentFolder;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntryType;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import consulo.util.io.URLUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.pointer.LightFilePointer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.awt.*;
import java.io.File;
import java.util.function.Consumer;

@Singleton
@ServiceImpl
public class OrderEntryAppearanceServiceImpl extends OrderEntryAppearanceService {

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public Consumer<ColoredTextContainer> getRenderForOrderEntry(@Nonnull OrderEntry orderEntry) {
    OrderEntryType<?> type = orderEntry.getType();
    OrderEntryTypeEditor editor = OrderEntryTypeEditor.getEditor(type.getId());
    return editor.getRender(orderEntry);
  }

  @Nonnull
  @Override
  public Consumer<ColoredTextContainer> getRenderForModule(@Nonnull Module module) {
    return it -> {
      it.setIcon(PlatformIconGroup.nodesModule());
      it.append(module.getName());
    };
  }

  @Nonnull
  @Override
  public CellAppearanceEx forLibrary(Project project, @Nonnull final Library library, final boolean hasInvalidRoots) {
    final LibrariesConfigurator context = ShowSettingsUtil.getInstance().getLibrariesModel(project);

    final Image icon = LibraryPresentationManager.getInstance().getCustomIcon(library, context);

    final String name = library.getName();
    if (name != null) {
      return normalOrRedWaved(name, (icon != null ? icon : AllIcons.Nodes.PpLib), hasInvalidRoots);
    }

    final String[] files = library.getUrls(BinariesOrderRootType.getInstance());
    if (files.length == 0) {
      return SimpleTextCellAppearance.invalid(ProjectLocalize.libraryEmptyLibraryItem().get(), AllIcons.Nodes.PpLib);
    }
    else if (files.length == 1) {
      return forVirtualFilePointer(new LightFilePointer(files[0]));
    }

    final String url = StringUtil.trimEnd(files[0], URLUtil.ARCHIVE_SEPARATOR);
    return SimpleTextCellAppearance.regular(PathUtil.getFileName(url), AllIcons.Nodes.PpLib);
  }

  @Nonnull
  @Override
  public CellAppearanceEx forSdk(@Nullable final Sdk jdk, final boolean isInComboBox, final boolean selected, final boolean showVersion) {
    if (jdk == null) {
      return SimpleTextCellAppearance.invalid(ProjectLocalize.unknownSdk().get(), AllIcons.Actions.Help);
    }

    String name = jdk.getName();
    CompositeAppearance appearance = new CompositeAppearance();
    SdkType sdkType = (SdkType)jdk.getSdkType();
    appearance.setIcon(SdkUtil.getIcon(jdk));
    SimpleTextAttributes attributes = getTextAttributes(sdkType.sdkHasValidPath(jdk), selected);
    CompositeAppearance.DequeEnd ending = appearance.getEnding();
    ending.addText(name, attributes);

    if (showVersion) {
      String versionString = jdk.getVersionString();
      if (versionString != null && !versionString.equals(name)) {
        SimpleTextAttributes textAttributes = isInComboBox && !selected
          ? SimpleTextAttributes.SYNTHETIC_ATTRIBUTES
          : Platform.current().os().isMac() && selected ? new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, Color.WHITE) : SimpleTextAttributes.GRAY_ATTRIBUTES;
        ending.addComment(versionString, textAttributes);
      }
    }

    return ending.getAppearance();
  }

  private static SimpleTextAttributes getTextAttributes(final boolean valid, final boolean selected) {
    if (!valid) {
      return SimpleTextAttributes.ERROR_ATTRIBUTES;
    }
    else {
      return SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES;
    }
  }

  @Nonnull
  @Override
  public CellAppearanceEx forContentFolder(@Nonnull final ContentFolder folder) {
    return formatRelativePath(folder, folder.getType().getChildDirectoryIcon(null, null));
  }

  @Nonnull
  @Override
  public CellAppearanceEx forModule(@Nonnull final Module module) {
    return SimpleTextCellAppearance.regular(module.getName(), AllIcons.Nodes.Module);
  }

  @Nonnull
  private static CellAppearanceEx normalOrRedWaved(@Nonnull final String text, @Nullable final Image icon, final boolean waved) {
    return waved ? new SimpleTextCellAppearance(text, icon, new SimpleTextAttributes(SimpleTextAttributes.STYLE_WAVED, null, JBColor.RED)) : SimpleTextCellAppearance.regular(text, icon);
  }

  @Nonnull
  private static CellAppearanceEx forVirtualFilePointer(@Nonnull final LightFilePointer filePointer) {
    final VirtualFile file = filePointer.getFile();
    return file != null ? FileAppearanceService.getInstance().forVirtualFile(file) : FileAppearanceService.getInstance().forInvalidUrl(filePointer.getPresentableUrl());
  }

  @Nonnull
  private static CellAppearanceEx formatRelativePath(@Nonnull final ContentFolder folder, @Nonnull final Image icon) {
    LightFilePointer folderFile = new LightFilePointer(folder.getUrl());
    VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(folder.getContentEntry().getUrl());
    if (file == null) return FileAppearanceService.getInstance().forInvalidUrl(folderFile.getPresentableUrl());

    String contentPath = file.getPath();
    String relativePath;
    SimpleTextAttributes textAttributes;
    VirtualFile folderFileFile = folderFile.getFile();
    if (folderFileFile == null) {
      String absolutePath = folderFile.getPresentableUrl();
      relativePath = absolutePath.startsWith(contentPath) ? absolutePath.substring(contentPath.length()) : absolutePath;
      textAttributes = SimpleTextAttributes.ERROR_ATTRIBUTES;
    }
    else {
      relativePath = VfsUtilCore.getRelativePath(folderFileFile, file, File.separatorChar);
      textAttributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;
    }

    relativePath = StringUtil.isEmpty(relativePath) ? "." + File.separatorChar : relativePath;
    return new SimpleTextCellAppearance(relativePath, icon, textAttributes);
  }
}

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

package consulo.ide.impl.idea.ide.projectView.impl.nodes;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AllIcons;
import consulo.content.OrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkUtil;
import consulo.content.library.Library;
import consulo.ide.IdeBundle;
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryPresentationManager;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.setting.module.OrderEntryTypeEditor;
import consulo.ide.ui.OrderEntryAppearanceService;
import consulo.language.pom.NavigatableWithText;
import consulo.module.content.layer.orderEntry.*;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ColoredStringBuilder;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class NamedLibraryElementNode extends ProjectViewNode<NamedLibraryElement> implements NavigatableWithText {
  public NamedLibraryElementNode(Project project, NamedLibraryElement value, ViewSettings viewSettings) {
    super(project, value, viewSettings);
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public Collection<AbstractTreeNode> getChildren() {
    final List<AbstractTreeNode> children = new ArrayList<AbstractTreeNode>();
    LibraryGroupNode.addLibraryChildren(getValue().getOrderEntry(), children, getProject(), this);
    return children;
  }

  @Override
  public String getTestPresentation() {
    return "Library: " + getValue().getName();
  }

  @Override
  public String getName() {
    return getValue().getName();
  }

  @Override
  public boolean contains(@Nonnull VirtualFile file) {
    return orderEntryContainsFile(getValue().getOrderEntry(), file);
  }

  private static boolean orderEntryContainsFile(OrderEntry orderEntry, VirtualFile file) {
    for (OrderRootType rootType : OrderRootType.getAllTypes()) {
      if (containsFileInOrderType(orderEntry, rootType, file)) return true;
    }
    return false;
  }

  private static boolean containsFileInOrderType(final OrderEntry orderEntry, final OrderRootType orderType, final VirtualFile file) {
    if (!orderEntry.isValid()) return false;
    VirtualFile[] files = orderEntry.getFiles(orderType);
    for (VirtualFile virtualFile : files) {
      boolean ancestor = VfsUtilCore.isAncestor(virtualFile, file, false);
      if (ancestor) return true;
    }
    return false;
  }

  @Override
  public void update(PresentationData presentation) {
    presentation.setPresentableText(getValue().getName());
    final OrderEntry orderEntry = getValue().getOrderEntry();

    if (orderEntry instanceof ModuleExtensionWithSdkOrderEntry) {
      final ModuleExtensionWithSdkOrderEntry sdkOrderEntry = (ModuleExtensionWithSdkOrderEntry)orderEntry;
      final Sdk sdk = sdkOrderEntry.getSdk();
      presentation.setIcon(SdkUtil.getIcon(((ModuleExtensionWithSdkOrderEntry)orderEntry).getSdk()));
      if (sdk != null) { //jdk not specified
        final String path = sdk.getHomePath();
        if (path != null) {
          presentation.setLocationString(FileUtil.toSystemDependentName(path));
        }
      }
      presentation.setTooltip(null);
    }
    else if (orderEntry instanceof LibraryOrderEntry) {
      presentation.setIcon(getIconForLibrary(orderEntry));
      presentation.setTooltip(StringUtil.capitalize(IdeBundle.message("node.projectview.library", ((LibraryOrderEntry)orderEntry).getLibraryLevel())));
    }
    else if(orderEntry instanceof OrderEntryWithTracking) {
      Consumer<ColoredTextContainer> renderForOrderEntry = OrderEntryAppearanceService.getInstance().getRenderForOrderEntry(orderEntry);
      ColoredStringBuilder builder = new ColoredStringBuilder();
      renderForOrderEntry.accept(builder);

      Image icon = builder.getIcon();
      presentation.setIcon(icon == null ? AllIcons.Actions.Help : icon);
    }
  }

  private static Image getIconForLibrary(OrderEntry orderEntry) {
    if (orderEntry instanceof LibraryOrderEntry) {
      Library library = ((LibraryOrderEntry)orderEntry).getLibrary();
      if (library != null) {
        return LibraryPresentationManager.getInstance().getNamedLibraryIcon(library, null);
      }
    }
    return AllIcons.Nodes.PpLib;
  }

  @Override
  @SuppressWarnings("unchecked")
  @RequiredUIAccess
  public void navigate(final boolean requestFocus) {
    OrderEntryType type = getValue().getOrderEntry().getType();
    OrderEntryTypeEditor editor = OrderEntryTypeEditor.getEditor(type.getId());
    editor.navigate(getValue().getOrderEntry());
  }

  @Override
  public boolean canNavigate() {
    return true;
  }

  @Override
  public String getNavigateActionText(boolean focusEditor) {
    return "Open Library Settings";
  }
}

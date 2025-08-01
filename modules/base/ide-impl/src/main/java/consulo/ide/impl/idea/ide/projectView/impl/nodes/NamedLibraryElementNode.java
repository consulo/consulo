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
import consulo.content.OrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkUtil;
import consulo.content.library.Library;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryPresentationManager;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.localize.IdeLocalize;
import consulo.ide.setting.module.OrderEntryTypeEditor;
import consulo.ide.ui.OrderEntryAppearanceService;
import consulo.navigation.NavigatableWithText;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.orderEntry.*;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.view.internal.node.NamedLibraryElement;
import consulo.project.ui.view.localize.ProjectUIViewLocalize;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ColoredStringBuilder;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.image.Image;
import consulo.util.io.FileUtil;
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

    @Nonnull
    @Override
    @RequiredReadAction
    public Collection<AbstractTreeNode> getChildren() {
        List<AbstractTreeNode> children = new ArrayList<>();
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
            if (containsFileInOrderType(orderEntry, rootType, file)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsFileInOrderType(OrderEntry orderEntry, OrderRootType orderType, VirtualFile file) {
        if (!orderEntry.isValid()) {
            return false;
        }
        VirtualFile[] files = orderEntry.getFiles(orderType);
        for (VirtualFile virtualFile : files) {
            boolean ancestor = VfsUtilCore.isAncestor(virtualFile, file, false);
            if (ancestor) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void update(PresentationData presentation) {
        presentation.setPresentableText(getValue().getName());
        OrderEntry orderEntry = getValue().getOrderEntry();

        if (orderEntry instanceof ModuleExtensionWithSdkOrderEntry sdkOrderEntry) {
            Sdk sdk = sdkOrderEntry.getSdk();
            presentation.setIcon(SdkUtil.getIcon(sdkOrderEntry.getSdk()));
            if (sdk != null) { //jdk not specified
                String path = sdk.getHomePath();
                if (path != null) {
                    presentation.setLocationString(FileUtil.toSystemDependentName(path));
                }
            }
            presentation.setTooltip(null);
        }
        else if (orderEntry instanceof LibraryOrderEntry libraryOrderEntry) {
            presentation.setIcon(getIconForLibrary(orderEntry));
            presentation.setTooltip(StringUtil.capitalize(IdeLocalize.nodeProjectviewLibrary(libraryOrderEntry.getLibraryLevel()).get()));
        }
        else if (orderEntry instanceof OrderEntryWithTracking) {
            Consumer<ColoredTextContainer> renderForOrderEntry =
                OrderEntryAppearanceService.getInstance().getRenderForOrderEntry(orderEntry);
            ColoredStringBuilder builder = new ColoredStringBuilder();
            renderForOrderEntry.accept(builder);

            Image icon = builder.getIcon();
            presentation.setIcon(icon == null ? PlatformIconGroup.actionsHelp() : icon);
        }
    }

    private static Image getIconForLibrary(OrderEntry orderEntry) {
        if (orderEntry instanceof LibraryOrderEntry libraryOrderEntry) {
            Library library = libraryOrderEntry.getLibrary();
            if (library != null) {
                return LibraryPresentationManager.getInstance().getNamedLibraryIcon(library, null);
            }
        }
        return PlatformIconGroup.nodesPplib();
    }

    @Override
    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    public void navigate(boolean requestFocus) {
        OrderEntryType type = getValue().getOrderEntry().getType();
        OrderEntryTypeEditor editor = OrderEntryTypeEditor.getEditor(type.getId());
        editor.navigate(getValue().getOrderEntry());
    }

    @Override
    public boolean canNavigate() {
        return true;
    }

    @Nonnull
    @Override
    public LocalizeValue getNavigateActionText(boolean focusEditor) {
        return ProjectUIViewLocalize.actionOpenLibrarySettingsText();
    }
}

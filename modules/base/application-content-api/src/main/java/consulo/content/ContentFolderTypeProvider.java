/*
 * Copyright 2013-2016 consulo.io
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
package consulo.content;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.content.base.ExcludedContentFolderTypeProvider;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2013-10-31
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ContentFolderTypeProvider {
    public static Predicate<ContentFolderTypeProvider> allExceptExcluded() {
        return typeProvider -> !(typeProvider instanceof ExcludedContentFolderTypeProvider);
    }

    public static Predicate<ContentFolderTypeProvider> onlyExcluded() {
        return typeProvider -> typeProvider instanceof ExcludedContentFolderTypeProvider;
    }

    private final String myId;

    protected ContentFolderTypeProvider(String id) {
        myId = id;
    }

    public String getId() {
        return myId;
    }

    public int getWeight() {
        return Integer.MAX_VALUE;
    }

    /**
     * Return child directory icon
     * If psiDirectory is null it require force package support if this provider is supported it
     *
     * @param dir child directory
     * @return icon of child directory
     */
    @RequiredReadAction
    public Image getChildDirectoryIcon(@Nullable VirtualFile dir, @Nullable ComponentManager project) {
        return getChildDirectoryIcon();
    }

    
    public Image getChildDirectoryIcon() {
        return PlatformIconGroup.nodesTreeopen();
    }

    public @Nullable Image getChildPackageIcon() {
        return null;
    }

    public abstract Image getIcon();

    public abstract LocalizeValue getName();

    public abstract ColorValue getGroupColor();

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ContentFolderTypeProvider that = (ContentFolderTypeProvider)o;

        return myId.equals(that.myId);
    }

    @Override
    public int hashCode() {
        return myId.hashCode();
    }

    public static List<ContentFolderTypeProvider> filter(Predicate<ContentFolderTypeProvider> predicate) {
        return Application.get().getExtensionPoint(ContentFolderTypeProvider.class).collectFiltered(predicate);
    }

    public static @Nullable ContentFolderTypeProvider byId(String attributeValue) {
        return Application.get().getExtensionPoint(ContentFolderTypeProvider.class)
            .findFirstSafe(provider -> Objects.equals(attributeValue, provider.getId()));
    }
}

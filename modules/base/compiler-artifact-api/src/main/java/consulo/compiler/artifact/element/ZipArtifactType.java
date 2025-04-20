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
package consulo.compiler.artifact.element;

import consulo.annotation.component.ExtensionImpl;
import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2013-06-18
 */
@ExtensionImpl(id = "zip-artifact", order = "first")
public class ZipArtifactType extends ArtifactType {
    public ZipArtifactType() {
        super("zip", LocalizeValue.localizeTODO("Zip"));
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return PlatformIconGroup.nodesArtifact();
    }

    @Override
    public String getDefaultPathFor(@Nonnull PackagingElementOutputKind kind) {
        return "/";
    }

    @Nonnull
    @Override
    public CompositePackagingElement<?> createRootElement(
        @Nonnull PackagingElementFactory packagingElementFactory,
        @Nonnull String artifactName
    ) {
        return new ZipArchivePackagingElement(ArtifactUtil.suggestArtifactFileName(artifactName) + ".zip");
    }
}

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
package consulo.sandboxPlugin.ide.artifact;

import consulo.annotation.component.ExtensionImpl;
import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.element.ArtifactRootElementImpl;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.compiler.artifact.element.PackagingElementOutputKind;
import consulo.ide.impl.idea.openapi.module.ModuleUtil;
import consulo.module.content.layer.ModulesProvider;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.sandboxPlugin.ide.module.extension.SandModuleExtension;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2014-03-19
 */
@ExtensionImpl
public class SandArtifactType extends ArtifactType {
    public static final String ID = "sand";

    public static SandArtifactType getInstance() {
        return EP_NAME.findExtension(SandArtifactType.class);
    }

    public SandArtifactType() {
        super(ID, "Sand");
    }

    @Override
    public boolean isAvailableForAdd(@Nonnull ModulesProvider modulesProvider) {
        return ModuleUtil.hasModuleExtension(modulesProvider, SandModuleExtension.class);
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

    @Override
    @Nonnull
    public CompositePackagingElement<?> createRootElement(@Nonnull PackagingElementFactory project, @Nonnull String artifactName) {
        return new ArtifactRootElementImpl();
    }
}

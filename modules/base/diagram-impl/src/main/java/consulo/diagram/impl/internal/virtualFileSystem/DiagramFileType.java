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
package consulo.diagram.impl.internal.virtualFileSystem;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.fileType.FileType;

/**
 * @author VISTALL
 * @since 2025-09-02
 */
public class DiagramFileType implements FileType {
    public static final DiagramFileType INSTANCE = new DiagramFileType();

    
    @Override
    public String getId() {
        return "DIAGRAM";
    }

    
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Diagram");
    }

    
    @Override
    public LocalizeValue getDescription() {
        return LocalizeValue.localizeTODO("Diagram");
    }

    
    @Override
    public Image getIcon() {
        return PlatformIconGroup.filetypesDiagram();
    }
}

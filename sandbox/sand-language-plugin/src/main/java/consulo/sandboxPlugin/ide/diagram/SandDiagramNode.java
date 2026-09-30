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
package consulo.sandboxPlugin.ide.diagram;

import consulo.diagram.DiagramNodeBase;
import consulo.diagram.DiagramProvider;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
public class SandDiagramNode extends DiagramNodeBase<String> {
    private final String myName;
    private final @Nullable Image myIcon;

    public SandDiagramNode(DiagramProvider<String> provider, String name, @Nullable Image icon) {
        super(provider);
        myName = name;
        myIcon = icon;
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public @Nullable Image getIcon() {
        return myIcon;
    }

    @Override
    public String getIdentifyingElement() {
        return myName;
    }
}

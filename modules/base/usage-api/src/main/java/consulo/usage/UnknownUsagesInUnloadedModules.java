/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.usage;

import consulo.application.AllIcons;
import consulo.module.UnloadedModuleDescription;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ObjectUtil;

import jakarta.annotation.Nonnull;

import java.util.Collection;

/**
 * @author nik
 */
public class UnknownUsagesInUnloadedModules extends UsageAdapter implements Usage {
    private final String myExplanationText;

    public UnknownUsagesInUnloadedModules(Collection<UnloadedModuleDescription> unloadedModules) {
        String modulesText = unloadedModules.size() > 1
            ? unloadedModules.size() + " unloaded modules"
            : "unloaded module '" + ObjectUtil.assertNotNull(ContainerUtil.getFirstItem(unloadedModules)).getName() + "'";
        myExplanationText = "There may be usages in " + modulesText + "." +
            " Load all modules and repeat refactoring to ensure that all the usages will be updated.";
    }

    @Nonnull
    @Override
    public UsagePresentation getPresentation() {
        return new UsagePresentation() {
            @Nonnull
            @Override
            public TextChunk[] getText() {
                return new TextChunk[]{new TextChunk(
                    TextAttributesUtil.toTextAttributes(SimpleTextAttributes.REGULAR_ATTRIBUTES),
                    myExplanationText
                )};
            }

            @Nonnull
            @Override
            public String getPlainText() {
                return myExplanationText;
            }

            @Override
            public Image getIcon() {
                return AllIcons.General.Warning;
            }

            @Override
            public String getTooltipText() {
                return null;
            }
        };
    }

    @Override
    public boolean isValid() {
        return true;
    }
}

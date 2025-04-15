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

import consulo.localize.LocalizeValue;
import consulo.module.ModuleDescription;
import consulo.module.UnloadedModuleDescription;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.ui.image.Image;
import consulo.usage.localize.UsageLocalize;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author nik
 */
public class UnknownUsagesInUnloadedModules extends UsageAdapter implements Usage {
    private final LocalizeValue myExplanationText;

    private static final int LISTED_MODULES_LIMIT = 10;

    public UnknownUsagesInUnloadedModules(Collection<UnloadedModuleDescription> unloadedModules) {
        int n = unloadedModules.size();
        LocalizeValue modulesText;
        if (n == 1) {
            String theName = unloadedModules.iterator().next().getName();
            modulesText = UsageLocalize.messagePartUnloadedModule0(theName);
        }
        else if (n <= LISTED_MODULES_LIMIT) {
            String listStr = StringUtil.join(unloadedModules, ModuleDescription::getName, ", ");
            modulesText = UsageLocalize.messagePartSmallNumberOfUnloadedModules(n, listStr);
        }
        else {
            String listStr = unloadedModules.stream()
                .limit(LISTED_MODULES_LIMIT)
                .map(ModuleDescription::getName)
                .collect(Collectors.joining(", "));
            modulesText = UsageLocalize.messagePartLargeNumberOfUnloadedModules(n, listStr, n - LISTED_MODULES_LIMIT);
        }

        myExplanationText = UsageLocalize.messageThereMayBeUsagesIn0LoadAllModulesAndRepeat(modulesText);
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
                    myExplanationText.get()
                )};
            }

            @Nonnull
            @Override
            public String getPlainText() {
                return myExplanationText.get();
            }

            @Override
            public Image getIcon() {
                return PlatformIconGroup.generalWarning();
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

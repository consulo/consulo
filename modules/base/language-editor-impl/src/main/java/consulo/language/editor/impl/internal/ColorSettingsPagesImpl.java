/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.editor.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.language.editor.internal.ColorSettingsPages;
import consulo.util.lang.Pair;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ServiceImpl
public class ColorSettingsPagesImpl implements ColorSettingsPages {
    private final Application myApplication;

    @Inject
    public ColorSettingsPagesImpl(Application application) {
        myApplication = application;
    }

    @Override
    @Nullable
    public Pair<ColorSettingsPage, AttributesDescriptor> getAttributeDescriptor(TextAttributesKey key) {
        return myApplication.getExtensionPoint(ColorSettingsPage.class).computeSafeIfAny(page -> {
            for (AttributesDescriptor descriptor : page.getAttributeDescriptors()) {
                if (descriptor.getKey() == key) {
                    return Pair.create(page, descriptor);
                }
            }
            return null;
        });
    }
}

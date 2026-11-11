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
package consulo.ide.impl.idea.application.options.editor;

import consulo.localize.LocalizeValue;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.configurable.search.SearchableOptionContributor;
import consulo.configurable.search.SearchableOptionProcessor;
import consulo.language.editor.gutter.GutterIconDescriptor;
import consulo.language.editor.impl.internal.daemon.GutterIconsConfigurable;
import jakarta.inject.Inject;

import java.util.List;

/**
 * The gutter icons depend on the installed plugins, so they are added to the settings search when it is built
 * rather than indexed with the platform.
 *
 * @author VISTALL
 * @since 2026-10-12
 */
@ExtensionImpl
public class GutterIconSearchableOptionContributor extends SearchableOptionContributor {
    private final Application myApplication;

    @Inject
    public GutterIconSearchableOptionContributor(Application application) {
        myApplication = application;
    }

    @Override
    public void processOptions(SearchableOptionProcessor processor) {
        for (List<GutterIconDescriptor> descriptors : GutterIconsConfigurable.collectGroups(myApplication).values()) {
            for (GutterIconDescriptor descriptor : descriptors) {
                LocalizeValue name = descriptor.getName();
                processor.addOption(name, name, GutterIconsConfigurable.ID, GutterIconsConfigurable.DISPLAY_NAME);
            }
        }
    }
}

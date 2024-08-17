/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.project;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.extension.preview.ExtensionPreview;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.container.plugin.PluginId;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import org.jdom.Element;

import java.util.HashSet;
import java.util.Set;

/**
 * User: anna
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@State(name = "UnknownFeatures", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class UnknownFeaturesCollector implements PersistentStateComponent<Element> {
    private static final String API_PLUGIN_ID = "apiPluginId";
    private static final String API_CLASS_NAME = "apiClassName";
    private static final String IMPL_PLUGIN_ID = "implPluginId";
    private static final String IMPL_ID = "implId";

    private final Set<ExtensionPreview> myUnknownExtensions = new HashSet<>();
    private final Set<ExtensionPreview> myIgnoredUnknownExtensions = new HashSet<>();

    public static UnknownFeaturesCollector getInstance(Project project) {
        return project.getInstance(UnknownFeaturesCollector.class);
    }

    public void registerUnknownFeature(Class<?> extensionClass, String implementationName) {
        final ExtensionPreview extensionPreview = ExtensionPreview.of(extensionClass, implementationName);
        if (!isIgnored(extensionPreview)) {
            myUnknownExtensions.add(extensionPreview);
        }
    }

    public boolean isIgnored(ExtensionPreview feature) {
        return myIgnoredUnknownExtensions.contains(feature);
    }

    public void ignoreFeature(ExtensionPreview feature) {
        myIgnoredUnknownExtensions.add(feature);
    }

    public Set<ExtensionPreview> getUnknownExtensions() {
        return myUnknownExtensions;
    }

    @Nullable
    @Override
    public Element getState() {
        if (myIgnoredUnknownExtensions.isEmpty()) {
            return null;
        }

        final Element ignored = new Element("ignoreExtensions");
        for (ExtensionPreview feature : myIgnoredUnknownExtensions) {
            final Element option = new Element("ignoreExtensions");
            option.setAttribute(API_PLUGIN_ID, feature.apiPluginId().getIdString());
            option.setAttribute(API_CLASS_NAME, feature.apiClassName());
            option.setAttribute(IMPL_PLUGIN_ID, feature.implPluginId().getIdString());
            option.setAttribute(IMPL_ID, feature.implId());
            ignored.addContent(option);
        }
        return ignored;
    }

    @Override
    public void loadState(Element state) {
        myIgnoredUnknownExtensions.clear();
        for (Element element : state.getChildren()) {
            String apiPluginIdStr = element.getAttributeValue(API_PLUGIN_ID);
            String apiClassName = element.getAttributeValue(API_CLASS_NAME);
            String implId = element.getAttributeValue(IMPL_ID);
            String implPluginIdStr = element.getAttributeValue(IMPL_PLUGIN_ID);
            if (implPluginIdStr == null) {
                implPluginIdStr = apiPluginIdStr;
            }

            PluginId apiPluginId = PluginId.getId(apiPluginIdStr);
            PluginId implPluginId = PluginId.getId(implPluginIdStr);

            myIgnoredUnknownExtensions.add(new ExtensionPreview(apiPluginId, apiClassName, implPluginId, implId));
        }
    }
}

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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactProperties;
import consulo.compiler.artifact.ArtifactPropertiesProvider;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.ModifiableArtifact;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.ArtifactPropertiesEditor;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nik
 */
public class ArtifactPropertiesEditors {
    private static final List<LocalizeValue> STANDARD_TABS_ORDER = List.of(
        ArtifactPropertiesEditor.VALIDATION_TAB,
        ArtifactPropertiesEditor.PRE_PROCESSING_TAB,
        ArtifactPropertiesEditor.POST_PROCESSING_TAB
    );

    private final ArtifactEditorContext myContext;
    private final Artifact myOriginalArtifact;
    private final List<PropertiesEditorInfo> myEditors = new ArrayList<>();
    private final Map<Tab, LocalizeValue> myTabs = new IdentityHashMap<>();
    private boolean myUICreated;

    public ArtifactPropertiesEditors(ArtifactEditorContext context, Artifact originalArtifact, Artifact artifact) {
        myContext = context;
        myOriginalArtifact = originalArtifact;
        for (ArtifactPropertiesProvider provider : artifact.getPropertiesProviders()) {
            myEditors.add(new PropertiesEditorInfo(provider));
        }
    }

    @RequiredUIAccess
    public void addTabs(TabbedLayout tabbedLayout, Disposable uiDisposable) {
        Map<LocalizeValue, VerticalLayout> panels = new LinkedHashMap<>();
        for (PropertiesEditorInfo info : myEditors) {
            VerticalLayout panel = panels.computeIfAbsent(info.myEditor.getTabName(), name -> VerticalLayout.create());
            panel.add(info.myEditor.createUIComponent(uiDisposable));
            info.myEditor.reset();
        }
        myUICreated = true;

        List<LocalizeValue> sortedTabs = new ArrayList<>(panels.keySet());
        sortedTabs.sort(Comparator.comparingInt(ArtifactPropertiesEditors::getStandardTabIndex).thenComparing(LocalizeValue::get));

        for (LocalizeValue tabName : sortedTabs) {
            Tab tab = tabbedLayout.createTab();
            tab.setRenderer((it, presentation) -> presentation.append(tabName));
            tabbedLayout.addTab(tab, ScrollableLayout.create(panels.get(tabName)));
            myTabs.put(tab, tabName);
        }
    }

    @RequiredUIAccess
    public void removeTabs(TabbedLayout tabbedLayout) {
        for (Tab tab : myTabs.keySet()) {
            tabbedLayout.removeTab(tab);
        }
        myTabs.clear();
    }

    private static int getStandardTabIndex(LocalizeValue tabName) {
        int index = STANDARD_TABS_ORDER.indexOf(tabName);
        return index == -1 ? STANDARD_TABS_ORDER.size() : index;
    }

    @RequiredUIAccess
    public void applyProperties() {
        if (!myUICreated) {
            return;
        }

        for (PropertiesEditorInfo editor : myEditors) {
            if (editor.myEditor.isModified()) {
                editor.applyProperties();
            }
        }
    }

    @RequiredUIAccess
    public boolean isModified() {
        if (!myUICreated) {
            return false;
        }

        for (PropertiesEditorInfo editor : myEditors) {
            if (editor.myEditor.isModified()) {
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    public void disposeUIResources() {
        if (!myUICreated) {
            return;
        }

        for (PropertiesEditorInfo editor : myEditors) {
            editor.myEditor.disposeUIResources();
        }
        myUICreated = false;
    }

    public @Nullable String getHelpId(Tab tab) {
        LocalizeValue tabName = myTabs.get(tab);
        if (ArtifactPropertiesEditor.VALIDATION_TAB.equals(tabName)) {
            return "reference.project.structure.artifacts.validation";
        }
        else if (ArtifactPropertiesEditor.PRE_PROCESSING_TAB.equals(tabName)) {
            return "reference.project.structure.artifacts.preprocessing";
        }
        else if (ArtifactPropertiesEditor.POST_PROCESSING_TAB.equals(tabName)) {
            return "reference.project.structure.artifacts.postprocessing";
        }
        return null;
    }

    private class PropertiesEditorInfo {
        private final ArtifactPropertiesEditor myEditor;
        private final ArtifactProperties<?> myProperties;
        private final ArtifactPropertiesProvider myProvider;

        private PropertiesEditorInfo(ArtifactPropertiesProvider provider) {
            myProvider = provider;
            myProperties = provider.createProperties(myOriginalArtifact.getArtifactType());
            ArtifactProperties<?> originalProperties = myOriginalArtifact.getProperties(provider);
            if (originalProperties != null) {
                ArtifactUtil.copyProperties(originalProperties, myProperties);
            }
            myEditor = myProperties.createEditor(myContext);
        }

        @RequiredUIAccess
        private void applyProperties() {
            myEditor.apply();
            ModifiableArtifact artifact = myContext.getOrCreateModifiableArtifactModel().getOrCreateModifiableArtifact(myOriginalArtifact);
            artifact.setProperties(myProvider, myProperties);
        }
    }
}

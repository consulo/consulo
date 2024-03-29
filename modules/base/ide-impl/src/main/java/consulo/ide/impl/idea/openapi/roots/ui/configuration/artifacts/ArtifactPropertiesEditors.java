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

import consulo.ui.ex.awt.VerticalFlowLayout;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactProperties;
import consulo.compiler.artifact.ArtifactPropertiesProvider;
import consulo.compiler.artifact.ModifiableArtifact;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.ArtifactPropertiesEditor;
import consulo.ui.ex.awt.TabbedPaneWrapper;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.*;

/**
 * @author nik
 */
public class ArtifactPropertiesEditors {
  private static final List<String> STANDARD_TABS_ORDER = Arrays.asList(
    ArtifactPropertiesEditor.VALIDATION_TAB, ArtifactPropertiesEditor.PRE_PROCESSING_TAB, ArtifactPropertiesEditor.POST_PROCESSING_TAB
  );
  private final Map<String, JPanel> myMainPanels;
  private final ArtifactEditorContext myContext;
  private final Artifact myOriginalArtifact;
  private final List<PropertiesEditorInfo> myEditors;

  public ArtifactPropertiesEditors(ArtifactEditorContext context, Artifact originalArtifact, Artifact artifact) {
    myContext = context;
    myOriginalArtifact = originalArtifact;
    myMainPanels = new HashMap<String, JPanel>();
    myEditors = new ArrayList<PropertiesEditorInfo>();
    for (ArtifactPropertiesProvider provider : artifact.getPropertiesProviders()) {
      final PropertiesEditorInfo editorInfo = new PropertiesEditorInfo(provider);
      myEditors.add(editorInfo);
      final String tabName = editorInfo.myEditor.getTabName();
      JPanel panel = myMainPanels.get(tabName);
      if (panel == null) {
        panel = new JPanel(new VerticalFlowLayout());
        myMainPanels.put(tabName, panel);
      }
      panel.add(editorInfo.myEditor.createComponent());
    }
  }

  public void applyProperties() {
    for (PropertiesEditorInfo editor : myEditors) {
      if (editor.isModified()) {
        editor.applyProperties();
      }
    }
  }

  public void addTabs(TabbedPaneWrapper tabbedPane) {
    List<String> sortedTabs = new ArrayList<String>(myMainPanels.keySet());
    Collections.sort(sortedTabs, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        int i1 = STANDARD_TABS_ORDER.indexOf(o1);
        if (i1 == -1) i1 = STANDARD_TABS_ORDER.size();
        int i2 = STANDARD_TABS_ORDER.indexOf(o2);
        if (i2 == -1) i2 = STANDARD_TABS_ORDER.size();
        if (i1 != i2) {
          return i1 - i2;
        }
        return o1.compareTo(o2);
      }
    });
    for (String tab : sortedTabs) {
      tabbedPane.addTab(tab, myMainPanels.get(tab));
    }
  }

  public boolean isModified() {
    for (PropertiesEditorInfo editor : myEditors) {
      if (editor.isModified()) {
        return true;
      }
    }
    return false;
  }

  public void removeTabs(TabbedPaneWrapper tabbedPane) {
    for (String tabName : myMainPanels.keySet()) {
      for (int i = 0; i < tabbedPane.getTabCount(); i++) {
        if (tabName.equals(tabbedPane.getTitleAt(i))) {
          tabbedPane.removeTabAt(i);
          break;
        }
      }
    }
  }

  @Nullable
  public static String getHelpId(String title) {
    if (ArtifactPropertiesEditor.VALIDATION_TAB.equals(title)) {
      return "reference.project.structure.artifacts.validation";
    }
    else if (ArtifactPropertiesEditor.PRE_PROCESSING_TAB.equals(title)) {
      return "reference.project.structure.artifacts.preprocessing";
    }
    else if (ArtifactPropertiesEditor.POST_PROCESSING_TAB.equals(title)) {
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
      final ArtifactProperties<?> originalProperties = myOriginalArtifact.getProperties(provider);
      if (originalProperties != null) {
        ArtifactUtil.copyProperties(originalProperties, myProperties);
      }
      myEditor = myProperties.createEditor(myContext);
      myEditor.reset();
    }

    public void applyProperties() {
      myEditor.apply();
      final ModifiableArtifact artifact = myContext.getOrCreateModifiableArtifactModel().getOrCreateModifiableArtifact(myOriginalArtifact);
      artifact.setProperties(myProvider, myProperties);
    }

    public boolean isModified() {
      return myEditor.isModified();
    }
  }
}

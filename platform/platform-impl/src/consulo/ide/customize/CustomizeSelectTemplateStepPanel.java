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
package consulo.ide.customize;

import com.intellij.ide.customize.AbstractCustomizeWizardStep;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.containers.MultiMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * @author VISTALL
 * @since 29.11.14
 */
public class CustomizeSelectTemplateStepPanel extends AbstractCustomizeWizardStep {
  private final MultiMap<String, String> myPredefinedTemplates;
  private CheckBoxList<String> myCheckBoxList;

  public CustomizeSelectTemplateStepPanel(MultiMap<String, String> predefinedTemplates) {
    myPredefinedTemplates = predefinedTemplates;
    setLayout(new BorderLayout());

    myCheckBoxList = new CheckBoxList<>();

    myCheckBoxList.setItems(new ArrayList<>(predefinedTemplates.keySet()), s -> s);
    add(ScrollPaneFactory.createScrollPane(myCheckBoxList, true), BorderLayout.CENTER);
  }

  @NotNull
  public Set<String> getEnablePluginSet() {
    Set<String> set = new THashSet<>();
    for (int i = 0; i < myCheckBoxList.getItemsCount(); i++) {
      String name = myCheckBoxList.getItemAt(i);
      if(!myCheckBoxList.isItemSelected(i)) {
        continue;
      }
      Collection<String> strings = myPredefinedTemplates.get(name);
      set.addAll(strings);
    }
    return set;
  }

  @Override
  protected String getTitle() {
    return "Predefined Plugin Sets";
  }

  @Override
  protected String getHTMLHeader() {
    return "<html><body><h2>Select predefined plugin sets</h2></body></html>";
  }

  @Override
  protected String getHTMLFooter() {
    return null;
  }
}

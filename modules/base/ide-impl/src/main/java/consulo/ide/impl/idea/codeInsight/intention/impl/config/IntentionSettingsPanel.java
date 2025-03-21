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

package consulo.ide.impl.idea.codeInsight.intention.impl.config;

import consulo.configurable.SearchableConfigurable;
import consulo.ide.impl.idea.ide.ui.search.SearchUtil;
import consulo.configurable.SearchableOptionsRegistrar;
import consulo.language.editor.impl.internal.intention.IntentionManagerSettings;
import consulo.language.editor.internal.intention.IntentionActionMetaData;
import consulo.language.editor.internal.intention.TextDescriptor;
import consulo.ui.ex.awt.OnePixelSplitter;
import consulo.ui.ex.awt.util.Alarm;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IntentionSettingsPanel {
  private final OnePixelSplitter myComponent;
  private final IntentionSettingsTree myIntentionSettingsTree;
  private final IntentionDescriptionPanel myIntentionDescriptionPanel = new IntentionDescriptionPanel();

  private Alarm myResetAlarm = new Alarm();

  public IntentionSettingsPanel() {
    myIntentionSettingsTree = new IntentionSettingsTree() {
      @Override
      protected void selectionChanged(Object selected) {
        if (selected instanceof IntentionActionMetaData) {
          final IntentionActionMetaData actionMetaData = (IntentionActionMetaData)selected;
          final Runnable runnable = new Runnable() {
            @Override
            public void run() {
              intentionSelected(actionMetaData);
            }
          };
          myResetAlarm.cancelAllRequests();
          myResetAlarm.addRequest(runnable, 100);
        }
        else {
          categorySelected((String)selected);
        }
      }

      @Override
      protected List<IntentionActionMetaData> filterModel(String filter, final boolean force) {
        final List<IntentionActionMetaData> list = IntentionManagerSettings.getInstance().getMetaData();
        if (filter == null || filter.length() == 0) return list;
        final HashSet<String> quoted = new HashSet<String>();
        List<Set<String>> keySetList = SearchUtil.findKeys(filter, quoted);
        List<IntentionActionMetaData> result = new ArrayList<IntentionActionMetaData>();
        for (IntentionActionMetaData metaData : list) {
          if (isIntentionAccepted(metaData, filter, force, keySetList, quoted)) {
            result.add(metaData);
          }
        }
        final Set<String> filters = SearchableOptionsRegistrar.getInstance().getProcessedWords(filter);
        if (force && result.isEmpty()) {
          if (filters.size() > 1) {
            result = filterModel(filter, false);
          }
        }
        return result;
      }
    };
    myComponent = new OnePixelSplitter(false, 0.4f);

    myComponent.setFirstComponent(myIntentionSettingsTree.getComponent());
    myComponent.setSecondComponent(myIntentionDescriptionPanel.getComponent());
  }

  private void intentionSelected(IntentionActionMetaData actionMetaData) {
    myIntentionDescriptionPanel.reset(actionMetaData, myIntentionSettingsTree.getFilter());
  }

  private void categorySelected(String intentionCategory) {
    myIntentionDescriptionPanel.reset(intentionCategory);
  }

  public void reset() {
    myIntentionSettingsTree.reset();
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        myIntentionDescriptionPanel.init(myComponent.getWidth() / 2);
      }
    });
  }

  public void apply() {
    myIntentionSettingsTree.apply();
  }

  public JComponent getComponent() {
    return myComponent;
  }

  public JTree getIntentionTree() {
    return myIntentionSettingsTree.getTree();
  }

  public boolean isModified() {
    return myIntentionSettingsTree.isModified();
  }

  public void dispose() {
    myIntentionSettingsTree.dispose();
    myIntentionDescriptionPanel.dispose();
  }

  public void selectIntention(String familyName) {
    myIntentionSettingsTree.selectIntention(familyName);
  }

  private static boolean isIntentionAccepted(IntentionActionMetaData metaData, @NonNls String filter, boolean forceInclude, final List<Set<String>> keySetList, final HashSet<String> quoted) {
    if (StringUtil.containsIgnoreCase(metaData.getActionText(), filter)) {
      return true;
    }
    for (String category : metaData.myCategory) {
      if (category != null && StringUtil.containsIgnoreCase(category, filter)) {
        return true;
      }
    }
    for (String stripped : quoted) {
      if (StringUtil.containsIgnoreCase(metaData.getActionText(), stripped)) {
        return true;
      }
      for (String category : metaData.myCategory) {
        if (category != null && StringUtil.containsIgnoreCase(category, stripped)) {
          return true;
        }
      }
      try {
        final TextDescriptor description = metaData.getDescription();
        if (description != null) {
          if (StringUtil.containsIgnoreCase(description.getText(), stripped)) {
            if (!forceInclude) return true;
          }
          else if (forceInclude) return false;
        }
      }
      catch (IOException e) {
        //skip then
      }
    }
    for (Set<String> keySet : keySetList) {
      if (keySet.contains(metaData.getActionText())) {
        if (!forceInclude) {
          return true;
        }
      }
      else {
        if (forceInclude) {
          return false;
        }
      }
    }
    return forceInclude;
  }

  public Runnable showOption(final SearchableConfigurable configurable, final String option) {
    return new Runnable() {
      @Override
      public void run() {
        myIntentionSettingsTree.filter(myIntentionSettingsTree.filterModel(option, true));
        myIntentionSettingsTree.setFilter(option);
      }
    };
  }

  public void clearSearch() {
  }
}

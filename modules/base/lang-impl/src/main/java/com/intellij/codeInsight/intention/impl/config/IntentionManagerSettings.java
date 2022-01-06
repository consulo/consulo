/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

/*
 * Created by IntelliJ IDEA.
 * User: mike
 * Date: Aug 23, 2002
 * Time: 8:15:58 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.intellij.codeInsight.intention.impl.config;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.IntentionActionBean;
import com.intellij.codeInsight.intention.IntentionManager;
import com.intellij.ide.ui.search.SearchableOptionContributor;
import com.intellij.ide.ui.search.SearchableOptionProcessor;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.Interner;
import consulo.logging.Logger;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

@Singleton
@State(name = "IntentionManagerSettings", storages = @Storage("intentionSettings.xml"))
public class IntentionManagerSettings implements PersistentStateComponent<Element> {
  private static final Logger LOG = Logger.getInstance(IntentionManagerSettings.class);
  private static final Interner<String> ourStringInterner = Interner.createStringInterner();

  private static class MetaDataKey extends Pair<String, String> {
    private MetaDataKey(@Nonnull String[] categoryNames, @Nonnull final String familyName) {
      super(StringUtil.join(categoryNames, ":"), ourStringInterner.intern(familyName));
    }
  }

  private final Set<String> myIgnoredActions = new LinkedHashSet<>();

  private final Map<MetaDataKey, IntentionActionMetaData> myMetaData = new LinkedHashMap<>();

  private static final String IGNORE_ACTION_TAG = "ignoreAction";
  private static final String NAME_ATT = "name";
  private static final Pattern HTML_PATTERN = Pattern.compile("<[^<>]*>");


  public static IntentionManagerSettings getInstance() {
    return ServiceManager.getService(IntentionManagerSettings.class);
  }

  public void registerIntentionMetaData(@Nonnull IntentionAction intentionAction, @Nonnull String[] category, @Nonnull String descriptionDirectoryName) {
    registerMetaData(new IntentionActionMetaData(intentionAction, getClassLoader(intentionAction), category, descriptionDirectoryName));
  }

  private static ClassLoader getClassLoader(@Nonnull IntentionAction intentionAction) {
    return intentionAction instanceof IntentionActionWrapper ? ((IntentionActionWrapper)intentionAction).getImplementationClassLoader() : intentionAction.getClass().getClassLoader();
  }

  public IntentionManagerSettings() {
    for (IntentionActionBean bean : IntentionManager.EP_INTENTION_ACTIONS.getExtensionList()) {
      String[] categories = bean.getCategories();
      if (categories != null) {
        String descriptionDirectoryName = bean.getDescriptionDirectoryName();

        IntentionActionWrapper intentionAction = new IntentionActionWrapper(bean);
        if (descriptionDirectoryName == null) {
          descriptionDirectoryName = IntentionManagerImpl.getDescriptionDirectoryName(intentionAction);
        }

        registerIntentionMetaData(intentionAction, categories, descriptionDirectoryName);
      }
    }
  }

  public synchronized boolean isShowLightBulb(@Nonnull IntentionAction action) {
    return !myIgnoredActions.contains(action.getFamilyName());
  }

  @Override
  public void loadState(Element element) {
    myIgnoredActions.clear();
    List<Element> children = element.getChildren(IGNORE_ACTION_TAG);
    for (Element e : children) {
      myIgnoredActions.add(e.getAttributeValue(NAME_ATT));
    }
  }

  @Override
  public Element getState() {
    Element element = new Element("state");
    for (String name : myIgnoredActions) {
      element.addContent(new Element(IGNORE_ACTION_TAG).setAttribute(NAME_ATT, name));
    }
    return element;
  }

  @Nonnull
  public synchronized List<IntentionActionMetaData> getMetaData() {
    return new ArrayList<>(myMetaData.values());
  }

  public synchronized boolean isEnabled(@Nonnull IntentionActionMetaData metaData) {
    return !myIgnoredActions.contains(getFamilyName(metaData));
  }

  private static String getFamilyName(@Nonnull IntentionActionMetaData metaData) {
    return StringUtil.join(metaData.myCategory, "/") + "/" + metaData.getFamily();
  }

  private static String getFamilyName(@Nonnull IntentionAction action) {
    return action instanceof IntentionActionWrapper ? ((IntentionActionWrapper)action).getFullFamilyName() : action.getFamilyName();
  }

  public synchronized void setEnabled(@Nonnull IntentionActionMetaData metaData, boolean enabled) {
    if (enabled) {
      myIgnoredActions.remove(getFamilyName(metaData));
    }
    else {
      myIgnoredActions.add(getFamilyName(metaData));
    }
  }

  public synchronized boolean isEnabled(@Nonnull IntentionAction action) {
    return !myIgnoredActions.contains(getFamilyName(action));
  }

  public synchronized void setEnabled(@Nonnull IntentionAction action, boolean enabled) {
    if (enabled) {
      myIgnoredActions.remove(getFamilyName(action));
    }
    else {
      myIgnoredActions.add(getFamilyName(action));
    }
  }

  public synchronized void registerMetaData(@Nonnull IntentionActionMetaData metaData) {
    myMetaData.put(new MetaDataKey(metaData.myCategory, metaData.getFamily()), metaData);
  }

  public synchronized void unregisterMetaData(@Nonnull IntentionAction intentionAction) {
    for (Map.Entry<MetaDataKey, IntentionActionMetaData> entry : myMetaData.entrySet()) {
      if (entry.getValue().getAction() == intentionAction) {
        myMetaData.remove(entry.getKey());
        break;
      }
    }
  }

  public static class IntentionSearchableOptionContributor extends SearchableOptionContributor {

    @Override
    public void processOptions(@Nonnull SearchableOptionProcessor processor) {
      for (IntentionActionBean bean : IntentionManager.EP_INTENTION_ACTIONS.getExtensionList()) {
        String[] categories = bean.getCategories();

        if (categories == null) {
          continue;
        }
        String descriptionDirectoryName = bean.getDescriptionDirectoryName();

        IntentionActionWrapper intentionAction = new IntentionActionWrapper(bean);
        if (descriptionDirectoryName == null) {
          descriptionDirectoryName = IntentionManagerImpl.getDescriptionDirectoryName(intentionAction);
        }

        IntentionActionMetaData data = new IntentionActionMetaData(intentionAction, getClassLoader(intentionAction), categories, descriptionDirectoryName);

        final TextDescriptor description = data.getDescription();

        try {
          String descriptionText = description.getText().toLowerCase();
          descriptionText = HTML_PATTERN.matcher(descriptionText).replaceAll(" ");
          final Set<String> words = processor.getProcessedWordsWithoutStemming(descriptionText);
          words.addAll(processor.getProcessedWords(data.getFamily()));
          for (String word : words) {
            processor.addOption(word, data.getFamily(), data.getFamily(), "editor.code.intentions", CodeInsightBundle.message("intention.settings"));
          }
        }
        catch (IOException e) {
          LOG.error(e);
        }
      }
    }
  }
}

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
package consulo.ide.impl.idea.openapi.actionSystem.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.openapi.actionSystem.AbbreviationManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import jakarta.inject.Singleton;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

/**
 * @author Konstantin Bulenkov
 */
@Singleton
@State(name = "AbbreviationManager", storages = @Storage(value = "abbreviations.xml", roamingType = RoamingType.PER_OS))
@ServiceImpl
public class AbbreviationManagerImpl extends AbbreviationManager implements PersistentStateComponent<Element> {
  private final Map<String, List<String>> myAbbreviation2ActionId = new HashMap<>();
  private final Map<String, LinkedHashSet<String>> myActionId2Abbreviations = new HashMap<>();
  private final Map<String, LinkedHashSet<String>> myPluginsActionId2Abbreviations = new HashMap<>();

  @Nullable
  @Override
  public Element getState() {
    Element actions = new Element("actions");
    Element abbreviations = new Element("abbreviations");
    actions.addContent(abbreviations);
    for (String key : myActionId2Abbreviations.keySet()) {
      LinkedHashSet<String> abbrs = myActionId2Abbreviations.get(key);
      LinkedHashSet<String> pluginAbbrs = myPluginsActionId2Abbreviations.get(key);
      if (abbrs == pluginAbbrs || (abbrs != null && abbrs.equals(pluginAbbrs))) {
        continue;
      }
      if (abbrs != null) {
        Element action = new Element("action");
        action.setAttribute("id", key);
        abbreviations.addContent(action);
        for (String abbr : abbrs) {
          Element abbreviation = new Element("abbreviation");
          abbreviation.setAttribute("name", abbr);
          action.addContent(abbreviation);
        }
      }
    }

    return actions;
  }

  @Override
  public void loadState(Element state) {
    List<Element> abbreviations = state.getChildren("abbreviations");
    if (abbreviations != null && abbreviations.size() == 1) {
      List<Element> actions = abbreviations.get(0).getChildren("action");
      if (actions != null && actions.size() > 0) {
        for (Element action : actions) {
          String actionId = action.getAttributeValue("id");
          LinkedHashSet<String> values = myActionId2Abbreviations.get(actionId);
          if (values == null) {
            values = new LinkedHashSet<>(1);
            myActionId2Abbreviations.put(actionId, values);
          }

          List<Element> abbreviation = action.getChildren("abbreviation");
          if (abbreviation != null) {
            for (Element abbr : abbreviation) {
              String abbrValue = abbr.getAttributeValue("name");
              if (abbrValue != null) {
                values.add(abbrValue);
              }
            }
          }
        }
      }
    }
  }

  @Override
  public Set<String> getAbbreviations() {
    Set<String> result = new HashSet<>();
    for (Set<String> abbrs : myActionId2Abbreviations.values()) {
      result.addAll(abbrs);
    }
    return result;
  }

  @Override
  public Set<String> getAbbreviations(String actionId) {
    LinkedHashSet<String> abbreviations = myActionId2Abbreviations.get(actionId);
    if (abbreviations == null) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(abbreviations);
  }

  @Override
  public List<String> findActions(String abbreviation) {
    List<String> actions = myAbbreviation2ActionId.get(abbreviation);
    return actions == null ? Collections.<String>emptyList() : Collections.unmodifiableList(actions);
  }


  public void register(String abbreviation, String actionId, Map<String, LinkedHashSet<String>> storage) {
    LinkedHashSet<String> abbreviations = storage.get(actionId);
    if (abbreviations == null) {
      abbreviations = new LinkedHashSet<>(1);
      storage.put(actionId, abbreviations);
    }
    abbreviations.add(abbreviation);
  }

  public void register(String abbreviation, String actionId, boolean fromPluginXml) {
    if (fromPluginXml && myActionId2Abbreviations.containsKey(actionId)) {
      register(abbreviation, actionId, myPluginsActionId2Abbreviations);
      return;
    }
    register(abbreviation, actionId, myActionId2Abbreviations);
    if (fromPluginXml) {
      register(abbreviation, actionId, myPluginsActionId2Abbreviations);
    }

    List<String> ids = myAbbreviation2ActionId.get(abbreviation);
    if (ids == null) {
      ids = new ArrayList<>(0);
      myAbbreviation2ActionId.put(abbreviation, ids);
    }

    if (!ids.contains(actionId)) {
      ids.add(actionId);
    }
  }

  @Override
  public void register(String abbreviation, String actionId) {
    register(abbreviation, actionId, false);
  }

  @Override
  public void remove(String abbreviation, String actionId) {
    List<String> actions = myAbbreviation2ActionId.get(abbreviation);
    if (actions != null) {
      actions.remove(actionId);
    }
    LinkedHashSet<String> abbreviations = myActionId2Abbreviations.get(actionId);
    if (abbreviations != null) {
      abbreviations.remove(abbreviation);
    }
    else {
      LinkedHashSet<String> abbrs = myActionId2Abbreviations.get(actionId);
      if (abbrs != null) {
        LinkedHashSet<String> customValues = new LinkedHashSet<>(abbrs);
        customValues.remove(abbreviation);
        myActionId2Abbreviations.put(actionId, customValues);
      }
    }
  }

  @Override
  public void removeAllAbbreviations(@Nonnull String actionId) {
    Set<String> abbreviations = getAbbreviations(actionId);
    for (String abbreviation : abbreviations) {
      myAbbreviation2ActionId.get(abbreviation).remove(actionId);
    }
    myActionId2Abbreviations.remove(actionId);
  }
}

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
package consulo.ide.impl.idea.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.ServiceManager;
import consulo.project.Project;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author ven
 */
@Singleton
@State(name = "RecentsManager", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class RecentsManager implements PersistentStateComponent<Element> {
  private final Map<String, LinkedList<String>> myMap = new HashMap<>();

  private int myRecentsNumberToKeep = 5;
  
  private static final String KEY_ELEMENT_NAME = "key";
  private static final String RECENT_ELEMENT_NAME = "recent";
  protected static final String NAME_ATTR = "name";

  public static RecentsManager getInstance(Project project) {
    return ServiceManager.getService(project, RecentsManager.class);
  }

  @Nullable
  public List<String> getRecentEntries(String key) {
    return myMap.get(key);
  }

  public void registerRecentEntry(String key, String recentEntry) {
    LinkedList<String> recents = myMap.get(key);
    if (recents == null) {
      recents = new LinkedList<String>();
      myMap.put(key, recents);
    }

    add(recents, recentEntry);
  }

  private void add(final LinkedList<String> recentEntrues, final String newEntry) {
    final int oldIndex = recentEntrues.indexOf(newEntry);
    if (oldIndex >= 0) {
      recentEntrues.remove(oldIndex);
    }
    else if (recentEntrues.size() == myRecentsNumberToKeep) {
      recentEntrues.removeLast();
    }

    recentEntrues.addFirst(newEntry);
  }

  @Override
  public void loadState(Element element) {
    myMap.clear();
    final List keyElements = element.getChildren(KEY_ELEMENT_NAME);
    for (Iterator iterator = keyElements.iterator(); iterator.hasNext(); ) {
      Element keyElement = (Element)iterator.next();
      final String key = keyElement.getAttributeValue(NAME_ATTR);
      LinkedList<String> recents = new LinkedList<String>();
      final List children = keyElement.getChildren(RECENT_ELEMENT_NAME);
      for (Iterator<Element> iterator1 = children.iterator(); iterator1.hasNext(); ) {
        recents.addLast(iterator1.next().getAttributeValue(NAME_ATTR));
      }

      myMap.put(key, recents);
    }
  }

  @Override
  public Element getState() {
    Element element = new Element("state");
    final Set<Map.Entry<String, LinkedList<String>>> entries = myMap.entrySet();
    for (Map.Entry<String, LinkedList<String>> entry : entries) {
      final Element keyElement = new Element(KEY_ELEMENT_NAME);
      keyElement.setAttribute(NAME_ATTR, entry.getKey());
      final LinkedList<String> recents = entry.getValue();
      for (String recent : recents) {
        final Element recentElement = new Element(RECENT_ELEMENT_NAME);
        recentElement.setAttribute(NAME_ATTR, recent);
        keyElement.addContent(recentElement);
      }
      element.addContent(keyElement);
    }
    return element;
  }

  public void setRecentsNumberToKeep(final int recentsNumberToKeep) {
    myRecentsNumberToKeep = recentsNumberToKeep;
  }
}

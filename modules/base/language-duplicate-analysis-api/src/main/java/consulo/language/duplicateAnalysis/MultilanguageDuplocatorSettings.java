// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.duplicateAnalysis;

import consulo.application.Application;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.language.Language;
import consulo.util.xml.serializer.SkipDefaultValuesSerializationFilters;
import consulo.util.xml.serializer.XmlSerializer;
import jakarta.annotation.Nonnull;
import org.jdom.Element;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Eugene.Kudelevsky
 */
@State(
  name = "MultiLanguageDuplocatorSettings",
  storages = @Storage("duplocatorSettings.xml")
)
public class MultilanguageDuplocatorSettings implements PersistentStateComponent<Element> {
  private final Map<String, ExternalizableDuplocatorState> mySettingsMap = new TreeMap<>();

  public static MultilanguageDuplocatorSettings getInstance() {
    return Application.get().getService(MultilanguageDuplocatorSettings.class);
  }

  public void registerState(@Nonnull Language language, @Nonnull ExternalizableDuplocatorState state) {
    synchronized (mySettingsMap) {
      mySettingsMap.put(language.getID(), state);
    }
  }

  public ExternalizableDuplocatorState getState(@Nonnull Language language) {
    synchronized (mySettingsMap) {
      return mySettingsMap.get(language.getID());
    }
  }

  @Override
  public Element getState() {
    synchronized (mySettingsMap) {
      Element state = new Element("state");
      if (mySettingsMap.isEmpty()) {
        return state;
      }

      SkipDefaultValuesSerializationFilters filter = new SkipDefaultValuesSerializationFilters();
      for (String name : mySettingsMap.keySet()) {
        Element child = XmlSerializer.serializeIfNotDefault(mySettingsMap.get(name), filter);
        if (child != null) {
          child.setName("object");
          child.setAttribute("language", name);
          state.addContent(child);
        }
      }
      return state;
    }
  }

  @Override
  public void loadState(@Nonnull Element state) {
    synchronized (mySettingsMap) {
      if (state == null) {
        return;
      }

      for (Element objectElement : state.getChildren("object")) {
        String language = objectElement.getAttributeValue("language");
        if (language != null) {
          ExternalizableDuplocatorState stateObject = mySettingsMap.get(language);
          if (stateObject != null) {
            XmlSerializer.deserializeInto(stateObject, objectElement);
          }
        }
      }
    }
  }
}

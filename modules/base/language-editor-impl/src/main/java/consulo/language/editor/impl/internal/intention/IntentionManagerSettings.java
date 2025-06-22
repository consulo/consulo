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
package consulo.language.editor.impl.internal.intention;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionActionDelegate;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.editor.internal.intention.IntentionActionMetaData;
import consulo.logging.Logger;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Pattern;

/**
 * @author mike
 * @since 2002-08-23
 */
@Singleton
@State(name = "IntentionManagerSettings", storages = @Storage("intentionSettings.xml"))
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class IntentionManagerSettings implements PersistentStateComponent<Element> {
  private static final ExtensionPointCacheKey<IntentionAction, List<IntentionActionMetaData>> CACHE_KEY = ExtensionPointCacheKey.create("IntentionActionMetaData", walker -> {
    List<IntentionActionMetaData> metadata = new ArrayList<>();
    walker.walk(action -> register(action, metadata));
    return metadata;
  });

  private static final Logger LOG = Logger.getInstance(IntentionManagerSettings.class);
  private final Set<String> myIgnoredActions = new ConcurrentSkipListSet<>();
  private final Application myApplication;

  private static final String IGNORE_ACTION_TAG = "ignoreAction";
  private static final String NAME_ATT = "name";
  public static final Pattern HTML_PATTERN = Pattern.compile("<[^<>]*>");

  public static IntentionManagerSettings getInstance() {
    return Application.get().getInstance(IntentionManagerSettings.class);
  }

  private static void register(@Nonnull IntentionAction intentionAction, List<IntentionActionMetaData> actionMetaDatas) {
    IntentionMetaData intentionMetaData = intentionAction.getClass().getAnnotation(IntentionMetaData.class);
    if (intentionMetaData == null) {
      LOG.error("@IntentionMetaData missed on intention " + intentionAction.getClass().getName());
      return;
    }

    String descriptionDirectoryName = getDescriptionDirectoryName(intentionAction);
    actionMetaDatas.add(new IntentionActionMetaData(intentionAction, intentionMetaData.categories(), descriptionDirectoryName));
  }

  @Nonnull
  public static String getDescriptionDirectoryName(final IntentionAction action) {
    return getDescriptionDirectoryName(action.getClass().getName());
  }

  private static String getDescriptionDirectoryName(final String fqn) {
    return fqn.substring(fqn.lastIndexOf('.') + 1).replaceAll("\\$", "");
  }

  @Inject
  public IntentionManagerSettings(Application application) {
    myApplication = application;
  }

  public boolean isShowLightBulb(@Nonnull IntentionAction action) {
    if (isSyntheticIntention(action)) {
      return true;
    }

    return isEnabled(action);
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
  public List<IntentionActionMetaData> getMetaData() {
    return myApplication.getExtensionPoint(IntentionAction.class).getOrBuildCache(CACHE_KEY);
  }

  public boolean isEnabled(@Nonnull IntentionActionMetaData metaData) {
    if (isSyntheticIntention(metaData.getAction())) {
      return true;
    }

    return !myIgnoredActions.contains(getIgnoreId(metaData.getAction()));
  }

  @Nonnull
  private static String getIgnoreId(@Nonnull IntentionAction action) {
    while (action instanceof IntentionActionDelegate) {
      action = ((IntentionActionDelegate)action).getDelegate();
    }

    IntentionMetaData annotation = action.getClass().getAnnotation(IntentionMetaData.class);
    if (annotation != null) {
      return annotation.ignoreId();
    }

    LOG.error("Missed @IntentionMetaData on " + action.getClass());
    return action.getClass().getName();
  }

  private static boolean isSyntheticIntention(@Nonnull IntentionAction action) {
    if (action instanceof SyntheticIntentionAction) {
      return true;
    }

    while (action instanceof IntentionActionDelegate) {
      action = ((IntentionActionDelegate)action).getDelegate();
    }

    return action instanceof SyntheticIntentionAction;
  }

  public void setEnabled(@Nonnull IntentionActionMetaData metaData, boolean enabled) {
    if (enabled) {
      myIgnoredActions.remove(getIgnoreId(metaData.getAction()));
    }
    else {
      myIgnoredActions.add(getIgnoreId(metaData.getAction()));
    }
  }

  public boolean isEnabled(@Nonnull IntentionAction action) {
    if (isSyntheticIntention(action)) {
      return true;
    }
    return !myIgnoredActions.contains(getIgnoreId(action));
  }

  public void setEnabled(@Nonnull IntentionAction action, boolean enabled) {
    if (enabled) {
      myIgnoredActions.remove(getIgnoreId(action));
    }
    else {
      myIgnoredActions.add(getIgnoreId(action));
    }
  }
}

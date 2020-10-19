// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything;

import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.AbstractCollection;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.intellij.util.xmlb.annotations.Tag;

import javax.annotation.Nonnull;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@State(name = "RunAnythingCache", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class RunAnythingCache implements PersistentStateComponent<RunAnythingCache.State> {
  private final State mySettings = new State();

  public static RunAnythingCache getInstance(Project project) {
    return ServiceManager.getService(project, RunAnythingCache.class);
  }

  /**
   * @return true is group is visible; false if it's hidden
   */
  public boolean isGroupVisible(@Nonnull String key) {
    return mySettings.myKeys.get(key);
  }

  /**
   * Saves group visibility flag
   *
   * @param key     to store visibility flag
   * @param visible true if group should be shown
   */
  public void saveGroupVisibilityKey(@Nonnull String key, boolean visible) {
    mySettings.myKeys.put(key, visible);
  }

  @Nonnull
  @Override
  public State getState() {
    return mySettings;
  }

  @Override
  public void loadState(@Nonnull State state) {
    XmlSerializerUtil.copyBean(state, mySettings);

    updateNewProvidersGroupVisibility(mySettings);
  }

  /**
   * Updates group visibilities store for new providers
   */
  private static void updateNewProvidersGroupVisibility(@Nonnull State settings) {
    Map<String, Boolean> defaultKeys = State.getDefaultKeys();

    for (String key : defaultKeys.keySet()) {
      if (!settings.myKeys.containsKey(key)) {
        settings.myKeys.put(key, Boolean.TRUE);
      }
    }
  }

  public static class State {
    @Nonnull
    @MapAnnotation(entryTagName = "visibility", keyAttributeName = "group", valueAttributeName = "flag")
    public final Map<String, Boolean> myKeys = getDefaultKeys();

    private static Map<String, Boolean> getDefaultKeys() {
      List<RunAnythingProvider> extensionList = RunAnythingProvider.EP_NAME.getExtensionList();

      return extensionList.stream().filter(it -> it.getCompletionGroupTitle() != null).collect(Collectors.toMap(RunAnythingProvider::getCompletionGroupTitle, runAnythingProvider -> true));
    }

    @Nonnull
    @Tag("commands")
    @AbstractCollection(surroundWithTag = false, elementTag = "command")
    public final List<String> myCommands = new ArrayList<>();

    @Nonnull
    public List<String> getCommands() {
      return myCommands;
    }
  }
}
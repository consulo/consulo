// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.template.CustomLiveTemplate;
import com.intellij.lang.Language;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.project.Project;
import javax.annotation.Nonnull;

public final class SurroundWithLogger /*extends CounterUsagesCollector*/ {
  //private static final EventLogGroup GROUP = new EventLogGroup("surround.with", 3);
  //private static final VarargEventId LIVE_TEMPLATE_EXECUTED = registerLiveTemplateEvent(GROUP, "live.template.executed");
  //private static final ClassEventField CLASS = EventFields.Class("class");
  //private static final VarargEventId SURROUNDER_EXECUTED = GROUP.registerVarargEvent("surrounder.executed", EventFields.PluginInfo, EventFields.Language, CLASS);
  //private static final VarargEventId CUSTOM_TEMPLATE_EXECUTED = GROUP.registerVarargEvent("custom.template.executed", EventFields.PluginInfo, EventFields.Language, CLASS);
  //
  //@Override
  //public EventLogGroup getGroup() {
  //  return GROUP;
  //}

  public static void logSurrounder(Surrounder surrounder, @Nonnull Language language, @Nonnull Project project) {
    // SURROUNDER_EXECUTED.log(project, buildEventData(surrounder.getClass(), language));
  }

  //private static List<EventPair<?>> buildEventData(@NotNull Class<?> elementClass, @NotNull Language language) {
  //  PluginInfo pluginInfo = PluginInfoDetectorKt.getPluginInfo(elementClass);
  //  List<EventPair<?>> data = new ArrayList<>();
  //  data.add(EventFields.PluginInfo.with(pluginInfo));
  //  data.add(EventFields.Language.with(language));
  //  data.add(CLASS.with(elementClass));
  //  return data;
  //}

  static void logTemplate(@Nonnull TemplateImpl template, @Nonnull Language language, @Nonnull Project project) {
    //final List<EventPair<?>> data = LiveTemplateRunLogger.createTemplateData(template, language);
    //if (data != null) {
    //  LIVE_TEMPLATE_EXECUTED.log(project, data);
    //}
  }

  static void logCustomTemplate(@Nonnull CustomLiveTemplate template, @Nonnull Language language, @Nonnull Project project) {
    //CUSTOM_TEMPLATE_EXECUTED.log(project, buildEventData(template.getClass(), language));
  }
}

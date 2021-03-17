// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.events;

import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Vladislav.Soroka
 */
public interface MessageEvent extends BuildEvent {
  enum Kind {
    ERROR, WARNING, INFO, STATISTICS, SIMPLE
  }

  @Nonnull
  Kind getKind();

  @Nonnull
  @BuildEventsNls.Title
  String getGroup();

  @Nullable
  Navigatable getNavigatable(@Nonnull Project project);

  MessageEventResult getResult();
}

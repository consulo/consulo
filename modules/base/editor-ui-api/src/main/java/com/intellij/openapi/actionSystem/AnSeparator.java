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
package com.intellij.openapi.actionSystem;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.text.StringUtil;
import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a separator.
 */
public final class AnSeparator extends AnAction implements DumbAware {
  private static final AnSeparator ourInstance = new AnSeparator();

  @Nonnull
  public static AnSeparator getInstance() {
    return ourInstance;
  }

  @Nonnull
  public static AnSeparator create() {
    return ourInstance;
  }

  @Nonnull
  @Deprecated
  @DeprecationInfo("Use #create(LocalizeValue)")
  public static AnSeparator create(@Nullable String text) {
    return StringUtil.isEmptyOrSpaces(text) ? ourInstance : new AnSeparator(text);
  }

  @Nonnull
  public static AnSeparator create(@Nonnull LocalizeValue textValue) {
    return textValue == LocalizeValue.empty() ? ourInstance : new AnSeparator(textValue);
  }

  @Nonnull
  private final LocalizeValue myTextValue;

  public AnSeparator() {
    this(LocalizeValue.empty());
  }

  public AnSeparator(@Nullable String text) {
    myTextValue = StringUtil.isEmptyOrSpaces(text) ? LocalizeValue.empty() : LocalizeValue.of(text);
  }

  public AnSeparator(@Nonnull LocalizeValue textValue) {
    myTextValue = textValue;
  }

  @Nullable
  @Deprecated
  @DeprecationInfo("Use #getTextValue()")
  public String getText() {
    return StringUtil.nullize(myTextValue.getValue());
  }

  @Nonnull
  public LocalizeValue getTextValue() {
    return myTextValue;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    throw new UnsupportedOperationException();
  }
}

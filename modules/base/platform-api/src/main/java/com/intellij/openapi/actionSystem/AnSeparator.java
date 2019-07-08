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
import javax.annotation.Nonnull;

import javax.annotation.Nullable;

/**
 * Represents a separator.
 */
public final class AnSeparator extends AnAction implements DumbAware {
  private static final AnSeparator ourInstance = new AnSeparator();


  @Nonnull
  public static AnSeparator create() {
    return create(null);
  }

  @Nonnull
  public static AnSeparator create(@Nullable String text) {
    return StringUtil.isEmptyOrSpaces(text) ? ourInstance : new AnSeparator(text);
  }

  private String myText;

  public AnSeparator() {
  }

  public AnSeparator(@Nullable final String text) {
    myText = text;
  }

  public String getText() {
    return myText;
  }

  public static AnSeparator getInstance() {
    return ourInstance;
  }

  @Override
  public void actionPerformed(AnActionEvent e){
    throw new UnsupportedOperationException();
  }
}

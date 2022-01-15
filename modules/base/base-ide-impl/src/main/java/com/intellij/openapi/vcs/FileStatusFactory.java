/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.vcs;

import com.intellij.openapi.editor.colors.EditorColorKey;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import consulo.localize.LocalizeValue;
import consulo.ui.color.ColorValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class FileStatusFactory {
  private static final FileStatusFactory ourInstance = new FileStatusFactory();
  private final List<FileStatus> myStatuses = new ArrayList<>();

  private FileStatusFactory() {
  }

  public synchronized FileStatus createFileStatus(@Nonnull String id, @Nonnull LocalizeValue description) {
    return createFileStatus(id, description, null);
  }

  public synchronized FileStatus createFileStatus(@Nonnull String id, @Nonnull LocalizeValue description, @Nullable ColorValue color) {
    FileStatusImpl result = new FileStatusImpl(id, EditorColorKey.createColorKey("FILESTATUS_" + id, color), description);
    myStatuses.add(result);
    return result;
  }

  public synchronized FileStatus[] getAllFileStatuses() {
    return myStatuses.toArray(new FileStatus[myStatuses.size()]);
  }

  public static FileStatusFactory getInstance() {
    return ourInstance;
  }

  /**
   * author: lesya
   */
  private static class FileStatusImpl implements FileStatus {
    private final String myStatus;
    private final EditorColorKey myColorKey;
    private final LocalizeValue myText;

    public FileStatusImpl(@Nonnull String status, @Nonnull EditorColorKey key, LocalizeValue text) {
      myStatus = status;
      myColorKey = key;
      myText = text;
    }

    @Nonnull
    public String toString() {
      return myStatus;
    }

    @Nonnull
    @Override
    public LocalizeValue getText() {
      return myText;
    }

    @Override
    public ColorValue getColor() {
      return EditorColorsManager.getInstance().getGlobalScheme().getColor(getColorKey());
    }

    @Nonnull
    @Override
    public EditorColorKey getColorKey() {
      return myColorKey;
    }

    @Nonnull
    @Override
    public String getId() {
      return myStatus;
    }
  }
}

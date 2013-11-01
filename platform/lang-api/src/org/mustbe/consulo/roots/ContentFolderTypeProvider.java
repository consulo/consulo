/*
 * Copyright 2013 must-be.org
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
package org.mustbe.consulo.roots;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 22:32/31.10.13
 */
public abstract class ContentFolderTypeProvider {
  public static final ExtensionPointName<ContentFolderTypeProvider> EP_NAME =
    ExtensionPointName.create("org.mustbe.consulo.contentFolderTypeProvider");

  private final String myId;

  protected ContentFolderTypeProvider(String id) {
    myId = id;
  }

  public boolean isValid() {
    return true;
  }

  @NotNull
  public abstract AnAction createMarkAction();

  @NotNull
  public String getId() {
    return myId;
  }

  public Icon getChildDirectoryIcon() {
    return AllIcons.Nodes.TreeOpen;
  }

  @NotNull
  public abstract Icon getIcon();

  @NotNull
  public abstract String getName();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ContentFolderTypeProvider that = (ContentFolderTypeProvider)o;

    if (!myId.equals(that.myId)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myId.hashCode();
  }
}

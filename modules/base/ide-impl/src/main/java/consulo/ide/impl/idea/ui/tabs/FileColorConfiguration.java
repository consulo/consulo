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

package consulo.ide.impl.idea.ui.tabs;

import consulo.project.Project;
import consulo.language.editor.scope.NamedScopeManager;
import consulo.ui.ex.awt.util.ColorUtil;
import org.jdom.Element;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author spleaner
 * @author Konstantin Bulenkov
 */
class FileColorConfiguration implements Cloneable {
  private static final String COLOR = "color";
  private static final String SCOPE_NAME = "scope";

  private String myScopeName;
  private String myColorName;

  public FileColorConfiguration() {
  }

  public FileColorConfiguration(final String scopeName, final String colorName) {
    myScopeName = scopeName;
    myColorName = colorName;
  }

  public String getScopeName() {
    return myScopeName;
  }

  public void setScopeName(String scopeName) {
    myScopeName = scopeName;
  }

  public String getColorName() {
    return myColorName;
  }

  public String getColorPresentableName() {
    return ColorUtil.fromHex(myColorName, null) == null ? myColorName : "Custom";
  }

  public void setColorName(final String colorName) {
    myColorName = colorName;
  }

  public boolean isValid(Project project) {
    if (myScopeName == null || myScopeName.length() == 0) {
      return false;
    }

    if (myColorName == null) {
      return false;
    }

    if (project != null) {
      return NamedScopeManager.getScope(project, myScopeName) != null;
    } else {
      return true;
    }
  }

  public void save(@Nonnull final Element e) {
    if (!isValid(null)) {
      return;
    }

    final Element tab = new Element(FileColorsModel.FILE_COLOR);

    tab.setAttribute(SCOPE_NAME, getScopeName());
    tab.setAttribute(COLOR, myColorName);

    e.addContent(tab);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FileColorConfiguration that = (FileColorConfiguration)o;

    if (!myColorName.equals(that.myColorName)) return false;
    if (!myScopeName.equals(that.myScopeName)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myScopeName.hashCode();
    result = 31 * result + myColorName.hashCode();
    return result;
  }

  @Override
  public FileColorConfiguration clone() throws CloneNotSupportedException {
    final FileColorConfiguration result = new FileColorConfiguration();

    result.myColorName = myColorName;
    result.myScopeName = myScopeName;

    return result;
  }

  @Nullable
  public static FileColorConfiguration load(@Nonnull final Element e) {
    final String path = e.getAttributeValue(SCOPE_NAME);
    if (path == null) {
      return null;
    }

    final String colorName = e.getAttributeValue(COLOR);
    if (colorName == null) {
      return null;
    }

    return new FileColorConfiguration(path, colorName);
  }
}

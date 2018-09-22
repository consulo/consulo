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
package com.intellij.psi.codeStyle;

import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.DifferenceFilter;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

/**
 * @author peter
 */
public abstract class CustomCodeStyleSettings implements Cloneable {
  private final CodeStyleSettings myContainer;
  private final String myTagName;

  protected CustomCodeStyleSettings(@NonNls @Nonnull String tagName, CodeStyleSettings container) {
    myTagName = tagName;
    myContainer = container;
  }

  public final CodeStyleSettings getContainer() {
    return myContainer;
  }

  @NonNls
  @Nonnull
  public final String getTagName() {
    return myTagName;
  }

  /**
   * in case settings save additional top-level tags, list the list of them to prevent serializer to treat such tag as unknown settings.
   */
  @Nonnull
  public List<String> getKnownTagNames() {
    return Collections.singletonList(getTagName());
  }

  public void readExternal(Element parentElement) throws InvalidDataException {
    DefaultJDOMExternalizer.readExternal(this, parentElement.getChild(myTagName));
  }

  public void writeExternal(Element parentElement, @Nonnull final CustomCodeStyleSettings parentSettings) throws WriteExternalException {
    final Element childElement = new Element(myTagName);
    DefaultJDOMExternalizer.writeExternal(this, childElement, new DifferenceFilter<CustomCodeStyleSettings>(this, parentSettings));
    if (!childElement.getContent().isEmpty()) {
      parentElement.addContent(childElement);
    }
  }

  @Override
  public Object clone() {
    try {
      return super.clone();
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * For compatibility with old code style settings stored in CodeStyleSettings.
   */
  protected void importLegacySettings(@Nonnull CodeStyleSettings rootSettings) {
  }

  /**
   * Fired before loading.
   */
  protected void beforeLoading() {
  }


  /**
   * Fired when settings just loaded.
   * <p>
   * <p>
   * When the common version (the {@link CodeStyleSettings#myVersion} is not changed, this method is called just after loading.
   * When the common version is changed, this method called after {@link CustomCodeStyleSettings#importLegacySettings}.
   * </p>
   */
  protected void afterLoaded() {
  }
}

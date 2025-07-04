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
package consulo.language.codeStyle.impl.internal;

import consulo.logging.Logger;
import consulo.component.persist.scheme.ExternalInfo;
import consulo.component.persist.scheme.ExternalizableScheme;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.language.codeStyle.CodeStyleScheme;
import consulo.language.codeStyle.CodeStyleSchemes;
import consulo.language.codeStyle.CodeStyleSettings;
import org.jdom.Element;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class CodeStyleSchemeImpl implements JDOMExternalizable, CodeStyleScheme, ExternalizableScheme {
  private static final Logger LOG = Logger.getInstance(CodeStyleSchemeImpl.class);

  private static final String NAME = "name";
  private static final String PARENT = "parent";

  private String myName;
  private Element myRootElement;
  private String myParentSchemeName;
  private final boolean myIsDefault;
  private volatile CodeStyleSettings myCodeStyleSettings;
  private final ExternalInfo myExternalInfo = new ExternalInfo();

  public CodeStyleSchemeImpl(@Nonnull String name, String parentSchemeName, Element rootElement) {
    myName = name;
    myRootElement = rootElement;
    myIsDefault = false;
    myParentSchemeName = parentSchemeName;
  }

  public CodeStyleSchemeImpl(@Nonnull String name, boolean isDefault, CodeStyleScheme parentScheme){
    myName = name;
    myIsDefault = isDefault;
    init(parentScheme, null);
  }

  public void init(@Nonnull CodeStyleSchemes schemesManager) {
    LOG.assertTrue(myCodeStyleSettings == null, "Already initialized");
    init(myParentSchemeName == null ? null : schemesManager.findSchemeByName(myParentSchemeName), myRootElement);
    myParentSchemeName = null;
    myRootElement = null;
  }

  private void init(@Nullable CodeStyleScheme parentScheme, Element root) {
    if (parentScheme == null) {
      myCodeStyleSettings = new CodeStyleSettings();
    }
    else {
      CodeStyleSettings parentSettings = parentScheme.getCodeStyleSettings();
      myCodeStyleSettings = parentSettings.clone();
      while (parentSettings.getParentSettings() != null) {
        parentSettings = parentSettings.getParentSettings();
      }
      myCodeStyleSettings.setParentSettings(parentSettings);
    }
    if (root != null) {
      try {
        readExternal(root);
      }
      catch (InvalidDataException e) {
        LOG.error(e);
      }
    }
  }

  @Override
  public CodeStyleSettings getCodeStyleSettings(){
    return myCodeStyleSettings;
  }

  public void setCodeStyleSettings(@Nonnull CodeStyleSettings codeStyleSettings){
    myCodeStyleSettings = codeStyleSettings;
  }

  @Override
  @Nonnull
  public String getName(){
    return myName;
  }

  @Override
  public boolean isDefault(){
    return myIsDefault;
  }

  public String toString(){
    return getName();
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException{
    myCodeStyleSettings.writeExternal(element);
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException{
    myCodeStyleSettings.readExternal(element);
  }

  @Nonnull
  public static CodeStyleSchemeImpl readScheme(@Nonnull Element element) {
    return new CodeStyleSchemeImpl(element.getAttributeValue(NAME), element.getAttributeValue(PARENT), element);
  }

  public Element saveToDocument() throws WriteExternalException {
    Element newElement = new Element("code_scheme");
    newElement.setAttribute(NAME, getName());
    writeExternal(newElement);
    return newElement;
  }

  @Override
  public void setName(@Nonnull final String name) {
    myName = name;
  }

  @Override
  @Nonnull
  public ExternalInfo getExternalInfo() {
    return myExternalInfo;
  }
}

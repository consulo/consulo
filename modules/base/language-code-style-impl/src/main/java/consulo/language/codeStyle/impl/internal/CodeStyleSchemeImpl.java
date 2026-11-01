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
import org.jspecify.annotations.Nullable;

public class CodeStyleSchemeImpl implements JDOMExternalizable, CodeStyleScheme, ExternalizableScheme {
  private static final Logger LOG = Logger.getInstance(CodeStyleSchemeImpl.class);

  private static final String NAME = "name";
  private static final String PARENT = "parent";

  private String myName;
  private final boolean myIsDefault;
  private final Object myLock = new Object();
  private @Nullable CodeStyleSchemes mySchemes;
  private @Nullable Element myRootElement;
  private @Nullable String myParentSchemeName;
  private volatile @Nullable CodeStyleSettings myCodeStyleSettings;
  private final ExternalInfo myExternalInfo = new ExternalInfo();

  public CodeStyleSchemeImpl(String name, @Nullable String parentSchemeName, Element rootElement, CodeStyleSchemes schemes) {
    myName = name;
    myRootElement = rootElement;
    myIsDefault = false;
    myParentSchemeName = parentSchemeName;
    mySchemes = schemes;
  }

  public CodeStyleSchemeImpl(String name, boolean isDefault, @Nullable CodeStyleScheme parentScheme){
    myName = name;
    myIsDefault = isDefault;
    myCodeStyleSettings = createSettings(parentScheme, null);
  }

  public void init() {
    getCodeStyleSettings();
  }

  private static CodeStyleSettings createSettings(@Nullable CodeStyleScheme parentScheme, @Nullable Element root) {
    CodeStyleSettings settings;
    if (parentScheme == null) {
      settings = new CodeStyleSettings();
    }
    else {
      CodeStyleSettings parentSettings = parentScheme.getCodeStyleSettings();
      settings = parentSettings.clone();
      while (parentSettings.getParentSettings() != null) {
        parentSettings = parentSettings.getParentSettings();
      }
      settings.setParentSettings(parentSettings);
    }
    if (root != null) {
      try {
        settings.readExternal(root);
      }
      catch (InvalidDataException e) {
        LOG.error(e);
      }
    }
    return settings;
  }

  @Override
  public CodeStyleSettings getCodeStyleSettings(){
    CodeStyleSettings settings = myCodeStyleSettings;
    if (settings != null) {
      return settings;
    }

    synchronized (myLock) {
      settings = myCodeStyleSettings;
      if (settings != null) {
        return settings;
      }

      String parentSchemeName = myParentSchemeName;
      CodeStyleSchemes schemes = mySchemes;
      myParentSchemeName = null;
      CodeStyleScheme parentScheme = parentSchemeName == null || schemes == null ? null : schemes.findSchemeByName(parentSchemeName);
      settings = createSettings(parentScheme, myRootElement);
      myCodeStyleSettings = settings;
      myRootElement = null;
      mySchemes = null;
      return settings;
    }
  }

  public void setCodeStyleSettings(CodeStyleSettings codeStyleSettings){
    synchronized (myLock) {
      myCodeStyleSettings = codeStyleSettings;
      myRootElement = null;
      myParentSchemeName = null;
      mySchemes = null;
    }
  }

  @Override
  
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
    getCodeStyleSettings().writeExternal(element);
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException{
    getCodeStyleSettings().readExternal(element);
  }

  
  public static CodeStyleSchemeImpl readScheme(Element element, CodeStyleSchemes schemes) {
    return new CodeStyleSchemeImpl(element.getAttributeValue(NAME), element.getAttributeValue(PARENT), element, schemes);
  }

  public Element saveToDocument() throws WriteExternalException {
    Element newElement = new Element("code_scheme");
    newElement.setAttribute(NAME, getName());
    writeExternal(newElement);
    return newElement;
  }

  @Override
  public void setName(String name) {
    myName = name;
  }

  @Override
  
  public ExternalInfo getExternalInfo() {
    return myExternalInfo;
  }
}

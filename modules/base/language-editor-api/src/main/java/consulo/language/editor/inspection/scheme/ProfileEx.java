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
package consulo.language.editor.inspection.scheme;

import consulo.project.Project;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.util.xml.serializer.SmartSerializer;
import consulo.util.xml.serializer.annotation.OptionTag;
import consulo.util.xml.serializer.annotation.Transient;
import consulo.logging.Logger;
import org.jdom.Element;

/**
 * @author anna
 * @since 2005-12-01
 */
@Deprecated
public abstract class ProfileEx implements Profile {
  private static final Logger LOG = Logger.getInstance(ProfileEx.class);

  public static final String SCOPE = "scope";
  public static final String NAME = "name";

  private final SmartSerializer mySerializer;

  
  protected String myName;

  @SuppressWarnings("unused")
  @OptionTag("myLocal")
  // exists only to preserve compatibility
  private boolean myLocal;

  protected ProfileManager myProfileManager;

  private boolean myIsProjectLevel;

  public ProfileEx(String name) {
    this(name, SmartSerializer.skipEmptySerializer());
  }

  protected ProfileEx(String name, SmartSerializer serializer) {
    myName = name;
    mySerializer = serializer;
  }

  @Override
  
  // ugly name to preserve compatibility
  @OptionTag("myName")
  public String getName() {
    return myName;
  }

  @Override
  public void copyFrom(Profile profile) {
    try {
      Element config = new Element("config");
      profile.writeExternal(config);
      readExternal(config);
    }
    catch (WriteExternalException e) {
      LOG.error(e);
    }
    catch (InvalidDataException e) {
      LOG.error(e);
    }
  }

  @Override
  @Transient
  public boolean isLocal() {
    return !myIsProjectLevel;
  }

  @Override
  @Transient
  public boolean isProjectLevel() {
    return myIsProjectLevel;
  }

  @Override
  public void setProjectLevel(boolean isProjectLevel) {
    myIsProjectLevel = isProjectLevel;
  }

  @Override
  public void setLocal(boolean isLocal) {
    myIsProjectLevel = !isLocal;
  }

  @Override
  public void setName(String name) {
    myName = name;
  }

  @Override
  
  @Transient
  public ProfileManager getProfileManager() {
    return myProfileManager;
  }

  @Override
  public void setProfileManager(ProfileManager profileManager) {
    myProfileManager = profileManager;
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    mySerializer.readExternal(this, element);
  }

  public void serializeInto(Element element, boolean preserveCompatibility) {
    mySerializer.writeExternal(this, element, preserveCompatibility);
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    serializeInto(element, true);
  }

  public void profileChanged() {
  }

  public boolean equals(Object o) {
    return this == o || o instanceof ProfileEx && myName.equals(((ProfileEx)o).myName);
  }

  public int hashCode() {
    return myName.hashCode();
  }

  @Override
  public int compareTo(Profile o) {
    return getName().compareToIgnoreCase(o.getName());
  }

  public void convert(Element element, Project project) {
  }
}

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
package com.intellij.profile;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.SmartSerializer;
import com.intellij.util.xmlb.annotations.OptionTag;
import com.intellij.util.xmlb.annotations.Transient;
import consulo.logging.Logger;
import org.jdom.Element;

import javax.annotation.Nonnull;

/**
 * User: anna
 * Date: 01-Dec-2005
 */
@Deprecated
public abstract class ProfileEx implements Profile {
  private static final Logger LOG = Logger.getInstance(ProfileEx.class);

  public static final String SCOPE = "scope";
  public static final String NAME = "name";

  private final SmartSerializer mySerializer;

  @Nonnull
  protected String myName;

  @SuppressWarnings("unused")
  @OptionTag("myLocal")
  // exists only to preserve compatibility
  private boolean myLocal;

  protected ProfileManager myProfileManager;

  private boolean myIsProjectLevel;

  public ProfileEx(@Nonnull String name) {
    this(name, SmartSerializer.skipEmptySerializer());
  }

  protected ProfileEx(@Nonnull String name, @Nonnull SmartSerializer serializer) {
    myName = name;
    mySerializer = serializer;
  }

  @Override
  @Nonnull
  // ugly name to preserve compatibility
  @OptionTag("myName")
  public String getName() {
    return myName;
  }

  @Override
  public void copyFrom(@Nonnull Profile profile) {
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
  public void setName(@Nonnull String name) {
    myName = name;
  }

  @Override
  @Nonnull
  @Transient
  public ProfileManager getProfileManager() {
    return myProfileManager;
  }

  @Override
  public void setProfileManager(@Nonnull ProfileManager profileManager) {
    myProfileManager = profileManager;
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    mySerializer.readExternal(this, element);
  }

  public void serializeInto(@Nonnull Element element, boolean preserveCompatibility) {
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
  public int compareTo(@Nonnull Object o) {
    if (o instanceof Profile) {
      return getName().compareToIgnoreCase(((Profile)o).getName());
    }
    return 0;
  }

  public void convert(@Nonnull Element element, @Nonnull Project project) {
  }
}

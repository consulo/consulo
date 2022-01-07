/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.options;

import com.intellij.util.text.UniqueNameGenerator;
import consulo.logging.Logger;
import consulo.util.pointers.Named;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public abstract class AbstractSchemesManager<T extends Named, E extends ExternalizableScheme> extends SchemesManager<T, E> {
  private static final Logger LOG = Logger.getInstance(AbstractSchemesManager.class);

  protected final List<T> mySchemes = new ArrayList<T>();
  private volatile T myCurrentScheme;
  private String myCurrentSchemeName;

  @Override
  public void addNewScheme(@Nonnull T scheme, boolean replaceExisting) {
    int toReplace = -1;
    for (int i = 0; i < mySchemes.size(); i++) {
      T existingScheme = mySchemes.get(i);
      if (existingScheme.getName().equals(scheme.getName())) {
        toReplace = i;
        break;
      }
    }
    if (toReplace == -1) {
      mySchemes.add(scheme);
    }
    else if (replaceExisting || !(scheme instanceof ExternalizableScheme)) {
      mySchemes.set(toReplace, scheme);
    }
    else {
      //noinspection unchecked
      renameScheme((ExternalizableScheme)scheme, UniqueNameGenerator.generateUniqueName(scheme.getName(), collectExistingNames(mySchemes)));
      mySchemes.add(scheme);
    }
    schemeAdded(scheme);
    checkCurrentScheme(scheme);
  }

  protected void checkCurrentScheme(@Nonnull Named scheme) {
    if (myCurrentScheme == null && Objects.equals(scheme.getName(), myCurrentSchemeName)) {
      //noinspection unchecked
      myCurrentScheme = (T)scheme;
    }
  }

  @Nonnull
  private Collection<String> collectExistingNames(@Nonnull Collection<T> schemes) {
    Set<String> result = new HashSet<String>(schemes.size());
    for (T scheme : schemes) {
      result.add(scheme.getName());
    }
    return result;
  }

  @Override
  public void clearAllSchemes() {
    for (T myScheme : mySchemes) {
      schemeDeleted(myScheme);
    }
    mySchemes.clear();
  }

  @Override
  @Nonnull
  public List<T> getAllSchemes() {
    return Collections.unmodifiableList(mySchemes);
  }

  @Override
  @Nullable
  public T findSchemeByName(@Nonnull String schemeName) {
    for (T scheme : mySchemes) {
      if (scheme.getName().equals(schemeName)) {
        return scheme;
      }
    }
    return null;
  }

  @Override
  public void setCurrentSchemeName(@Nullable String schemeName) {
    myCurrentSchemeName = schemeName;
    myCurrentScheme = schemeName == null ? null : findSchemeByName(schemeName);
  }

  @Override
  @Nullable
  public T getCurrentScheme() {
    T currentScheme = myCurrentScheme;
    return currentScheme == null ? null : findSchemeByName(currentScheme.getName());
  }

  @Override
  public void removeScheme(@Nonnull T scheme) {
    for (int i = 0, n = mySchemes.size(); i < n; i++) {
      T s = mySchemes.get(i);
      if (scheme.getName().equals(s.getName())) {
        schemeDeleted(s);
        mySchemes.remove(i);
        break;
      }
    }
  }

  protected void schemeDeleted(@Nonnull Named scheme) {
    if (myCurrentScheme == scheme) {
      myCurrentScheme = null;
    }
  }

  @Override
  @Nonnull
  public Collection<String> getAllSchemeNames() {
    List<String> names = new ArrayList<String>(mySchemes.size());
    for (T scheme : mySchemes) {
      names.add(scheme.getName());
    }
    return names;
  }

  protected abstract void schemeAdded(@Nonnull T scheme);

  protected static void renameScheme(@Nonnull ExternalizableScheme scheme, @Nonnull String newName) {
    if (!newName.equals(scheme.getName())) {
      scheme.setName(newName);
      LOG.assertTrue(newName.equals(scheme.getName()));
    }
  }
}

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
package consulo.ide.impl.idea.openapi.options;

import consulo.component.util.text.UniqueNameGenerator;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public abstract class AbstractSchemesManager<T, E extends ExternalizableScheme> extends SchemesManager<T, E> {
  private static final Logger LOG = Logger.getInstance(AbstractSchemesManager.class);

  protected final List<T> mySchemes = new ArrayList<>();
  private volatile T myCurrentScheme;
  private String myCurrentSchemeName;

  @Override
  public void addNewScheme(@Nonnull T scheme, boolean replaceExisting) {
    int toReplace = -1;
    for (int i = 0; i < mySchemes.size(); i++) {
      T existingScheme = mySchemes.get(i);
      if (getName(existingScheme).equals(getName(scheme))) {
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
      renameScheme((ExternalizableScheme)scheme, UniqueNameGenerator.generateUniqueName(getName(scheme), collectExistingNames(mySchemes)));
      mySchemes.add(scheme);
    }
    schemeAdded(scheme);
    checkCurrentScheme(scheme);
  }

  protected void checkCurrentScheme(@Nonnull T scheme) {
    if (myCurrentScheme == null && Objects.equals(getName(scheme), myCurrentSchemeName)) {
      //noinspection unchecked
      myCurrentScheme = (T)scheme;
    }
  }

  @Nonnull
  protected abstract String getName(T value);

  @Nonnull
  private Collection<String> collectExistingNames(@Nonnull Collection<T> schemes) {
    Set<String> result = new HashSet<String>(schemes.size());
    for (T scheme : schemes) {
      result.add(getName(scheme));
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
      if (getName(scheme).equals(schemeName)) {
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
    return currentScheme == null ? null : findSchemeByName(getName(currentScheme));
  }

  @Override
  public void removeScheme(@Nonnull T scheme) {
    for (int i = 0, n = mySchemes.size(); i < n; i++) {
      T s = mySchemes.get(i);
      if (getName(scheme).equals(getName(s))) {
        schemeDeleted(s);
        mySchemes.remove(i);
        break;
      }
    }
  }

  protected void schemeDeleted(@Nonnull T scheme) {
    if (myCurrentScheme == scheme) {
      myCurrentScheme = null;
    }
  }

  @Override
  @Nonnull
  public Collection<String> getAllSchemeNames() {
    List<String> names = new ArrayList<String>(mySchemes.size());
    for (T scheme : mySchemes) {
      names.add(getName(scheme));
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

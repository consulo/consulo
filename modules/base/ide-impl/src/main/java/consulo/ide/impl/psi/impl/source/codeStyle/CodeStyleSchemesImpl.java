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
package consulo.ide.impl.psi.impl.source.codeStyle;

import consulo.component.persist.RoamingType;
import consulo.component.persist.StoragePathMacros;
import consulo.component.persist.scheme.BaseSchemeProcessor;
import consulo.component.persist.scheme.SchemeProcessor;
import consulo.component.persist.scheme.SchemeManagerFactory;
import consulo.component.persist.scheme.SchemeManager;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.language.codeStyle.CodeStyleScheme;
import consulo.language.codeStyle.CodeStyleSchemes;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;

public abstract class CodeStyleSchemesImpl extends CodeStyleSchemes {
  @NonNls
  public static final String DEFAULT_SCHEME_NAME = CodeStyleScheme.DEFAULT_SCHEME_NAME;

  @SuppressWarnings("SpellCheckingInspection")
  static final String CODE_STYLES_DIR_PATH = StoragePathMacros.ROOT_CONFIG + "/codestyles";

  private boolean myIsInitialized = false;

  private final SchemeManager<CodeStyleScheme, CodeStyleSchemeImpl> mySchemeManager;

  public CodeStyleSchemesImpl(SchemeManagerFactory schemeManagerFactory) {
    SchemeProcessor<CodeStyleScheme, CodeStyleSchemeImpl> processor = new BaseSchemeProcessor<>() {
      @Nonnull
      @Override
      public CodeStyleSchemeImpl readScheme(@Nonnull Element element) {
        return CodeStyleSchemeImpl.readScheme(element);
      }

      @Override
      public Element writeScheme(@Nonnull final CodeStyleSchemeImpl scheme) throws WriteExternalException {
        return scheme.saveToDocument();
      }

      @Override
      public boolean shouldBeSaved(@Nonnull final CodeStyleSchemeImpl scheme) {
        return !scheme.isDefault();
      }

      @Override
      public void initScheme(@Nonnull final CodeStyleSchemeImpl scheme) {
        scheme.init(CodeStyleSchemesImpl.this);
      }

      @Nonnull
      @Override
      public String getName(@Nonnull CodeStyleScheme immutableElement) {
        return immutableElement.getName();
      }
    };

    mySchemeManager = schemeManagerFactory.createSchemeManager(CODE_STYLES_DIR_PATH, processor, RoamingType.DEFAULT);

    init();
    addScheme(new CodeStyleSchemeImpl(DEFAULT_SCHEME_NAME, true, null));
    setCurrentScheme(getDefaultScheme());
  }

  @Override
  public CodeStyleScheme[] getSchemes() {
    Collection<CodeStyleScheme> schemes = mySchemeManager.getAllSchemes();
    return schemes.toArray(new CodeStyleScheme[schemes.size()]);
  }

  @Override
  public CodeStyleScheme getCurrentScheme() {
    return mySchemeManager.getCurrentScheme();
  }

  @Override
  public void setCurrentScheme(CodeStyleScheme scheme) {
    String schemeName = scheme == null ? null : scheme.getName();
    mySchemeManager.setCurrentSchemeName(schemeName);
  }

  @SuppressWarnings("ForLoopThatDoesntUseLoopVariable")
  @Override
  public CodeStyleScheme createNewScheme(String preferredName, CodeStyleScheme parentScheme) {
    String name;
    if (preferredName == null) {
      if (parentScheme == null) throw new IllegalArgumentException("parentScheme must not be null");
      // Generate using parent name
      name = null;
      for (int i = 1; name == null; i++) {
        String currName = parentScheme.getName() + " (" + i + ")";
        if (findSchemeByName(currName) == null) {
          name = currName;
        }
      }
    }
    else {
      name = null;
      for (int i = 0; name == null; i++) {
        String currName = i == 0 ? preferredName : preferredName + " (" + i + ")";
        if (findSchemeByName(currName) == null) {
          name = currName;
        }
      }
    }
    return new CodeStyleSchemeImpl(name, false, parentScheme);
  }

  @Override
  public void deleteScheme(CodeStyleScheme scheme) {
    if (scheme.isDefault()) {
      throw new IllegalArgumentException("Unable to delete default scheme!");
    }
    CodeStyleSchemeImpl currScheme = (CodeStyleSchemeImpl)getCurrentScheme();
    if (currScheme == scheme) {
      CodeStyleScheme newCurrentScheme = getDefaultScheme();
      if (newCurrentScheme == null) {
        throw new IllegalStateException("Unable to load default scheme!");
      }
      setCurrentScheme(newCurrentScheme);
    }
    mySchemeManager.removeScheme(scheme);
  }

  @Override
  public CodeStyleScheme getDefaultScheme() {
    return findSchemeByName(DEFAULT_SCHEME_NAME);
  }

  @Nullable
  @Override
  public CodeStyleScheme findSchemeByName(@Nonnull String name) {
    return mySchemeManager.findSchemeByName(name);
  }

  @Override
  public void addScheme(CodeStyleScheme scheme) {
    mySchemeManager.addNewScheme(scheme, true);
  }

  protected void init() {
    if (myIsInitialized) return;
    myIsInitialized = true;
    mySchemeManager.loadSchemes();
  }

  public SchemeManager<CodeStyleScheme, CodeStyleSchemeImpl> getSchemeManager() {
    return mySchemeManager;
  }
}

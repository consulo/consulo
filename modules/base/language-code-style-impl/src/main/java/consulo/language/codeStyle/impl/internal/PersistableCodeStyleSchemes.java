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

import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.scheme.SchemeManagerFactory;
import consulo.language.codeStyle.CodeStyleScheme;
import consulo.util.xml.serializer.Accessor;
import consulo.util.xml.serializer.SerializationFilter;
import consulo.util.xml.serializer.XmlSerializer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Rustam Vishnyakov
 */
@Singleton
@State(name = "CodeStyleSchemeSettings", storages = @Storage("code.style.schemes.xml"), additionalExportFile = CodeStyleSchemesImpl.CODE_STYLES_DIR_PATH)
@ServiceImpl
public class PersistableCodeStyleSchemes extends CodeStyleSchemesImpl implements PersistentStateComponent<Element> {
  public String CURRENT_SCHEME_NAME = CodeStyleScheme.DEFAULT_SCHEME_NAME;

  @Inject
  public PersistableCodeStyleSchemes(@Nonnull SchemeManagerFactory schemeManagerFactory) {
    super(schemeManagerFactory);
  }

  @Nullable
  @Override
  public Element getState() {
    CodeStyleScheme currentScheme = getCurrentScheme();
    CURRENT_SCHEME_NAME = currentScheme == null ? null : currentScheme.getName();
    return XmlSerializer.serialize(this, new SerializationFilter() {
      @Override
      public boolean accepts(@Nonnull Accessor accessor, @Nonnull Object bean) {
        if ("CURRENT_SCHEME_NAME".equals(accessor.getName())) {
          return !CodeStyleScheme.DEFAULT_SCHEME_NAME.equals(accessor.read(bean));
        }
        else {
          return accessor.getValueClass().equals(String.class);
        }
      }
    });
  }

  @Override
  public void loadState(@Nonnull Element state) {
    CURRENT_SCHEME_NAME = CodeStyleScheme.DEFAULT_SCHEME_NAME;
    XmlSerializer.deserializeInto(this, state);
    CodeStyleScheme current = CURRENT_SCHEME_NAME == null ? null : findSchemeByName(CURRENT_SCHEME_NAME);
    setCurrentScheme(current == null ? getDefaultScheme() : current);
  }
}

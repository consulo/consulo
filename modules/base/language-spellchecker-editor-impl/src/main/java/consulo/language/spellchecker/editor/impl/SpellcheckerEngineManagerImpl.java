/*
 * Copyright 2013-2023 consulo.io
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
package consulo.language.spellchecker.editor.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.language.spellchecker.editor.SpellcheckerEngine;
import consulo.language.spellchecker.editor.SpellcheckerEngineManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nullable;
import java.util.List;

/**
 * TODO handle multiple engines
 *
 * @author VISTALL
 * @since 26/03/2023
 */
@Singleton
@ServiceImpl
public class SpellcheckerEngineManagerImpl implements SpellcheckerEngineManager {
  private final Application myApplication;

  @Inject
  public SpellcheckerEngineManagerImpl(Application application) {
    myApplication = application;
  }

  @Nullable
  @Override
  public SpellcheckerEngine getActiveEngine() {
    List<SpellcheckerEngine> list = myApplication.getExtensionList(SpellcheckerEngine.class);
    return list.isEmpty() ? null : list.get(0);
  }
}

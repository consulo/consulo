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

package com.intellij.testFramework.fixtures.impl;

import com.intellij.testFramework.builders.ModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.HeavyIdeaTestFixture;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.intellij.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * @author mike
*/
class HeavyTestFixtureBuilderImpl implements TestFixtureBuilder<IdeaProjectTestFixture> {
  private final HeavyIdeaTestFixtureImpl myFixture;
  private final Map<Class<? extends ModuleFixtureBuilder>, Class<? extends ModuleFixtureBuilder>> myProviders;

  public HeavyTestFixtureBuilderImpl(HeavyIdeaTestFixtureImpl fixture, final Map<Class<? extends ModuleFixtureBuilder>, Class<? extends ModuleFixtureBuilder>> providers) {
    myFixture = fixture;
    myProviders = providers;
  }

  private <M extends ModuleFixtureBuilder> M createModuleBuilder(Class<M> key) {
    Class<? extends ModuleFixtureBuilder> implClass = myProviders.get(key);
    assert implClass != null: key;
    return (M)ReflectionUtil.newInstance(implClass);
  }

  @Override
  public HeavyIdeaTestFixture getFixture() {
    return myFixture;
  }

  @Override
  public <M extends ModuleFixtureBuilder> M addModule(final Class<M> builderClass) {
    loadClassConstants(builderClass);
    final M builder = createModuleBuilder(builderClass);
    myFixture.addModuleFixtureBuilder(builder);
    return builder;
  }

  private static void loadClassConstants(final Class builderClass) {
    try {
      for (final Field field : builderClass.getFields()) {
        field.get(null);
      }
    }
    catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}

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

import com.intellij.testFramework.TestModuleDescriptor;
import com.intellij.testFramework.builders.EmptyModuleFixtureBuilder;
import com.intellij.testFramework.builders.ModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * This is used in IdeaTestFixtureFactory
 * @author mike
 */
@SuppressWarnings({"UnusedDeclaration"})
public class IdeaTestFixtureFactoryImpl extends IdeaTestFixtureFactory {
  protected final Map<Class<? extends ModuleFixtureBuilder>, Class<? extends ModuleFixtureBuilder>> myFixtureBuilderProviders =
    new HashMap<Class<? extends ModuleFixtureBuilder>, Class<? extends ModuleFixtureBuilder>>();

  public IdeaTestFixtureFactoryImpl() {
    registerFixtureBuilder(EmptyModuleFixtureBuilder.class, MyEmptyModuleFixtureBuilderImpl.class);
  }

  @Override
  public final <T extends ModuleFixtureBuilder> void registerFixtureBuilder(@Nonnull Class<T> aClass, @Nonnull Class<? extends T> implClass) {
    myFixtureBuilderProviders.put(aClass, implClass);
  }

  @Override
  public void registerFixtureBuilder(@Nonnull Class<? extends ModuleFixtureBuilder> aClass, @Nonnull String implClassName) {
    try {
      final Class implClass = Class.forName(implClassName);
      assert aClass.isAssignableFrom(implClass);
      registerFixtureBuilder(aClass, implClass);
    }
    catch (ClassNotFoundException e) {
      throw new RuntimeException("Cannot instantiate fixture builder implementation", e);
    }
  }

  @Override
  public TestFixtureBuilder<IdeaProjectTestFixture> createFixtureBuilder(@Nonnull String name) {
    return new HeavyTestFixtureBuilderImpl(new HeavyIdeaTestFixtureImpl(name), myFixtureBuilderProviders);
  }

  @Override
  public TestFixtureBuilder<IdeaProjectTestFixture> createLightFixtureBuilder() {
    return new LightTestFixtureBuilderImpl<IdeaProjectTestFixture>(new LightIdeaTestFixtureImpl(
            TestModuleDescriptor.EMPTY));
  }

  @Override
  public TestFixtureBuilder<IdeaProjectTestFixture> createLightFixtureBuilder(@Nullable TestModuleDescriptor projectDescriptor) {
    if (projectDescriptor == null) {
      projectDescriptor = TestModuleDescriptor.EMPTY;
    }
    return new LightTestFixtureBuilderImpl<IdeaProjectTestFixture>(new LightIdeaTestFixtureImpl(projectDescriptor));
  }

  @Override
  public CodeInsightTestFixture createCodeInsightFixture(@Nonnull IdeaProjectTestFixture projectFixture) {
    return createCodeInsightFixture(projectFixture, new TempDirTestFixtureImpl());
  }

  @Override
  public CodeInsightTestFixture createCodeInsightFixture(@Nonnull IdeaProjectTestFixture projectFixture, @Nonnull TempDirTestFixture tempDirFixture) {
    return new CodeInsightTestFixtureImpl(projectFixture, tempDirFixture);
  }

  @Override
  public TempDirTestFixture createTempDirTestFixture() {
    return new TempDirTestFixtureImpl();
  }

  public static class MyEmptyModuleFixtureBuilderImpl extends EmptyModuleFixtureBuilderImpl {
    public MyEmptyModuleFixtureBuilderImpl(final TestFixtureBuilder<? extends IdeaProjectTestFixture> testFixtureBuilder) {
      super(testFixtureBuilder);
    }

    @Override
    protected ModuleFixture instantiateFixture() {
      return new ModuleFixtureImpl(this);
    }
  }
}

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

package com.intellij.openapi.options;

import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * SearchableConfigurable instances would be instantiated on buildSearchableOptions step during Installer's build to index of all available options.
 * {@link #com.intellij.ide.ui.search.TraverseUIStarter}
 */
public interface SearchableConfigurable extends Configurable {
  @Nonnull
  String getId();

  @Nullable
  default Runnable enableSearch(String option) {
    return null;
  }

  /**
   * When building an index of searchable options, it's important to know a class which caused the creation of a configurable.
   * It often happens that the configurable is created based on a provider from an arbitrary extension point.
   * In such a case, the provider's class should be returned from this method.
   * <br/>
   * When the configurable is based on several providers consider extending {@link com.intellij.openapi.options.CompositeConfigurable}.
   * <br/>
   * Keep in mind that this method can be expensive as it can load previously unloaded class.
   *
   * @return a class which is a cause of the creation of this configurable
   */
  @Nonnull
  default Class<?> getOriginalClass() {
    return this.getClass();
  }

  interface Parent extends SearchableConfigurable, Composite {
    boolean hasOwnContent();

    boolean isVisible();


    abstract class Abstract implements Parent {
      private Configurable[] myKids;

      @RequiredUIAccess
      @Override
      @Deprecated
      public JComponent createComponent() {
        return null;
      }

      @RequiredUIAccess
      @Nullable
      @Override
      public JComponent createComponent(@Nonnull Disposable parentDisposable) {
        return createComponent();
      }

      @RequiredUIAccess
      @Nullable
      @Override
      public Component createUIComponent() {
        return null;
      }

      @RequiredUIAccess
      @Nullable
      @Override
      public Component createUIComponent(@Nonnull Disposable parentDisposable) {
        return createUIComponent();
      }

      @Override
      public boolean hasOwnContent() {
        return false;
      }


      @RequiredUIAccess
      @Override
      public boolean isModified() {
        return false;
      }

      @RequiredUIAccess
      @Override
      public void apply() throws ConfigurationException {
      }

      @RequiredUIAccess
      @Override
      public void reset() {
      }

      @RequiredUIAccess
      @Override
      public void disposeUIResources() {
        myKids = null;
      }

      @Override
      public Runnable enableSearch(final String option) {
        return null;
      }

      @Override
      public boolean isVisible() {
        return true;
      }

      @Nonnull
      @Override
      public final Configurable[] getConfigurables() {
        if (myKids != null) return myKids;
        myKids = buildConfigurables();
        return myKids;
      }

      protected abstract Configurable[] buildConfigurables();
    }
  }
}

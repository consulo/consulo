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
package com.intellij.xdebugger.impl.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SmartList;
import com.intellij.xdebugger.XDebuggerBundle;
import com.intellij.xdebugger.breakpoints.XBreakpointType;
import com.intellij.xdebugger.settings.DebuggerSettingsCategory;
import consulo.disposer.Disposable;
import consulo.options.ConfigurableUIMigrationUtil;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class DebuggerConfigurable implements SearchableConfigurable.Parent {
  public static final String DISPLAY_NAME = XDebuggerBundle.message("debugger.configurable.display.name");

  static final Configurable[] EMPTY_CONFIGURABLES = new Configurable[0];
  private static final DebuggerSettingsCategory[] MERGED_CATEGORIES = {DebuggerSettingsCategory.STEPPING, DebuggerSettingsCategory.HOTSWAP};

  private Configurable myRootConfigurable;
  private Configurable[] myChildren;

  @Override
  public String getDisplayName() {
    return DISPLAY_NAME;
  }

  @Nonnull
  @Override
  public Configurable[] getConfigurables() {
    compute();

    if (myChildren.length == 0 && myRootConfigurable instanceof SearchableConfigurable.Parent) {
      return ((Parent)myRootConfigurable).getConfigurables();
    }
    else {
      return myChildren;
    }
  }

  private void compute() {
    if (myChildren != null) {
      return;
    }

    List<Configurable> configurables = new SmartList<>();
    configurables.add(new DataViewsConfigurable());

    computeMergedConfigurables(configurables);

    configurables.addAll(XDebuggerConfigurableProvider.getConfigurables(DebuggerSettingsCategory.ROOT));

    MergedCompositeConfigurable mergedGeneralConfigurable = computeGeneralConfigurables();
    if (configurables.isEmpty() && mergedGeneralConfigurable == null) {
      myRootConfigurable = null;
      myChildren = EMPTY_CONFIGURABLES;
    }
    else if (configurables.size() == 1) {
      Configurable firstConfigurable = configurables.get(0);
      if (mergedGeneralConfigurable == null) {
        myRootConfigurable = firstConfigurable;
        myChildren = EMPTY_CONFIGURABLES;
      }
      else {
        Configurable[] generalConfigurables = mergedGeneralConfigurable.children;
        Configurable[] mergedArray = new Configurable[generalConfigurables.length + 1];
        System.arraycopy(generalConfigurables, 0, mergedArray, 0, generalConfigurables.length);
        mergedArray[generalConfigurables.length] = firstConfigurable;
        myRootConfigurable = new MergedCompositeConfigurable("", "", mergedArray);
        myChildren = firstConfigurable instanceof SearchableConfigurable.Parent ? ((Parent)firstConfigurable).getConfigurables() : EMPTY_CONFIGURABLES;
      }
    }
    else {
      myChildren = configurables.toArray(new Configurable[configurables.size()]);
      myRootConfigurable = mergedGeneralConfigurable;
    }
  }

  private static void computeMergedConfigurables(@Nonnull List<Configurable> result) {
    for (DebuggerSettingsCategory category : MERGED_CATEGORIES) {
      Collection<Configurable> configurables = XDebuggerConfigurableProvider.getConfigurables(category);
      if (!configurables.isEmpty()) {
        String id = category.name().toLowerCase(Locale.ENGLISH);
        result.add(new MergedCompositeConfigurable("debugger." + id, XDebuggerBundle.message("debugger." + id + ".display.name"),
                                                   configurables.toArray(new Configurable[configurables.size()])));
      }
    }
  }

  @Nullable
  private static MergedCompositeConfigurable computeGeneralConfigurables() {
    Collection<Configurable> rootConfigurables = XDebuggerConfigurableProvider.getConfigurables(DebuggerSettingsCategory.GENERAL);
    if (rootConfigurables.isEmpty()) {
      return null;
    }

    Configurable[] mergedRootConfigurables = rootConfigurables.toArray(new Configurable[rootConfigurables.size()]);
    // move unnamed to top
    Arrays.sort(mergedRootConfigurables, (o1, o2) -> {
      boolean c1e = StringUtil.isEmpty(o1.getDisplayName());
      return c1e == StringUtil.isEmpty(o2.getDisplayName()) ? 0 : (c1e ? -1 : 1);
    });
    return new MergedCompositeConfigurable("", "", mergedRootConfigurables);
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    if (myRootConfigurable != null) {
      myRootConfigurable.apply();
    }
  }

  @Override
  public boolean hasOwnContent() {
    compute();
    return myRootConfigurable != null;
  }

  @Override
  public boolean isVisible() {
    return XBreakpointType.EXTENSION_POINT_NAME.hasAnyExtensions();
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent(@Nonnull Disposable parent) {
    compute();
    return myRootConfigurable != null ? ConfigurableUIMigrationUtil.createComponent(myRootConfigurable, parent) : null;
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public Component createUIComponent(@Nonnull Disposable parent) {
    compute();
    return myRootConfigurable != null ? myRootConfigurable.createUIComponent(parent) : null;
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return myRootConfigurable != null && myRootConfigurable.isModified();
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    if (myRootConfigurable != null) {
      myRootConfigurable.reset();
    }
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    if (myRootConfigurable != null) {
      myRootConfigurable.disposeUIResources();
    }
  }

  @Override
  @Nonnull
  public String getId() {
    return "project.propDebugger";
  }
}

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

/*
 * User: anna
 * Date: 20-Apr-2009
 */
package consulo.language.editor.internal.inspection;

import consulo.configurable.ConfigurationException;
import consulo.configurable.UnnamedConfigurable;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.disposer.Disposable;
import consulo.language.editor.inspection.InspectionTool;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.jdom.JDOMUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ScopeToolState {
  private NamedScope myScope;
  @Nonnull
  private final String myScopeId;
  private InspectionToolWrapper<InspectionTool> myToolWrapper;
  private boolean myEnabled;
  private HighlightDisplayLevel myLevel;

  private SimpleReference<UnnamedConfigurable> myConfigurableRef;

  private static final Logger LOG = Logger.getInstance(ScopeToolState.class);

  public ScopeToolState(@Nonnull NamedScope scope,
                        @Nonnull InspectionToolWrapper toolWrapper,
                        boolean enabled,
                        @Nonnull HighlightDisplayLevel level) {
    this(scope.getScopeId(), toolWrapper, enabled, level);
    myScope = scope;
  }

  public ScopeToolState(@Nonnull String scopeName,
                        @Nonnull InspectionToolWrapper toolWrapper,
                        boolean enabled,
                        @Nonnull HighlightDisplayLevel level) {
    myScopeId = scopeName;
    myToolWrapper = toolWrapper;
    myEnabled = enabled;
    myLevel = level;
  }

  @Nullable
  public NamedScope getScope(Project project) {
    if (myScope == null) {
      if (project != null) {
        myScope = NamedScopesHolder.getScope(project, myScopeId);
      }
    }
    return myScope;
  }

  @Nonnull
  public String getScopeId() {
    return myScopeId;
  }

  @Nonnull
  public InspectionToolWrapper getTool() {
    return myToolWrapper;
  }

  public boolean isEnabled() {
    return myEnabled;
  }

  @Nonnull
  public HighlightDisplayLevel getLevel() {
    return myLevel;
  }

  public void setEnabled(boolean enabled) {
    myEnabled = enabled;
  }

  public void setLevel(@Nonnull HighlightDisplayLevel level) {
    myLevel = level;
  }

  @Nullable
  @RequiredUIAccess
  public Component getConfigurablePanel(@Nonnull Disposable parentDisposable) {
    if (myConfigurableRef != null) {
      UnnamedConfigurable unnamedConfigurable = myConfigurableRef.get();
      return unnamedConfigurable == null ? null : unnamedConfigurable.createUIComponent(parentDisposable);
    }

    UnnamedConfigurable configurable = myToolWrapper.getToolState().createConfigurable();
    myConfigurableRef = SimpleReference.create(configurable);
    if (configurable == null) return null;
    else {
      Component uiComponent = configurable.createUIComponent(parentDisposable);
      configurable.initialize();
      configurable.reset();
      return uiComponent;
    }
  }

  @RequiredUIAccess
  public boolean isModified() {
    SimpleReference<UnnamedConfigurable> configurableRef = myConfigurableRef;
    if (configurableRef == null) {
      return false;
    }

    UnnamedConfigurable unnamedConfigurable = configurableRef.get();
    return unnamedConfigurable != null && unnamedConfigurable.isModified();
  }

  @RequiredUIAccess
  public void resetConfigPanel() {
    if (myConfigurableRef != null) {
      UnnamedConfigurable unnamedConfigurable = myConfigurableRef.get();
      if (unnamedConfigurable != null) {
        unnamedConfigurable.disposeUIResources();
      }
    }
    myConfigurableRef = null;
  }

  @RequiredUIAccess
  public void disposeUIResources() {
    if (myConfigurableRef != null) {
      UnnamedConfigurable unnamedConfigurable = myConfigurableRef.get();
      if (unnamedConfigurable != null) {
        unnamedConfigurable.disposeUIResources();
      }
    }
    myConfigurableRef = null;
  }

  @RequiredUIAccess
  public void applyConfigPanel() throws ConfigurationException {
    if (myConfigurableRef != null) {
      UnnamedConfigurable unnamedConfigurable = myConfigurableRef.get();
      if (unnamedConfigurable != null) {
        unnamedConfigurable.apply();
      }
    }
  }

  public void setTool(@Nonnull InspectionToolWrapper tool) {
    myToolWrapper = tool;
  }

  public boolean equalTo(@Nonnull ScopeToolState state2) {
    if (isEnabled() != state2.isEnabled()) return false;
    if (getLevel() != state2.getLevel()) return false;
    InspectionToolWrapper toolWrapper = getTool();
    InspectionToolWrapper toolWrapper2 = state2.getTool();
    if (!toolWrapper.isInitialized() && !toolWrapper2.isInitialized()) return true;
    try {
      String tempRoot = "root";
      Element oldToolSettings = new Element(tempRoot);
      toolWrapper.writeExternal(oldToolSettings);
      Element newToolSettings = new Element(tempRoot);
      toolWrapper2.writeExternal(newToolSettings);
      return JDOMUtil.areElementsEqual(oldToolSettings, newToolSettings);
    }
    catch (WriteExternalException e) {
      LOG.error(e);
    }
    return false;
  }

  public void scopesChanged() {
    myScope = null;
  }
}
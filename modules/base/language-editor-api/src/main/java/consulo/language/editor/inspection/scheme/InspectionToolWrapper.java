/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.editor.inspection.scheme;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.bind.InjectingBinding;
import consulo.component.internal.inject.InjectingBindingLoader;
import consulo.language.Language;
import consulo.language.editor.inspection.CleanupLocalInspectionTool;
import consulo.language.editor.inspection.GlobalInspectionContext;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.io.ResourceUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * @author Dmitry Avdeev
 * Date: 9/28/11
 */
public abstract class InspectionToolWrapper<T extends InspectionProfileEntry> {
  private static final Logger LOG = Logger.getInstance(InspectionToolWrapper.class);

  protected final T myTool;

  protected InspectionToolWrapper(@Nonnull T tool) {
    myTool = tool;
  }

  @SuppressWarnings("unchecked")
  protected InspectionToolWrapper(@Nonnull InspectionToolWrapper<T> other) {
    // find injecting binding and create new instance
    Map<String, List<InjectingBinding>> bindings = InjectingBindingLoader.INSTANCE.getHolder(ExtensionAPI.class, ComponentScope.APPLICATION).getBindings();
    List<InjectingBinding> injectingBindings = bindings.get(getToolClass().getName());

    InjectingBinding binding = null;
    for (InjectingBinding injectingBinding : injectingBindings) {
      try {
        if (injectingBinding.getImplClass() == other.myTool.getClass())  {
          binding = injectingBinding;
          break;
        }
      }
      catch (Exception ignored) {
      }
    }

    if (binding == null) {
      throw new IllegalArgumentException("Can't find injecting binding for " + other.myTool.getClass());
    }

    final InjectingBinding finalBinding = binding;
    myTool = (T)Application.get().getUnbindedInstance((Class)other.myTool.getClass(), binding.getParameterTypes(), args -> (T)finalBinding.create(args));
  }

  @Nonnull
  protected abstract Class<? extends InspectionProfileEntry> getToolClass();

  public void initialize(@Nonnull GlobalInspectionContext context) {
  }

  @Nonnull
  public abstract InspectionToolWrapper<T> createCopy();

  @Nonnull
  public T getTool() {
    return myTool;
  }

  public boolean isInitialized() {
    return myTool != null;
  }

  @Nullable
  public Language getLanguage() {
    return getTool().getLanguage();
  }

  public boolean isApplicable(@Nonnull Language language) {
    return getLanguage() == language;
  }

  public boolean isCleanupTool() {
    return getTool() instanceof CleanupLocalInspectionTool;
  }

  @Nonnull
  public String getShortName() {
    return getTool().getShortName();
  }

  @Nonnull
  public String getDisplayName() {
    return getTool().getDisplayName();
  }

  @Nonnull
  public String getGroupDisplayName() {
    return getTool().getGroupDisplayName();
  }

  public boolean isEnabledByDefault() {
    return getTool().isEnabledByDefault();
  }

  @Nonnull
  public HighlightDisplayLevel getDefaultLevel() {
    return getTool().getDefaultLevel();
  }

  @Nonnull
  public String[] getGroupPath() {
    return getTool().getGroupPath();
  }

  public void projectOpened(@Nonnull Project project) {

  }

  public void projectClosed(@Nonnull Project project) {

  }

  public String getStaticDescription() {
    return getTool().getStaticDescription();
  }

  public String loadDescription() {
    final String description = getStaticDescription();
    if (description != null) return description;
    try {
      URL descriptionUrl = getDescriptionUrl();
      if (descriptionUrl == null) return null;
      return ResourceUtil.loadText(descriptionUrl);
    }
    catch (IOException ignored) {
    }

    return getTool().loadDescription();
  }

  protected URL getDescriptionUrl() {
    return superGetDescriptionUrl();
  }

  @Nullable
  protected URL superGetDescriptionUrl() {
    final String fileName = getDescriptionFileName();
    return ResourceUtil.getResource(getDescriptionContextClass(), "/inspectionDescriptions", fileName);
  }

  @Nonnull
  public String getDescriptionFileName() {
    return getShortName() + ".html";
  }

  @Nonnull
  public final String getFolderName() {
    return getShortName();
  }

  @Nonnull
  public Class<? extends InspectionProfileEntry> getDescriptionContextClass() {
    return getTool().getClass();
  }

  public String getMainToolId() {
    return getTool().getMainToolId();
  }

  @Override
  public String toString() {
    return getShortName();
  }

  public void cleanup(Project project) {
    T tool = myTool;
    if (tool != null) {
      tool.cleanup(project);
    }
  }

  @Nonnull
  public abstract JobDescriptor[] getJobDescriptors(@Nonnull GlobalInspectionContext context);
}

/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.annotation.DeprecationInfo;
import consulo.content.scope.NamedScope;
import consulo.language.editor.inspection.InspectionTool;
import consulo.language.editor.internal.inspection.ScopeToolState;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author anna
 * @since 2004-12-07
 */
public interface InspectionProfile extends Profile {

  HighlightDisplayLevel getErrorLevel(@Nonnull HighlightDisplayKey inspectionToolKey, PsiElement element);

  /**
   * If you need to modify tool's settings, please use {@link #modifyToolSettings}
   */
  InspectionToolWrapper getInspectionTool(@Nonnull String shortName, @Nonnull PsiElement element);

  @Nullable
  InspectionToolWrapper getInspectionTool(@Nonnull String shortName, Project project);

  @Nullable
  InspectionToolWrapper getToolById(@Nonnull String id, @Nonnull PsiElement element);

  /**
   * @return tool by shortName and scope
   */
  <T extends InspectionTool> T getUnwrappedTool(@Nonnull String shortName, @Nonnull PsiElement element);

  /**
   * @return nullable if tool by shortName not found
   */
  @Nullable
  <S> S getToolState(@Nonnull String shortName, @Nonnull PsiElement element);

  void modifyProfile(@Nonnull Consumer<ModifiableModel> modelConsumer);

  /**
   * Allows a plugin to modify the settings of the inspection tool with the specified ID programmatically, without going through
   * the settings dialog.
   *
   * @param shortName the ID of the tool to change.
   * @param psiElement   the element for which the settings should be changed.
   * @param toolConsumer the callback that receives the tool.
   * @since 12.1
   */
  <T extends InspectionTool, S> void modifyToolSettings(@Nonnull String shortName, @Nonnull PsiElement psiElement, @Nonnull BiConsumer<T, S> toolConsumer);

  /**
   * @param element context element
   * @return all (both enabled and disabled) tools
   */
  @Nonnull
  InspectionToolWrapper[] getInspectionTools(@Nullable PsiElement element);

  void cleanup(@Nonnull Project project);

  /**
   * @see #modifyProfile(Consumer)
   */
  @Nonnull
  ModifiableModel getModifiableModel();

  boolean isToolEnabled(HighlightDisplayKey key, PsiElement element);

  boolean isToolEnabled(HighlightDisplayKey key);

  boolean isExecutable(Project project);

  boolean isEditable();

  @Nonnull
  String getDisplayName();

  void scopesChanged();

  @Nonnull
  List<Tools> getAllEnabledInspectionTools(Project project);

  @Deprecated
  @DeprecationInfo("internal impl")
  HighlightDisplayLevel getErrorLevel(@Nonnull HighlightDisplayKey ky, NamedScope scope, Project project);

  @Deprecated
  @DeprecationInfo("internal impl")
  ScopeToolState addScope(@Nonnull InspectionToolWrapper toolWrapper, NamedScope scope, @Nonnull HighlightDisplayLevel level, boolean enabled, Project project);

  @Deprecated
  @DeprecationInfo("internal impl")
  void removeScope(@Nonnull String toolId, @Nonnull String scopeName, Project project);
}

// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.template.context;

import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.util.lang.lazy.ClearableLazyValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Supplier;

/**
 * Implement this class to describe some particular context that the user may associate with a live template, e.g., "Java String Start".
 * Contexts are available for the user in the Live Template management UI.
 */
public abstract class BaseTemplateContextType implements TemplateContextType {

  @Nonnull
  private final String myContextId;
  @Nonnull
  private final LocalizeValue myPresentableName;
  private final ClearableLazyValue<? extends BaseTemplateContextType> myBaseContextType;

  protected BaseTemplateContextType(@Nonnull String id, @Nonnull LocalizeValue presentableName) {
    this(id, presentableName, EverywhereContextType.class);
  }

  protected BaseTemplateContextType(@Nonnull String id, @Nonnull LocalizeValue presentableName, @Nullable Class<? extends BaseTemplateContextType> baseContextType) {
    myContextId = id;
    myPresentableName = presentableName;
    myBaseContextType = ClearableLazyValue.nullable((Supplier<? extends BaseTemplateContextType>)() -> baseContextType == null ? null : EP_NAME.findExtensionOrFail(baseContextType));
  }

  /**
   * @return context presentable name for templates editor
   */
  @Override
  @Nonnull
  public LocalizeValue getPresentableName() {
    return myPresentableName;
  }

  /**
   * @return unique ID to be used on configuration files to flag if this context is enabled for particular template
   */
  @Override
  @Nonnull
  public String getContextId() {
    return myContextId;
  }

  /**
   * @deprecated use {@link #isInContext(TemplateActionContext)}
   */
  @Override
  @Deprecated(forRemoval = true)
  public boolean isInContext(@Nonnull PsiFile file, int offset) {
    throw new RuntimeException("Please, implement isInContext(TemplateActionContext) method and don't invoke this method directly");
  }

  /**
   * @return true iff this context type permits using template associated with it according to {@code templateActionContext}
   */
  @Override
  public boolean isInContext(@Nonnull TemplateActionContext templateActionContext) {
    return isInContext(templateActionContext.getFile(), templateActionContext.getStartOffset());
  }

  /**
   * @return parent context type. Parent context serves two purposes:
   * <ol>
   * <li>Context types hierarchy shown as a tree in template editor</li>
   * <li>When template applicability is computed, IDE finds all deepest applicable context types for the current {@link TemplateActionContext}
   * and excludes checking of all of their parent contexts. Then, IDE checks that at least one of these deepest applicable contexts is
   * enabled for the template.</li>
   * </ol>
   */
  @Override
  @Nullable
  public BaseTemplateContextType getBaseContextType() {
    return myBaseContextType.get();
  }

  public void clearCachedBaseContextType() {
    myBaseContextType.clear();
  }
}

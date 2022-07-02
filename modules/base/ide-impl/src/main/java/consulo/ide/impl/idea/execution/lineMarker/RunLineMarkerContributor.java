/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.execution.lineMarker;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.openapi.actionSystem.impl.SimpleDataContext;
import consulo.ide.impl.idea.util.Function;
import consulo.language.Language;
import consulo.language.editor.CommonDataKeys;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToMany;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class RunLineMarkerContributor implements LanguageExtension {
  private static final ExtensionPointCacheKey<RunLineMarkerContributor, ByLanguageValue<List<RunLineMarkerContributor>>> KEY =
          ExtensionPointCacheKey.create("RunLineMarkerContributor", LanguageOneToMany.build(false));

  @Nonnull
  public static List<RunLineMarkerContributor> forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(RunLineMarkerContributor.class).getOrBuildCache(KEY).requiredGet(language);
  }

  public static class Info {
    public final Image icon;
    public final AnAction[] actions;
    public final Function<PsiElement, String> tooltipProvider;

    public Info(Image icon, @Nullable Function<PsiElement, String> tooltipProvider, @Nonnull AnAction... actions) {
      this.icon = icon;
      this.actions = actions;
      this.tooltipProvider = tooltipProvider;
    }

    public Info(@Nonnull final AnAction action) {
      this(action.getTemplatePresentation().getIcon(), element -> getText(action, element), action);
    }
  }

  @Nullable
  @RequiredReadAction
  public abstract Info getInfo(PsiElement element);

  /**
   * @param action
   * @param element
   * @return null means disabled
   */
  @Nullable
  protected static String getText(@Nonnull AnAction action, @Nonnull PsiElement element) {
    DataContext parent = DataManager.getInstance().getDataContext();
    DataContext dataContext = SimpleDataContext.getSimpleContext(CommonDataKeys.PSI_ELEMENT, element, parent);
    AnActionEvent event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.STATUS_BAR_PLACE, dataContext);
    action.update(event);
    Presentation presentation = event.getPresentation();
    return presentation.isEnabled() && presentation.isVisible() ? presentation.getText() : null;
  }
}

/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.execution.icon.ExecutionIconGroup;
import consulo.execution.lineMarker.RunLineMarkerContributor;
import consulo.language.Language;
import consulo.language.editor.Pass;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProviderDescriptor;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class RunLineMarkerProvider extends LineMarkerProviderDescriptor {

  @Nonnull
  @Override
  public Language getLanguage() {
    return Language.ANY;
  }

  @RequiredReadAction
  @Nullable
  @Override
  public LineMarkerInfo getLineMarkerInfo(@Nonnull PsiElement element) {
    List<RunLineMarkerContributor> contributors = RunLineMarkerContributor.forLanguage(element.getLanguage());
    ActionGroup.Builder builder = null;
    Image icon = null;
    final List<RunLineMarkerContributor.Info> infos = new ArrayList<>();
    for (RunLineMarkerContributor contributor : contributors) {
      RunLineMarkerContributor.Info info = contributor.getInfo(element);
      if (info == null) {
        continue;
      }
      if (icon == null) {
        icon = info.icon;
      }
      if (builder == null) {
        builder = ActionGroup.newImmutableBuilder();
      }
      infos.add(info);
      for (AnAction action : info.actions) {
        builder.add(new LineMarkerActionWrapper(element, action));
      }
      builder.add(new AnSeparator());
    }
    if (icon == null) return null;

    final ActionGroup finalActionGroup = builder == null ? null : builder.build();
    Function<PsiElement, String> tooltipProvider = element1 -> {
      final StringBuilder tooltip = new StringBuilder();
      for (RunLineMarkerContributor.Info info : infos) {
        if (info.tooltipProvider != null) {
          String string = info.tooltipProvider.apply(element1);
          if (string == null) continue;
          if (tooltip.length() != 0) {
            tooltip.append("\n");
          }
          tooltip.append(string);
        }
      }

      return tooltip.length() == 0 ? null : tooltip.toString();
    };
    return new LineMarkerInfo<>(element, element.getTextRange(), icon, Pass.LINE_MARKERS, tooltipProvider, null, GutterIconRenderer.Alignment.CENTER) {
      @Nullable
      @Override
      public GutterIconRenderer createGutterRenderer() {
        return new LineMarkerGutterIconRenderer<>(this) {
          @Override
          public AnAction getClickAction() {
            return null;
          }

          @Override
          public boolean isNavigateAction() {
            return true;
          }

          @Nullable
          @Override
          public ActionGroup getPopupMenuActions() {
            return finalActionGroup;
          }
        };
      }
    };
  }

  @Nonnull
  @Override
  public String getName() {
    return "Run line marker";
  }

  @Nullable
  @Override
  public Image getIcon() {
    return ExecutionIconGroup.gutterRun();
  }
}
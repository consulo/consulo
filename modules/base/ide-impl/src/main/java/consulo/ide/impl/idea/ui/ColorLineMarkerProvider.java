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
package consulo.ide.impl.idea.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.Pass;
import consulo.language.editor.WriteCommandAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.language.editor.gutter.*;
import consulo.language.editor.util.PsiUtilBase;
import consulo.ide.impl.idea.util.FunctionUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.AllIcons;
import consulo.language.psi.ElementColorProvider;
import consulo.language.psi.PsiElement;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.ColorChooser;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import jakarta.annotation.Nonnull;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Function;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public final class ColorLineMarkerProvider implements LineMarkerProvider {
  private static class MyInfo extends MergeableLineMarkerInfo<PsiElement> {
    private final ColorValue myColor;

    @RequiredReadAction
    public MyInfo(@Nonnull final PsiElement element, ColorValue color, final ElementColorProvider colorProvider) {
      super(element, element.getTextRange(), ImageEffects.colorFilled(12, 12, color), Pass.UPDATE_ALL, FunctionUtil.nullConstant(), new GutterIconNavigationHandler<PsiElement>() {
        @Override
        @RequiredUIAccess
        public void navigate(MouseEvent e, PsiElement elt) {
          if (!elt.isWritable()) return;

          final Editor editor = PsiUtilBase.findEditor(element);
          assert editor != null;

          ColorChooser.chooseColor(editor.getComponent(), "Choose Color", TargetAWT.to(color), true, c -> {
            if (c != null) {
              WriteCommandAction.runWriteCommandAction(element.getProject(), () -> colorProvider.setColorTo(element, TargetAWT.from(c)));
            }
          });
        }
      }, GutterIconRenderer.Alignment.LEFT);
      myColor = color;
    }

    @Override
    public boolean canMergeWith(@Nonnull MergeableLineMarkerInfo<?> info) {
      return info instanceof MyInfo;
    }

    @Nonnull
    @Override
    public Image getCommonIcon(@Nonnull List<MergeableLineMarkerInfo> infos) {
      if (infos.size() == 2 && infos.get(0) instanceof MyInfo && infos.get(1) instanceof MyInfo) {
        return ImageEffects.twoColorFilled(12, 12, ((MyInfo)infos.get(1)).myColor, ((MyInfo)infos.get(0)).myColor);
      }
      return AllIcons.Gutter.Colors;
    }

    @Nonnull
    @Override
    public Function<? super PsiElement, String> getCommonTooltip(@Nonnull List<MergeableLineMarkerInfo> infos) {
      return FunctionUtil.nullConstant();
    }
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return Language.ANY;
  }

  @RequiredReadAction
  @Override
  public LineMarkerInfo getLineMarkerInfo(@Nonnull PsiElement element) {
    for (ElementColorProvider colorProvider : ElementColorProvider.EP.getExtensionList(element.getProject())) {
      final ColorValue color = colorProvider.getColorFrom(element);
      if (color != null) {
        MyInfo info = new MyInfo(element, color, colorProvider);
        NavigateAction.setNavigateAction(info, "Choose color", null);
        return info;
      }
    }
    return null;
  }
}

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
package com.intellij.ui;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.*;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ElementColorProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.util.Function;
import com.intellij.util.FunctionUtil;
import com.intellij.util.ui.ColorIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.TwoColorsIcon;
import consulo.annotations.RequiredDispatchThread;
import consulo.annotations.RequiredReadAction;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public final class ColorLineMarkerProvider implements LineMarkerProvider {
  private static class MyInfo extends MergeableLineMarkerInfo<PsiElement> {
    private final Color myColor;

    @RequiredReadAction
    public MyInfo(@Nonnull final PsiElement element, final Color color, final ElementColorProvider colorProvider) {
      super(element, element.getTextRange(), new ColorIcon(JBUI.scaleFontSize(12), color), Pass.UPDATE_ALL, FunctionUtil.nullConstant(), new GutterIconNavigationHandler<PsiElement>() {
        @Override
        @RequiredDispatchThread
        public void navigate(MouseEvent e, PsiElement elt) {
          if (!elt.isWritable()) return;

          final Editor editor = PsiUtilBase.findEditor(element);
          assert editor != null;
          final Color c = ColorChooser.chooseColor(editor.getComponent(), "Choose Color", color, true);
          if (c != null) {
            WriteAction.run(() -> colorProvider.setColorTo(element, c));
          }
        }
      }, GutterIconRenderer.Alignment.LEFT);
      myColor = color;
    }

    @Override
    public boolean canMergeWith(@Nonnull MergeableLineMarkerInfo<?> info) {
      return info instanceof MyInfo;
    }

    @Override
    public Icon getCommonIcon(@Nonnull List<MergeableLineMarkerInfo> infos) {
      if (infos.size() == 2 && infos.get(0) instanceof MyInfo && infos.get(1) instanceof MyInfo) {
        return new TwoColorsIcon(JBUI.scale(12), ((MyInfo)infos.get(1)).myColor, ((MyInfo)infos.get(0)).myColor);
      }
      return AllIcons.Gutter.Colors;
    }

    @Nonnull
    @Override
    public Function<? super PsiElement, String> getCommonTooltip(@Nonnull List<MergeableLineMarkerInfo> infos) {
      return FunctionUtil.nullConstant();
    }
  }

  private final ElementColorProvider[] myExtensions = ElementColorProvider.EP_NAME.getExtensions();

  @RequiredReadAction
  @Override
  public LineMarkerInfo getLineMarkerInfo(@Nonnull PsiElement element) {
    for (ElementColorProvider colorProvider : myExtensions) {
      final Color color = colorProvider.getColorFrom(element);
      if (color != null) {
        MyInfo info = new MyInfo(element, color, colorProvider);
        NavigateAction.setNavigateAction(info, "Choose color", null);
        return info;
      }
    }
    return null;
  }
}

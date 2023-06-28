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
package consulo.desktop.awt.codeInsight.lookup;

import consulo.application.util.matcher.MatcherTextRange;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.*;
import consulo.colorScheme.EditorFontType;
import consulo.colorScheme.TextAttributes;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.refactoring.rename.inplace.InplaceRefactoring;
import consulo.ui.ex.JBColor;
import consulo.util.collection.FList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author peter
 */
class LookupPreview {
  private final List<Inlay> myInlays = new ArrayList<>();
  private final LookupEx myLookup;

  LookupPreview(LookupEx lookup) {
    myLookup = lookup;
  }

  void updatePreview(@Nullable LookupElement item) {
    if (!Registry.is("ide.lookup.preview.insertion")) return;

    myInlays.forEach(Disposer::dispose);
    myInlays.clear();

    String suffix = getSuffixText(item);
    Editor editor = myLookup.getTopLevelEditor();
    if (!suffix.isEmpty() && editor instanceof RealEditor &&
        !editor.getSelectionModel().hasSelection() &&
        InplaceRefactoring.getActiveInplaceRenamer(editor) == null) {
      myLookup.performGuardedChange(() -> {
        for (Caret caret : editor.getCaretModel().getAllCarets()) {
          ensureCaretBeforeInlays(caret);
          addInlay(suffix, caret.getOffset());
        }
      });
    }
  }

  private static void ensureCaretBeforeInlays(Caret caret) {
    LogicalPosition position = caret.getLogicalPosition();
    if (position.leansForward) {
      caret.moveToLogicalPosition(position.leanForward(false));
    }
  }

  private String getSuffixText(@Nullable LookupElement item) {
    if (item != null) {
      String itemText = StringUtil.notNullize(LookupElementPresentation.renderElement(item).getItemText());
      String prefix = myLookup.itemPattern(item);
      if (prefix.isEmpty()) {
        return itemText;
      }

      FList<MatcherTextRange> fragments = LookupCellRenderer.getMatchingFragments(prefix, itemText);
      if (fragments != null && !fragments.isEmpty()) {
        List<MatcherTextRange> list = new ArrayList<>(fragments);
        return itemText.substring(list.get(list.size() - 1).getEndOffset(), itemText.length());
      }
    }
    return "";
  }

  private void addInlay(String suffix, int caretOffset) {
    Inlay inlay = myLookup.getTopLevelEditor().getInlayModel().addInlineElement(caretOffset, createGrayRenderer(suffix));
    if (inlay != null) {
      myInlays.add(inlay);
      Disposer.register(myLookup, inlay);
    }
  }

  @Nonnull
  private static EditorCustomElementRenderer createGrayRenderer(final String suffix) {
    return new EditorCustomElementRenderer() {
      @Override
      public int calcWidthInPixels(@Nonnull Inlay inlay) {
        Editor editor = inlay.getEditor();
        return editor.getContentComponent().getFontMetrics(getFont(editor)).stringWidth(suffix);
      }

      @Override
      public void paint(@Nonnull Inlay inlay, @Nonnull Graphics g, @Nonnull Rectangle r, @Nonnull TextAttributes textAttributes) {
        Editor editor = inlay.getEditor();
        g.setColor(JBColor.GRAY);
        g.setFont(getFont(editor));
        g.drawString(suffix, r.x, r.y + editor.getAscent());
      }

      private Font getFont(@Nonnull Editor editor) {
        return editor.getColorsScheme().getFont(EditorFontType.PLAIN);
      }
    };
  }
}

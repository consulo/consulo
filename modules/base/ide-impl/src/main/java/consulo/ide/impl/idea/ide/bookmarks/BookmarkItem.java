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
package consulo.ide.impl.idea.ide.bookmarks;

import consulo.bookmark.Bookmark;
import consulo.bookmark.BookmarkManager;
import consulo.ui.ex.ColoredTextContainer;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.language.editor.FileColorManager;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.codeEditor.util.popup.DetailView;
import consulo.codeEditor.util.popup.ItemWrapper;
import consulo.colorScheme.TextAttributes;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.colorScheme.EffectType;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.virtualFileSystem.VirtualFile;

import javax.swing.*;
import java.awt.*;

/**
 * @author zajac
 * @since 2012-05-06
 */
public class BookmarkItem extends ItemWrapper {
  private final Bookmark myBookmark;

  public BookmarkItem(Bookmark bookmark) {
    myBookmark = bookmark;
  }

  public Bookmark getBookmark() {
    return myBookmark;
  }

  @Override
  public void setupRenderer(ColoredTextContainer renderer, Project project, boolean selected) {
    setupRenderer(renderer, project, myBookmark, selected);
  }

  public static void setupRenderer(ColoredTextContainer renderer, Project project, Bookmark bookmark, boolean selected) {
    VirtualFile file = bookmark.getFile();
    if (!file.isValid()) {
      return;
    }

    PsiManager psiManager = PsiManager.getInstance(project);

    PsiElement fileOrDir = file.isDirectory() ? psiManager.findDirectory(file) : psiManager.findFile(file);
    if (fileOrDir != null) {
      renderer.setIcon(IconDescriptorUpdaters.getIcon(fileOrDir, 0));
    }

    String description = bookmark.getDescription();
    if (description != null) {
      renderer.append(description + " ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    }

    FileStatus fileStatus = FileStatusManager.getInstance(project).getStatus(file);
    TextAttributes attributes = new TextAttributes(fileStatus.getColor(), null, null, EffectType.LINE_UNDERSCORE, Font.PLAIN);
    renderer.append(file.getName(), TextAttributesUtil.fromTextAttributes(attributes));
    if (bookmark.getLine() >= 0) {
      renderer.append(":", SimpleTextAttributes.GRAYED_ATTRIBUTES);
      renderer.append(String.valueOf(bookmark.getLine() + 1), SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }

    if (!selected) {
      FileColorManager colorManager = FileColorManager.getInstance(project);
      if (fileOrDir instanceof PsiFile) {
        Color color = colorManager.getRendererBackground((PsiFile)fileOrDir);
        if (color != null) {
          renderer.setBackground(color);
        }
      }
    }
  }

  @Override
  public void updateAccessoryView(JComponent component) {
    JLabel label = (JLabel)component;
    final char mnemonic = myBookmark.getMnemonic();
    if (mnemonic != 0) {
      label.setText(Character.toString(mnemonic) + '.');
    }
    else {
      label.setText("");
    }
  }

  @Override
  public String speedSearchText() {
    return myBookmark.getFile().getName() + " " + myBookmark.getDescription();
  }

  @Override
  public String footerText() {
    return myBookmark.getFile().getPresentableUrl();
  }

  @Override
  protected void doUpdateDetailView(DetailView panel, boolean editorOnly) {
    panel.navigateInPreviewEditor(DetailView.PreviewEditorState.create(myBookmark.getFile(), myBookmark.getLine()));
  }

  @Override
  public boolean allowedToRemove() {
    return true;
  }

  @Override
  public void removed(Project project) {
    BookmarkManager.getInstance(project).removeBookmark(getBookmark());
  }
}

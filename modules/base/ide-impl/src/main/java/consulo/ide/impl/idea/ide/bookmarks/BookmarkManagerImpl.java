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

import consulo.annotation.component.ServiceImpl;
import consulo.bookmark.Bookmark;
import consulo.bookmark.BookmarkManager;
import consulo.bookmark.event.BookmarksListener;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.event.EditorEventMulticaster;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.component.messagebus.MessageBus;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.platform.Platform;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@State(name = "BookmarkManagerImpl", storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE))
@Singleton
@ServiceImpl
public class BookmarkManagerImpl implements BookmarkManager, PersistentStateComponent<Element> {

  private static final int MAX_AUTO_DESCRIPTION_SIZE = 50;

  private final List<BookmarkImpl> myBookmarks = new ArrayList<>();

  private final MessageBus myBus;

  private final Project myProject;

  private AtomicReference<List<BookmarkImpl>> myPendingBookmarks = new AtomicReference<>();

  @Inject
  public BookmarkManagerImpl(Project project, EditorFactory editorFactory) {
    myBus = project.getMessageBus();
    myProject = project;
    EditorEventMulticaster multicaster = editorFactory.getEventMulticaster();
    multicaster.addDocumentListener(new MyDocumentListener(), myProject);
    multicaster.addEditorMouseListener(new MyEditorMouseListener(), myProject);
  }

  static void documentCreated(@Nonnull final Document document, PsiFile ignored, Project project) {
    BookmarkManagerImpl bookmarkManager = (BookmarkManagerImpl)BookmarkManager.getInstance(project);

    final VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    if (file == null) return;
    for (final BookmarkImpl bookmark : bookmarkManager.myBookmarks) {
      if (Comparing.equal(bookmark.getFile(), file)) {
        UIUtil.invokeLaterIfNeeded(() -> {
          if (project.isDisposed()) return;
          bookmark.createHighlighter(DocumentMarkupModel.forDocument(document, project, true));
        });
      }
    }
  }

  @Override
  public void editDescription(@Nonnull Bookmark bookmark) {
    String description = Messages.showInputDialog(
      myProject,
      IdeLocalize.actionBookmarkEditDescriptionDialogMessage().get(),
      IdeLocalize.actionBookmarkEditDescriptionDialogTitle().get(),
      Messages.getQuestionIcon(),
      bookmark.getDescription(),
      null
    );
    if (description != null) {
      setDescription(bookmark, description);
    }
  }

  @Override
  public void addEditorBookmark(Editor editor, int lineIndex) {
    Document document = editor.getDocument();
    PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
    if (psiFile == null) return;

    final VirtualFile virtualFile = psiFile.getVirtualFile();
    if (virtualFile == null) return;

    addTextBookmark(virtualFile, lineIndex, getAutoDescription(editor, lineIndex));
  }

  @Override
  public BookmarkImpl addTextBookmark(VirtualFile file, int lineIndex, String description) {
    BookmarkImpl b = new BookmarkImpl(myProject, file, lineIndex, description, true);
    myBookmarks.add(0, b);
    myBus.syncPublisher(BookmarksListener.class).bookmarkAdded(b);
    return b;
  }

  public static String getAutoDescription(final Editor editor, final int lineIndex) {
    String autoDescription = editor.getSelectionModel().getSelectedText();
    if (autoDescription == null) {
      Document document = editor.getDocument();
      autoDescription = document.getCharsSequence().subSequence(document.getLineStartOffset(lineIndex), document.getLineEndOffset(lineIndex)).toString().trim();
    }
    if (autoDescription.length() > MAX_AUTO_DESCRIPTION_SIZE) {
      return autoDescription.substring(0, MAX_AUTO_DESCRIPTION_SIZE) + "...";
    }
    return autoDescription;
  }

  @Override
  @Nullable
  public BookmarkImpl addFileBookmark(VirtualFile file, String description) {
    if (file == null) return null;
    if (findFileBookmark(file) != null) return null;

    BookmarkImpl b = new BookmarkImpl(myProject, file, -1, description, true);
    myBookmarks.add(0, b);
    myBus.syncPublisher(BookmarksListener.class).bookmarkAdded(b);
    return b;
  }

  @Override
  @Nonnull
  public List<Bookmark> getValidBookmarks() {
    List<Bookmark> answer = new ArrayList<>();
    for (BookmarkImpl bookmark : myBookmarks) {
      if (bookmark.isValid()) answer.add(bookmark);
    }
    return answer;
  }


  @Override
  @Nullable
  public BookmarkImpl findEditorBookmark(@Nonnull Document document, int line) {
    for (BookmarkImpl bookmark : myBookmarks) {
      if (bookmark.getDocument() == document && bookmark.getLine() == line) {
        return bookmark;
      }
    }

    return null;
  }

  @Override
  @Nullable
  public BookmarkImpl findFileBookmark(@Nonnull VirtualFile file) {
    for (BookmarkImpl bookmark : myBookmarks) {
      if (Comparing.equal(bookmark.getFile(), file) && bookmark.getLine() == -1) return bookmark;
    }

    return null;
  }

  @Override
  @Nullable
  public BookmarkImpl findBookmarkForMnemonic(char m) {
    final char mm = Character.toUpperCase(m);
    for (BookmarkImpl bookmark : myBookmarks) {
      if (mm == bookmark.getMnemonic()) return bookmark;
    }
    return null;
  }

  @Override
  public boolean hasBookmarksWithMnemonics() {
    for (BookmarkImpl bookmark : myBookmarks) {
      if (bookmark.getMnemonic() != 0) return true;
    }

    return false;
  }

  @Override
  public void removeBookmark(@Nonnull Bookmark bookmark) {
    myBookmarks.remove(bookmark);
    ((BookmarkImpl)bookmark).release();
    myBus.syncPublisher(BookmarksListener.class).bookmarkRemoved(bookmark);
  }

  @Override
  public Element getState() {
    Element container = new Element("BookmarkManagerImpl");
    writeExternal(container);
    return container;
  }

  @Override
  public void loadState(final Element state) {
    myPendingBookmarks.set(readExternal(state));
  }

  public void registerAllFromState() {
    registerAll(myPendingBookmarks.getAndSet(null));
  }

  private void registerAll(@Nullable List<BookmarkImpl> bookmarksForUpdate) {
    BookmarksListener publisher = myBus.syncPublisher(BookmarksListener.class);
    for (BookmarkImpl bookmark : myBookmarks) {
      bookmark.release();
      publisher.bookmarkRemoved(bookmark);
    }
    myBookmarks.clear();

    if (bookmarksForUpdate != null) {
      for (BookmarkImpl bookmark : bookmarksForUpdate) {
        myBookmarks.add(bookmark);

        publisher.bookmarkAdded(bookmark);

        bookmark.addHighlighter();
      }
    }
  }

  @Nonnull
  private List<BookmarkImpl> readExternal(Element element) {
    List<BookmarkImpl> bookmarks = new ArrayList<>();

    for (final Element bookmarkElement : element.getChildren()) {
      if ("bookmark".equals(bookmarkElement.getName())) {
        String url = bookmarkElement.getAttributeValue("url");
        String line = bookmarkElement.getAttributeValue("line");
        String description = StringUtil.notNullize(bookmarkElement.getAttributeValue("description"));
        String mnemonic = bookmarkElement.getAttributeValue("mnemonic");

        BookmarkImpl b = null;
        VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
        if (file != null) {
          if (line != null) {
            try {
              int lineIndex = Integer.parseInt(line);
              b = new BookmarkImpl(myProject, file, lineIndex, description, false);
            }
            catch (NumberFormatException e) {
              // Ignore. Will miss bookmark if line number cannot be parsed
            }
          }
          else {
            b = new BookmarkImpl(myProject, file, -1, description, false);
          }
        }

        if (b != null && mnemonic != null && mnemonic.length() == 1) {
          b.setMnemonic(mnemonic.charAt(0));
        }

        if (b != null) {
          bookmarks.add(b);
        }
      }
    }

    return bookmarks;
  }

  private void writeExternal(Element element) {
    List<BookmarkImpl> reversed = new ArrayList<>(myBookmarks);
    Collections.reverse(reversed);

    for (BookmarkImpl bookmark : reversed) {
      if (!bookmark.isValid()) continue;
      Element bookmarkElement = new Element("bookmark");

      bookmarkElement.setAttribute("url", bookmark.getFile().getUrl());

      String description = bookmark.getNotEmptyDescription();
      if (description != null) {
        bookmarkElement.setAttribute("description", description);
      }

      int line = bookmark.getLine();
      if (line >= 0) {
        bookmarkElement.setAttribute("line", String.valueOf(line));
      }

      char mnemonic = bookmark.getMnemonic();
      if (mnemonic != 0) {
        bookmarkElement.setAttribute("mnemonic", String.valueOf(mnemonic));
      }

      element.addContent(bookmarkElement);
    }
  }

  /**
   * Try to move bookmark one position up in the list
   *
   * @return bookmark list after moving
   */
  @Override
  @Nonnull
  public List<? extends Bookmark> moveBookmarkUp(@Nonnull Bookmark bookmark) {
    final int index = myBookmarks.indexOf(bookmark);
    if (index > 0) {
      Collections.swap(myBookmarks, index, index - 1);
      SwingUtilities.invokeLater(() -> {
        myBus.syncPublisher(BookmarksListener.class).bookmarkChanged(myBookmarks.get(index));
        myBus.syncPublisher(BookmarksListener.class).bookmarkChanged(myBookmarks.get(index - 1));
      });
    }
    return myBookmarks;
  }


  /**
   * Try to move bookmark one position down in the list
   *
   * @return bookmark list after moving
   */
  @Override
  @Nonnull
  public List<? extends Bookmark> moveBookmarkDown(@Nonnull Bookmark bookmark) {
    final int index = myBookmarks.indexOf(bookmark);
    if (index < myBookmarks.size() - 1) {
      Collections.swap(myBookmarks, index, index + 1);
      SwingUtilities.invokeLater(() -> {
        myBus.syncPublisher(BookmarksListener.class).bookmarkChanged(myBookmarks.get(index));
        myBus.syncPublisher(BookmarksListener.class).bookmarkChanged(myBookmarks.get(index + 1));
      });
    }

    return myBookmarks;
  }

  @Override
  @Nullable
  public Bookmark getNextBookmark(@Nonnull Editor editor, boolean isWrapped) {
    Bookmark[] bookmarksForDocument = getBookmarksForDocument(editor.getDocument());
    int lineNumber = editor.getCaretModel().getLogicalPosition().line;
    for (Bookmark bookmark : bookmarksForDocument) {
      if (bookmark.getLine() > lineNumber) return bookmark;
    }
    if (isWrapped && bookmarksForDocument.length > 0) {
      return bookmarksForDocument[0];
    }
    return null;
  }

  @Override
  @Nullable
  public Bookmark getPreviousBookmark(@Nonnull Editor editor, boolean isWrapped) {
    Bookmark[] bookmarksForDocument = getBookmarksForDocument(editor.getDocument());
    int lineNumber = editor.getCaretModel().getLogicalPosition().line;
    for (int i = bookmarksForDocument.length - 1; i >= 0; i--) {
      Bookmark bookmark = bookmarksForDocument[i];
      if (bookmark.getLine() < lineNumber) return bookmark;
    }
    if (isWrapped && bookmarksForDocument.length > 0) {
      return bookmarksForDocument[bookmarksForDocument.length - 1];
    }
    return null;
  }

  @Nonnull
  private Bookmark[] getBookmarksForDocument(@Nonnull Document document) {
    ArrayList<Bookmark> answer = new ArrayList<>();
    for (Bookmark bookmark : getValidBookmarks()) {
      if (document.equals(bookmark.getDocument())) {
        answer.add(bookmark);
      }
    }

    Bookmark[] bookmarks = answer.toArray(new Bookmark[answer.size()]);
    Arrays.sort(bookmarks, (o1, o2) -> o1.getLine() - o2.getLine());
    return bookmarks;
  }

  @Override
  public void setMnemonic(@Nonnull Bookmark bookmark, char c) {
    final BookmarkImpl old = findBookmarkForMnemonic(c);
    if (old != null) removeBookmark(old);

    ((BookmarkImpl)bookmark).setMnemonic(c);
    myBus.syncPublisher(BookmarksListener.class).bookmarkChanged(bookmark);
  }

  @Override
  public void setDescription(@Nonnull Bookmark bookmark, String description) {
    ((BookmarkImpl)bookmark).setDescription(description);
    myBus.syncPublisher(BookmarksListener.class).bookmarkChanged(bookmark);
  }

  public void colorsChanged() {
    for (BookmarkImpl bookmark : myBookmarks) {
      bookmark.updateHighlighter();
    }
  }


  private class MyEditorMouseListener implements EditorMouseListener {
    @Override
    public void mouseClicked(final EditorMouseEvent e) {
      if (e.getArea() != EditorMouseEventArea.LINE_MARKERS_AREA) return;
      if (e.getMouseEvent().isPopupTrigger()) return;
      if ((e.getMouseEvent().getModifiers() & (Platform.current().os().isMac() ? InputEvent.META_MASK : InputEvent.CTRL_MASK)) == 0) return;

      Editor editor = e.getEditor();
      int line = editor.xyToLogicalPosition(new Point(e.getMouseEvent().getX(), e.getMouseEvent().getY())).line;
      if (line < 0) return;

      Document document = editor.getDocument();

      BookmarkImpl bookmark = findEditorBookmark(document, line);
      if (bookmark == null) {
        addEditorBookmark(editor, line);
      }
      else {
        removeBookmark(bookmark);
      }
      e.consume();
    }
  }

  private class MyDocumentListener extends DocumentAdapter {
    @Override
    public void documentChanged(DocumentEvent e) {
      List<BookmarkImpl> bookmarksToRemove = null;
      for (BookmarkImpl bookmark : myBookmarks) {
        if (!bookmark.isValid()) {
          if (bookmarksToRemove == null) {
            bookmarksToRemove = new ArrayList<>();
          }
          bookmarksToRemove.add(bookmark);
        }
      }

      if (bookmarksToRemove != null) {
        for (BookmarkImpl bookmark : bookmarksToRemove) {
          removeBookmark(bookmark);
        }
      }
    }
  }
}


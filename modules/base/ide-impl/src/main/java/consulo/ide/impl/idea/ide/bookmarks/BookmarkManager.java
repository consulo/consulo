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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.util.SystemInfo;
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
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.project.startup.StartupActivity;
import consulo.project.startup.StartupManager;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@State(name = "BookmarkManager", storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE))
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class BookmarkManager implements PersistentStateComponent<Element> {

  private static final int MAX_AUTO_DESCRIPTION_SIZE = 50;

  @Nonnull
  public static BookmarkManager getInstance(Project project) {
    return project.getInstance(BookmarkManager.class);
  }

  private final List<Bookmark> myBookmarks = new ArrayList<>();

  private final MessageBus myBus;

  private final Project myProject;

  private AtomicReference<List<Bookmark>> myPendingBookmarks = new AtomicReference<>();

  @Inject
  public BookmarkManager(Project project, EditorFactory editorFactory) {
    myBus = project.getMessageBus();
    myProject = project;
    EditorEventMulticaster multicaster = editorFactory.getEventMulticaster();
    multicaster.addDocumentListener(new MyDocumentListener(), myProject);
    multicaster.addEditorMouseListener(new MyEditorMouseListener(), myProject);
  }

  static void documentCreated(@Nonnull final Document document, PsiFile ignored, Project project) {
    BookmarkManager bookmarkManager = BookmarkManager.getInstance(project);

    final VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    if (file == null) return;
    for (final Bookmark bookmark : bookmarkManager.myBookmarks) {
      if (Comparing.equal(bookmark.getFile(), file)) {
        UIUtil.invokeLaterIfNeeded(() -> {
          if (project.isDisposed()) return;
          bookmark.createHighlighter(DocumentMarkupModel.forDocument(document, project, true));
        });
      }
    }
  }

  public void editDescription(@Nonnull Bookmark bookmark) {
    String description = Messages.showInputDialog(myProject, IdeBundle.message("action.bookmark.edit.description.dialog.message"), IdeBundle.message("action.bookmark.edit.description.dialog.title"),
                                                  Messages.getQuestionIcon(), bookmark.getDescription(), null);
    if (description != null) {
      setDescription(bookmark, description);
    }
  }

  public void addEditorBookmark(Editor editor, int lineIndex) {
    Document document = editor.getDocument();
    PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
    if (psiFile == null) return;

    final VirtualFile virtualFile = psiFile.getVirtualFile();
    if (virtualFile == null) return;

    addTextBookmark(virtualFile, lineIndex, getAutoDescription(editor, lineIndex));
  }

  public Bookmark addTextBookmark(VirtualFile file, int lineIndex, String description) {
    Bookmark b = new Bookmark(myProject, file, lineIndex, description, true);
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

  @Nullable
  public Bookmark addFileBookmark(VirtualFile file, String description) {
    if (file == null) return null;
    if (findFileBookmark(file) != null) return null;

    Bookmark b = new Bookmark(myProject, file, -1, description, true);
    myBookmarks.add(0, b);
    myBus.syncPublisher(BookmarksListener.class).bookmarkAdded(b);
    return b;
  }


  @Nonnull
  public List<Bookmark> getValidBookmarks() {
    List<Bookmark> answer = new ArrayList<>();
    for (Bookmark bookmark : myBookmarks) {
      if (bookmark.isValid()) answer.add(bookmark);
    }
    return answer;
  }


  @Nullable
  public Bookmark findEditorBookmark(@Nonnull Document document, int line) {
    for (Bookmark bookmark : myBookmarks) {
      if (bookmark.getDocument() == document && bookmark.getLine() == line) {
        return bookmark;
      }
    }

    return null;
  }

  @Nullable
  public Bookmark findFileBookmark(@Nonnull VirtualFile file) {
    for (Bookmark bookmark : myBookmarks) {
      if (Comparing.equal(bookmark.getFile(), file) && bookmark.getLine() == -1) return bookmark;
    }

    return null;
  }

  @Nullable
  public Bookmark findBookmarkForMnemonic(char m) {
    final char mm = Character.toUpperCase(m);
    for (Bookmark bookmark : myBookmarks) {
      if (mm == bookmark.getMnemonic()) return bookmark;
    }
    return null;
  }

  public boolean hasBookmarksWithMnemonics() {
    for (Bookmark bookmark : myBookmarks) {
      if (bookmark.getMnemonic() != 0) return true;
    }

    return false;
  }

  public void removeBookmark(@Nonnull Bookmark bookmark) {
    myBookmarks.remove(bookmark);
    bookmark.release();
    myBus.syncPublisher(BookmarksListener.class).bookmarkRemoved(bookmark);
  }

  @Override
  public Element getState() {
    Element container = new Element("BookmarkManager");
    writeExternal(container);
    return container;
  }

  @Override
  public void loadState(final Element state) {
    List<Bookmark> bookmarks = readExternal(state);

    myPendingBookmarks.set(bookmarks);

    StartupManager.getInstance(myProject).runAfterOpened((StartupActivity.DumbAware)(project, uiAccess) -> {
      uiAccess.give(() -> registerAll(myPendingBookmarks.getAndSet(null)));
    });
  }

  private void registerAll(@Nullable List<Bookmark> bookmarksForUpdate) {
    BookmarksListener publisher = myBus.syncPublisher(BookmarksListener.class);
    for (Bookmark bookmark : myBookmarks) {
      bookmark.release();
      publisher.bookmarkRemoved(bookmark);
    }
    myBookmarks.clear();

    if (bookmarksForUpdate != null) {
      for (Bookmark bookmark : bookmarksForUpdate) {
        myBookmarks.add(bookmark);

        publisher.bookmarkAdded(bookmark);

        bookmark.addHighlighter();
      }
    }
  }

  @Nonnull
  private List<Bookmark> readExternal(Element element) {
    List<Bookmark> bookmarks = new ArrayList<>();

    for (final Element bookmarkElement : element.getChildren()) {
      if ("bookmark".equals(bookmarkElement.getName())) {
        String url = bookmarkElement.getAttributeValue("url");
        String line = bookmarkElement.getAttributeValue("line");
        String description = StringUtil.notNullize(bookmarkElement.getAttributeValue("description"));
        String mnemonic = bookmarkElement.getAttributeValue("mnemonic");

        Bookmark b = null;
        VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
        if (file != null) {
          if (line != null) {
            try {
              int lineIndex = Integer.parseInt(line);
              b = new Bookmark(myProject, file, lineIndex, description, false);
            }
            catch (NumberFormatException e) {
              // Ignore. Will miss bookmark if line number cannot be parsed
            }
          }
          else {
            b = new Bookmark(myProject, file, -1, description, false);
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
    List<Bookmark> reversed = new ArrayList<>(myBookmarks);
    Collections.reverse(reversed);

    for (Bookmark bookmark : reversed) {
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
  @Nonnull
  public List<Bookmark> moveBookmarkUp(@Nonnull Bookmark bookmark) {
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
  @Nonnull
  public List<Bookmark> moveBookmarkDown(@Nonnull Bookmark bookmark) {
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

  public void setMnemonic(@Nonnull Bookmark bookmark, char c) {
    final Bookmark old = findBookmarkForMnemonic(c);
    if (old != null) removeBookmark(old);

    bookmark.setMnemonic(c);
    myBus.syncPublisher(BookmarksListener.class).bookmarkChanged(bookmark);
  }

  public void setDescription(@Nonnull Bookmark bookmark, String description) {
    bookmark.setDescription(description);
    myBus.syncPublisher(BookmarksListener.class).bookmarkChanged(bookmark);
  }

  public void colorsChanged() {
    for (Bookmark bookmark : myBookmarks) {
      bookmark.updateHighlighter();
    }
  }


  private class MyEditorMouseListener implements EditorMouseListener {
    @Override
    public void mouseClicked(final EditorMouseEvent e) {
      if (e.getArea() != EditorMouseEventArea.LINE_MARKERS_AREA) return;
      if (e.getMouseEvent().isPopupTrigger()) return;
      if ((e.getMouseEvent().getModifiers() & (SystemInfo.isMac ? InputEvent.META_MASK : InputEvent.CTRL_MASK)) == 0) return;

      Editor editor = e.getEditor();
      int line = editor.xyToLogicalPosition(new Point(e.getMouseEvent().getX(), e.getMouseEvent().getY())).line;
      if (line < 0) return;

      Document document = editor.getDocument();

      Bookmark bookmark = findEditorBookmark(document, line);
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
      List<Bookmark> bookmarksToRemove = null;
      for (Bookmark bookmark : myBookmarks) {
        if (!bookmark.isValid()) {
          if (bookmarksToRemove == null) {
            bookmarksToRemove = new ArrayList<>();
          }
          bookmarksToRemove.add(bookmark);
        }
      }

      if (bookmarksToRemove != null) {
        for (Bookmark bookmark : bookmarksToRemove) {
          removeBookmark(bookmark);
        }
      }
    }
  }
}


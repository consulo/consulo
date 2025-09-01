/*
 * Copyright 2013-2025 consulo.io
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
package consulo.bookmark.ui.view.impl.internal.action;

import consulo.bookmark.Bookmark;
import consulo.bookmark.BookmarkManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

public class BookmarkInContextInfo {
    private final DataContext myDataContext;
    private final Project myProject;
    private Bookmark myBookmarkAtPlace;
    private VirtualFile myFile;
    private int myLine;

    public BookmarkInContextInfo(DataContext dataContext, Project project) {
        myDataContext = dataContext;
        myProject = project;
    }

    public Bookmark getBookmarkAtPlace() {
        return myBookmarkAtPlace;
    }

    public VirtualFile getFile() {
        return myFile;
    }

    public int getLine() {
        return myLine;
    }

    public BookmarkInContextInfo invoke() {
        myBookmarkAtPlace = null;
        myFile = null;
        myLine = -1;

        BookmarkManager bookmarkManager = BookmarkManager.getInstance(myProject);

        Editor editor = myDataContext.getData(Editor.KEY);
        if (editor != null) {
            Document document = editor.getDocument();

            Integer line = myDataContext.getData(EditorGutterComponentEx.LOGICAL_LINE_AT_CURSOR);
            myLine = line != null ? line : editor.getCaretModel().getLogicalPosition().line;

            myFile = FileDocumentManager.getInstance().getFile(document);
            myBookmarkAtPlace = bookmarkManager.findEditorBookmark(document, myLine);
        }

        if (myFile == null) {
            myFile = myDataContext.getData(VirtualFile.KEY);
            myLine = -1;

            if (myBookmarkAtPlace == null && myFile != null) {
                myBookmarkAtPlace = bookmarkManager.findFileBookmark(myFile);
            }
        }
        return this;
    }
}

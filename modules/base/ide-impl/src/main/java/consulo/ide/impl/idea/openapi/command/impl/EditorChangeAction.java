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
package consulo.ide.impl.idea.openapi.command.impl;

import consulo.undoRedo.BasicUndoableAction;
import consulo.undoRedo.UnexpectedUndoException;
import consulo.document.event.DocumentEvent;
import consulo.document.internal.DocumentEx;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.status.FileStatusManager;
import consulo.virtualFileSystem.status.impl.internal.FileStatusManagerImpl;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.file.light.LightVirtualFile;
import consulo.index.io.CompressionUtil;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nonnull;

public class EditorChangeAction extends BasicUndoableAction {
  private final int myOffset;
  private final Object myOldString;
  private final Object myNewString;
  private final long myOldTimeStamp;
  private final long myNewTimeStamp;
  private final int myOldLength;
  private final int myNewLength;

  public EditorChangeAction(DocumentEvent e) {
    this((DocumentEx)e.getDocument(), e.getOffset(), e.getOldFragment(), e.getNewFragment(), e.getOldTimeStamp());
  }

  public EditorChangeAction(@Nonnull DocumentEx document,
                            int offset,
                            @Nonnull CharSequence oldString,
                            @Nonnull CharSequence newString,
                            long oldTimeStamp) {
    super(document);
    myOffset = offset;
    myOldString = CompressionUtil.compressStringRawBytes(oldString);
    myNewString = CompressionUtil.compressStringRawBytes(newString);
    myOldTimeStamp = oldTimeStamp;
    myNewTimeStamp = document.getModificationStamp();
    myNewLength = document.getTextLength();
    myOldLength = myNewLength - newString.length() + oldString.length();
  }

  @Override
  public void undo() throws UnexpectedUndoException {
    validateDocumentLength(myNewLength);
    DocumentUndoProvider.startDocumentUndo(getDocument());
    try {
      performUndo();
    }
    finally {
      DocumentUndoProvider.finishDocumentUndo(getDocument());
    }

    getDocument().setModificationStamp(myOldTimeStamp);
    refreshFileStatus();
  }

  public void performUndo() {
    CharSequence oldString = CompressionUtil.uncompressStringRawBytes(myOldString);
    CharSequence newString = CompressionUtil.uncompressStringRawBytes(myNewString);
    exchangeStrings(newString, oldString);
  }

  @Override
  public void redo() throws UnexpectedUndoException {
    validateDocumentLength(myOldLength);
    DocumentUndoProvider.startDocumentUndo(getDocument());
    try {
      CharSequence oldString = CompressionUtil.uncompressStringRawBytes(myOldString);
      CharSequence newString = CompressionUtil.uncompressStringRawBytes(myNewString);
      exchangeStrings(oldString, newString);
    }
    finally {
      DocumentUndoProvider.finishDocumentUndo(getDocument());
    }
    getDocument().setModificationStamp(myNewTimeStamp);
    refreshFileStatus();
  }

  private void exchangeStrings(@Nonnull CharSequence newString, @Nonnull CharSequence oldString) {
    DocumentEx d = getDocument();

    if (newString.length() > 0 && oldString.length() == 0) {
      d.deleteString(myOffset, myOffset + newString.length());
    }
    else if (oldString.length() > 0 && newString.length() == 0) {
      d.insertString(myOffset, oldString);
    }
    else if (oldString.length() > 0 && newString.length() > 0) {
      d.replaceString(myOffset, myOffset + newString.length(), oldString);
    }
  }

  private void validateDocumentLength(int expectedLength) throws UnexpectedUndoException {
    if (getDocument().getTextLength() != expectedLength) throw new UnexpectedUndoException("Unexpected document state");
  }

  private void refreshFileStatus() {
    VirtualFile f = getAffectedDocuments()[0].getFile();
    if (f == null || f instanceof LightVirtualFile) return;

    for (Project each : ProjectManager.getInstance().getOpenProjects()) {
      FileStatusManagerImpl statusManager = (FileStatusManagerImpl)FileStatusManager.getInstance(each);
      statusManager.refreshFileStatusFromDocument(f, getDocument());
    }
  }


  private DocumentEx getDocument() {
    return (DocumentEx)getAffectedDocuments()[0].getDocument();
  }

  @Override
  @NonNls
  public String toString() {
    return "editor change: '" + myOldString + "' to '" + myNewString + "'" + " at: " + myOffset;
  }
}


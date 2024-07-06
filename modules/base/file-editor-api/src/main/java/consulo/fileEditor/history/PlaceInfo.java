// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor.history;

import consulo.document.RangeMarker;
import consulo.fileEditor.FileEditorState;
import consulo.fileEditor.FileEditorWindow;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

public final class PlaceInfo {
  private final VirtualFile myFile;
  private final FileEditorState myNavigationState;
  private final String myEditorTypeId;
  private final Reference<FileEditorWindow> myWindow;
  @Nullable
  private final RangeMarker myCaretPosition;
  private final long myTimeStamp;

  public PlaceInfo(@Nonnull VirtualFile file,
                   @Nonnull FileEditorState navigationState,
                   @Nonnull String editorTypeId,
                   @Nullable FileEditorWindow window,
                   @Nullable RangeMarker caretPosition,
                   long stamp) {
    myNavigationState = navigationState;
    myFile = file;
    myEditorTypeId = editorTypeId;
    myWindow = new WeakReference<>(window);
    myCaretPosition = caretPosition;
    myTimeStamp = stamp;
  }

  public FileEditorWindow getWindow() {
    return myWindow.get();
  }

  @Nonnull
  public FileEditorState getNavigationState() {
    return myNavigationState;
  }

  @Nonnull
  public VirtualFile getFile() {
    return myFile;
  }

  @Nonnull
  public String getEditorTypeId() {
    return myEditorTypeId;
  }

  @Override
  public String toString() {
    return getFile().getName() + " " + getNavigationState();
  }

  @Nullable
  public RangeMarker getCaretPosition() {
    return myCaretPosition;
  }

  public long getTimeStamp() {
    return myTimeStamp;
  }
}

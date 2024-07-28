// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.inject.impl.internal;

import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.FoldingGroup;
import consulo.document.internal.RangeMarkerEx;
import consulo.language.editor.inject.EditorWindow;
import consulo.document.DocumentWindow;
import jakarta.annotation.Nonnull;

public class FoldingRegionWindow extends RangeMarkerWindow implements FoldRegion {
  private final EditorWindow myEditorWindow;

  private final FoldRegion myHostRegion;

  FoldingRegionWindow(@Nonnull DocumentWindow documentWindow, @Nonnull EditorWindow editorWindow, @Nonnull FoldRegion hostRegion, int startShift, int endShift) {
    super(documentWindow, (RangeMarkerEx)hostRegion, startShift, endShift);
    myEditorWindow = editorWindow;
    myHostRegion = hostRegion;
  }

  @Override
  public boolean isExpanded() {
    return myHostRegion.isExpanded();
  }

  @Override
  public void setExpanded(boolean expanded) {
    myHostRegion.setExpanded(expanded);
  }

  @Override
  @Nonnull
  public String getPlaceholderText() {
    return myHostRegion.getPlaceholderText();
  }

  @Override
  public Editor getEditor() {
    return myEditorWindow;
  }

  @Override
  public FoldingGroup getGroup() {
    return myHostRegion.getGroup();
  }

  @Override
  public boolean shouldNeverExpand() {
    return false;
  }

  @Override
  public RangeMarkerEx getDelegate() {
    return (RangeMarkerEx)myHostRegion;
  }

  @Override
  public void setGutterMarkEnabledForSingleLine(boolean value) {
    myHostRegion.setGutterMarkEnabledForSingleLine(value);
  }

  @Override
  public boolean isGutterMarkEnabledForSingleLine() {
    return myHostRegion.isGutterMarkEnabledForSingleLine();
  }

  @Override
  public void setPlaceholderText(@Nonnull String text) {
    myHostRegion.setPlaceholderText(text);
  }
}

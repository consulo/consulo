package consulo.ide.impl.codeInsight.codeVision.ui.renderers;

import consulo.codeEditor.EditorCustomElementRenderer;
import consulo.language.editor.codeVision.CodeVisionEntry;
import consulo.language.editor.inlay.InputHandler;

import java.awt.Rectangle;

public interface CodeVisionInlayRenderer extends EditorCustomElementRenderer, InputHandler {
    Rectangle calculateCodeVisionEntryBounds(CodeVisionEntry element);
}

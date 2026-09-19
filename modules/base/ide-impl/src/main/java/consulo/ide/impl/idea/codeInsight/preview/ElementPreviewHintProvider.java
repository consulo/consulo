package consulo.ide.impl.idea.codeInsight.preview;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.language.editor.hint.HintManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.hint.LightweightHint;
import consulo.ui.ex.awt.hint.LightweightHintFactory;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

@ExtensionImpl
public class ElementPreviewHintProvider implements ElementPreviewProvider {
    private static final Logger LOG = Logger.getInstance(ElementPreviewHintProvider.class);

    private static final int HINT_HIDE_FLAGS = HintManager.HIDE_BY_ANY_KEY |
        HintManager.HIDE_BY_OTHER_HINT |
        HintManager.HIDE_BY_SCROLLING |
        HintManager.HIDE_BY_TEXT_CHANGE |
        HintManager.HIDE_IF_OUT_OF_EDITOR;
    private @Nullable LightweightHint hint;

    @Override
    public boolean isSupportedFile(PsiFile psiFile) {
        for (PreviewHintProvider hintProvider : PreviewHintProvider.EP_NAME.getExtensionList()) {
            if (hintProvider.isSupportedFile(psiFile)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @RequiredUIAccess
    public void show(PsiElement element, Editor editor, Point point, boolean keyTriggered) {
        LightweightHint newHint = getHint(element);
        hideCurrentHintIfAny();
        if (newHint == null) {
            return;
        }

        hint = newHint;
        HintManagerImpl manager = HintManagerImpl.getInstanceImpl();
        manager.showEditorHint(
            newHint,
            editor,
            manager.getHintPosition(newHint, editor, editor.xyToLogicalPosition(point), HintManager.RIGHT_UNDER),
            HINT_HIDE_FLAGS,
            0,
            false
        );
    }

    private void hideCurrentHintIfAny() {
        if (hint != null) {
            hint.hide();
            hint = null;
        }
    }

    @Override
    public void hide(@Nullable PsiElement element, Editor editor) {
        hideCurrentHintIfAny();
    }

    private static @Nullable LightweightHint getHint(PsiElement element) {
        for (PreviewHintProvider hintProvider : PreviewHintProvider.EP_NAME.getExtensionList()) {
            JComponent preview;
            try {
                preview = hintProvider.getPreviewComponent(element);
            }
            catch (Exception e) {
                LOG.error(e);
                continue;
            }
            if (preview != null) {
                return Application.get().getInstance(LightweightHintFactory.class).create(preview);
            }
        }
        return null;
    }
}
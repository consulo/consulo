package consulo.ide.impl.idea.codeInsight.preview;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.language.editor.hint.HintManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
    @Nullable
    private LightweightHintImpl hint;

    @Override
    public boolean isSupportedFile(@Nonnull PsiFile psiFile) {
        for (PreviewHintProvider hintProvider : PreviewHintProvider.EP_NAME.getExtensionList()) {
            if (hintProvider.isSupportedFile(psiFile)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void show(@Nonnull PsiElement element, @Nonnull Editor editor, @Nonnull Point point, boolean keyTriggered) {
        LightweightHintImpl newHint = getHint(element);
        hideCurrentHintIfAny();
        if (newHint == null) {
            return;
        }

        hint = newHint;
        HintManagerImpl manager = HintManagerImpl.getInstanceImpl();
        manager.showEditorHint(newHint, editor,
            manager.getHintPosition(newHint, editor, editor.xyToLogicalPosition(point), HintManager.RIGHT_UNDER),
            HINT_HIDE_FLAGS, 0, false);
    }

    private void hideCurrentHintIfAny() {
        if (hint != null) {
            hint.hide();
            hint = null;
        }
    }

    @Override
    public void hide(@Nullable PsiElement element, @Nonnull Editor editor) {
        hideCurrentHintIfAny();
    }

    @Nullable
    private static LightweightHintImpl getHint(@Nonnull PsiElement element) {
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
                return new LightweightHintImpl(preview);
            }
        }
        return null;
    }
}
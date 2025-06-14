package consulo.language.editor.inlay;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPoint;
import consulo.language.psi.PsiFile;

/**
 * Allows programmatic suppression of parameter hints in specific places.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ParameterNameHintsSuppressor {
    public static boolean isSuppressedForImpl(PsiFile file, InlayInfo inlayInfo) {
        ExtensionPoint<ParameterNameHintsSuppressor> point = file.getApplication().getExtensionPoint(ParameterNameHintsSuppressor.class);

        return point.anyMatchSafe(s -> s.isSuppressedFor(file, inlayInfo));
    }

    /**
     * Returns true if hints for the given inlayInfo in this file should be suppressed.
     */
    boolean isSuppressedFor(PsiFile file, InlayInfo inlayInfo);
}

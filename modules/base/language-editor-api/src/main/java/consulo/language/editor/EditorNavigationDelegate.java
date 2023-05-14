package consulo.language.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;

import jakarta.annotation.Nonnull;

/**
 * Defines contract for extending editor navigation functionality. 
 * 
 * @author Denis Zhdanov
 * @since 5/26/11 3:31 PM
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface EditorNavigationDelegate {
  enum Result {
    /**
     * Navigation request is completely handled by the current delegate and no further processing is required.
     */
    STOP,

    /**
     * Continue navigation request processing.
     */
    CONTINUE
  }

  @Nonnull
  Result navigateToLineEnd(@Nonnull Editor editor, @Nonnull DataContext dataContext);
}

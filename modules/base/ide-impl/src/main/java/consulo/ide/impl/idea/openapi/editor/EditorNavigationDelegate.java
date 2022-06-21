package consulo.ide.impl.idea.openapi.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Extension;
import consulo.dataContext.DataContext;
import consulo.component.extension.ExtensionPointName;
import consulo.codeEditor.Editor;

import javax.annotation.Nonnull;

/**
 * Defines contract for extending editor navigation functionality. 
 * 
 * @author Denis Zhdanov
 * @since 5/26/11 3:31 PM
 */
@Extension(ComponentScope.APPLICATION)
public interface EditorNavigationDelegate {

  ExtensionPointName<EditorNavigationDelegate> EP_NAME = ExtensionPointName.create(EditorNavigationDelegate.class);
  
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

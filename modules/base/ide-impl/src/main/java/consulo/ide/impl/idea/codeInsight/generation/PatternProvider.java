package consulo.ide.impl.idea.codeInsight.generation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Extension;
import consulo.dataContext.DataContext;

/**
 * @author Dmitry Avdeev
 */
@Extension(ComponentScope.APPLICATION)
public interface PatternProvider {
  PatternDescriptor[] getDescriptors();

  boolean isAvailable(DataContext context);
}

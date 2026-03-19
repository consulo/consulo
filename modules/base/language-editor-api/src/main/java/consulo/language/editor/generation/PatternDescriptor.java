package consulo.language.editor.generation;

import consulo.language.editor.template.Template;
import consulo.dataContext.DataContext;
import org.jspecify.annotations.Nullable;

import javax.swing.*;

/**
* @author Dmitry Avdeev
*/
public abstract class PatternDescriptor {

  public static final String ROOT = "root";

  public @Nullable String getId() {
    return null;
  }

  
  public abstract String getParentId();

  
  public abstract String getName();

  public abstract @Nullable Icon getIcon();

  public abstract @Nullable Template getTemplate();

  public abstract void actionPerformed(DataContext context);
}

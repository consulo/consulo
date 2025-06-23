package consulo.ide.impl.idea.codeInspection.ui;

import consulo.application.AllIcons;
import consulo.module.Module;
import consulo.ui.image.Image;

/**
 * @author anna
 * @since 2006-01-09
 */
public class InspectionModuleNode extends InspectionTreeNode{
  private final Module myModule;
  public InspectionModuleNode(final Module module) {
    super(module);
    myModule = module;
  }

  @Override
  public Image getIcon() {
    return AllIcons.Nodes.Module;
  }

  public String getName(){
    return myModule.getName();
  }

  public String toString() {
    return getName();
  }
}

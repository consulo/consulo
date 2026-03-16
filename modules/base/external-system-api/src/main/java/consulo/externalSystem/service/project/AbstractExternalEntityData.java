package consulo.externalSystem.service.project;

import consulo.externalSystem.model.ProjectSystemId;

/**
 * @author Denis Zhdanov
 * @since 8/25/11 3:44 PM
 */
public abstract class AbstractExternalEntityData implements ExternalEntityData {

  private static final long serialVersionUID = 1L;
  
  
  private ProjectSystemId myOwner;
  
  public AbstractExternalEntityData(ProjectSystemId owner) {
    myOwner = owner;
  }

  @Override
  
  public ProjectSystemId getOwner() {
    return myOwner;
  }

  @Override
  public int hashCode() {
    return myOwner.hashCode();
  }

  @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    AbstractExternalEntityData that = (AbstractExternalEntityData)obj;
    return myOwner.equals(that.myOwner);
  }
} 
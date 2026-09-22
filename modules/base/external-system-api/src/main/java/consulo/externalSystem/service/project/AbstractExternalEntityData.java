package consulo.externalSystem.service.project;

import consulo.externalSystem.model.ProjectSystemId;
import org.jspecify.annotations.Nullable;

/**
 * @author Denis Zhdanov
 * @since 2011-08-25
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

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AbstractExternalEntityData that = (AbstractExternalEntityData) obj;
        return myOwner.equals(that.myOwner);
    }
}

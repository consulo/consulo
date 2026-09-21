package consulo.externalSystem.service.project;

import consulo.externalSystem.model.ProjectSystemId;
import org.jspecify.annotations.Nullable;

/**
 * @author Denis Zhdanov
 * @since 2011-08-25
 */
public abstract class AbstractNamedData extends AbstractExternalEntityData implements Named {
    private static final long serialVersionUID = 1L;

    private String myExternalName;

    private String myInternalName;

    public AbstractNamedData(ProjectSystemId owner, String externalName) {
        this(owner, externalName, externalName);
    }

    public AbstractNamedData(ProjectSystemId owner, String externalName, String internalName) {
        super(owner);
        myExternalName = externalName;
        myInternalName = internalName;
    }

    /**
     * please use {@link #getExternalName()} or {@link #getInternalName()} instead
     */
    @Deprecated
    @Override
    public String getName() {
        return getExternalName();
    }

    /**
     * please use {@link #setExternalName(String)} or {@link #setInternalName(String)} instead
     */
    @Deprecated
    @Override
    public void setName(String name) {
        setExternalName(name);
    }

    @Override
    public String getExternalName() {
        return myExternalName;
    }

    @Override
    public void setExternalName(String name) {
        myExternalName = name;
    }

    @Override
    public String getInternalName() {
        return myInternalName;
    }

    @Override
    public void setInternalName(String name) {
        myInternalName = name;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + myExternalName.hashCode();
        result = 31 * result + myInternalName.hashCode();
        return result;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!super.equals(o)) {
            return false;
        }

        AbstractNamedData that = (AbstractNamedData) o;

        return myExternalName.equals(that.myExternalName)
            && myInternalName.equals(that.myInternalName);
    }
}

package consulo.execution.impl.internal.compound;

import com.dslplatform.json.CompiledJson;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Tag;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A configuration a compound configuration runs, by type id and name.
 */
@CompiledJson
@Tag("toRun")
public class CompoundRunConfigurationTarget {
    private @Nullable String myType;
    private @Nullable String myName;

    public CompoundRunConfigurationTarget() {
    }

    public CompoundRunConfigurationTarget(String type, String name) {
        myType = type;
        myName = name;
    }

    @Attribute("type")
    public @Nullable String getType() {
        return myType;
    }

    public void setType(@Nullable String type) {
        myType = type;
    }

    @Attribute("name")
    public @Nullable String getName() {
        return myName;
    }

    public void setName(@Nullable String name) {
        myName = name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CompoundRunConfigurationTarget that && Objects.equals(myType, that.myType) && Objects.equals(myName, that.myName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myType, myName);
    }
}

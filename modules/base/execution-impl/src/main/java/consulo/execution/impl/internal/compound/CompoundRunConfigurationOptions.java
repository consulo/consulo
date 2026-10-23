package consulo.execution.impl.internal.compound;

import com.dslplatform.json.CompiledJson;
import consulo.execution.configuration.RunConfigurationOptions;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Property;

import java.util.ArrayList;
import java.util.List;

@CompiledJson
public class CompoundRunConfigurationOptions extends RunConfigurationOptions {
    private List<CompoundRunConfigurationTarget> myTargets = new ArrayList<>();

    @Property(surroundWithTag = false)
    @AbstractCollection(surroundWithTag = false)
    public List<CompoundRunConfigurationTarget> getTargets() {
        return myTargets;
    }

    public void setTargets(List<CompoundRunConfigurationTarget> targets) {
        myTargets = new ArrayList<>(targets);
    }
}

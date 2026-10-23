package consulo.execution.configuration.fragment;

import com.dslplatform.json.CompiledJson;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.MapAnnotation;
import consulo.util.xml.serializer.annotation.Property;

import java.util.LinkedHashMap;
import java.util.Map;

@CompiledJson
public class EnvironmentFragmentState {
    private Map<String, String> myEnvs = new LinkedHashMap<>();
    private boolean myPassParentEnvs = true;

    public EnvironmentFragmentState() {
    }

    public EnvironmentFragmentState(Map<String, String> envs, boolean passParentEnvs) {
        setEnvs(envs);
        myPassParentEnvs = passParentEnvs;
    }

    @Property(surroundWithTag = false)
    @MapAnnotation(
        surroundWithTag = false,
        surroundKeyWithTag = false,
        surroundValueWithTag = false,
        keyAttributeName = "name",
        valueAttributeName = "value",
        entryTagName = "env"
    )
    public Map<String, String> getEnvs() {
        return myEnvs;
    }

    public void setEnvs(Map<String, String> envs) {
        myEnvs = new LinkedHashMap<>(envs);
    }

    @Attribute("passParentEnvs")
    public boolean isPassParentEnvs() {
        return myPassParentEnvs;
    }

    public void setPassParentEnvs(boolean passParentEnvs) {
        myPassParentEnvs = passParentEnvs;
    }
}

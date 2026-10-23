package consulo.execution.configuration.fragment;

import com.dslplatform.json.CompiledJson;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import org.jspecify.annotations.Nullable;

@CompiledJson
public class ProgramParametersFragmentState {
    private @Nullable String myValue;

    public ProgramParametersFragmentState() {
    }

    public ProgramParametersFragmentState(@Nullable String value) {
        setValue(value);
    }

    @Attribute("value")
    public @Nullable String getValue() {
        return myValue;
    }

    public void setValue(@Nullable String value) {
        myValue = StringUtil.nullize(value);
    }
}

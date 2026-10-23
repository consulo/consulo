package consulo.execution.configuration.fragment;

import com.dslplatform.json.CompiledJson;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import org.jspecify.annotations.Nullable;

@CompiledJson
public class ModuleFragmentState {
    private @Nullable String myName;

    public ModuleFragmentState() {
    }

    public ModuleFragmentState(@Nullable String name) {
        setName(name);
    }

    @Attribute("name")
    public @Nullable String getName() {
        return myName;
    }

    public void setName(@Nullable String name) {
        myName = StringUtil.nullize(name, true);
    }
}

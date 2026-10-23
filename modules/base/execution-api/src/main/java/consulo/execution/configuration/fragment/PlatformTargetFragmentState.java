package consulo.execution.configuration.fragment;

import com.dslplatform.json.CompiledJson;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import org.jspecify.annotations.Nullable;

/**
 * The platform target a {@link consulo.execution.configuration.PlatformAwareRunProfile} runs on, {@code null} for the local machine.
 */
@CompiledJson
public class PlatformTargetFragmentState {
    private @Nullable String myTargetId;

    public PlatformTargetFragmentState() {
    }

    public PlatformTargetFragmentState(@Nullable String targetId) {
        setTargetId(targetId);
    }

    @Attribute("targetId")
    public @Nullable String getTargetId() {
        return myTargetId;
    }

    public void setTargetId(@Nullable String targetId) {
        myTargetId = StringUtil.nullize(targetId);
    }
}

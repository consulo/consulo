package consulo.sandboxPlugin.ide.coverage;

import com.dslplatform.json.CompiledJson;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-23
 */
@CompiledJson
public class SandCoverageFilterState {
    private @Nullable String myIncludePattern;

    @Attribute("includePattern")
    public @Nullable String getIncludePattern() {
        return myIncludePattern;
    }

    public void setIncludePattern(@Nullable String includePattern) {
        myIncludePattern = StringUtil.nullize(includePattern);
    }
}

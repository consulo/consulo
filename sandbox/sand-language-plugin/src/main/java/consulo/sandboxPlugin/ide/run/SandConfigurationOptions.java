package consulo.sandboxPlugin.ide.run;

import com.dslplatform.json.CompiledJson;
import consulo.execution.configuration.RunConfigurationOptions;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import org.jspecify.annotations.Nullable;

@CompiledJson
public class SandConfigurationOptions extends RunConfigurationOptions {
    private @Nullable String myEntryPoint;
    private @Nullable String myExpression;
    private @Nullable String myProfile;
    private @Nullable String myTags;

    @Attribute("entryPoint")
    public @Nullable String getEntryPoint() {
        return myEntryPoint;
    }

    public void setEntryPoint(@Nullable String entryPoint) {
        myEntryPoint = StringUtil.nullize(entryPoint);
    }

    @Attribute("expression")
    public @Nullable String getExpression() {
        return myExpression;
    }

    public void setExpression(@Nullable String expression) {
        myExpression = StringUtil.nullize(expression);
    }

    @Attribute("profile")
    public @Nullable String getProfile() {
        return myProfile;
    }

    public void setProfile(@Nullable String profile) {
        myProfile = StringUtil.nullize(profile);
    }

    @Attribute("tags")
    public @Nullable String getTags() {
        return myTags;
    }

    public void setTags(@Nullable String tags) {
        myTags = StringUtil.nullize(tags);
    }
}

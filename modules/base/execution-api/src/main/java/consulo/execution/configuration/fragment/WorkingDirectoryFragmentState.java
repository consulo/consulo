package consulo.execution.configuration.fragment;

import com.dslplatform.json.CompiledJson;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.virtualFileSystem.VirtualFileManager;
import org.jspecify.annotations.Nullable;

@CompiledJson
public class WorkingDirectoryFragmentState {
    private static final String URL_SEPARATOR = "://";

    private @Nullable String myPath;

    public WorkingDirectoryFragmentState() {
    }

    public WorkingDirectoryFragmentState(@Nullable String path) {
        setPath(path);
    }

    @Attribute("path")
    public @Nullable String getPath() {
        return myPath;
    }

    public void setPath(@Nullable String path) {
        String value = StringUtil.nullize(path, true);
        if (value == null) {
            myPath = null;
            return;
        }

        String localPath = value.contains(URL_SEPARATOR) ? VirtualFileManager.extractPath(value) : value;
        myPath = StringUtil.nullize(localPath.replace('\\', '/'), true);
    }
}

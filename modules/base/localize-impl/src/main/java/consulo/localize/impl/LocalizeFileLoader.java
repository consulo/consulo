package consulo.localize.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
class LocalizeFileLoader {
  public static class LocalizeValueInstance {
    private String myText;

    public String getText() {
      return myText;
    }
  }

  private static final Logger LOG = Logger.getInstance(LocalizeFileLoader.class);

  private String myFilePath;
  private ClassLoader myClassLoader;

  private final Map<String, LocalizeValueInstance> myValues = new HashMap<>();

  public LocalizeFileLoader(String filePath, ClassLoader classLoader) {
    myFilePath = filePath;
    myClassLoader = classLoader;
  }

  @Nonnull
  public LocalizeValueInstance get(String id) {
    return myValues.computeIfAbsent(id, s -> {
      LocalizeValueInstance instance = new LocalizeValueInstance();
      instance.myText = id;
      return instance;
    });
  }

  public void parse() {
    try {
      Yaml yaml = new Yaml();

      InputStream stream = myClassLoader.getResourceAsStream(myFilePath);

      Map<String, Map<String, String>> o = yaml.load(stream);

      for (Map.Entry<String, Map<String, String>> entry : o.entrySet()) {
        String key = entry.getKey();
        Map<String, String> value = entry.getValue();

        LocalizeValueInstance instance = new LocalizeValueInstance();
        instance.myText = StringUtil.notNullize(value.get("text"));

        myValues.put(key, instance);
      }
    }
    catch (Exception e) {
      LOG.error(e);
    }
  }
}

package consulo.ide.plugins;

/**
 * @author VISTALL
 * @since 30-Aug-16
 *
 * https://raw.githubusercontent.com/consulo/consulo-webservice-api/master/src/main/java/consulo/webService/update/PluginNode.java
 */
public class PluginJsonNode {
  public String id;
  public String name;
  public String description;
  public String category;
  public String vendor;
  public int downloads;
  public Long length;
  public Long date;
  public Integer rating;
  public String version;
  public String platformVersion;
  public String[] dependencies;
  public String[] optionalDependencies;
}
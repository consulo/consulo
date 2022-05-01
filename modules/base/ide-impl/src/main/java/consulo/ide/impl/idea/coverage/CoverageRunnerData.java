package consulo.ide.impl.idea.coverage;

import consulo.execution.configuration.RunnerSettings;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;

/**
 * User: anna
 * Date: 9/30/11
 */
public class CoverageRunnerData implements RunnerSettings{
  @Override
  public void readExternal(Element element) throws InvalidDataException {}

  @Override
  public void writeExternal(Element element) throws WriteExternalException {}
}

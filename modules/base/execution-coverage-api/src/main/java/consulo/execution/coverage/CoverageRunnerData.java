package consulo.execution.coverage;

import consulo.execution.configuration.RunnerSettings;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;

/**
 * @author anna
 * @since 2011-09-30
 */
public class CoverageRunnerData implements RunnerSettings {
    @Override
    public void readExternal(Element element) throws InvalidDataException {
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
    }
}

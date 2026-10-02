package consulo.endpoint.impl.internal.client.generator;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.eap.EarlyAccessProgramDescriptor;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.localize.LocalizeValue;

@ExtensionImpl
public final class ClientGeneratorInlayActionEapDescriptor extends EarlyAccessProgramDescriptor {
    @Override
    public LocalizeValue getName() {
        return EndpointLocalize.clientGeneratorInlayActionEapName();
    }

    @Override
    public LocalizeValue getDescription() {
        return EndpointLocalize.clientGeneratorInlayActionEapDescription();
    }
}

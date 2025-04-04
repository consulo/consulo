package consulo.credentialStorage.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;

@ServiceAPI(ComponentScope.APPLICATION)
public interface CredentialStoreUiService {

    static CredentialStoreUiService getInstance() {
        return Application.get().getInstance(CredentialStoreUiService.class);
    }

    boolean showChangeMainPasswordDialog(@Nullable Component contextComponent,
                                         @Nonnull ChangePasswordHandler setNewMainPassword);



    @FunctionalInterface
    interface ChangePasswordHandler {
        boolean change(char[] current, char[] newPassword);
    }
}

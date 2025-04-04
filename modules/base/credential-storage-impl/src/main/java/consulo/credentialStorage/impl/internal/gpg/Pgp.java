package consulo.credentialStorage.impl.internal.gpg;


import consulo.credentialStorage.impl.internal.SharedLogger;
import consulo.application.ApplicationManager;
import consulo.process.ExecutionException;
import consulo.util.collection.SmartList;
import consulo.util.lang.StringUtil;

import java.util.List;

public class Pgp {
    private final GpgToolWrapper gpgTool;

    public Pgp() {
        this.gpgTool = createGpg();
    }

    public Pgp(GpgToolWrapper gpgTool) {
        this.gpgTool = gpgTool;
    }

    // Only keys with "Encrypt" capability are returned.
    public List<PgpKey> listKeys() throws ExecutionException {
        List<PgpKey> result = new SmartList<>();
        String currentKeyId = null;
        String currentCapabilities = null;
        String secretKeysOutput = gpgTool.listSecretKeys();
        String normalizedOutput = StringUtil.convertLineSeparators(secretKeysOutput);
        // Split by newline.
        String[] lines = normalizedOutput.split("\n");
        for (String line : lines) {
            // Split each line by colon.
            String[] fields = line.split(":");
            if (fields.length == 0) {
                continue;
            }
            String tag = fields[0];
            if ("sec".equals(tag)) {
                // Expect at least 12 fields: Field 5 (index 4) is KeyID; Field 12 (index 11) is key capabilities.
                if (fields.length >= 12) {
                    currentKeyId = fields[4];
                    currentCapabilities = fields[11];
                }
            }
            else if ("uid".equals(tag)) {
                // A uid line following a sec line.
                if (currentCapabilities != null && !currentCapabilities.contains("D") && currentCapabilities.contains("E")) {
                    // Field 10 (index 9) is the User-ID; replace the quoted colon sequence.
                    if (fields.length >= 10) {
                        String userId = fields[9].replace("=\\x3a=", ":");
                        result.add(new PgpKey(currentKeyId, userId));
                    }
                }
            }
        }
        return result;
    }

    public byte[] decrypt(byte[] data) {
        return gpgTool.decrypt(data);
    }

    public byte[] encrypt(byte[] data, String recipient) {
        return gpgTool.encrypt(data, recipient);
    }

    private static GpgToolWrapper createGpg() {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return new DummyGpgToolWrapper();
        }
        GpgToolWrapper result = new GpgToolWrapperImpl();
        try {
            // Verify the tool is working.
            result.version();
        }
        catch (Exception e) {
            SharedLogger.LOG.debug(e);
            return new DummyGpgToolWrapper();
        }
        return result;
    }

    // A dummy implementation for cases when gpg is not available.
    private static class DummyGpgToolWrapper implements GpgToolWrapper {
        @Override
        public byte[] encrypt(byte[] data, String recipient) {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] decrypt(byte[] data) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String listSecretKeys() {
            return "";
        }

        @Override
        public String version() {
            return "";
        }
    }
}

package attune.alarm.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class ReceiptTokenHasher {

    private ReceiptTokenHasher() {
    }

    public static String hash(String receiptToken) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(receiptToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    public static boolean matches(String receiptToken, String storedHash) {
        if (receiptToken == null || storedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(receiptToken).getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8)
        );
    }
}

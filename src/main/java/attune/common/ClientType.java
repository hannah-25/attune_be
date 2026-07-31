package attune.common;

public enum ClientType {
    WEB, IOS, ANDROID;

    public static ClientType from(String value) {
        if (value == null) return WEB;
        return switch (value.toLowerCase()) {
            case "ios" -> IOS;
            case "android" -> ANDROID;
            default -> WEB;
        };
    }

    public boolean isMobile() {
        return this == IOS || this == ANDROID;
    }
}

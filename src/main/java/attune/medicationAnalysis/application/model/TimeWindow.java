package attune.medicationAnalysis.application.model;

import java.time.LocalTime;

public enum TimeWindow {
    W00("00:00~02:59", 0),
    W03("03:00~05:59", 3),
    W06("06:00~08:59", 6),
    W09("09:00~11:59", 9),
    W12("12:00~14:59", 12),
    W15("15:00~17:59", 15),
    W18("18:00~20:59", 18),
    W21("21:00~23:59", 21);

    private final String label;
    private final int startHour;

    TimeWindow(String label, int startHour) {
        this.label = label;
        this.startHour = startHour;
    }

    public String getLabel() { return label; }
    public int getStartHour() { return startHour; }

    public static TimeWindow of(LocalTime time) {
        int hour = time.getHour();
        if (hour < 3) return W00;
        if (hour < 6) return W03;
        if (hour < 9) return W06;
        if (hour < 12) return W09;
        if (hour < 15) return W12;
        if (hour < 18) return W15;
        if (hour < 21) return W18;
        return W21;
    }
}

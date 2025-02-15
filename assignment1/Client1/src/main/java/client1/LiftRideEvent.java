package client1;

public class LiftRideEvent {
    private final int skierID;
    private final int resortID;
    private final int liftID;
    private final String seasonID;
    private final String dayID;
    private final int time;

    public LiftRideEvent(int skierID, int resortID, int liftID, String seasonID, String dayID, int time) {
        this.skierID = skierID;
        this.resortID = resortID;
        this.liftID = liftID;
        this.seasonID = seasonID;
        this.dayID = dayID;
        this.time = time;
    }

    public int getSkierID() { return skierID; }
    public int getResortID() { return resortID; }
    public int getLiftID() { return liftID; }
    public String getSeasonID() { return seasonID; }
    public String getDayID() { return dayID; }
    public int getTime() { return time; }
}

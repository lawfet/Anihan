package time;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;

/**
 * GameClock handles the in-game day/night cycle, seasons, and ambient lighting.
 */
public class GameClock {

    public enum Season {
        SPRING("Spring"),
        SUMMER("Summer"),
        FALL("Fall"),
        WINTER("Winter");

        private final String displayName;

        Season(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private int minute = 0;
    private int hour = 6; // Starts at 6:00 AM
    private int day = 1;
    private Season currentSeason = Season.SPRING;
    private int year = 1;

    private int tickCounter = 0;
    private static final int TICKS_PER_MINUTE = 360; // Six real seconds per ten game minutes.

    private boolean isDayAdvancing = false;
    private float dayFadeAlpha = 0.0f; // For fade-to-black on sleep / next day
    private boolean isSleeping = false;
    private Runnable newDayCallback;

    public GameClock(Runnable newDayCallback) {
        this.newDayCallback = newDayCallback;
    }

    public void update() {
        if (isSleeping) {
            dayFadeAlpha += 0.04f;
            if (dayFadeAlpha >= 1.0f) {
                dayFadeAlpha = 1.0f;
                executeDayAdvance();
                isSleeping = false;
            }
            return;
        }

        if (dayFadeAlpha > 0.0f) {
            dayFadeAlpha -= 0.03f;
            if (dayFadeAlpha < 0.0f) {
                dayFadeAlpha = 0.0f;
            }
        }

        tickCounter++;
        if (tickCounter >= TICKS_PER_MINUTE) {
            tickCounter = 0;
            minute += 10; // In 10-minute intervals
            if (minute >= 60) {
                minute = 0;
                hour++;
                if (hour >= 26) { // 26 = 2:00 AM of next morning -> pass out
                    passOutFromExhaustion();
                }
            }
        }
    }

    public void sleep() {
        if (!isSleeping) {
            isSleeping = true;
        }
    }

    private void passOutFromExhaustion() {
        sleep();
    }

    public void executeDayAdvance() {
        isSleeping = false;
        day++;
        hour = 6;
        minute = 0;
        tickCounter = 0;

        if (day > 28) { // 28 days per season like classic farming games
            day = 1;
            Season[] seasons = Season.values();
            int nextSeason = (currentSeason.ordinal() + 1) % seasons.length;
            currentSeason = seasons[nextSeason];
            if (nextSeason == 0) {
                year++;
            }
        }

        if (newDayCallback != null) {
            newDayCallback.run();
        }
    }

    /**
     * Renders ambient lighting and sleep transitions over the entire screen.
     */
    public void renderAmbientLighting(Graphics2D g2, int screenWidth, int screenHeight, int playerScreenX, int playerScreenY) {
        // Calculate ambient color based on time of day
        // 6 to 17: Day
        // 17 to 20: Sunset (Amber/Golden)
        // 20 to 22: Dusk (Deep Blue)
        // 22 to 26 (2am): Night (Dark Navy)
        float totalHours = hour + (minute / 60.0f);
        Color ambientColor = null;

        if (totalHours >= 6.0f && totalHours < 8.0f) {
            // Early morning dawn: gentle warm peach
            float factor = (8.0f - totalHours) / 2.0f;
            ambientColor = new Color(255, 180, 100, (int) (60 * factor));
        } else if (totalHours >= 8.0f && totalHours < 16.5f) {
            // Full daylight
            ambientColor = null;
        } else if (totalHours >= 16.5f && totalHours < 19.5f) {
            // Sunset: rich warm golden hour
            float factor = (totalHours - 16.5f) / 3.0f;
            ambientColor = new Color(240, 120, 40, (int) (80 * factor));
        } else if (totalHours >= 19.5f && totalHours < 21.0f) {
            // Twilight: violet dusk
            float factor = (totalHours - 19.5f) / 1.5f;
            ambientColor = new Color(40, 20, 90, (int) (60 + 80 * factor));
        } else {
            // Night (21:00 to 06:00)
            ambientColor = new Color(10, 15, 45, 175);
        }

        if (ambientColor != null) {
            if (totalHours >= 19.5f || totalHours < 6.0f) {
                // Night time: render darkness with lantern cutout around player
                float radius = 130.0f;
                Point2D center = new Point2D.Float(playerScreenX, playerScreenY);
                float[] dist = {0.0f, 0.4f, 1.0f};
                Color[] colors = {
                    new Color(255, 230, 150, 20),
                    new Color(15, 20, 50, 100),
                    ambientColor
                };
                RadialGradientPaint p = new RadialGradientPaint(center, radius, dist, colors);
                g2.setPaint(p);
                g2.fillRect(0, 0, screenWidth, screenHeight);
            } else {
                g2.setColor(ambientColor);
                g2.fillRect(0, 0, screenWidth, screenHeight);
            }
        }

        // Sleep / day transition black overlay
        if (dayFadeAlpha > 0.0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1.0f, dayFadeAlpha)));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, screenWidth, screenHeight);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }

    public String getTimeFormatted() {
        int displayHour = hour % 24;
        String period = (displayHour >= 12 && displayHour < 24) ? "PM" : "AM";
        int standardHour = displayHour % 12;
        if (standardHour == 0) standardHour = 12;
        return String.format("%02d:%02d %s", standardHour, minute, period);
    }

    public String getDateFormatted() {
        return String.format("Day %d, %s (Year %d)", day, currentSeason.getDisplayName(), year);
    }

    public int getDay() { return day; }
    public int getHour() { return hour; }
    public int getMinute() { return minute; }
    public Season getSeason() { return currentSeason; }
    public boolean isSleeping() { return isSleeping; }
}

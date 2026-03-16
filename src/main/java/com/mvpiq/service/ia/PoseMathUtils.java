package com.mvpiq.service.ia;

import com.mvpiq.dto.BallPointDTO;
import com.mvpiq.dto.KeyPointDTO;

public final class PoseMathUtils {

    private PoseMathUtils() {
    }

    public static double distance(KeyPointDTO a, BallPointDTO b) {
        if (a == null || b == null) {
            return Double.MAX_VALUE;
        }

        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static double distance(KeyPointDTO a, KeyPointDTO b) {
        if (a == null || b == null) {
            return Double.MAX_VALUE;
        }

        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static Double angle(KeyPointDTO a, KeyPointDTO b, KeyPointDTO c) {
        if (a == null || b == null || c == null) {
            return null;
        }

        double abx = a.getX() - b.getX();
        double aby = a.getY() - b.getY();
        double cbx = c.getX() - b.getX();
        double cby = c.getY() - b.getY();

        double dot = abx * cbx + aby * cby;
        double mag1 = Math.sqrt(abx * abx + aby * aby);
        double mag2 = Math.sqrt(cbx * cbx + cby * cby);

        if (mag1 == 0 || mag2 == 0) {
            return null;
        }

        double cos = dot / (mag1 * mag2);
        cos = Math.max(-1.0, Math.min(1.0, cos));

        return Math.toDegrees(Math.acos(cos));
    }
}
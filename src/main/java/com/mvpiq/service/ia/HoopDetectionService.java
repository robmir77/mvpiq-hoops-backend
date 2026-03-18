package com.mvpiq.service.ia;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;

import javax.imageio.ImageIO;

@ApplicationScoped
public class HoopDetectionService {

    private static final Logger LOG = Logger.getLogger(HoopDetectionService.class);

    public Hoop detectHoop(File frameFile) {

        try {

            BufferedImage img = ImageIO.read(frameFile);

            if (img == null) {
                LOG.warn("Image read failed");
                return null;
            }

            int width = img.getWidth();
            int height = img.getHeight();

            LOG.infof("Frame size -> w=%d h=%d", width, height);

            // cartella debug
            File debugDir = new File("C:/temp");
            if (!debugDir.exists()) {
                debugDir.mkdirs();
            }

            // salva frame originale
            ImageIO.write(
                    img,
                    "png",
                    new File(debugDir, "frame_" + frameFile.getName() + ".png")
            );

            // -------------------------
            // 1️⃣ Detect white backboard
            // -------------------------
            // cerchiamo un rettangolo bianco nella zona alto-sinistra
            // usando blob detection invece di una bbox globale di tutti i pixel bianchi

            int searchWidth = width / 3;
            int searchHeight = height / 3;

            boolean[][] whiteMask = new boolean[searchHeight][searchWidth];

            int whitePixels = 0;

            for (int y = 0; y < searchHeight; y++) {
                for (int x = 0; x < searchWidth; x++) {

                    int rgb = img.getRGB(x, y);

                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;

                    boolean isWhite =
                            r > 200 &&
                                    g > 200 &&
                                    b > 200 &&
                                    Math.abs(r - g) < 20 &&
                                    Math.abs(r - b) < 20 &&
                                    Math.abs(g - b) < 20;

                    whiteMask[y][x] = isWhite;

                    if (isWhite) {
                        whitePixels++;
                    }
                }
            }

            LOG.info("White pixels detected: " + whitePixels);

            if (whitePixels < 200) {
                LOG.warn("Backboard not detected (not enough white pixels)");
                return null;
            }

            boolean[][] whiteVisited = new boolean[searchHeight][searchWidth];

            int boardMinX = -1;
            int boardMinY = -1;
            int boardMaxX = -1;
            int boardMaxY = -1;
            int bestBoardArea = 0;
            double bestBoardScore = -1;

            int[] dx = {1, -1, 0, 0};
            int[] dy = {0, 0, 1, -1};

            for (int startY = 0; startY < searchHeight; startY++) {
                for (int startX = 0; startX < searchWidth; startX++) {

                    if (!whiteMask[startY][startX] || whiteVisited[startY][startX]) {
                        continue;
                    }

                    ArrayDeque<Point> queue = new ArrayDeque<>();
                    queue.add(new Point(startX, startY));
                    whiteVisited[startY][startX] = true;

                    int minX = startX;
                    int minY = startY;
                    int maxX = startX;
                    int maxY = startY;
                    int area = 0;

                    while (!queue.isEmpty()) {

                        Point p = queue.poll();
                        int x = p.x;
                        int y = p.y;

                        area++;

                        if (x < minX) minX = x;
                        if (y < minY) minY = y;
                        if (x > maxX) maxX = x;
                        if (y > maxY) maxY = y;

                        for (int i = 0; i < 4; i++) {
                            int nx = x + dx[i];
                            int ny = y + dy[i];

                            if (nx < 0 || ny < 0 || nx >= searchWidth || ny >= searchHeight) {
                                continue;
                            }

                            if (whiteVisited[ny][nx] || !whiteMask[ny][nx]) {
                                continue;
                            }

                            whiteVisited[ny][nx] = true;
                            queue.add(new Point(nx, ny));
                        }
                    }

                    int blobWidth = maxX - minX + 1;
                    int blobHeight = maxY - minY + 1;

                    LOG.debugf(
                            "White blob -> x=%d y=%d w=%d h=%d area=%d",
                            minX, minY, blobWidth, blobHeight, area
                    );

                    // -------------------------
                    // filtro dimensione tabellone
                    // -------------------------

                    if (blobWidth < 60 || blobHeight < 20) {
                        continue;
                    }

                    // -------------------------
                    // filtro rapporto dimensioni
                    // -------------------------

                    double ratio = (double) blobWidth / blobHeight;
                    LOG.info("White blob ratio: " + ratio);

                    if (ratio < 1.2 || ratio > 2.4) {
                        continue;
                    }

                    // il tabellone tende ad essere abbastanza pieno nella sua bbox
                    double density = (double) area / (blobWidth * blobHeight);

                    LOG.info("White blob density: " + density);

                    if (density < 0.45) {
                        continue;
                    }

                    // premiamo blob grandi, densi e in alto a sinistra
                    double score =
                            area * 2.0 +
                                    density * 1000.0 -
                                    minX * 0.5 -
                                    minY * 0.5;

                    LOG.info("White blob score: " + score);

                    if (score > bestBoardScore) {
                        bestBoardScore = score;
                        bestBoardArea = area;
                        boardMinX = minX;
                        boardMinY = minY;
                        boardMaxX = maxX;
                        boardMaxY = maxY;
                    }
                }
            }

            if (boardMaxX < 0 || boardMaxY < 0) {
                LOG.warn("Backboard not detected (no valid white blob found)");
                return null;
            }

            int minX = boardMinX;
            int minY = boardMinY;
            int maxX = boardMaxX;
            int maxY = boardMaxY;

            int boardWidth = maxX - minX + 1;
            int boardHeight = maxY - minY + 1;

            LOG.infof(
                    "Selected backboard -> x=%d y=%d w=%d h=%d area=%d score=%.2f",
                    minX, minY, boardWidth, boardHeight, bestBoardArea, bestBoardScore
            );

            double ratio = (double) boardWidth / boardHeight;
            LOG.info("Backboard ratio: " + ratio);

            int boardCenterX = (minX + maxX) / 2;
            int boardBottomY = maxY;

            LOG.infof("Backboard center -> x=%d bottomY=%d", boardCenterX, boardBottomY);

            // -------------------------
            // 2️⃣ ROI sotto il tabellone
            // -------------------------
            // il ferro sta poco sotto il tabellone, quindi teniamo una ROI bassa e stretta

            int roiTop = boardBottomY + 1;
            int roiHeight = Math.max(20, boardHeight / 2);

            int roiLeft = boardCenterX - boardWidth / 2;
            int roiWidth = boardWidth;

            roiLeft = Math.max(0, roiLeft);
            roiTop = Math.max(0, roiTop);

            if (roiLeft >= width || roiTop >= height) {
                LOG.warn("Invalid ROI position");
                return null;
            }

            if (roiLeft + roiWidth > width) {
                roiWidth = width - roiLeft;
            }

            if (roiTop + roiHeight > height) {
                roiHeight = height - roiTop;
            }

            if (roiWidth <= 0 || roiHeight <= 0) {
                LOG.warn("Invalid ROI size");
                return null;
            }

            LOG.infof("ROI -> left=%d top=%d w=%d h=%d",
                    roiLeft, roiTop, roiWidth, roiHeight);

            BufferedImage roi = img.getSubimage(roiLeft, roiTop, roiWidth, roiHeight);

            // salva ROI debug
            ImageIO.write(
                    roi,
                    "png",
                    new File(debugDir, "roi_" + frameFile.getName() + ".png")
            );

            // -------------------------
            // 3️⃣ Orange rim detection
            // -------------------------
            // cerchiamo il miglior blob arancione nella ROI,
            // invece di accorpare tutti i pixel arancioni in una sola bbox

            boolean[][] orangeMask = new boolean[roiHeight][roiWidth];
            int orangePixels = 0;

            for (int y = 0; y < roiHeight; y++) {
                for (int x = 0; x < roiWidth; x++) {

                    int rgb = roi.getRGB(x, y);

                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;

                    float[] hsv = Color.RGBtoHSB(r, g, b, null);

                    float hue = hsv[0] * 360f;
                    float sat = hsv[1];
                    float val = hsv[2];

                    boolean isOrange =
                            hue > 5 && hue < 45 &&
                                    sat > 0.25f &&
                                    val > 0.20f &&
                                    r >= g;

                    orangeMask[y][x] = isOrange;

                    if (isOrange) {
                        orangePixels++;
                    }
                }
            }

            LOG.info("Orange pixels detected: " + orangePixels);

            if (orangePixels < 3) {
                LOG.warn("Rim not detected (not enough orange pixels)");
                return null;
            }

            boolean[][] orangeVisited = new boolean[roiHeight][roiWidth];

            int orangeMinX = -1;
            int orangeMinY = -1;
            int orangeMaxX = -1;
            int orangeMaxY = -1;
            int bestOrangeArea = 0;
            double bestOrangeScore = -1;

            for (int startY = 0; startY < roiHeight; startY++) {
                for (int startX = 0; startX < roiWidth; startX++) {

                    if (!orangeMask[startY][startX] || orangeVisited[startY][startX]) {
                        continue;
                    }

                    ArrayDeque<Point> queue = new ArrayDeque<>();
                    queue.add(new Point(startX, startY));
                    orangeVisited[startY][startX] = true;

                    int minBlobX = startX;
                    int minBlobY = startY;
                    int maxBlobX = startX;
                    int maxBlobY = startY;
                    int area = 0;

                    while (!queue.isEmpty()) {

                        Point p = queue.poll();
                        int x = p.x;
                        int y = p.y;

                        area++;

                        if (x < minBlobX) minBlobX = x;
                        if (y < minBlobY) minBlobY = y;
                        if (x > maxBlobX) maxBlobX = x;
                        if (y > maxBlobY) maxBlobY = y;

                        for (int i = 0; i < 4; i++) {
                            int nx = x + dx[i];
                            int ny = y + dy[i];

                            if (nx < 0 || ny < 0 || nx >= roiWidth || ny >= roiHeight) {
                                continue;
                            }

                            if (orangeVisited[ny][nx] || !orangeMask[ny][nx]) {
                                continue;
                            }

                            orangeVisited[ny][nx] = true;
                            queue.add(new Point(nx, ny));
                        }
                    }

                    int blobWidth = maxBlobX - minBlobX + 1;
                    int blobHeight = maxBlobY - minBlobY + 1;

                    LOG.infof(
                            "Orange blob -> x=%d y=%d w=%d h=%d area=%d",
                            minBlobX, minBlobY, blobWidth, blobHeight, area
                    );

                    if (blobWidth <= 0 || blobHeight <= 0) {
                        continue;
                    }

                    // -------------------------
                    // filtro dimensione ferro
                    // -------------------------

                    if (blobWidth < 6 || blobWidth > 80) {
                        continue;
                    }

                    // -------------------------
                    // filtro forma ferro
                    // -------------------------
                    // il ferro è più largo che alto

                    double rimRatio = (double) blobWidth / blobHeight;
                    LOG.info("Orange blob ratio: " + rimRatio);

                    if (rimRatio < 1.5 || rimRatio > 4.0) {
                        continue;
                    }

                    if (blobHeight > blobWidth / 2) {
                        LOG.info("Orange blob rejected (too tall)");
                        continue;
                    }

                    int localCenterX = (minBlobX + maxBlobX) / 2;
                    int localCenterY = (minBlobY + maxBlobY) / 2;

                    int candidateCenterX = roiLeft + localCenterX;
                    int candidateCenterY = roiTop + localCenterY;

                    // -------------------------
                    // filtro posizione del ferro
                    // -------------------------
                    // il ferro deve stare circa sotto il centro del tabellone

                    int deltaXFromBoardCenter = Math.abs(candidateCenterX - boardCenterX);
                    LOG.info("Orange blob deltaX from board center: " + deltaXFromBoardCenter);

                    if (deltaXFromBoardCenter > boardWidth / 3) {
                        continue;
                    }

                    // il ferro deve stare poco sotto il tabellone

                    int deltaYFromBoardBottom = candidateCenterY - boardBottomY;
                    LOG.info("Orange blob deltaY from board bottom: " + deltaYFromBoardBottom);

                    if (deltaYFromBoardBottom < 0 || deltaYFromBoardBottom > boardHeight / 2) {
                        continue;
                    }

                    // premiamo blob orizzontali, non troppo piccoli e vicini al centro del tabellone
                    double score =
                            area * 3.0 +
                                    blobWidth * 2.0 -
                                    deltaXFromBoardCenter * 2.0 -
                                    Math.abs(deltaYFromBoardBottom) * 2.0;

                    LOG.info("Orange blob score: " + score);

                    if (score > bestOrangeScore) {
                        bestOrangeScore = score;
                        bestOrangeArea = area;
                        orangeMinX = minBlobX;
                        orangeMinY = minBlobY;
                        orangeMaxX = maxBlobX;
                        orangeMaxY = maxBlobY;
                    }
                }
            }

            if (orangeMaxX < 0 || orangeMaxY < 0) {
                LOG.warn("Rim not detected (no valid orange blob found)");
                return null;
            }

            int rimWidth = orangeMaxX - orangeMinX + 1;
            int rimHeight = orangeMaxY - orangeMinY + 1;

            LOG.infof(
                    "Selected rim -> x=%d y=%d w=%d h=%d area=%d score=%.2f",
                    orangeMinX, orangeMinY, rimWidth, rimHeight, bestOrangeArea, bestOrangeScore
            );

            int localCenterX = (orangeMinX + orangeMaxX) / 2;
            int localCenterY = (orangeMinY + orangeMaxY) / 2;

            int centerX = roiLeft + localCenterX;
            int centerY = roiTop + localCenterY;

            int radius = Math.max(1, rimWidth / 2);

            LOG.infof("Hoop detected -> x=%d y=%d r=%d", centerX, centerY, radius);

            // -------------------------
            // 4️⃣ salva immagine debug con cerchio
            // -------------------------

            BufferedImage debugImg = new BufferedImage(
                    img.getWidth(),
                    img.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );

            Graphics2D g = debugImg.createGraphics();

            try {

                g.drawImage(img, 0, 0, null);

                // bbox backboard
                g.setColor(Color.GREEN);
                g.setStroke(new BasicStroke(2));
                g.drawRect(minX, minY, boardWidth, boardHeight);

                // roi
                g.setColor(Color.YELLOW);
                g.setStroke(new BasicStroke(2));
                g.drawRect(roiLeft, roiTop, roiWidth, roiHeight);

                // bbox rim
                g.setColor(Color.ORANGE);
                g.setStroke(new BasicStroke(2));
                g.drawRect(
                        roiLeft + orangeMinX,
                        roiTop + orangeMinY,
                        rimWidth,
                        rimHeight
                );

                // hoop circle
                g.setColor(Color.RED);
                g.setStroke(new BasicStroke(3));
                g.drawOval(
                        centerX - radius,
                        centerY - radius,
                        radius * 2,
                        radius * 2
                );

            } finally {
                g.dispose();
            }

            ImageIO.write(
                    debugImg,
                    "png",
                    new File(debugDir, "debug_hoop_" + frameFile.getName() + ".png")
            );

            return new Hoop(centerX, centerY, radius);

        } catch (Exception e) {

            LOG.error("Hoop detection failed", e);
        }
        return null;
    }
}
package com.mvpiq.service;

import com.mvpiq.model.VideoAnalysisFrame;
import com.mvpiq.model.VideoAnalysisSession;
import com.mvpiq.repositories.VideoAnalysisFrameRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class VideoAnalysisFrameService {

    private static final Logger LOG = Logger.getLogger(VideoAnalysisFrameService.class);

    @Inject
    VideoAnalysisFrameRepository frameRepository;

    public List<File> extractFrames(File video, VideoAnalysisSession session, double fps) {
        List<File> frames = new ArrayList<>();
        File tempDir = null;

        try {
            LOG.info("🖼 Creating temp frame directory...");

            tempDir = new File(System.getProperty("java.io.tmpdir"), "frames-" + session.id);
            tempDir.mkdirs();

            String outputPattern = tempDir.getAbsolutePath() + "/frame-%03d.jpg";

            LOG.info("🎬 Extracting frames from video: " + video.getAbsolutePath());
            LOG.info("📏 Video size: " + (video.length() / (1024 * 1024)) + " MB");
            LOG.info("⏱ Using FPS: " + fps);

            // Avvia FFmpeg
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i", video.getAbsolutePath(),
                    "-vf", "fps=" + fps,
                    outputPattern
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Leggi i log di FFmpeg in tempo reale
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOG.info("[ffmpeg] " + line);
                }
            }

            boolean finished = process.waitFor(10, TimeUnit.MINUTES); // Timeout lungo
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("FFmpeg timeout");
            }

            LOG.info("📂 Reading extracted frames...");
            File[] files = tempDir.listFiles();
            if (files == null || files.length == 0) {
                throw new RuntimeException("No frames extracted by ffmpeg");
            }

            Arrays.sort(files);

            // Log per ogni frame creato
            for (File frame : files) {
                LOG.info("🖼 Frame created: " + frame.getName() + " | Size: " + (frame.length() / 1024) + " KB");
                frames.add(frame);
            }

            LOG.info("✅ Total frames extracted: " + frames.size());

        } catch (Exception e) {
            LOG.error("❌ Frame extraction failed", e);
            throw new RuntimeException(e);
        }

        return frames;
    }

    // Persistenza dei frame in DB, dentro transazione breve
    @Transactional
    public void persistFrames(VideoAnalysisSession session, List<File> frames) {
        int index = 0;
        for (File frame : frames) {
            VideoAnalysisFrame entity = new VideoAnalysisFrame();
            entity.session = session;
            entity.frameIndex = index++;
            entity.frameUrl = frame.getAbsolutePath();
            frameRepository.persist(entity);
        }
        LOG.info("✅ Frames persisted: " + frames.size());
    }
}
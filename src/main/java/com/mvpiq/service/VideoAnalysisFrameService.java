package com.mvpiq.service;

import com.mvpiq.model.VideoAnalysisFrame;
import com.mvpiq.model.VideoAnalysisSession;
import com.mvpiq.repositories.VideoAnalysisFrameRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class VideoAnalysisFrameService {

    private static final Logger LOG = Logger.getLogger(VideoAnalysisFrameService.class);

    @Inject
    VideoAnalysisFrameRepository frameRepository;

    // Solo estrazione file, fuori da qualsiasi transazione
    public List<File> extractFrames(File video, VideoAnalysisSession session) {
        List<File> frames = new ArrayList<>();
        try {
            LOG.info("🖼 Creating temp frame directory...");

            File tempDir = new File(System.getProperty("java.io.tmpdir"), "frames-" + session.id);
            tempDir.mkdirs();

            String outputPattern = tempDir.getAbsolutePath() + "/frame-%03d.jpg";

            LOG.info("🎞 Running ffmpeg...");
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-i", video.getAbsolutePath(), "-vf", "fps=10", outputPattern
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
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
            frames.addAll(Arrays.asList(files));

            LOG.info("✅ Frames extracted: " + frames.size());

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
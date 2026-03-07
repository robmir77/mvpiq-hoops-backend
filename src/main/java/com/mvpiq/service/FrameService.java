package com.mvpiq.service;

import com.mvpiq.model.VideoAnalysisSession;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.File;
import java.util.*;

@ApplicationScoped
public class FrameService {

    public List<File> extractFrames(File videoFile, VideoAnalysisSession session) {

        List<File> frames = new ArrayList<>();

        try {

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i", videoFile.getAbsolutePath(),
                    "-vf", "fps=1",
                    "/tmp/frame_%03d.jpg"
            );

            pb.redirectErrorStream(true);

            Process process = pb.start();
            process.waitFor();

            File dir = new File("/tmp");

            for (File f : dir.listFiles()) {

                if (f.getName().startsWith("frame_")) {
                    frames.add(f);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Frame extraction failed", e);
        }

        return frames;
    }
}
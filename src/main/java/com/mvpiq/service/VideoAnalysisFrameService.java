package com.mvpiq.service;

import com.mvpiq.model.VideoAnalysisFrame;
import com.mvpiq.model.VideoAnalysisSession;
import com.mvpiq.repositories.VideoAnalysisFrameRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class VideoAnalysisFrameService {

    @Inject
    VideoAnalysisFrameRepository frameRepository;

    public List<File> extractFrames(File video, VideoAnalysisSession session) {

        List<File> frames = new ArrayList<>();

        try {

            File frame1 = File.createTempFile("frame1", ".jpg");
            File frame2 = File.createTempFile("frame2", ".jpg");
            File frame3 = File.createTempFile("frame3", ".jpg");

            frames.add(frame1);
            frames.add(frame2);
            frames.add(frame3);

            int index = 0;

            for (File frame : frames) {

                VideoAnalysisFrame entity = new VideoAnalysisFrame();
                entity.session = session;
                entity.frameIndex = index++;
                entity.frameUrl = frame.getAbsolutePath();

                frameRepository.persist(entity);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return frames;
    }
}
package com.mvpiq.service;

import com.mvpiq.model.VideoAnalysisFrame;
import com.mvpiq.model.VideoAnalysisSession;
import com.mvpiq.repositories.VideoAnalysisFrameRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.BytePointer;
import org.jboss.logging.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.swscale.*;

@ApplicationScoped
public class VideoAnalysisFrameService {

    private static final Logger LOG = Logger.getLogger(VideoAnalysisFrameService.class);

    @Inject
    VideoAnalysisFrameRepository frameRepository;

    public List<File> extractFrames(File video, VideoAnalysisSession session, double fps) {
        List<File> frames = new ArrayList<>();
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "frames-" + session.id);
        tempDir.mkdirs();

        AVFormatContext formatContext = new AVFormatContext(null);
        AVCodecContext codecContext = null;
        AVPacket packet = new AVPacket();
        AVFrame frame = av_frame_alloc();
        AVFrame rgbFrame = av_frame_alloc();

        int videoStreamIndex = -1;

        try {
            LOG.info("🎬 Opening video: " + video.getAbsolutePath());
            if (avformat_open_input(formatContext, video.getAbsolutePath(), null, null) != 0) {
                throw new RuntimeException("Cannot open video file");
            }

            if (avformat_find_stream_info(formatContext, (AVDictionary) null) < 0) {
                throw new RuntimeException("Cannot find stream info");
            }

            // Trova il primo stream video
            for (int i = 0; i < formatContext.nb_streams(); i++) {
                if (formatContext.streams(i).codecpar().codec_type() == AVMEDIA_TYPE_VIDEO) {
                    videoStreamIndex = i;
                    break;
                }
            }

            if (videoStreamIndex == -1) {
                throw new RuntimeException("No video stream found");
            }

            AVStream videoStream = formatContext.streams(videoStreamIndex);
            AVCodec codec = avcodec_find_decoder(videoStream.codecpar().codec_id());
            if (codec == null) throw new RuntimeException("Unsupported codec");

            codecContext = avcodec_alloc_context3(codec);
            if (avcodec_parameters_to_context(codecContext, videoStream.codecpar()) < 0) {
                throw new RuntimeException("Failed to copy codec parameters");
            }

            if (avcodec_open2(codecContext, codec, (AVDictionary) null) < 0) {
                throw new RuntimeException("Failed to open codec");
            }

            // Conversione frame in RGB
            SwsContext swsCtx = sws_getContext(
                    codecContext.width(), codecContext.height(), codecContext.pix_fmt(),
                    codecContext.width(), codecContext.height(), AV_PIX_FMT_RGB24,
                    SWS_BILINEAR, null, null, (double[]) null
            );

            BytePointer buffer = new BytePointer(av_malloc(av_image_get_buffer_size(
                    AV_PIX_FMT_RGB24, codecContext.width(), codecContext.height(), 1)));

            av_image_fill_arrays(
                    rgbFrame.data(), rgbFrame.linesize(),
                    buffer, AV_PIX_FMT_RGB24,
                    codecContext.width(), codecContext.height(), 1
            );

            int frameIndex = 0;
            long frameInterval = (long) (formatContext.duration() / AV_TIME_BASE * fps);

            // Leggi i pacchetti
            while (av_read_frame(formatContext, packet) >= 0) {
                if (packet.stream_index() == videoStreamIndex) {
                    if (avcodec_send_packet(codecContext, packet) < 0) continue;

                    while (avcodec_receive_frame(codecContext, frame) == 0) {
                        // Converti in RGB
                        sws_scale(
                                swsCtx, frame.data(), frame.linesize(),
                                0, codecContext.height(),
                                rgbFrame.data(), rgbFrame.linesize()
                        );

                        // Salva frame come JPEG
                        File outFile = new File(tempDir, String.format("frame-%03d.jpg", frameIndex++));
                        saveFrameAsJPEG(rgbFrame, codecContext.width(), codecContext.height(), outFile);
                        LOG.info("🖼 Frame created: " + outFile.getName() + " | Size: " + (outFile.length() / 1024) + " KB");
                        frames.add(outFile);
                    }
                }
                av_packet_unref(packet);
            }

            LOG.info("✅ Total frames extracted: " + frames.size());

        } catch (Exception e) {
            LOG.error("❌ Frame extraction failed", e);
            throw new RuntimeException(e);
        } finally {
            av_frame_free(frame);
            av_frame_free(rgbFrame);
            if (codecContext != null) avcodec_free_context(codecContext);
            avformat_close_input(formatContext);
        }

        return frames;
    }

    private void saveFrameAsJPEG(AVFrame frame, int width, int height, File outFile) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        BytePointer data = frame.data(0);
        int linesize = frame.linesize(0);

        // Copia riga per riga
        for (int y = 0; y < height; y++) {
            int rowStart = y * width * 3;
            for (int x = 0; x < width; x++) {
                int index = x * 3;
                int r = data.get(y * linesize + index) & 0xFF;
                int g = data.get(y * linesize + index + 1) & 0xFF;
                int b = data.get(y * linesize + index + 2) & 0xFF;
                int rgb = (r << 16) | (g << 8) | b;
                img.setRGB(x, y, rgb);
            }
        }

        ImageIO.write(img, "jpg", outFile);
    }

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
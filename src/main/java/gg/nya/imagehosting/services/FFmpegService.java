package gg.nya.imagehosting.services;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FFmpegService {

    private static final Logger log = LoggerFactory.getLogger(FFmpegService.class);

    private final FFmpeg ffmpeg;
    private final FFprobe ffprobe;
    private final FFmpegExecutor executor;

    public FFmpegService() throws IOException {
        String ffmpegPath = getClass().getClassLoader().getResource("ffmpeg/ffmpeg").getPath();
        String ffprobePath = getClass().getClassLoader().getResource("ffmpeg/ffprobe").getPath();

        this.ffmpeg = new FFmpeg(ffmpegPath);
        this.ffprobe = new FFprobe(ffprobePath);
        this.executor = new FFmpegExecutor(ffmpeg, ffprobe);
    }

    /**
     * Convert a video file to MP4 format with the same resolution as input.
     * 
     * @param inputStream The input video stream
     * @param originalFilename The original filename for format detection
     * @return InputStream of the converted MP4 file
     * @throws IOException if conversion fails
     */
    public InputStream convertToMp4(InputStream inputStream, String originalFilename) throws IOException {
        final Path tempInputFile;
        final Path tempOutputFile;

        UUID tempFileId = UUID.randomUUID();
        try {
            String fileExtension = getFileExtension(originalFilename);
            tempInputFile = Files.createTempFile("input_video_" + tempFileId, "." + fileExtension);
            tempOutputFile = Files.createTempFile("output_video_" + tempFileId, ".mp4");
            
            Files.copy(inputStream, tempInputFile, StandardCopyOption.REPLACE_EXISTING);
            
            log.debug("convertToMp4, converting video from {} to MP4: {} -> {}",
                fileExtension, tempInputFile, tempOutputFile);
            
            FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(tempInputFile.toString())
                .overrideOutputFiles(true)
                .addOutput(tempOutputFile.toString())
                .setFormat("mp4")
                .setVideoCodec("libx264")
                .setAudioCodec("aac")
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                .done();
            
            // Execute conversion
            executor.createJob(builder).run();
            
            log.debug("convertToMp4, video conversion completed successfully");
            
            // Return the converted file as InputStream
            return new FileInputStream(tempOutputFile.toFile()) {
                @Override
                public void close() throws IOException {
                    super.close();
                    // Clean up temporary files when stream is closed
                    try {
                        Files.deleteIfExists(tempInputFile);
                        Files.deleteIfExists(tempOutputFile);
                    } catch (IOException e) {
                        log.warn("Failed to clean up temporary files", e);
                    }
                }
            };
            
        } catch (Exception e) {
            log.error("convertToMp4, failed to convert video to MP4", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to convert video to MP4", e);
        }
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
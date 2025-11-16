package core.video;
import org.monte.media.Format;
import org.monte.media.FormatKeys;
import org.monte.media.Registry;
import org.monte.media.math.Rational;
import org.monte.screenrecorder.ScreenRecorder;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import static org.monte.media.FormatKeys.*;
import static org.monte.media.VideoFormatKeys.*;

public class CoreTestRecorderUtil extends ScreenRecorder {
    private final String name;
    /**
     * Custom constructor to allow storing the test case name for dynamic file naming.
     *
     * @param cfg Graphics configuration of the screen.
     * @param captureArea The area of the screen to capture (usually full screen).
     * @param fileFormat The desired output file format (e.g., AVI).
     * @param screenFormat The format for the video stream.
     * @param mouseFormat The format for the mouse pointer visualization.
     * @param audioFormat The format for the audio stream (null for no audio).
     * @param movieFolder The directory where the video file will be saved.
     * @param name The name of the test case, used as the video filename prefix.
     * @throws IOException If an I/O error occurs.
     * @throws AWTException If the platform configuration prevents low-level input control.
     */
    public CoreTestRecorderUtil(GraphicsConfiguration cfg, Rectangle captureArea, Format fileFormat,
                                Format screenFormat, Format mouseFormat, Format audioFormat, File movieFolder, String name)
            throws IOException, AWTException {
        super(cfg, captureArea, fileFormat, screenFormat, mouseFormat, audioFormat, movieFolder);
        this.name = name;
    }

    @Override
    protected File createMovieFile(Format fileFormat) throws IOException {
        if (!movieFolder.exists()) {
            movieFolder.mkdirs();
        } else if (!movieFolder.isDirectory()) {
            throw new IOException("\"" + movieFolder + "\" is not a directory.");
        }
        // This is the correct logic for creating the file path
        return new File(movieFolder,
                name + "." + Registry.getInstance().getExtension(fileFormat));
    }

    /**
     * Factory method to create and configure a new TestRecorder instance with standard settings
     * for screen capture. It captures the entire screen.
     *
     * @param recordedVideoName The base name for the video file (usually the test method name).
     * @param userPath          The directory where the video should be saved.
     * @return A configured {@code TestRecorder} instance.
     * @throws IOException If an I/O error occurs during file setup.
     * @throws AWTException If the platform configuration prevents screen recording.
     */
    public static CoreTestRecorderUtil createConfiguredRecorder(String recordedVideoName, String userPath)
            throws IOException, AWTException {

        File file = new File(userPath);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle captureSize = new Rectangle(0, 0, screenSize.width, screenSize.height);

        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration();
        // Standard Monte Recorder configuration for AVI video capture
        return new CoreTestRecorderUtil(
                gc, captureSize,
                new Format(MediaTypeKey, FormatKeys.MediaType.FILE, MimeTypeKey, MIME_AVI),
                new Format(MediaTypeKey, MediaType.VIDEO, EncodingKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                        CompressorNameKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE, DepthKey, 24, FrameRateKey,
                        Rational.valueOf(15), QualityKey, 1.0f, KeyFrameIntervalKey, 15 * 60),
                new Format(MediaTypeKey, MediaType.VIDEO, EncodingKey, "black", FrameRateKey, Rational.valueOf(30)),
                null,
                file,
                recordedVideoName
        );
    }
}
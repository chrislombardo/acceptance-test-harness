package org.jenkinsci.test.acceptance.recorder;

import org.jenkinsci.test.acceptance.junit.FailureDiagnostics;
import org.jenkinsci.test.acceptance.junit.GlobalRule;
import org.jenkinsci.test.acceptance.utils.SystemEnvironmentVariables;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.monte.media.Format;
import org.monte.media.FormatKeys;
import org.monte.media.VideoFormatKeys;
import org.monte.media.math.Rational;
import org.monte.screenrecorder.ScreenRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.GraphicsEnvironment;
import java.awt.GraphicsConfiguration;
import java.awt.Dimension;
import java.awt.AWTException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * JUnit Rule that before executing a test it starts a recording current screen
 * and after the test is executed, it stops recording.
 */
@GlobalRule
public class TestRecorderRule extends TestWatcher {

    private static final Logger logger = LoggerFactory.getLogger(TestRecorderRule.class);

    private static final int MAX_RECORDING_TIME_SECS = 120000;
    private static final int FRAME_RATE_PER_SEC = 60;
    private static final int BIT_DEPTH = 16;
    private static final float QUALITY_RATIO = 0.97f;

    static final String OFF = "off";

    static final String FAILURES = "failuresOnly";
    static final String ALWAYS = "always";

    private static final String DEFAULT_MODE = FAILURES;

    static String RECORDER_OPTION = SystemEnvironmentVariables
            .getPropertyVariableOrEnvironment("RECORDER", DEFAULT_MODE).trim();

    private FailureDiagnostics diagnostics;
    private JUnitScreenRecorder screenRecorder;

    @Inject
    public TestRecorderRule(FailureDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    @Override
    protected void starting(Description description) {
        if (isRecorderEnabled()) {
            startRecording(description);
        }
    }

    private void startRecording(Description des) {
        try {
            GraphicsConfiguration gc = GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration();

            String mimeType = FormatKeys.MIME_QUICKTIME;
            String videoFormatName = VideoFormatKeys.ENCODING_QUICKTIME_ANIMATION;
            String compressorName = VideoFormatKeys.COMPRESSOR_NAME_QUICKTIME_ANIMATION;
            Dimension outputDimension = gc.getBounds().getSize();
            int bitDepth = BIT_DEPTH;
            float quality = QUALITY_RATIO;
            int screenRate = FRAME_RATE_PER_SEC;

            Format outputFormatForScreenCapture = getOutputFormatForScreenCapture(videoFormatName,
                    compressorName, outputDimension,
                    bitDepth, quality, screenRate);

            this.screenRecorder = new JUnitScreenRecorder
                    (gc, gc.getBounds(), getFileFormat(mimeType),
                            outputFormatForScreenCapture, null, null, diagnostics);
            this.screenRecorder.start();
        } catch (UnsupportedOperationException e) {
            // E.g. java.awt.HeadlessException
            logger.warn("Exception starting test recording {}", e);
        } catch (IOException e) {
            logger.warn("Exception starting test recording {}", e);
        } catch (AWTException e) {
            logger.warn("Exception starting test recording {}", e);
        }
    }

    @Override
    protected void succeeded(Description description) {
        if (this.screenRecorder != null) {
            if (saveAllExecutions()) {
                stopRecordingWithFinalWaiting();
            } else {
                stopRecording();
                this.screenRecorder.removeMovieFile();
            }
        }
    }

    @Override
    protected void finished(Description description) {
        stopRecordingWithFinalWaiting();
    }

    private boolean isRecorderEnabled() {
        return !OFF.equals(RECORDER_OPTION);
    }

    private boolean saveAllExecutions() {
        return ALWAYS.equals(RECORDER_OPTION);
    }

    private void stopRecording() {
        stopRecording(false);
    }

    private void stopRecordingWithFinalWaiting() {
        stopRecording(true);
    }

    private void stopRecording(boolean waitTime) {
        if (this.screenRecorder != null && this.screenRecorder.getState() == ScreenRecorder.State.RECORDING) {
            try {
                if (waitTime) {
                    waitUntilLastFramesAreRecorded();
                }
                screenRecorder.stop();
            } catch (IOException e) {
                logger.warn("Exception stoping test recording {}.", e);
            } catch (InterruptedException e) {
                logger.warn("Exception stoping test recording {}.", e);
            }
        }
    }

    private void waitUntilLastFramesAreRecorded() throws InterruptedException {
        //Values below 500 milliseconds result in no recording last frames.
        TimeUnit.MILLISECONDS.sleep(500);
    }

    private Format getFileFormat(String mimeType) {
        return new Format(FormatKeys.MediaTypeKey, FormatKeys.MediaType.FILE, FormatKeys.MimeTypeKey, mimeType);
    }

    private Format getOutputFormatForScreenCapture(String videoFormatName, String compressorName,
                                                   Dimension outputDimension, int bitDepth, float quality,
                                                   int screenRate) {
        return new Format(FormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, FormatKeys.EncodingKey,
                videoFormatName,
                VideoFormatKeys.CompressorNameKey, compressorName,
                VideoFormatKeys.WidthKey, outputDimension.width,
                VideoFormatKeys.HeightKey, outputDimension.height,
                VideoFormatKeys.DepthKey, bitDepth, FormatKeys.FrameRateKey, Rational.valueOf(screenRate),
                VideoFormatKeys.QualityKey, quality,
                FormatKeys.KeyFrameIntervalKey, screenRate * 5 // one keyframe per 5 seconds
        );
    }
}

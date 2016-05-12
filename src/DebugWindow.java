
import org.opencv.core.Mat;


/**
 * {@link FaceDetector} can access the debug window through this interface.
 */
public interface DebugWindow {
    
    public void update(FaceDetector.Phase phase, float phaseMillis, Mat image, Face[] faces);
    
}

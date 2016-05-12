import org.opencv.core.Mat;


// Note: Design pattern for this is called "Null Object"
/**
 * {@link DebugWindow} that does nothing when its methods are called.
 */
public final class EmptyDebugWindow implements DebugWindow {
    
    public EmptyDebugWindow() {}


    @Override
    public void update(FaceDetector.Phase phase, float phaseMillis, Mat image, Face[] faces) {
        // do nothing  
    }

}

import org.opencv.core.Mat;
import org.opencv.core.Rect;

public class Face {
    
    /**
     * Rectangle (usually square) containing the face.
     */
    Rect faceRect;
    
    /**
     * Grayscale image data for the face, for reuse in detection. (with histogram normalization applied)
     */
    Mat faceData;
    
    
    Face(Rect faceRect, Mat faceData) {
        this.faceRect = faceRect;
        this.faceData = faceData;
    }
}

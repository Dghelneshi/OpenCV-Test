
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;

public class Face implements Cloneable {
    
    /**
     * Rectangle (usually square) containing the face.
     */
    public Rect faceRect;
    
    /**
     * Normalized coordinates of {@link Face#faceRect}. <br>
     * Coordinate system is from bottom left (-1, -1) to top right (1, 1).
     */
    public Point topLeft;
    public Point bottomRight;
    
    /**
     * Center point coordinates of {@link Face#faceRect}. <br>
     * Coordinate system is from bottom left (-1, -1) to top right (1, 1).
     */
    public Point center;
    
    /**
     * Horizontal angle between face center and camera view direction (+ is right).
     */
    public float hAngle;
    
    /**
     * Vertical angle between face center and camera view direction (+ is up).
     */
    public float vAngle;
    
    /**
     * Grayscale image data for the face, only for reuse in detection. (with histogram normalization applied)
     */
    public Mat faceData;
    
    /**
     * Unique ID per face.
     */
    public int faceID;
    
    public int retainedCount;
    
    
    public Face(Rect faceRect, Mat faceData, int id, Camera cam) {
        this.faceRect = faceRect;
        this.faceData = faceData;
        this.faceID = id;
        this.retainedCount = 0;

        topLeft = cam.normalizeCoord(faceRect.tl());
        bottomRight = cam.normalizeCoord(faceRect.br());

        center = new Point((topLeft.x + bottomRight.x) * 0.5, (topLeft.y + bottomRight.y) * 0.5);
        hAngle = cam.hAngle(center.x);
        vAngle = cam.hAngle(center.y);
    }

    public Face(Rect faceRect, Mat faceData, int id, Camera cam, int retainedCount) {
        this(faceRect, faceData, id, cam);
        this.retainedCount = retainedCount;
    }
    
    
    /**
     * Important: Does not clone {@code faceData} (only shallow copy) to save some microseconds!
     */
    @Override
    public Face clone() {
        try {
            Face result = (Face) super.clone();
            result.faceRect = faceRect.clone();
            result.topLeft = topLeft.clone();
            result.bottomRight = bottomRight.clone();
            result.center = center.clone();
            return result;
        }
        catch (CloneNotSupportedException e) {
            assert(false);  // unreachable code
            return null;
        } 
    }
}

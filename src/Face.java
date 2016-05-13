
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
    
    
    public Face(Rect faceRect, Mat faceData, int id) {
        this.faceRect = faceRect;
        this.faceData = faceData;
        this.faceID = id;

        topLeft = FDMath.normalizeCoord(faceRect.tl());
        bottomRight = FDMath.normalizeCoord(faceRect.br());

        center = new Point((topLeft.x + bottomRight.x) * 0.5, (topLeft.y + bottomRight.y) * 0.5);
        hAngle = FDMath.hAngle(center.x);
        vAngle = FDMath.hAngle(center.y);
    }

    private Face(Rect faceRect, Mat faceData, Point topLeft, Point bottomRight, int id) {
        this.faceRect = faceRect;
        this.faceData = faceData;
        this.faceID = id;
        this.topLeft = topLeft;
        this.bottomRight = bottomRight;
        
        center = new Point((topLeft.x + bottomRight.x) * 0.5, (topLeft.y + bottomRight.y) * 0.5);
        hAngle = FDMath.hAngle(center.x);
        vAngle = FDMath.hAngle(center.y);
    }
    
    /** Important: Does not clone {@code faceData} (only shallow copy) to save some microseconds! */
    public Face clone() {
        return new Face(faceRect.clone(), faceData, topLeft.clone(), bottomRight.clone(), faceID);
    }
}

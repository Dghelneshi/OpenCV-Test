
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
     * Grayscale image data for the face, only for reuse in detection. (with histogram normalization applied)
     */
    public Mat faceData;
    
    /**
     * Unique ID per face.
     */
    public int faceID;
    
    
    public Face(Rect faceRect, Mat faceData, int imageWidth, int imageHeight, int id) {
        this.faceRect = faceRect;
        this.faceData = faceData;
        this.faceID = id;
        
        float halfWidth  = imageWidth  * 0.5f;
        float halfHeight = imageHeight * 0.5f;
        
        double x0 = (faceRect.x + 0.5f - halfWidth)  /  halfWidth;
        double y0 = (faceRect.y + 0.5f - halfHeight) / -halfHeight;
        topLeft = new Point(x0,y0);
        
        double x1 = (faceRect.x + faceRect.width  + 0.5f - halfWidth)  /  halfWidth;
        double y1 = (faceRect.y + faceRect.height + 0.5f - halfHeight) / -halfHeight;
        bottomRight = new Point(x1,y1);
        
        center = new Point((x0 + x1) * 0.5, (y0 + y1) * 0.5);
    }
    
    private Face(Rect faceRect, Mat faceData, Point topLeft, Point bottomRight, int id) {
        this.faceRect = faceRect;
        this.faceData = faceData;
        this.faceID = id;
        this.topLeft = topLeft;
        this.bottomRight = bottomRight;
        center = new Point((topLeft.x + bottomRight.x) * 0.5, (topLeft.y + bottomRight.y) * 0.5);
    }
    
    /** Important: Does not clone {@code faceData} (only shallow copy) to save some microseconds! */
    public Face clone() {
        return new Face(faceRect.clone(), faceData, topLeft.clone(), bottomRight.clone(), faceID);
    }
}

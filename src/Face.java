
import org.opencv.core.Mat;
import org.opencv.core.Rect;

public class Face {
    
    /**
     * Rectangle (usually square) containing the face.
     */
    Rect faceRect;
    
    /**
     * Float coordinates of {@link Face#faceRect}. From top left (x0, y0) to bottom right (x1, y1).<br>
     * Coordinate system is from bottom left (-1, -1) to top right (1, 1).
     */
    float x0;
    float y0;
    float x1;
    float y1;
    
    /**
     * Grayscale image data for the face, for reuse in detection. (with histogram normalization applied)
     */
    Mat faceData;
    
    /**
     * Unique ID per face.
     */
    int faceID;
    
    
    Face(Rect faceRect, Mat faceData, int imageWidth, int imageHeight, int id) {
        this.faceRect = faceRect;
        this.faceData = faceData;
        this.faceID = id;
        
        float halfWidth  = imageWidth  * 0.5f;
        float halfHeight = imageHeight * 0.5f;
        x0 = (faceRect.x + 0.5f - halfWidth)  / halfWidth;
        y0 = (faceRect.y + 0.5f - halfHeight) / -halfHeight;
        x1 = (faceRect.x + faceRect.width  + 0.5f - halfWidth)  / halfWidth;
        y1 = (faceRect.y + faceRect.height + 0.5f - halfHeight) / -halfHeight;
    }
    
    Face(Rect faceRect, Mat faceData, float x0, float y0, float x1, float y1, int id) {
        this.faceRect = faceRect;
        this.faceData = faceData;
        this.faceID = id;
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
    }
    
    /** Important: Does not clone {@code faceData} (only shallow copy) to save some microseconds! */
    public Face clone() {
        return new Face(faceRect.clone(), faceData, x0, y0, x1, y1, faceID);
    }
}


import java.util.Arrays;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;


public class Face {
    Rect faceRect;
    Mat faceData;
    List<Rect> eyeRects;
    List<Mat> eyeData;
    
    Face(Rect faceRect, Mat faceData, List<Rect> eyeRects, List<Mat> eyeData) {
        this.faceRect = faceRect;
        this.faceData = faceData;
        this.eyeRects = eyeRects;
        this.eyeData  = eyeData;
    }
    
    Face(Rect faceRect, Mat faceData, Rect[] eyeRects, Mat[] eyeData) {
        this.faceRect = faceRect;
        this.faceData = faceData;
        this.eyeRects = Arrays.asList(eyeRects);
        this.eyeData  = Arrays.asList(eyeData);
    }
    
    Face(Rect faceRect, Mat faceData, Rect[] eyeRects, List<Mat> eyeData) {
        this.faceRect = faceRect;
        this.faceData = faceData;
        this.eyeRects = Arrays.asList(eyeRects);
        this.eyeData  = eyeData;
    }
    
    Face(Rect faceRect, Mat faceData, List<Rect> eyeRects, Mat[] eyeData) {
        this.faceRect = faceRect;
        this.faceData = faceData;
        this.eyeRects = eyeRects;
        this.eyeData  = Arrays.asList(eyeData);
    }
    
    Face(Rect faceRect, Mat faceData, MatOfRect eyeRects, Mat[] eyeData) {
        this.faceRect = faceRect;
        this.faceData = faceData;
        this.eyeRects = eyeRects.toList();
        this.eyeData  = Arrays.asList(eyeData);
    }
    
    Face(Rect faceRect, Mat faceData, MatOfRect eyeRects, List<Mat> eyeData) {
        this.faceRect = faceRect;
        this.faceData = faceData;
        this.eyeRects = eyeRects.toList();
        this.eyeData  = eyeData;
    }
}

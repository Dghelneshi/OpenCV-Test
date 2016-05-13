import org.opencv.core.Core;


public class Main {

    public static void main(String[] args) {
        
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // load OpenCV library
        
        new FDMath(60, 45, 640, 480); // TODO: get these values from somewhere.
        
        FaceDetector fd = new FaceDetector(0, 2);
        new Thread(fd, "Face Detector Thread").start();
        
        DebugWindow window = new Imshow("Debug Window", fd);
    }
}

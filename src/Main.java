import org.opencv.core.Core;


public class Main {

    public static boolean isRunning = true;


    public static void main(String[] args) {
        
        
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // load OpenCV library
        
        FaceDetector fd = new FaceDetector(0, 2);
        
        DebugWindow window = new Imshow("Debug Window", fd);
        
        fd.setDebugWindow(window);
        
        new Thread(fd, "Face Detector Thread").start();
    }
}

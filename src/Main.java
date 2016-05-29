import org.opencv.core.Core;

public class Main {

    public static void main(String[] args) {

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // load OpenCV library

        FaceDetector fd = new FaceDetector(0);
        new Thread(fd, "Face Detector Thread").start();

        //DebugWindow window = new Imshow("Debug Window", fd);
        DebugWindow window = new DebugingWindow("Debug Window", fd);
    }

}

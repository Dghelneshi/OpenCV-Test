import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

public class Main {

    public static boolean isRunning = true;


    
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        Imshow im = new Imshow("Video Preview");

        im.Window.setResizable(true);
        
        Mat m = new Mat();
        VideoCapture vcam = new VideoCapture(0);
        
        im.Window.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                isRunning = false;
                vcam.release(); // required to let java exit properly
            }
        });

        // loop until VideoCamera is Available
        while (vcam.isOpened() == false)
            ;

        // Bug Fix: Loop until initial image frames are not empty
        while (m.empty()) {
            vcam.read(m);
        }

        while (isRunning) {

            vcam.read(m);

            // System.out.println(m.dump());
            im.showImage(m);

        }
        System.exit(0);
    }
}

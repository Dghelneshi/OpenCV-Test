import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Point;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class Camera extends VideoCapture {

    private float half_hFov;
    private float half_vFov;

    private double halfWidth;
    private double halfHeight;
    
    private int cameraIndex;

    public Camera(int cameraIndex, float hFov, float vFov) {
        super(cameraIndex);
        
        this.cameraIndex = cameraIndex;

        double imageWidth = get(Videoio.CAP_PROP_FRAME_WIDTH);
        double imageHeight = get(Videoio.CAP_PROP_FRAME_HEIGHT);

        if (imageWidth > 0.0 && imageHeight > 0.0) {
            setImageSize(imageWidth, imageHeight);
        }
        else {
            // TODO: what to do? could set boolean and check in face detector, but makes code uglier
            throw new AssertionError("ERROR: Could not get image size from camera");
        }

        setFov(hFov, vFov);
    }

    public Point normalizeCoord(Point pixelCoord) {
        double x0 = (pixelCoord.x + 0.5f - halfWidth) / halfWidth;
        double y0 = (pixelCoord.y + 0.5f - halfHeight) / -halfHeight;
        return new Point(x0, y0);
    }

    public float hAngle(double x) {
        return (float) (x * half_hFov);
    }

    public float vAngle(double y) {
        return (float) (y * half_vFov);
    }

    public void setFov(float hFov, float vFov) {
        half_hFov = hFov * 0.5f;
        half_vFov = vFov * 0.5f;
    }

    public void setImageSize(double imageWidth, double imageHeight) {
        halfWidth = imageWidth * 0.5;
        halfHeight = imageHeight * 0.5;
    }

    /**
     * Gets the real camera index.
     */
    public int getRealIndex() {
        return cameraIndex;
    }
    
    /**
     * Brute force searches all possible camera ids, puts the camera indices in the list.
     * 
     * @param camIndexList
     *          the list to fill with camera indices. is emptied before adding!
     * @return
     *          the number of cameras
     */
    public static int enumerateCameras(List<Integer> camIndexList) {

        int numCameras = 0;
        camIndexList.clear();

        ArrayList<Integer> drivers = new ArrayList<>(16);
        drivers.add(Videoio.CAP_VFW);
        drivers.add(Videoio.CAP_FIREWARE);
        drivers.add(Videoio.CAP_QT);
        drivers.add(Videoio.CAP_UNICAP);
        drivers.add(Videoio.CAP_DSHOW);
        drivers.add(Videoio.CV_CAP_MSMF);
        drivers.add(Videoio.CAP_PVAPI);
        drivers.add(Videoio.CAP_OPENNI);
        drivers.add(Videoio.CAP_OPENNI_ASUS);
        drivers.add(Videoio.CV_CAP_ANDROID);
        drivers.add(Videoio.CV_CAP_ANDROID_BACK);
        drivers.add(Videoio.CV_CAP_ANDROID_FRONT);
        drivers.add(Videoio.CV_CAP_XIAPI);
        drivers.add(Videoio.CV_CAP_AVFOUNDATION);
        drivers.add(Videoio.CV_CAP_GIGANETIX);
        drivers.add(Videoio.CAP_INTELPERC);
        
        for (int driver : drivers) {

            int maxID = 100; // 100 IDs per driver
            if (driver == Videoio.CAP_VFW)
                maxID = 10; // VFW opens same camera after 10
            else if (driver == Videoio.CV_CAP_ANDROID)
                maxID = 98; // 98 and 99 are front and back cam
            else if ((driver == Videoio.CV_CAP_ANDROID_FRONT) || (driver == Videoio.CV_CAP_ANDROID_BACK))
                maxID = 1;

            for (int id = 0; id < maxID; id++) {
                int index = driver + id;

                VideoCapture cam = new VideoCapture(index);

                if (cam.isOpened()) {
                    camIndexList.add(index);
                    numCameras++;
//                    System.out.println("camera at " + index);
                }
                cam.release();
            }
        }
        
        return numCameras;
    }
}

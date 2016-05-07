import java.util.ArrayList;
import java.util.List;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;

/* TODO(Dgh):
 *  - look up parameters on detectMultiScale (rejectLevel, etc)
 *  - implement a very simple state machine to handle different detection phases
 *  - refactor out of main() and into a separate class
 *  - try hybrid tracking approach to give reliable IDs to faces
 *      a) use detection inside tracked rect + tolerance, then detect new faces *only outside that rect* (problems with overlap for profiles)
 *      b) track first, then detect, then try to match detected faces to tracked faces (how to handle each possible case?)
 *      c) combine a+b, use tracking as strong hint for detection (change detect parameters for tracked area to be more lenient)      
 *      d) detect first, then match with last known faces (center position and/or template match)
 *      e) always track in addition to detect, only remove tracking completely if rect touches image border
 *       \- need to test cases where tracking *should* be lost, maybe decay maximum valid diff over time?
 *       \- possibly too complicated to account for every edge case with this method 
 *       
 *  - detect faces over 2-3 frames, choose only those which are in all frames
 *  - find a method for handling people moving out of camera fov while they are being tracked (re-detect on hitting edge?)
 *  - determine tracking roi size using distance estimation + max angular velocity 
 *  - provide a simple 3D velocity vector for faces across some time span (500ms?), handle Z-axis via size changes (math!)
 *   \- time span needs to be at least as long as tracking phase for Z-axis to work! (synchronize?)
 *  - refactor program into smaller, specialized functions (detection of arbitrary object? readability/maintainability benefit?)
 *  - implement threadsafe access to List<Face> (offer copy? direct modification in synchronized sub-functions?)
 *   \- think about threading architecture in general, look at what Java offers (Runnables?) 
 *  - fine-tune parameters according to available camera
 *  - try out more classifiers
 *  - look at possibilities for face recognition/biometrics (performance, accuracy, need face database?)
 */

public class Main {

    public static boolean isRunning = true;

    public static Imshow im;
    public static VideoCapture vcam;

    public static int cameraIndex = 0;
    public static int numCameras = 2;

    private static int desiredFrameRate = 30;
    private static float desiredFrameTime = (1000.0f / desiredFrameRate);

    /**
     * Specifies how exact the match of a face between two frames has to be, smaller is more exact.
     */
    public static double matchTolerance = 0.1;
    
    

    public static void main(String[] args) {

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // setup window
        im = new Imshow("Video Preview");

        im.window.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                isRunning = false;
                vcam.release(); // required to let java exit properly
            }
        });

        im.window.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                // click to change camera (e.g. from webcam to virtual camera, for testing)
                vcam.release();
                cameraIndex++;
                cameraIndex = cameraIndex % numCameras;
                vcam = new VideoCapture(cameraIndex);
            }
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });

        // setup video capture
        vcam = new VideoCapture(cameraIndex);

        if (vcam.isOpened() == false) {
            // something went wrong opening the camera, just exit for now
            System.exit(1);
        }

        Mat image = new Mat();
        Mat grayImage = new Mat();

        // try reading from camera until image frames are not empty
        while (image.empty() && isRunning) {
            vcam.read(image);
        }

        CascadeClassifier faceCascade = new CascadeClassifier("data/haarcascades/haarcascade_frontalface_alt.xml");

        int minFaceSize = Math.round(image.rows() * 0.1f); // faces cannot be smaller than 10% of frame height
        List<Face> faces = new ArrayList<Face>();

        int frameCounter = 0;
        long startTime = System.nanoTime();

        while (isRunning) {

            if (vcam.isOpened()) {

                vcam.read(image);

                // convert to grayscale and equalize histogram for further processing
                Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
                Imgproc.equalizeHist(grayImage, grayImage);

                long detectStartTime = System.nanoTime();

                if (faces.isEmpty()) { // we haven't detected anything yet
                    detectFaces(grayImage, faces, faceCascade, minFaceSize);
                }
                else if ((frameCounter & 31) == 0) { // periodically reset faces, need something more sophisticated later on
                    faces.clear();
                    detectFaces(grayImage, faces, faceCascade, minFaceSize);
                }
                else { // we have some faces we can track cheaply
                    trackFaces(grayImage, faces);
                }

                // measure time spent on detection, display in window title
                long detectEndTime = System.nanoTime();
                float detectMillis = ((detectEndTime - detectStartTime) / 1000000.0f);
                im.window.setTitle("Camera: " + cameraIndex + " - Detection/Tracking: " + detectMillis + "ms");

                drawDebugRectangles(image, faces);
                
                im.showImage(image);

                frameCounter++;

                // measure total time spent
                long endTime = System.nanoTime();
                float totalMillis = ((endTime - startTime) / 1000000.0f);

                // update at desired framerate
                try {
                    float timeRemaining = (desiredFrameTime - totalMillis);
                    long sleepMillis = (long) timeRemaining;
                    int sleepNanos = (int) (1000000 * (timeRemaining - sleepMillis));
                    
                    if ((sleepMillis > 0) && (sleepNanos > 0)) {
                        Thread.sleep(sleepMillis, sleepNanos);
                    }
                }
                catch (InterruptedException e1) {}
            }
            startTime = System.nanoTime();
        }
        vcam.release();
    }

    /**
     * Tracks faces by matching the data contained in {@code faceList} to a new image in {@code grayImage}. Draws yellow
     * rectangles around their positions in {@code image}.<br>
     * Make sure that {@code grayImage} is in grayscale and has a histogram equalization applied.
     * 
     * @param grayImage
     *            {@code image} converted to grayscale, with histogram equalization applied
     * @param faceList
     */
    public static void trackFaces(Mat grayImage, List<Face> faceList) {

        int rectExpand = grayImage.cols() / 18; // TODO: find good name for divisor constant, pull out as member field

        for (int i = 0; i < faceList.size(); i++) {

            Face f = faceList.get(i);

            // make a slightly larger rect than the face (region of interest)
            Rect roi = f.faceRect.clone();
            roi.x = Math.max(roi.x - rectExpand, 0);
            roi.y = Math.max(roi.y - rectExpand, 0);
            roi.width  = Math.min(roi.width  + rectExpand * 2, grayImage.width()  - roi.x);
            roi.height = Math.min(roi.height + rectExpand * 2, grayImage.height() - roi.y);

            // try to match the old face data in the new image, inside the region of interest
            Mat faceMat = f.faceData;
            Mat matchMatrix = new Mat();
            Imgproc.matchTemplate(grayImage.submat(roi), faceMat, matchMatrix, Imgproc.TM_SQDIFF_NORMED);

            // get best match location
            MinMaxLocResult minMax = Core.minMaxLoc(matchMatrix);

            // likely match (use minVal for TM_SQDIFF_NORMED, maxVal and (1.0 - trackingTolerance) for other methods)
            if (minMax.minVal <= matchTolerance) {

                // update face in list with new values
                f.faceRect.x = (int) (roi.x + minMax.minLoc.x);
                f.faceRect.y = (int) (roi.y + minMax.minLoc.y);
                f.faceData = grayImage.submat(f.faceRect).clone();
            }
            else {
                // not matched, remove face from active list
                faceList.remove(i);
                i--;
            }
        }
    }

    /**
     * Detects faces in {@code grayImage} and adds them to {@code faceList}.<br>
     * Make sure that {@code grayImage} is in grayscale and has a histogram equalization applied.
     * 
     * @param grayImage
     *            image converted to grayscale, with histogram equalization applied
     * @param faceList
     * @param faceCascade
     * @param minFaceSize
     *            faces cannot be smaller than this size in pixels (width or height)
     */
    public static void detectFaces(Mat grayImage, List<Face> faceList, CascadeClassifier faceCascade, int minFaceSize) {

        MatOfRect faceRects = new MatOfRect();

        // detect faces in grayscale image
        faceCascade.detectMultiScale(grayImage, faceRects, 1.1, 3, Objdetect.CASCADE_SCALE_IMAGE, new Size(minFaceSize, minFaceSize), new Size());

        Rect[] faceRectArray = faceRects.toArray();
        for (Rect rect : faceRectArray) {

            // get pixel data for face
            Mat faceMat = grayImage.submat(rect).clone();

            // add detected face to our list
            Face f = new Face(rect, faceMat);
            faceList.add(f);
        }
    }
    
    /**
     * Draws green rectangles around faces.
     * @param image
     *          image to draw to
     * @param faceList
     *          list of faces
     */
    public static void drawDebugRectangles(Mat image, List<Face> faceList) {
        for (Face face : faceList) {
            Point faceTl = face.faceRect.tl();
            Point faceBr = face.faceRect.br();
            Imgproc.rectangle(image, faceTl, faceBr, new Scalar(0, 255, 0, 255), 2);
        }
    }
}

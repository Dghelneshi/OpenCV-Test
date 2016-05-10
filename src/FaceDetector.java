import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;

/* TODO(Dgh):
 *  - put some of these up as git issues
 *  - look up parameters on detectMultiScale (rejectLevel, etc)
 *  - implement a very simple state machine to handle different detection phases
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
 *  - check program behavior with different threads ending in different orders is fine (e.g. FD thread stops before window is closed)
 *  - maybe determine tracking roi size using distance estimation + max angular velocity 
 *  - maybe provide a 3D velocity vector for faces across some time span (500ms?), handle Z-axis via size changes (math!)
 *   \- time span needs to be at least as long as tracking phase for Z-axis to work! (synchronize?)
 *  - use messages from the main part of the project as signals to start/stop threads or open a debug window
 *  - fine-tune parameters according to available camera
 *  - try out more classifiers
 *  - look at possibilities for face recognition/biometrics (performance, accuracy, need face database?)
 */

public class FaceDetector implements Runnable {

    private boolean isRunning = false;

    private DebugWindow debugWindow = new EmptyDebugWindow();
    
    private VideoCapture vcam;
    
    private List<Face> faces = new ArrayList<Face>();

    private static int faceCounter = 0; // used to give unique IDs to faces

    private int cameraIndex = 0;
    private int numCameras = 1;

    private int desiredFrameRate = 30;
    private float desiredFrameTime = (1000.0f / desiredFrameRate);

    /**
     * Specifies how exact the match of a face between two frames has to be.
     * Smaller value is more accurate, but may cause the program to lose tracking with fast head movements.
     */
    private double matchTolerance = 0.1;
    
    
    public FaceDetector() {}
    
    public FaceDetector(int cameraIndex, int numCameras) {
        this.cameraIndex = cameraIndex % numCameras;
    }
    
    public void run() {
        
        isRunning = true;
        
        vcam = new VideoCapture(cameraIndex);
        
        if (vcam.isOpened() == false) {
            // something went wrong opening the camera, just stop the thread for now
            stop();
        }

        Mat image = new Mat();
        Mat grayImage = new Mat();

        CascadeClassifier faceCascade = new CascadeClassifier("data/haarcascades/haarcascade_frontalface_alt.xml");

        int minFaceSize = Math.round(image.rows() * 0.1f); // faces cannot be smaller than 10% of frame height
      
        long frameCounter = 0;
        float redetectTimer = 0; // TODO: make constant, check if implementation is reasonable
        long oldTime = 0;
        long startTime = System.nanoTime();
        
        while (isRunning) {

            synchronized(vcam) {
                vcam.read(image);
            }

            // wait if camera doesn't provide an image
            if (image.empty()) {
            
                debugWindow.setTitle("Camera: " + cameraIndex + " - waiting for camera...");
                
                try {
                    Thread.sleep(33);
                }
                catch (InterruptedException e) {}
                
                continue;
            }
            
            // convert to grayscale and equalize histogram for further processing
            Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
            Imgproc.equalizeHist(grayImage, grayImage);

            ////////////////////
            // FACE DETECTION //
            ////////////////////
            
            long detectStartTime = System.nanoTime();
                        
            if (faces.isEmpty()) { // we haven't detected anything yet
                detectFaces(grayImage, faces, faceCascade, minFaceSize);
            }
            else if (redetectTimer >= 500) { // periodically reset faces
                redetectTimer -= 500;
                synchronized(faces) {
                    faces.clear();
                }
                detectFaces(grayImage, faces, faceCascade, minFaceSize);
            }
            else { // we have some faces we can track cheaply
                trackFaces(grayImage, faces);
            }

            long detectEndTime = System.nanoTime();
            float detectMillis = deltaMillis(detectStartTime, detectEndTime);
            
            ////////////////////////
            // END FACE DETECTION //
            ////////////////////////
            
            debugWindow.setTitle("Camera: " + cameraIndex + " - Detection/Tracking: " + detectMillis + "ms");
            debugWindow.drawDebugRectangles(image, faces);        
            debugWindow.showImage(image);
            
            frameCounter++;

            // measure total busy time spent
            long endTime = System.nanoTime();
            float totalMillis = deltaMillis(startTime, endTime);

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
            
            oldTime = startTime;
            startTime = System.nanoTime();
            redetectTimer += deltaMillis(oldTime, startTime);
        }
        stop(); // required to let java exit properly
    }
    
    /**
     * @param t1
     *      time value in nanoseconds
     * @param t2
     *      time value in nanoseconds
     * @return 
     *      milliseconds between {@code tStart} and {@code tEnd}
     */
    private float deltaMillis(long tStart, long tEnd) {
        return (tEnd - tStart) / 1000000.0f;
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
    private void trackFaces(Mat grayImage, List<Face> faceList) {

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

            // likely match (use minVal for TM_SQDIFF_NORMED, maxVal and (1.0 - matchTolerance) for other methods)
            if (minMax.minVal <= matchTolerance) {

                // update face in list with new values
                synchronized(faceList) {
                    
                    int newX = (int) (roi.x + minMax.minLoc.x);
                    int newY = (int) (roi.y + minMax.minLoc.y);
                    Rect newRect = new Rect(newX, newY, f.faceRect.width, f.faceRect.height);
                    Mat newFaceData = grayImage.submat(newRect).clone();
                    
                    Face newFace = new Face(newRect, newFaceData, grayImage.width(), grayImage.height(), f.faceID);
                    
                    faceList.set(i, newFace);
                }
            }
            else {
                // not matched, remove face from active list
                synchronized(faceList) {
                    faceList.remove(i);
                    i--;
                }
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
    private void detectFaces(Mat grayImage, List<Face> faceList, CascadeClassifier faceCascade, int minFaceSize) {

        MatOfRect faceRects = new MatOfRect();

        // detect faces in grayscale image
        faceCascade.detectMultiScale(grayImage, faceRects, 1.1, 3, Objdetect.CASCADE_SCALE_IMAGE, new Size(minFaceSize, minFaceSize), new Size());

        Rect[] faceRectArray = faceRects.toArray();
        for (Rect rect : faceRectArray) {

            // get pixel data for face
            Mat faceMat = grayImage.submat(rect).clone();

            // add detected face to our list
            Face f = new Face(rect, faceMat, grayImage.width(), grayImage.height(), faceCounter++);
            
            synchronized(faceList) {
                faceList.add(f);
            }
        }
    }
    
    
    public Face[] getFaces() {
        
        // could also make a new copy every frame and just offer that in case we have many consumers
        synchronized(faces) {
            Face array[] = new Face[faces.size()];
            for (int i = 0; i < array.length; i++) {
                array[i] = faces.get(i).clone();
            }
            return array;
        }
    }
    
    public synchronized boolean isRunning() {
        return isRunning;
    }

    public synchronized void stop() {
        isRunning = false;
        
        if (vcam != null) { // can only happen if run() was never called. not sure if we want to even support that?
            synchronized(vcam) {
                vcam.release(); // required to let java exit properly
            }
        }
    }

    public int getDesiredFrameRate() {
        return desiredFrameRate;
    }

    public void setDesiredFrameRate(int desiredFrameRate) {
        this.desiredFrameRate = desiredFrameRate;
        this.desiredFrameTime = (1000.0f / desiredFrameRate);
    }

    public double getMatchTolerance() {
        return matchTolerance;
    }

    /**
     * {@code matchTolerance} specifies how exact the match of a face between two frames has to be,
     * smaller is more accurate, but may cause the program to lose tracking with fast head movements.
     */
    public void setMatchTolerance(double matchTolerance) {
        this.matchTolerance = matchTolerance;
    }

    public int getCameraIndex() {
        return cameraIndex;
    }

    public void setCameraIndex(int i) {
        synchronized(vcam) {
            vcam.release();
            
            i = i % numCameras;
            this.cameraIndex = i;
            vcam = new VideoCapture(cameraIndex);
            
            synchronized(faces) {
                faces.clear();
            }
        }
    }
    
    public void setDebugWindow(DebugWindow window) {
        // TODO: maybe handle disposing of old window or remove listeners? synchronize all DebugWindow methods?
        this.debugWindow = window;
    }
    
    public DebugWindow getDebugWindow() {
        return debugWindow;
    }

    public int getNumCameras() {
        return numCameras;
    }

    public void setNumCameras(int numCameras) {
        this.numCameras = numCameras;
    }

}

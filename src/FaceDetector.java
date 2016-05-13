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
 *  - FIXME: Track down bug with threads not exiting! Seems to only happen if started without debugger attached -_-
 *  - FIXME: just setting nextPhase directly in cycleCameras is not thread safe
 *  - get camera fov and image size from somewhere, don't start until we have it
 *   \- not sure how to handle/support camera change? maybe make values possible to edit at runtime and save to config file
 *  - put some of these up as git issues
 *  - try hybrid tracking approach to give reliable IDs to faces
 *      a) use detection inside tracked rect + tolerance, then detect new faces *only outside that rect* (problems with overlap for profiles)
 *      b) track first, then detect, then try to match detected faces to tracked faces (how to handle each possible case?)
 *      c) combine a+b, use tracking as strong hint for detection (change detect parameters for tracked area to be more lenient)      
 *      d) detect first, then match with last known faces (center position and/or template match)
 *      e) always track in addition to detect, only remove tracking completely if rect touches image border
 *       \- need to test cases where tracking *should* be lost, maybe decay maximum valid diff over time?
 *       \- possibly too complicated to account for every edge case with this method 
 *  
 *  - detect faces over 2-3 frames, choose only those which are in all frames (use groupRectangles())
 *  - find a method for handling people moving out of camera fov while they are being tracked (re-detect on hitting edge?)
 *  - maybe separate Face datastructure for internal (detection) and external use, copy over relevant data each frame
 *   \- check every data type. do we really need doubles?
 *  - more perf statistics in debug window
 *  - check program behavior with different threads ending in different orders is fine (e.g. FD thread stops before window is closed)
 *   \- window listeners need to be removed, but where?
 *  - try to make update() run in the window's thread to minimize performance impact of UI
 *  - maybe determine tracking roi size using distance estimation + max angular velocity 
 *  - maybe provide a 3D velocity vector for faces across some time span (500ms?), handle Z-axis via size changes (math!)
 *   \- time span needs to be at least as long as tracking phase for Z-axis to work! (synchronize?)
 *  - use messages from the main part of the project as signals to start/stop threads or open a debug window
 *  - ask the operating system how many cameras are available instead of manual numCameras crap!
 *  - test what happens in hard error cases like disconnecting a camera mid operation
 *  - fine-tune parameters according to available camera
 *  - try out more classifiers
 *  - look at possibilities for face recognition/biometrics (performance, accuracy, need face database?)
 */

public class FaceDetector implements Runnable {

    public static enum Phase {
        FD_PHASE_INIT,
        FD_PHASE_WAIT,
        FD_PHASE_DETECT,
        FD_PHASE_TRACK,
    }
    
    private boolean isRunning = false;

    private Phase activePhase = Phase.FD_PHASE_INIT;
    private Phase nextPhase = Phase.FD_PHASE_DETECT;
    
    private DebugWindow debugWindow = new EmptyDebugWindow();
    
    private VideoCapture vcam;
    
    private final List<Face> faces = new ArrayList<Face>();

    private static int faceCounter = 0; // used to give unique IDs to faces

    private int cameraIndex = 0;
    
    /**
     * The number of cameras available to the system (there is currently no system in place to ask the operating system
     * to list the currently available cameras).
     */
    private int numCameras = 1;

    private int desiredFrameRate = 30;
    private float desiredFrameTime = (1000.0f / desiredFrameRate);

    
    //////////////////////////
    // DETECTION PARAMETERS //
    //////////////////////////
    
    /**
     * Specifies how exact the match of a face between two frames has to be.
     * Smaller value is more accurate, but may cause the program to lose tracking with fast head movements.<br>
     * Recommended values are from 0.05 to 0.2
     */
    private double matchTolerance = 0.1;
    
    /**
     * This influences the size of the rectangle to search for a matching face betwen two frames. Smaller value is slower,
     * but allows for larger movement between frames. May cause strange behavior if too small.<br>
     * Recommended values are from 32 to 8
     */
    private int matchRectExpandDivisor = 18; // TODO: This variable name is too long and confusing!
    
    /**
     * Specifies how much the image is scaled in each stage of face detection.
     * Smaller value is more accurate, but slower. <b>Must be greater than 1.0</b> <br>
     * Recommended values are between 1.05 and 1.6
     */
    private double detectScaleFactor = 1.1;
    
    /**
     * Specifies how many neighboring faces should be detected in a candidate area before it is considered an actual face.
     * Larger value avoids false positives, but may reject some actual faces.<br>
     * Recommended values are from 2 to 6
     */
    private int detectMinNeighbors = 3;
    
    /**
     * Specifies the minimum face size (width or height) for detection in pixels. Smaller values are slower, but can 
     * detect faces further away from the camera. However, this may also cause more detection of random noise as faces.<br>
     * Recommended values are between 10% and 25% of the frame height.
     */
    private int detectMinFaceSize = 64;
    
    
    //////////////////////////////
    // END DETECTION PARAMETERS //
    //////////////////////////////
    
    
    public FaceDetector() {}
    
    /**
     * @param cameraIndex
     * @param numCameras
     *      see {@link #numCameras}
     */
    public FaceDetector(int cameraIndex, int numCameras) {
        this.numCameras = numCameras;
        this.cameraIndex = cameraIndex % numCameras;
    }
    
    
    public void run() {
        
        isRunning = true;
        
        vcam = new VideoCapture(cameraIndex);

        Mat image = new Mat();
        Mat grayImage = new Mat();

        CascadeClassifier faceCascade = new CascadeClassifier("data/haarcascades/haarcascade_frontalface_alt.xml");

        float redetectTimer = 0; // TODO: make constant, check if implementation is reasonable
        long oldTime = 0;
        long startTime = System.nanoTime();
        
        while (isRunning) {

            synchronized(this) {
                vcam.read(image);
            }
            
            // TODO: only do this once per camera init (use phases once the system is more finished)
            FDMath.setImageSize(image.width(), image.height());

            // wait if camera doesn't provide an image
            if (image.empty() || !vcam.isOpened()) {
                handleError(image);
                continue;
            }
            
            ////////////////////
            // FACE DETECTION //
            ////////////////////
            
            long phaseStartTime = System.nanoTime();
            
            // convert to grayscale and equalize histogram for further processing
            Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
            Imgproc.equalizeHist(grayImage, grayImage);

            switch(activePhase) {
            
            case FD_PHASE_INIT: {
                faces.clear();
                detectFaces(grayImage, faces, faceCascade, detectMinFaceSize);
                nextPhase = Phase.FD_PHASE_TRACK;
            } break;

            
            case FD_PHASE_DETECT: {
                detectFaces(grayImage, faces, faceCascade, detectMinFaceSize);
                nextPhase = Phase.FD_PHASE_TRACK;
            } break;

            
            case FD_PHASE_TRACK: {
                trackFaces(grayImage, faces);
                
                nextPhase = Phase.FD_PHASE_TRACK; // usually continue tracking
                
                if (redetectTimer >= 500) { // tracking phase can last a maximum of 500ms
                    redetectTimer -= 500;
                    nextPhase = Phase.FD_PHASE_INIT;
                }
            } break;

            
            case FD_PHASE_WAIT: {
                nextPhase = Phase.FD_PHASE_DETECT;
            } break;
            }
            
            
            long phaseEndTime = System.nanoTime();
            float phaseMillis = deltaMillis(phaseStartTime, phaseEndTime);
            
            ////////////////////////
            // END FACE DETECTION //
            ////////////////////////
            
            // update the debug window
            Face[] facesCopy = makeFaceListCopy();
            debugWindow.update(activePhase, phaseMillis, image, facesCopy);
            
            
            // measure total busy time spent
            long endTime = System.nanoTime();
            float totalMillis = deltaMillis(startTime, endTime);

            // update at desired framerate
            sleepUntilNextUpdate(totalMillis);
            
            oldTime = startTime;
            startTime = System.nanoTime();
            redetectTimer += deltaMillis(oldTime, startTime);
            activePhase = nextPhase;
        }
        stop(); // required to let java exit properly
    }

    /**
     * Handle error case if camera doesn't provide a proper image.<br>
     * Currently just updates debug window and thenn sleeps until the next update period.
     */
    private void handleError(Mat image) {
        long phaseStartTime = System.nanoTime();
        
        activePhase = Phase.FD_PHASE_WAIT;
        
        debugWindow.update(activePhase, desiredFrameTime, image, makeFaceListCopy());
        
        long phaseEndTime = System.nanoTime();
        float totalMillis = deltaMillis(phaseStartTime, phaseEndTime);
        
        sleepUntilNextUpdate(totalMillis);
        
        activePhase = nextPhase;
    }

    /**
     * Sleeps until the next update is due according to {@link #desiredFrameTime}. <br>
     * I.e. it will sleep for approximately {@code desiredFrameTime - totalMillis} milliseconds.
     * 
     * @param totalMillis
     *          time spent in this update period so far
     */
    private void sleepUntilNextUpdate(float totalMillis) {
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
    
    /**
     * @param tStart
     *      time value in nanoseconds
     * @param tEnd
     *      time value in nanoseconds
     * @return 
     *      milliseconds between {@code tStart} and {@code tEnd}
     */
    public static float deltaMillis(long tStart, long tEnd) {
        return (tEnd - tStart) / 1000000.0f;
    }

    /**
     * Tracks faces by matching the old data contained in {@code faceList} to a new image in {@code grayImage}.<br>
     * Make sure that {@code grayImage} is in grayscale and has a histogram equalization applied.
     * 
     * @param grayImage
     *            {@code image} converted to grayscale, with histogram equalization applied
     * @param faceList
     */
    private void trackFaces(Mat grayImage, List<Face> faceList) {
       
        // each face rect will be expanded by this amount of pixels in each direction
        int rectExpand = grayImage.width() / matchRectExpandDivisor;

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

            // likely match (use minVal for TM_SQDIFF_NORMED, maxVal > (1.0 - matchTolerance) for other methods)
            if (minMax.minVal <= matchTolerance) {

                // update face in list with new values

                int newX = (int) (roi.x + minMax.minLoc.x);
                int newY = (int) (roi.y + minMax.minLoc.y);
                Rect newRect = new Rect(newX, newY, f.faceRect.width, f.faceRect.height);
                Mat newFaceData = grayImage.submat(newRect).clone();
                
                Face newFace = new Face(newRect, newFaceData, f.faceID);
                
                faceList.set(i, newFace);

            }
            else {
                // not matched, remove face from active list
                faceList.remove(i);
                i--;
            }
        }
    }

    /**
     * Detects faces in {@code grayImage} using {@code faceCascade} and adds them to {@code faceList}.<br>
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
        faceCascade.detectMultiScale(grayImage, faceRects, detectScaleFactor, detectMinNeighbors, 
                                     Objdetect.CASCADE_SCALE_IMAGE, new Size(minFaceSize,minFaceSize), new Size());

        Rect[] faceRectArray = faceRects.toArray();
        for (Rect rect : faceRectArray) {

            // get pixel data for face
            Mat faceMat = grayImage.submat(rect).clone();

            // add detected face to our list
            Face f = new Face(rect, faceMat, faceCounter++);
            
            faceList.add(f);
        }
    }
    
    
    /**
     * Makes a copy of the current available face data.
     */
    private Face[] makeFaceListCopy() {
        
        // IMPORTANT: Do not make this method public, never call it from another thread.
        //            Will cause weird race condition bugs otherwise.
        
        Face array[] = new Face[faces.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = faces.get(i).clone();
        }
        return array;
    }
    
    public synchronized boolean isRunning() {
        return isRunning;
    }

    /**
     * Stops this thread, releases the camera. Currently <b>not</b> supported to restart thread again.
     */
    public synchronized void stop() {
        isRunning = false;
        
        removeDebugWindow(); // just in case someone is irresponsible!
        
        if (vcam != null) { // can only happen if run() was never called. not sure if we want to even support that?
            synchronized(this) {
                vcam.release(); // required to let java exit properly
            }
        }
    }

    /**
     * This thread will never update faster than the desired frame rate dictates.
     */
    public void setDesiredFrameRate(int desiredFrameRate) {
        this.desiredFrameRate = desiredFrameRate;
        this.desiredFrameTime = (1000.0f / desiredFrameRate);
    }

    /**
     * @see #setDesiredFrameRate(int)
     */
    public int getDesiredFrameRate() {
        return desiredFrameRate;
    }

    /**
     * Cycles through all available cameras.<br>
     * Uses {@link #numCameras} to determine how many cameras to cycle through (there is currently no system in place to
     * ask the operating system to list the currently available cameras).
     * 
     * @see #getNumCameras()
     * @see #setNumCameras(int)
     */
    public void cycleCameras() {
        synchronized(this) {
            vcam.release();
            
            cameraIndex = (cameraIndex + 1) % numCameras;
            vcam = new VideoCapture(cameraIndex);

            nextPhase = Phase.FD_PHASE_INIT;
        }
    }
    
    /**
     * Releases the current camera and opens a new one at the given index <code>i</code>.<br>
     * To simply cycle through all cameras, please use {@link #cycleCameras()}.
     * 
     * @throws IllegalArgumentException
     *             If index is negative or greater than or equal to {@link #numCameras}.
     * @see #getCameraIndex()
     * @see #getNumCameras()
     * @see #setNumCameras(int)
     * @see #cycleCameras()
     */
    public void setCameraIndex(int i) {
        
        if (i >= numCameras || i < 0)
            throw new IllegalArgumentException();
        
        synchronized(this) {
            vcam.release();
            
            this.cameraIndex = i;
            vcam = new VideoCapture(cameraIndex);
            
            nextPhase = Phase.FD_PHASE_INIT;
        }
    }

    /**
     * @see #setCameraIndex
     */
    public int getCameraIndex() {
        return cameraIndex;
    }
    
    /**
     * Attach a new debug window to this class. Replaces the old one. Currently does not dispose of the old window.
     */
    public void attachDebugWindow(DebugWindow window) {
        if (window == null)
            throw new NullPointerException("tried to attach a null debug window");
        
        // TODO: maybe handle disposing of old window or remove listeners? synchronize all DebugWindow methods?
        this.debugWindow = window;
    }
    
    /**
     * Removes current debug window from the update loop. Currently does not dispose of the old window.
     */
    public void removeDebugWindow() {
        // TODO: maybe handle disposing of old window or remove listeners? synchronize all DebugWindow methods?
        debugWindow = new EmptyDebugWindow();
    }
    
    public DebugWindow getDebugWindow() {
        return debugWindow;
    }

    public boolean hasDebugWindow() {
        return !(debugWindow instanceof EmptyDebugWindow);
    }
    
    /**
     * Sets the number of cameras available to the system (there is currently no system in place to ask the operating
     * system to list the currently available cameras).
     * 
     * @throws IllegalArgumentException
     *          if {@code numCameras} is less than 1
     */
    public void setNumCameras(int numCameras) {
        if (numCameras < 1)
            throw new IllegalArgumentException("numCameras must be greater than 0");
        
        this.numCameras = numCameras;
    }

    /**
     * @see #setNumCameras(int)
     */
    public int getNumCameras() {
        return numCameras;
    }
    

    /**
     * {@link #matchTolerance} specifies how exact the match of a face between two frames has to be.
     * Smaller is more accurate, but may cause the program to lose tracking with fast head movements.<br>
     * Recommended values are from 0.05 to 0.2
     */
    public void setMatchTolerance(double matchTolerance) {        
        this.matchTolerance = matchTolerance;
    }
    
    /**
     * @see #setMatchTolerance(double)
     */
    public double getMatchTolerance() {
        return matchTolerance;
    }
    
    /**
     * Specifies how much the image is scaled down in each stage of face detection.
     * Smaller value is more accurate, but slower.<br>
     * Recommended values are between 1.05 and 1.6
     * 
     * @throws IllegalArgumentException
     *          If {@code detectScaleFactor} is less than <b>or equal</b> to 1.0.
     */
    public void setDetectScaleFactor(double detectScaleFactor) {
        if (detectScaleFactor <= 1.0)
            throw new IllegalArgumentException("Scale factor must be greater than 1.0!");
        
        this.detectScaleFactor = detectScaleFactor;
    }
    
    /**
     * @see #setDetectScaleFactor(double)
     */
    public double getDetectScaleFactor() {
        return detectScaleFactor;
    }

    /**
     * Specifies how many neighboring faces should be detected in a candidate area before it is considered an actual face.
     * Larger value avoids false positives, but may reject some actual faces. <br>
     * Recommended values are from 2 to 6.
     * 
     * @throws IllegalArgumentException
     *          if {@code detectMinNeighbors} is less than 1
     */
    public void setDetectMinNeighbors(int detectMinNeighbors) {
        if (detectScaleFactor < 1)
            throw new IllegalArgumentException("detectMinNeighbors must be greater than 0!");
        
        this.detectMinNeighbors = detectMinNeighbors;
    }
    
    /**
     * @see #setDetectMinNeighbors(int)
     */
    public int getDetectMinNeighbors() {
        return detectMinNeighbors;
    }

    /**
     * This influences the size of the rectangle to search for a matching face betwen two frames. Smaller value is slower,
     * but allows for larger movement between frames. May cause strange behavior if too small.<br>
     * Recommended values are from 32 to 8
     * 
     * @throws IllegalArgumentException
     *          if {@code matchRectExpandDivisor} is less than 1
     */
    public void setMatchRectExpandDivisor(int matchRectExpandDivisor) {
        if (matchRectExpandDivisor < 1)
            throw new IllegalArgumentException("matchRectExpandDivisor must be greater than 0!");
        
        this.matchRectExpandDivisor = matchRectExpandDivisor;
    }

    /**
     * @see #setMatchRectExpandDivisor(int)
     */
    public int getMatchRectExpandDivisor() {
        return matchRectExpandDivisor;
    }


    /**
     * Sets the minimum face size (width or height) for detection in pixels. Smaller values are faster and can 
     * detect faces further away from the camera, but may cause more detection of random noise as faces.<br>
     * Recommended values are between 10% and 20% of the frame height.
     * 
     * @throws IllegalArgumentException
     *          
     */
    public void setDetectMinFaceSize(int detectMinFaceSize) {
        if (detectMinFaceSize < 0)
            throw new IllegalArgumentException("detectMinFaceSize must be greater than 0!");
        
        this.detectMinFaceSize = detectMinFaceSize;
    }
    
    /**
     * @see #setDetectMinFaceSize(int)
     */
    public int getDetectMinFaceSize() {
        return detectMinFaceSize;
    }
}

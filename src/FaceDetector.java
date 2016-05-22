import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

/* TODO(Dgh):
 *  - get camera fov and image size from ROS Parameter Server, don't start until we have it
 *  - pull out some more parameters with getters/setters
 *  
 *  - try hybrid tracking approach to give reliable IDs to faces (WIP)
 *      a) use detection inside tracked rect + tolerance, then detect new faces *only outside that rect* (problems with overlap for profiles)
 *      b) track first, then detect, then try to match detected faces to tracked faces (how to handle each possible case?)
 *      c) combine a+b, use tracking as strong hint for detection (change detect parameters for tracked area to be more lenient)      
 *      d) detect first, then match with last known faces (center position and/or template match)
 *      e) always track in addition to detect, only remove tracking completely if rect touches image border
 *       \- need to test cases where tracking *should* be lost, maybe decay maximum valid diff over time?
 *       \- possibly too complicated to account for every edge case with this method 
 *  
 *  - maybe detect faces over 2-3 frames, choose only those which are in all frames
 *  - find a method for handling people moving out of camera fov while they are being tracked (re-detect on hitting edge?)
 *  - maybe separate Face datastructure for internal (detection) and external use, copy over relevant data each frame
 *   \- check every data type. do we really need doubles?
 *  - more perf statistics in debug window
 *  - check program behavior with different threads ending in different orders is fine (e.g. FD thread stops before window is closed)
 *   \- window listeners may need to be removed?
 *  - maybe determine tracking roi size using distance estimation + max angular velocity 
 *  - maybe provide a 3D velocity vector for faces across some time span (500ms?), handle Z-axis via size changes (math!)
 *   \- time span needs to be at least as long as tracking phase for Z-axis to work! (synchronize?)
 *  - use messages from the main part of the project as signals to start/stop threads or open a debug window
 *  - test what happens in hard error cases like disconnecting a camera mid operation
 *  - (later) maybe add passive "preview" phase that doesn't broadcast faces?
 *  - fine-tune parameters according to available camera
 *  - try out more classifiers
 *  - look at possibilities for face recognition/biometrics (performance, accuracy, need face database?)
 */

public class FaceDetector implements Runnable {

	public static enum Phase {
		FD_PHASE_INIT, FD_PHASE_WAIT, FD_PHASE_DETECT, FD_PHASE_TRACK, FD_PHASE_REDETECT,
	}

	private boolean isRunning = false;

	private Phase activePhase = Phase.FD_PHASE_INIT;
	private Phase nextPhase = Phase.FD_PHASE_DETECT;

	// private DebugWindow debugWindow = new DummyDebugWindow();

	private DebugWindow debugWindow = new DummyDebugWindow();

	private final ArrayList<Face> faces = new ArrayList<Face>();

	private static int faceCounter = 0; // used to give unique IDs to faces

	private Camera camera;
	private int cameraIndex = 0;
	private int numCameras;
	private List<Integer> cameras = new ArrayList<Integer>(); // list of camera
																// indices

	/**
	 * Used by other threads to request a camera change by setting it to a valid
	 * index (>= 0).
	 */
	private int requestedCameraIndex = -1;

	private int desiredFrameRate = 30;
	private float desiredFrameTime = (1000.0f / desiredFrameRate);

	//////////////////////////
	// DETECTION PARAMETERS //
	//////////////////////////

	/**
	 * Specifies how exact the match of a face between two frames has to be.<br>
	 * Smaller value is more accurate, but may cause the program to lose
	 * tracking with fast head movements.<br>
	 * Recommended values are from 0.05 to 0.2
	 */
	private double matchTolerance = 0.1;

	/**
	 * This influences the size of the rectangle to search for a matching face
	 * betwen two frames.<br>
	 * Smaller value is slower, but allows for larger movement between frames.
	 * May cause strange behavior if too small.<br>
	 * Recommended values are from 32 to 8
	 */
	private int matchRectExpandDivisor = 18; // TODO: This variable name is too
												// long and confusing!

	/**
	 * Specifies how long the tracking/matching phase should last before
	 * redetecting faces "properly" (in milliseconds).<br>
	 * Smaller value is slightly slower due to more redetection phases, but can
	 * reduce the time where faces are temporarily transplanted onto obvious
	 * non-face objects. Larger values can help reducing "lost" faces.<br>
	 * Recommended values are from 250 to 500.
	 */
	private int maxTrackingDuration = 500;

	/**
	 * Specifies how much the image is scaled in each stage of face detection.
	 * <br>
	 * Smaller value is more accurate, but slower. <b>Must be greater than
	 * 1.0</b> <br>
	 * Recommended values are between 1.05 and 1.6
	 */
	private double detectScaleFactor = 1.1;

	/**
	 * Specifies how many neighboring faces should be detected in a candidate
	 * area before it is considered an actual face.<br>
	 * Larger values avoid false positives, but may reject some actual faces.
	 * <br>
	 * Recommended values are from 2 to 6
	 */
	private int detectMinNeighbors = 3;

	/**
	 * Specifies the minimum face size (width or height) for detection in
	 * pixels. <br>
	 * Smaller values are slower, but can detect faces further away from the
	 * camera. However, this may also cause more detection of random noise as
	 * faces.<br>
	 * Recommended values are between 10% and 25% of the frame height.
	 */
	private int detectMinFaceSize = 64;

	//////////////////////////////
	// END DETECTION PARAMETERS //
	//////////////////////////////

	public FaceDetector() {
		numCameras = Camera.enumerateCameras(cameras);
	}

	/**
	 * @param cameraIndex
	 */
	public FaceDetector(int cameraIndex) {
		this();
		this.cameraIndex = cameraIndex % numCameras;
	}

	/**
	 * "main" function of this thread. <br>
	 * Important: None of the other methods are explicitly supported until this
	 * is called (usually via {@link java.lang.Thread#start Thread.start()}).
	 */
	public void run() {

		synchronized (this) {
			isRunning = true;

			// TODO: get fov from parameter server
			// TODO: maybe put this in constructor?
			camera = new Camera(cameras.get(cameraIndex), 90, 75);
		}

		Mat image = new Mat();
		Mat grayImage = new Mat();

		CascadeClassifier faceCascade = new CascadeClassifier("data/haarcascades/haarcascade_frontalface_alt.xml");

		float redetectTimer = 0;
		long oldTime = 0;
		long startTime = System.nanoTime();

		while (isRunning) {

			// another thread requested a camera change
			if (requestedCameraIndex >= 0) {
				this.cameraIndex = requestedCameraIndex;
				requestedCameraIndex = -1;

				// internal camera calls like read/release need to be
				// synchronized in case another thread calls stop()
				synchronized (this) {
					camera.release();
					camera = new Camera(cameras.get(cameraIndex), 90, 75);
				}
				image = new Mat();
				redetectTimer = 0;
				activePhase = Phase.FD_PHASE_INIT;
			}

			boolean readSuccess;
			synchronized (this) {
				readSuccess = camera.read(image);
			}

			// wait if camera doesn't provide an image
			if (!readSuccess || image.empty()) {
				executeWaitPhase(image);
				continue;
			}

			////////////////////
			// FACE DETECTION //
			////////////////////

			long phaseStartTime = System.nanoTime();

			// convert to grayscale and equalize histogram for further
			// processing
			Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
			Imgproc.equalizeHist(grayImage, grayImage);

			switch (activePhase) {

			case FD_PHASE_INIT: {
				faces.clear();
				detectFaces(grayImage, faces, faceCascade, detectMinFaceSize);
				nextPhase = Phase.FD_PHASE_TRACK;
			}
				break;

			// currently unused
			case FD_PHASE_DETECT: {
				detectFaces(grayImage, faces, faceCascade, detectMinFaceSize);
				nextPhase = Phase.FD_PHASE_TRACK;
			}
				break;

			case FD_PHASE_TRACK: {
				trackFaces(grayImage, faces);

				nextPhase = Phase.FD_PHASE_TRACK; // usually continue tracking

				if (redetectTimer >= maxTrackingDuration) { // tracking phase
															// can last a
															// maximum of 500ms
					redetectTimer = 0;
					nextPhase = Phase.FD_PHASE_REDETECT;
				}
			}
				break;

			// TODO:
			// current method compares center point of faces
			// another method would compare faces directly and match them
			// immediately
			// maybe don't retain faces if we get too close to image border
			case FD_PHASE_REDETECT: {

				ArrayList<Face> oldFaces = makeFaceListCopy();

				faces.clear();

				detectFaces(grayImage, faces, faceCascade, detectMinFaceSize); // detect
																				// new
																				// faces

				trackFaces(grayImage, oldFaces); // try to find new positions
													// for old faces

				// we're integrating over a fairly long period of time, so
				// tolerance is large
				// BUT: can lead to faces "transferring" to another person
				double tolerance = 0.1; // TODO: make parameter

				// compare new positions of old faces and all detected faces.
				// merge those that match within tolerance.
				for (Face oldFace : oldFaces) {

					boolean found = false;
					for (Face newFace : faces) {

						if (Math.abs(oldFace.center.x - newFace.center.x) < tolerance
								&& Math.abs(oldFace.center.y - newFace.center.y) < tolerance) {

							newFace.faceID = oldFace.faceID;
							newFace.retainedCount = 0;
							found = true;
							break;
						}
					}

					// we couldn't match the old face to a new one. retain it
					// once, in case it reappears next cycle.
					if (!found && oldFace.retainedCount < 1) { // TODO: make
																// parameter?
						oldFace.retainedCount++;
						faces.add(oldFace);
					}
				}

				nextPhase = Phase.FD_PHASE_TRACK;
			}
				break;

			default:
				assert (false);
				break;
			}

			long phaseEndTime = System.nanoTime();
			float phaseMillis = deltaMillis(phaseStartTime, phaseEndTime);

			////////////////////////
			// END FACE DETECTION //
			////////////////////////

			// update the debug window (in the window thread [AWT Event
			// Dispatcher])
			final ArrayList<Face> facesCopy = makeFaceListCopy();
			final Mat imageCopy = image.clone(); // could skip the copy if we
													// guarantee it won't be
													// modified by update()

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					debugWindow.update(activePhase, phaseMillis, imageCopy, facesCopy);
				}
			});

			// measure total busy time spent
			long endTime = System.nanoTime();
			float totalMillis = deltaMillis(startTime, endTime);

			// debug perf statistics (we could show these in debug window
			// instead)
			// System.out.println("---" + activePhase);
			// System.out.printf("read time: %6.2f\n", deltaMillis(startTime,
			// phaseStartTime)); // includes some waiting inside opencv api
			// System.out.printf("phase time: %6.2f\n", phaseMillis);
			// System.out.printf("update time: %6.2f\n",
			// deltaMillis(phaseEndTime, endTime));
			// System.out.printf("time left: %6.2f\n", (desiredFrameTime -
			// totalMillis));

			// update at desired framerate
			sleepUntilNextUpdate(totalMillis);

			oldTime = startTime;
			startTime = System.nanoTime();
			redetectTimer += deltaMillis(oldTime, startTime);
			activePhase = nextPhase;
		}
	}

	/**
	 * Handle error case if camera doesn't provide a proper image.<br>
	 * Currently just updates debug window and thenn sleeps until the next
	 * update period.
	 */
	private void executeWaitPhase(Mat image) {
		long phaseStartTime = System.nanoTime();

		activePhase = Phase.FD_PHASE_WAIT;

		debugWindow.update(activePhase, desiredFrameTime, image, makeFaceListCopy());

		long phaseEndTime = System.nanoTime();
		float totalMillis = deltaMillis(phaseStartTime, phaseEndTime);

		sleepUntilNextUpdate(totalMillis);

		activePhase = Phase.FD_PHASE_INIT;
	}

	/**
	 * Sleeps until the next update is due according to
	 * {@link #desiredFrameTime}. <br>
	 * I.e. it will sleep for approximately
	 * {@code desiredFrameTime - totalMillis} milliseconds.
	 * 
	 * @param totalMillis
	 *            time spent in this update period so far
	 */
	private void sleepUntilNextUpdate(float totalMillis) {
		try {
			float timeRemaining = (desiredFrameTime - totalMillis);
			long sleepMillis = (long) timeRemaining;
			int sleepNanos = (int) (1000000 * (timeRemaining - sleepMillis));

			if ((sleepMillis > 0) && (sleepNanos > 0)) {
				Thread.sleep(sleepMillis, sleepNanos);
			}
		} catch (InterruptedException e1) {
			System.out.println("DEBUG: Sleep was interrupted!");
		}
	}

	/**
	 * @param tStart
	 *            time value in nanoseconds
	 * @param tEnd
	 *            time value in nanoseconds
	 * @return milliseconds between {@code tStart} and {@code tEnd}
	 */
	public static float deltaMillis(long tStart, long tEnd) {
		return (tEnd - tStart) / 1000000.0f;
	}

	/**
	 * Tracks faces by matching the old data contained in {@code faceList} to a
	 * new image in {@code grayImage}.<br>
	 * Make sure that {@code grayImage} is in grayscale and has a histogram
	 * equalization applied.
	 * 
	 * @param grayImage
	 *            {@code image} converted to grayscale, with histogram
	 *            equalization applied
	 * @param faceList
	 */
	private void trackFaces(Mat grayImage, List<Face> faceList) {

		// each face rect will be expanded by this amount of pixels in each
		// direction
		int rectExpand = grayImage.width() / matchRectExpandDivisor;

		for (int i = 0; i < faceList.size(); i++) {

			Face f = faceList.get(i);

			// make a slightly larger rect than the face (region of interest)
			Rect roi = f.faceRect.clone();
			roi.x = Math.max(roi.x - rectExpand, 0);
			roi.y = Math.max(roi.y - rectExpand, 0);
			roi.width = Math.min(roi.width + rectExpand * 2, grayImage.width() - roi.x);
			roi.height = Math.min(roi.height + rectExpand * 2, grayImage.height() - roi.y);

			// try to match the old face data in the new image, inside the
			// region of interest
			Mat faceMat = f.faceData;
			Mat matchMatrix = new Mat();
			Imgproc.matchTemplate(grayImage.submat(roi), faceMat, matchMatrix, Imgproc.TM_SQDIFF_NORMED);

			// get best match location
			MinMaxLocResult minMax = Core.minMaxLoc(matchMatrix);

			// likely match (use minVal for TM_SQDIFF_NORMED, maxVal > (1.0 -
			// matchTolerance) for other methods)
			if (minMax.minVal <= matchTolerance) {

				// update face in list with new values

				int newX = (int) (roi.x + minMax.minLoc.x);
				int newY = (int) (roi.y + minMax.minLoc.y);
				Rect newRect = new Rect(newX, newY, f.faceRect.width, f.faceRect.height);
				Mat newFaceData = grayImage.submat(newRect).clone();

				Face newFace = new Face(newRect, newFaceData, f.faceID, camera, f.retainedCount);

				faceList.set(i, newFace);
			} else {
				// not matched, but retain old face for now, in case we can find
				// it again
				f.retainedCount++;

				// TODO: we could remove the face after a certain count to help
				// prevent possible transferring of faces

				// faceList.remove(i);
				// i--;
			}
		}
	}

	/**
	 * Detects faces in {@code grayImage} using {@code faceCascade} and adds
	 * them to {@code faceList}.<br>
	 * Make sure that {@code grayImage} is in grayscale and has a histogram
	 * equalization applied.
	 * 
	 * @param grayImage
	 *            image converted to grayscale, with histogram equalization
	 *            applied
	 * @param faceList
	 * @param faceCascade
	 * @param minFaceSize
	 *            faces cannot be smaller than this size in pixels (width or
	 *            height)
	 */
	private void detectFaces(Mat grayImage, List<Face> faceList, CascadeClassifier faceCascade, int minFaceSize) {

		MatOfRect faceRects = new MatOfRect();

		// detect faces in grayscale image
		faceCascade.detectMultiScale(grayImage, faceRects, detectScaleFactor, detectMinNeighbors,
				Objdetect.CASCADE_SCALE_IMAGE, new Size(minFaceSize, minFaceSize), new Size());

		Rect[] faceRectArray = faceRects.toArray();
		for (Rect rect : faceRectArray) {

			// reduce rect size to hopefully only include actual face pixels.
			// reduces tracking error
			rect.x = rect.x + rect.width / 5;
			rect.y = rect.y + rect.height / 12;
			rect.width = rect.width - 2 * (rect.width / 5);
			rect.height = rect.height - (rect.height / 12);

			// get pixel data for face
			Mat faceMat = grayImage.submat(rect).clone();

			// add detected face to our list
			Face f = new Face(rect, faceMat, faceCounter++, camera);

			faceList.add(f);
		}
	}

	/**
	 * Makes a copy of the current available face data.
	 */
	private ArrayList<Face> makeFaceListCopy() {

		// IMPORTANT: Do not make this method public, never call it from another
		// thread.
		// Will cause weird race condition bugs otherwise.

		ArrayList<Face> copy = new ArrayList<Face>(faces.size());
		for (Face f : faces) {
			copy.add(f.clone());
		}

		return copy;
	}

	public synchronized boolean isRunning() {
		return isRunning;
	}

	/**
	 * Stops this thread, releases the camera. Currently <b>not</b> supported to
	 * restart thread again.<br>
	 * Do not call this more than once, ever. May cause infinite loop in
	 * VideoCapture.release() for some reason.
	 */
	public synchronized void stop() {
		isRunning = false;

		removeDebugWindow(); // just in case someone is irresponsible!

		if (camera != null) { // can only happen if run() was never called. not
								// sure if we want to even support that?
			camera.release(); // required to let java exit properly
		}
	}

	/**
	 * This thread will never update faster than the desired frame rate
	 * dictates.
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
	 * Cycles through all available cameras.
	 */
	public void cycleCameras() {
		requestCameraChange((cameraIndex + 1) % numCameras);
	}

	/**
	 * Releases the current camera and opens a new one at the given index
	 * {@code i}. Will be executed on next update cycle.<br>
	 * 
	 * To simply cycle through all available cameras, use
	 * {@link #cycleCameras()}.
	 * 
	 * @throws IllegalArgumentException
	 *             if index is negative or greater than the number of available
	 *             cameras
	 * @see #getCameraIndex()
	 * @see #cycleCameras()
	 * @see #getNumCameras()
	 */
	public void requestCameraChange(int index) {

		if (index < 0 || index > numCameras)
			throw new IllegalArgumentException(
					"tried to set camera index to " + index + " (numCameras is " + numCameras + ")");

		requestedCameraIndex = index;
	}

	/**
	 * Gets number of available cameras.
	 * 
	 * @see #rescanCameras()
	 */
	public int getNumCameras() {
		return numCameras;
	}

	/**
	 * Scans to find all currently available cameras. If it cannot find the old
	 * camera again, it will open the first one it found.<br>
	 * Do not call this often, as it can take a while (expect >0.5s).
	 * 
	 * @return the number of cameras
	 */
	public synchronized int rescanCameras() {

		numCameras = Camera.enumerateCameras(cameras);

		// find old camera in new list
		boolean found = false;
		for (int cameraIndex = 0; cameraIndex < cameras.size(); cameraIndex++) {

			int realIndex = cameras.get(cameraIndex); // get actual index used
														// by VideoCapture

			if (realIndex == camera.getRealIndex()) {

				requestCameraChange(cameraIndex);
				found = true;
				break;
			}
		}
		if (!found) { // didn't find old camera for some reason, just open the
						// first one
			assert (false) : "DEBUG: Old camera not in rescan!";
			requestCameraChange(0);
		}

		return numCameras;
	}

	/**
	 * @see #requestCameraChange(int)
	 */
	public int getCameraIndex() {
		return cameraIndex;
	}

	/**
	 * Attach a new debug window to this class. Replaces the old one. Currently
	 * does not dispose of the old window.
	 */
	public void attachDebugWindow(DebugWindow window) {
		if (window == null)
			throw new NullPointerException("tried to attach a null debug window");

		this.debugWindow = window;
	}

	/**
	 * Removes current debug window from the update loop. Currently does not
	 * dispose of the old window.
	 */
	public void removeDebugWindow() {
		debugWindow = new DummyDebugWindow();
	}

	public DebugWindow getDebugWindow() {
		return debugWindow;
	}

	public boolean hasDebugWindow() {
		return !(debugWindow instanceof DummyDebugWindow);
	}

	/**
	 * {@link #matchTolerance} specifies how exact the match of a face between
	 * two frames has to be.<br>
	 * Smaller is more accurate, but may cause the program to lose tracking with
	 * fast head movements.<br>
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
	 * Specifies how much the image is scaled down in each stage of face
	 * detection.<br>
	 * Smaller value is more accurate, but slower.<br>
	 * Recommended values are between 1.05 and 1.6
	 * 
	 * @throws IllegalArgumentException
	 *             If {@code detectScaleFactor} is less than <b>or equal</b> to
	 *             1.0.
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
	 * Specifies how many neighboring faces should be detected in a candidate
	 * area before it is considered an actual face.<br>
	 * Larger values avoid false positives, but may reject some actual faces.
	 * <br>
	 * Recommended values are from 2 to 6
	 * 
	 * @throws IllegalArgumentException
	 *             if {@code detectMinNeighbors} is less than 1
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
	 * This influences the size of the rectangle to search for a matching face
	 * betwen two frames.<br>
	 * Smaller value is slower, but allows for larger movement between frames.
	 * May cause strange behavior if too small.<br>
	 * Recommended values are from 32 to 8
	 * 
	 * @throws IllegalArgumentException
	 *             if {@code matchRectExpandDivisor} is less than 1
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
	 * Specifies the minimum face size (width or height) for detection in
	 * pixels. <br>
	 * Smaller values are slower, but can detect faces further away from the
	 * camera. However, this may also cause more detection of random noise as
	 * faces.<br>
	 * Recommended values are between 10% and 25% of the frame height.
	 * 
	 * @throws IllegalArgumentException
	 *             if argument is negative
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

	/**
	 * Specifies how long the tracking/matching phase should last before
	 * redetecting faces "properly" (in milliseconds).<br>
	 * Smaller value is slightly slower due to more redetection phases, but can
	 * reduce the time where faces are temporarily transplanted onto obvious
	 * non-face objects. Larger values can help reducing "lost" faces.<br>
	 * Recommended values are from 250 to 500.
	 */
	public void setMaxTrackingDuration(int maxTrackingDuration) {
		this.maxTrackingDuration = maxTrackingDuration;
	}

	/**
	 * @see #setMaxTrackingDuration(int)
	 */
	public int getMaxTrackingDuration() {
		return maxTrackingDuration;
	}
}

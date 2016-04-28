import java.util.ArrayList;
import java.util.List;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;

/* TODO:
 *  - match eye template instead of moving with face
 *  - look up parameters on detectMultiScale (rejectLevel, etc)
 *  - try out more classifiers
 *  - maybe delete face and/or eye area from image after each detection to avoid multiples 
 *  - detect faces over 2-3 frames, choose only those which are in all frames, then track them over a longer period (1sec?)
 *  - find a method for handling people moving out of camera fov while they are being tracked (re-detect on hitting edge?)
 *  - fine-tune parameters according to available camera
 */

public class Main {

    public static boolean isRunning = true;
    public static Imshow  im;
    public static VideoCapture vcam;
    public static int cameraIndex = 0;
    public static int numCameras = 2;
        
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        im = new Imshow("Video Preview");
        im.Window.setResizable(true);
        
        im.Window.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                isRunning = false;
                vcam.release(); // required to let java exit properly
            }
        });
        
        im.Window.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
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
        
        Mat image = new Mat();
        vcam = new VideoCapture(cameraIndex);
        
        if (vcam.isOpened() == false)
            return;

        // loop until image frames are not empty
        while (image.empty()) {
            vcam.read(image);
        }
        
        
        CascadeClassifier faceCascade = new CascadeClassifier("data/haarcascades/haarcascade_frontalface_alt.xml");
        CascadeClassifier eyeCascade = new CascadeClassifier("data/haarcascades/haarcascade_eye.xml");

        List<Face> faces = new ArrayList<Face>();
        int minFaceSize = 0;
        int minEyeSize = 0;
        int height = image.rows();
        minFaceSize = Math.round(height * 0.1f);
        minEyeSize = Math.max(Math.round(minFaceSize * 0.1f), 10);

        int frameCounter = 0;
        
        while (isRunning) {
            
            if (vcam.isOpened()) {
            
                vcam.read(image);
    
                long t0 = System.nanoTime();
                
                if (faces.isEmpty()) {
                    detectFaces(image, faces, faceCascade, eyeCascade, minFaceSize, minEyeSize);
                }
                else if ((frameCounter & 15) == 0) { // periodically reset faces, need something more sophisticated later on
                    faces.clear();
                    detectFaces(image, faces, faceCascade, eyeCascade, minFaceSize, minEyeSize);
                }
                else {
                    trackFaces(image, faces);
                }
                
                long t1 = System.nanoTime();
                im.Window.setTitle("Camera: " + cameraIndex + " - " + ((t1 - t0) / 1000000.0f) + "ms");
    
                im.showImage(image);
    
                frameCounter++;
            }
        }
        vcam.release();
    }

    public static void trackFaces(Mat image, List<Face> faceList) {
        
        // NOTE: need to handle moving out of the frame

        Mat grayImage = new Mat();

        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(grayImage, grayImage);

        int tolerance = Math.max((int) Math.round(image.cols() * 0.02), 10);

        // try to match old faces in new frame
        for (int i = 0; i < faceList.size(); i++) {
            Rect roi = faceList.get(i).faceRect.clone();
            roi.x = Math.max(roi.x - tolerance, 0);
            roi.y = Math.max(roi.y - tolerance, 0);
            roi.width = Math.min(roi.width + tolerance * 2, image.width() - roi.x);
            roi.height = Math.min(roi.height + tolerance * 2, image.height() - roi.y);

            Mat faceMat = faceList.get(i).faceData;

            Mat matchedFace = new Mat(roi.width - faceMat.rows() + 1, roi.height - faceMat.cols() + 1, CvType.CV_8U);

            Imgproc.matchTemplate(grayImage.submat(roi), faceMat, matchedFace, Imgproc.TM_SQDIFF_NORMED);

            MinMaxLocResult minMax = Core.minMaxLoc(matchedFace);

            if (minMax.minVal <= 0.1) { // likely match
                Face f = faceList.get(i);

                int newFaceX = (int) (roi.x + minMax.minLoc.x);
                int newFaceY = (int) (roi.y + minMax.minLoc.y);
                
                f.faceRect.x = newFaceX;
                f.faceRect.y = newFaceY;
                f.faceData = grayImage.submat(f.faceRect).clone();

                Imgproc.rectangle(image, f.faceRect.tl(), f.faceRect.br(), new Scalar(0, 255, 255, 255), 2);

                for (int j = 0; j < f.eyeRects.size(); j++) {

                    Rect eyeRect = f.eyeRects.get(j);
                    
                    Point eyeRelTl = eyeRect.tl();
                    Point eyeRelBr = eyeRect.br();
                    
                    Point eyeAbsTl = new Point(eyeRelTl.x + newFaceX, eyeRelTl.y + newFaceY);
                    Point eyeAbsBr = new Point(eyeRelBr.x + newFaceX, eyeRelBr.y + newFaceY);

                    Imgproc.rectangle(image, eyeAbsTl, eyeAbsBr, new Scalar(255, 0, 255, 255), 1);
                }

            }
            else { // not matched
                faceList.remove(i);
                i--;
            }
        }
    }

    public static void detectFaces(Mat image, List<Face> faceList, CascadeClassifier faceCascade, CascadeClassifier eyeCascade,
                                   int minFaceSize, int minEyeSize) {

        Mat grayImage = new Mat();

        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(grayImage, grayImage);

        MatOfRect faceRects = new MatOfRect();

        faceCascade.detectMultiScale(grayImage, faceRects, 1.1, 3, Objdetect.CASCADE_SCALE_IMAGE, new Size(minFaceSize, minFaceSize), new Size());

        Rect[] facesArray = faceRects.toArray();
        for (int i = 0; i < facesArray.length; i++) {

            Point faceTl = facesArray[i].tl();
            Point faceBr = facesArray[i].br();

            Mat faceMat = grayImage.submat(facesArray[i]).clone();

            Imgproc.rectangle(image, faceTl, faceBr, new Scalar(0, 255, 0, 255), 2);

            MatOfRect eyeRects = new MatOfRect();

            eyeCascade.detectMultiScale(faceMat, eyeRects, 1.1, 5, Objdetect.CASCADE_SCALE_IMAGE, new Size(minEyeSize, minEyeSize), new Size());

            ArrayList<Mat> eyeMats = new ArrayList<Mat>();

            Rect[] eyesArray = eyeRects.toArray();
            for (int j = 0; j < eyesArray.length; j++) {

                Point eyeRelTl = eyesArray[j].tl();
                Point eyeRelBr = eyesArray[j].br();

                Point eyeAbsTl = new Point(eyeRelTl.x + faceTl.x, eyeRelTl.y + faceTl.y);
                Point eyeAbsBr = new Point(eyeRelBr.x + faceTl.x, eyeRelBr.y + faceTl.y);

                Rect eyeRect = new Rect(eyeAbsTl, eyeAbsBr);

                eyeMats.add(grayImage.submat(eyeRect).clone());

                Imgproc.rectangle(image, eyeAbsTl, eyeAbsBr, new Scalar(255, 0, 0, 255), 1);
            }

            Face f = new Face(facesArray[i], faceMat, eyesArray, eyeMats);
            faceList.add(f);
        }
    }
}

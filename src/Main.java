import java.util.ArrayList;
import java.util.List;
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

public class Main {

    public static boolean isRunning = true;
    public static Imshow  im;

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        im = new Imshow("Video Preview");

        im.Window.setResizable(true);

        Mat image = new Mat();
        VideoCapture vcam = new VideoCapture(0);

        im.Window.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                isRunning = false;
                vcam.release(); // required to let java exit properly
            }
        });

        // loop until VideoCamera is Available
        while (vcam.isOpened() == false)
            ;

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

        Mat hatimg = Imgcodecs.imread("data/hat2.png");

        int frameCounter = 0;
        while (isRunning) {
            vcam.read(image);

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

            drawHats(image, faces, hatimg);

            im.showImage(image);

            frameCounter++;
        }
        vcam.release();
        System.exit(0);
    }

    public static void drawHats(Mat image, List<Face> faces, Mat hatimg) {

        float hatAspect = hatimg.width() / (float) hatimg.height();

        for (int i = 0; i < faces.size(); i++) {

            Rect hatRect = faces.get(i).faceRect.clone();
            Size hatSize = new Size(hatRect.width * 0.8, hatRect.width * 0.8 / hatAspect);
            Mat hat = new Mat();
            Imgproc.resize(hatimg, hat, hatSize);

            hatRect.x = (int) Math.max(hatRect.x + 0.5 * (hatRect.width - hatSize.width), 0);
            hatRect.y = (int) Math.max(hatRect.y - hatSize.height, 0);
            hatRect.height = (int) Math.min(hatSize.height, image.height() - hatRect.y);
            hatRect.width = (int) Math.min(hatSize.width, image.width() - hatRect.x);
            hat.copyTo(image.submat(hatRect));
        }
    }

    public static void trackFaces(Mat image, List<Face> faceList) {

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

                f.faceRect.x = (int) (roi.x + minMax.minLoc.x);
                f.faceRect.y = (int) (roi.y + minMax.minLoc.y);
                f.faceData = grayImage.submat(f.faceRect).clone();

                Imgproc.rectangle(image, f.faceRect.tl(), f.faceRect.br(), new Scalar(0, 255, 255, 255), 3);

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

        faceCascade.detectMultiScale(grayImage, faceRects, 1.1, 2, Objdetect.CASCADE_SCALE_IMAGE, new Size(minFaceSize, minFaceSize), new Size());

        Rect[] facesArray = faceRects.toArray();
        for (int i = 0; i < facesArray.length; i++) {

            Point faceTl = facesArray[i].tl();
            Point faceBr = facesArray[i].br();

            Mat faceMat = grayImage.submat(facesArray[i]).clone();

            Imgproc.rectangle(image, faceTl, faceBr, new Scalar(0, 255, 0, 255), 3);

            MatOfRect eyeRects = new MatOfRect();

            eyeCascade.detectMultiScale(faceMat, eyeRects, 1.1, 10, Objdetect.CASCADE_SCALE_IMAGE, new Size(minEyeSize, minEyeSize), new Size());

            ArrayList<Mat> eyeMats = new ArrayList<Mat>();

            Rect[] eyesArray = eyeRects.toArray();
            for (int j = 0; j < eyesArray.length; j++) {

                Point eyeRelTl = eyesArray[j].tl();
                Point eyeRelBr = eyesArray[j].br();

                Point eyeAbsTl = new Point(eyeRelTl.x + faceTl.x, eyeRelTl.y + faceTl.y);
                Point eyeAbsBr = new Point(eyeRelBr.x + faceTl.x, eyeRelBr.y + faceTl.y);

                Rect eyeRect = new Rect(eyeAbsTl, eyeAbsBr);

                eyeMats.add(grayImage.submat(eyeRect).clone());

                Imgproc.rectangle(image, eyeAbsTl, eyeAbsBr, new Scalar(255, 0, 0, 255), 2);
            }

            Face f = new Face(facesArray[i], faceMat, eyesArray, eyeMats);
            faceList.add(f);
        }
    }
}

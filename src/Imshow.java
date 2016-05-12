/*
 * Modified from:
 * 
 * Original Author: ATUL (https://github.com/master-atul/ImShow-Java-OpenCV)
 * License: Apache License v2.0 (https://www.apache.org/licenses/LICENSE-2.0)
 */

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class Imshow implements DebugWindow {

    public JFrame window;
    private ImageIcon image;
    private JLabel label;
    private Boolean hasCustomSize;
    private Size size;
    private FaceDetector fd;

    public Imshow(String title, FaceDetector fd) {
        this.fd = fd;
        window = new JFrame();
        image = new ImageIcon();
        label = new JLabel();
        
        label.setIcon(image);
        window.getContentPane().add(label);
        window.setResizable(false);
        window.setTitle(title);
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        hasCustomSize = false;
        
        setupWindowListeners();

        window.setVisible(true);
        
        fd.attachDebugWindow(this);
    }

    public Imshow(String title, FaceDetector fd, int width, int height) {
        this(title, fd);
        
        hasCustomSize = true;
        this.size = new Size(width, height);
    }
    
    private void setupWindowListeners() {
        window.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                // stop face detection thread if we close the window, TODO/FIXME: remove later to decouple threads!
                fd.stop();
                fd.removeDebugWindow();
            }
        });

        window.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                // click anywhere to change camera (e.g. from webcam to virtual camera, for testing)
                fd.cycleCameras();
            }
        });
    }

    public void showImage(Mat img) {
        if (hasCustomSize) {
            Imgproc.resize(img, img, size);
        }
        image.setImage(toBufferedImage(img));
        window.pack(); // note: this basically prevents resizing the window if we want to allow it
        label.updateUI();
    }

    private BufferedImage toBufferedImage(Mat m) {
        
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (m.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        
        BufferedImage bufferedImage = new BufferedImage(m.cols(), m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();

        m.get(0,0,targetPixels); // copy over the image data
        
        return bufferedImage;
    }

    public void drawDebugRectangles(Mat image, Face[] faceList) {
        for (Face face : faceList) {
            Point faceTl = face.faceRect.tl();
            Point faceBr = face.faceRect.br();
            Imgproc.rectangle(image, faceTl, faceBr, new Scalar(0, 255, 0, 255), 1);
            
            Point above = new Point((faceTl.x + faceBr.x) * 0.5, faceTl.y - 10);
            
            Imgproc.putText(image, Integer.toString(face.faceID), above, 3, 0.5, new Scalar(255, 255, 255, 255), 1);
        }
    }

    public void update(FaceDetector.Phase phase, float phaseMillis, Mat image, Face[] faces) {
        if (phase == FaceDetector.Phase.FD_PHASE_WAIT) {
            window.setTitle("Camera: " + fd.getCameraIndex() + " - Waiting for camera...");
        }
        else {
            drawDebugRectangles(image, faces);      
            addPerfStat(phase, phaseMillis);
            window.setTitle("Camera: " + fd.getCameraIndex() + " - " + phase.toString() + ": " + phaseMillis + "ms");
            showImage(image);
        }
    }
    
    // very basic system for perf stats. code is a bit ugly, but it works.
    
    private float[] trackMillis = new float[16]; // Note: size must be a power of 2 for now
    private int trackMillisIndex = 0;
    private float[] detectMillis = new float[8]; // Note: size must be a power of 2 for now
    private int detectMillisIndex = 0;
    
    public void addPerfStat(FaceDetector.Phase phase, float millis) {
        
        switch (phase) {
        case FD_PHASE_DETECT:
            detectMillis[detectMillisIndex & (detectMillis.length - 1)] = millis;
            detectMillisIndex++;
            break;
        case FD_PHASE_TRACK:
            trackMillis[trackMillisIndex & (trackMillis.length - 1)] = millis;
            trackMillisIndex++;
            break;
        default:
            break;
        }
        
        if (trackMillisIndex > trackMillis.length) {
            
            float avgTrackMillis = 0;
            for (float f : trackMillis) {
                avgTrackMillis += f;
            }
            avgTrackMillis /= trackMillis.length;
            
            //System.out.println("Avg Track: " + avgTrackMillis);
        }
        if (detectMillisIndex > detectMillis.length) {
            
            float avgDetectMillis = 0;
            for (float f : detectMillis) {
                avgDetectMillis += f;
            }
            avgDetectMillis /= detectMillis.length;
            
            //System.out.println("Avg Detect: " + avgDetectMillis);
        }
    }
}

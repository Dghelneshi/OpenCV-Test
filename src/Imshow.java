/*
 * Modified from:
 * 
 * Original Author: ATUL (https://github.com/master-atul/ImShow-Java-OpenCV)
 * License: Apache License v2.0 (https://www.apache.org/licenses/LICENSE-2.0)
 */

import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.List;

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
        hasCustomSize = false;
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.setVisible(true);
        setupWindowListeners();
    }

    public Imshow(String title, FaceDetector fd, int height, int width) {
        this(title, fd);
        hasCustomSize = true;
        this.size = new Size(height, width);
    }
    
    private void setupWindowListeners() {
        window.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                // stop face detection thread if we close the window, TODO/FIXME: remove later to decouple threads!
                fd.stop();
            }
        });

        window.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                // click to change camera (e.g. from webcam to virtual camera, for testing)
                fd.setCameraIndex(fd.getCameraIndex() + 1);
            }
        });
    }

    public void showImage(Mat img) {
        if (hasCustomSize) {
            Imgproc.resize(img, img, size);
        }
        image.setImage(toBufferedImage(img));
        window.pack();
        label.updateUI();
    }

    private BufferedImage toBufferedImage(Mat m) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (m.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = m.channels() * m.cols() * m.rows();
        byte[] buf = new byte[bufferSize];
        m.get(0, 0, buf); // get all the pixels
        BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(buf, 0, targetPixels, 0, buf.length);
        return image;
    }

    public void drawDebugRectangles(Mat image, List<Face> faceList) {
        for (Face face : faceList) {
            Point faceTl = face.faceRect.tl();
            Point faceBr = face.faceRect.br();
            Imgproc.rectangle(image, faceTl, faceBr, new Scalar(0, 255, 0, 255), 1);
            
            Point above = new Point((faceTl.x + faceBr.x) * 0.5, faceTl.y - 10);
            
            Imgproc.putText(image, Integer.toString(face.faceID), above, 3, 0.5, new Scalar(255, 255, 255, 255), 1);
        }
    }
    
    public void setTitle(String s) {
        window.setTitle(s);
    }

    public void addWindowListener(WindowListener l) {
        window.addWindowListener(l);  
    }

    public void addMouseListener(MouseListener l) {
        window.addMouseListener(l);
    }
    
    public void addKeyListener(KeyListener l) {
        window.addKeyListener(l);
    }
    
    public void addMouseWheelListener(MouseWheelListener l) {
        window.addMouseWheelListener(l);
    }
}

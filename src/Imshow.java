/*
 * Modified from:
 * 
 * Original Author: ATUL (https://github.com/master-atul/ImShow-Java-OpenCV)
 * License: Apache License v2.0 (https://www.apache.org/licenses/LICENSE-2.0)
 */

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class Imshow {

    public JFrame window;
    private ImageIcon image;
    private JLabel label;
    private Boolean hasCustomSize;
    private Size size;

    public Imshow(String title) {
        window = new JFrame();
        image = new ImageIcon();
        label = new JLabel();
        label.setIcon(image);
        window.getContentPane().add(label);
        window.setResizable(false);
        window.setTitle(title);
        hasCustomSize = false;
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);
    }

    public Imshow(String title, int height, int width) {
        this(title);
        hasCustomSize = true;
        this.size = new Size(height, width);
    }

    public void showImage(Mat img) {
        if (hasCustomSize) {
            Imgproc.resize(img, img, size);
        }
        image.setImage(toBufferedImage(img));
        window.pack();
        label.updateUI();
    }

    public BufferedImage toBufferedImage(Mat m) {
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
}

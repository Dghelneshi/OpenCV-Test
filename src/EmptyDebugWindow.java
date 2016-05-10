import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowListener;
import java.util.List;

import org.opencv.core.Mat;

/**
 * {@link DebugWindow} that does nothing when its methods are called.
 */
public final class EmptyDebugWindow implements DebugWindow {
    
    public EmptyDebugWindow() {}

    public void setTitle(String s) {}

    public void showImage(Mat img) {}

    public void drawDebugRectangles(Mat image, List<Face> faceList) {}

    public void addWindowListener(WindowListener windowListener) {}

    public void addMouseListener(MouseListener mouseListener) {}
    
    public void addKeyListener(KeyListener l) {}
    
    public void addMouseWheelListener(MouseWheelListener l) {}
}

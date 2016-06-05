import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class FullDebugWindow extends Imshow implements ActionListener, ChangeListener {

    private JFrame buttonFrame = new JFrame();
    FaceDetector fd;

    private double matchTolerance;
    private float matchRectScaleFactor;
    private int maxTrackingDuration;
    private double detectScaleFactor;
    private int detectMinNeighbors;
    private int detectMinFaceSize;
    private double redetectMatchTolerance;

    JSlider sliderMT = new JSlider(5, 25);
    JSlider sliderMRSF = new JSlider(150, 250);
    JSlider sliderMTD = new JSlider(150, 500);
    JSlider sliderDSF = new JSlider(105, 200);
    JSlider sliderDMN = new JSlider(1, 8);
    JSlider sliderDMFS = new JSlider(16, 128);
    JSlider sliderRMT = new JSlider(5, 25);

    JTextArea showMT = new JTextArea();
    JTextArea showMRSF = new JTextArea();
    JTextArea showMTD = new JTextArea();
    JTextArea showDSF = new JTextArea();
    JTextArea showDMN = new JTextArea();
    JTextArea showDMFS = new JTextArea();
    JTextArea showRMT = new JTextArea();

    JButton setMT = new JButton("setMatchTolerance");
    JButton setMRSF = new JButton("setMatchRectScaleFactor");
    JButton setMTD = new JButton("setMaxTrackingDuration");
    JButton setDSF = new JButton("setDetectScaleFactor");
    JButton setDMN = new JButton("setDetectMinNeighbors");
    JButton setDMFS = new JButton("setDetectMinFaceSize");
    JButton setRMT = new JButton("setRedetectMatchTolerance");

    JButton changeCamera = new JButton("Kamara wechseln");

    public FullDebugWindow(String text, FaceDetector fd) {
        super(text, fd);
        this.fd = fd;

        JPanel p1 = new JPanel(new GridLayout(7, 0));
        JPanel p2 = new JPanel(new GridLayout(7, 0));
        JPanel p3 = new JPanel(new GridLayout(7, 0));
        JPanel pAll = new JPanel(new GridLayout(0, 4));

        matchTolerance = fd.getMatchTolerance();
        matchRectScaleFactor = fd.getMatchRectScaleFactor();
        maxTrackingDuration = fd.getMaxTrackingDuration();
        detectScaleFactor = fd.getDetectScaleFactor();
        detectMinNeighbors = fd.getDetectMinNeighbors();
        detectMinFaceSize = fd.getDetectMinFaceSize();
        redetectMatchTolerance = fd.getRedetectMatchTolerance();

        sliderMT.setValue((int) (matchTolerance * 100));
        sliderMRSF.setValue((int) (matchRectScaleFactor * 100));
        sliderMTD.setValue(maxTrackingDuration);
        sliderDSF.setValue((int) (detectScaleFactor * 100));
        sliderDMN.setValue(detectMinNeighbors);
        sliderDMFS.setValue(detectMinFaceSize);
        sliderRMT.setValue((int) (redetectMatchTolerance * 100));

        updateTextAreas();

        Border b = BorderFactory.createLineBorder(Color.black);

        showMT.setBorder(b);
        showMRSF.setBorder(b);
        showMTD.setBorder(b);
        showDSF.setBorder(b);
        showDMN.setBorder(b);
        showDMFS.setBorder(b);
        showRMT.setBorder(b);

        p1.setBorder(b);
        //p2.setBorder(BorderFactory.createLineBorder(Color.black));
        p3.setBorder(b);

        p1.add(sliderMT);
        p1.add(sliderMRSF);
        p1.add(sliderMTD);
        p1.add(sliderDSF);
        p1.add(sliderDMN);
        p1.add(sliderDMFS);
        p1.add(sliderRMT);

        p2.add(showMT);
        p2.add(showMRSF);
        p2.add(showMTD);
        p2.add(showDSF);
        p2.add(showDMN);
        p2.add(showDMFS);
        p2.add(showRMT);

        p3.add(setMT);
        p3.add(setMRSF);
        p3.add(setMTD);
        p3.add(setDSF);
        p3.add(setDMN);
        p3.add(setDMFS);
        p3.add(setRMT);

        setMT.addActionListener(this);
        setMRSF.addActionListener(this);
        setMTD.addActionListener(this);
        setDSF.addActionListener(this);
        setDMN.addActionListener(this);
        setDMFS.addActionListener(this);
        setRMT.addActionListener(this);

        sliderMT.addChangeListener(this);
        sliderMRSF.addChangeListener(this);
        sliderMTD.addChangeListener(this);
        sliderDSF.addChangeListener(this);
        sliderDMN.addChangeListener(this);
        sliderDMFS.addChangeListener(this);
        sliderRMT.addChangeListener(this);

        changeCamera.addActionListener(this);

        pAll.add(p1);
        pAll.add(p2);
        pAll.add(p3);
        pAll.add(changeCamera);

        buttonFrame.add(pAll);

        buttonFrame.pack();
        buttonFrame.setVisible(true);
        buttonFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void updateTextAreas() {
        showMT.setText("" + matchTolerance);
        showMRSF.setText("" + matchRectScaleFactor);
        showMTD.setText("" + maxTrackingDuration);
        showDSF.setText("" + detectScaleFactor);
        showDMN.setText("" + detectMinNeighbors);
        showDMFS.setText("" + detectMinFaceSize);
        showRMT.setText("" + redetectMatchTolerance);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        // NOTE: JSlider.setValue will eventually call the corresponding setter in ChangeListener.stateChanged() below

        Object btn = e.getSource();
        if (btn == setMT) {
            matchTolerance = Double.parseDouble(showMT.getText());
            sliderMT.setValue((int) (matchTolerance * 100));
        } else if (btn == setMRSF) {
            matchRectScaleFactor = Float.parseFloat(showMRSF.getText());
            sliderMRSF.setValue((int) (matchRectScaleFactor * 100));
        } else if (btn == setMTD) {
            maxTrackingDuration = Integer.parseInt(showMTD.getText());
            sliderMTD.setValue(maxTrackingDuration);
        } else if (btn == setDSF) {
            detectScaleFactor = Double.parseDouble(showDSF.getText());
            sliderDSF.setValue((int) (detectScaleFactor * 100));
        } else if (btn == setDMN) {
            detectMinNeighbors = Integer.parseInt(showDMN.getText());
            sliderDMN.setValue(detectMinNeighbors);
        } else if (btn == setDMFS) {
            detectMinFaceSize = Integer.parseInt(showDMFS.getText());
            sliderDMFS.setValue(detectMinFaceSize);
        } else if (btn == setRMT) {
            redetectMatchTolerance = Double.parseDouble(showRMT.getText());
            sliderRMT.setValue((int) (redetectMatchTolerance * 100));
        } else if (btn == changeCamera) {
            fd.cycleCameras();
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        Object slider = e.getSource();
        if (slider == sliderMT) {
            matchTolerance = sliderMT.getValue() / 100.0;
            fd.setMatchTolerance(matchTolerance);
        } else if (slider == sliderMRSF) {
            matchRectScaleFactor = sliderMRSF.getValue() / 100.0f;
            fd.setMatchRectScaleFactor(matchRectScaleFactor);
        } else if (slider == sliderMTD) {
            maxTrackingDuration = sliderMTD.getValue();
            fd.setMaxTrackingDuration(maxTrackingDuration);
        } else if (slider == sliderDSF) {
            detectScaleFactor = sliderDSF.getValue() / 100.0;
            fd.setDetectScaleFactor(detectScaleFactor);
        } else if (slider == sliderDMN) {
            detectMinNeighbors = sliderDMN.getValue();
            fd.setDetectMinNeighbors(detectMinNeighbors);
        } else if (slider == sliderDMFS) {
            detectMinFaceSize = sliderDMFS.getValue();
            fd.setDetectMinFaceSize(detectMinFaceSize);
        } else if (slider == sliderRMT) {
            redetectMatchTolerance = sliderRMT.getValue() / 100.0;
            fd.setRedetectMatchTolerance(redetectMatchTolerance);
        }

        updateTextAreas(); // too lazy to write more code, not supposed to be fast anyways
    }
}

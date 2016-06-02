import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.opencv.core.Mat;

public class DebugingWindow extends Imshow implements ActionListener, ChangeListener {

    private JFrame buttonFrame = new JFrame();
	FaceDetector fd;
	
	private double matchTolerance;
	private int matchRectExpandDivisor;
	private int maxTrackingDuration;
	private double detectScaleFactor;
	private int detectMinNeighbors;
	private int detectMinFaceSize;

	JSlider sliderVar1 = new JSlider(5, 25);
    JSlider sliderVar2 = new JSlider(8, 32);
    JSlider sliderVar3 = new JSlider(150, 500);
    JSlider sliderVar4 = new JSlider(105, 200);
    JSlider sliderVar5 = new JSlider(2, 6);
    JSlider sliderVar6 = new JSlider(24, 128);

	JTextArea showVar1 = new JTextArea();
	JTextArea showVar2 = new JTextArea();
	JTextArea showVar3 = new JTextArea();
	JTextArea showVar4 = new JTextArea();
	JTextArea showVar5 = new JTextArea();
	JTextArea showVar6 = new JTextArea();

	JButton setVar1 = new JButton("setMatchTolerance");
	JButton setVar2 = new JButton("setMatchRectExpandDivisor");
	JButton setVar3 = new JButton("setMaxTrackingDuration");
	JButton setVar4 = new JButton("setDetectScaleFactor");
	JButton setVar5 = new JButton("setDetectMinNeighbors");
	JButton setVar6 = new JButton("setDetectMinFaceSize");

	JButton changeCamera = new JButton("Kamara wechseln");
	
	public DebugingWindow(String text, FaceDetector fd) {
	    super(text, fd);
		this.fd = fd;
		
		JPanel p1 = new JPanel(new GridLayout(6, 0));
		JPanel p2 = new JPanel(new GridLayout(6, 0));
		JPanel p3 = new JPanel(new GridLayout(6, 0));
		JPanel pAll = new JPanel(new GridLayout(0, 4));

		matchTolerance = fd.getMatchTolerance();
		matchRectExpandDivisor = fd.getMatchRectExpandDivisor();
		maxTrackingDuration = fd.getMaxTrackingDuration();
		detectScaleFactor = fd.getDetectScaleFactor();
		detectMinNeighbors = fd.getDetectMinNeighbors();
		detectMinFaceSize = fd.getDetectMinFaceSize();
		
		sliderVar1.setValue((int) (matchTolerance * 100));
		sliderVar2.setValue(matchRectExpandDivisor);
		sliderVar3.setValue(maxTrackingDuration);
		sliderVar4.setValue((int) (detectScaleFactor * 100));
		sliderVar5.setValue(detectMinNeighbors);
		sliderVar6.setValue(detectMinFaceSize);
		
        updateTextAreas();
		
		showVar1.setBorder(BorderFactory.createLineBorder(Color.black));
		showVar2.setBorder(BorderFactory.createLineBorder(Color.black));
		showVar3.setBorder(BorderFactory.createLineBorder(Color.black));
		showVar4.setBorder(BorderFactory.createLineBorder(Color.black));
		showVar5.setBorder(BorderFactory.createLineBorder(Color.black));
		showVar6.setBorder(BorderFactory.createLineBorder(Color.black));

		p1.add(sliderVar1);
		p1.add(sliderVar2);
		p1.add(sliderVar3);
		p1.add(sliderVar4);
		p1.add(sliderVar5);
		p1.add(sliderVar6);

		p2.add(showVar1);
		p2.add(showVar2);
		p2.add(showVar3);
		p2.add(showVar4);
		p2.add(showVar5);
		p2.add(showVar6);

		p3.add(setVar1);
		p3.add(setVar2);
		p3.add(setVar3);
		p3.add(setVar4);
		p3.add(setVar5);
		p3.add(setVar6);
		
		setVar1.addActionListener(this);
		setVar2.addActionListener(this);
		setVar3.addActionListener(this);
		setVar4.addActionListener(this);
		setVar5.addActionListener(this);
		setVar6.addActionListener(this);
		
		sliderVar1.addChangeListener(this);
		sliderVar2.addChangeListener(this);
		sliderVar3.addChangeListener(this);
		sliderVar4.addChangeListener(this);
		sliderVar5.addChangeListener(this);
		sliderVar6.addChangeListener(this);
		
		changeCamera.addActionListener(this);

		p1.setBorder(BorderFactory.createLineBorder(Color.black));
		//p2.setBorder(BorderFactory.createLineBorder(Color.black));
		p3.setBorder(BorderFactory.createLineBorder(Color.black));

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
        showVar1.setText("" + matchTolerance);
        showVar2.setText("" + matchRectExpandDivisor);
        showVar3.setText("" + maxTrackingDuration);
        showVar4.setText("" + detectScaleFactor);
        showVar5.setText("" + detectMinNeighbors);
        showVar6.setText("" + detectMinFaceSize);
    }

	@Override
	public void actionPerformed(ActionEvent e) {
	    
	    // NOTE: JSlider.setValue will eventually call the corresponding setter in ChangeListener.stateChanged() below
	    
		Object btn = e.getSource();
		if (btn == setVar1) {
			matchTolerance = Double.parseDouble(showVar1.getText());
			sliderVar1.setValue((int) (matchTolerance * 100));
		}
		else if (btn == setVar2) {
			matchRectExpandDivisor = Integer.parseInt(showVar2.getText());
			sliderVar2.setValue(matchRectExpandDivisor);
		}
		else if (btn == setVar3) {
			maxTrackingDuration = Integer.parseInt(showVar3.getText());
            sliderVar3.setValue(maxTrackingDuration);
		}
		else if (btn == setVar4) {
			detectScaleFactor = Double.parseDouble(showVar4.getText());
            sliderVar4.setValue((int) (detectScaleFactor * 100));
		}
		else if (btn == setVar5) {
			detectMinNeighbors = Integer.parseInt(showVar5.getText());
	        sliderVar5.setValue(detectMinNeighbors);
		}
		else if (btn == setVar6) {
			detectMinFaceSize = Integer.parseInt(showVar6.getText());
	        sliderVar6.setValue(detectMinFaceSize);
		}
		else if(btn == changeCamera){
			fd.cycleCameras();
		}
	}
	
    @Override
    public void stateChanged(ChangeEvent e) {
        Object slider = e.getSource();
        if (slider == sliderVar1) {
            matchTolerance = sliderVar1.getValue() / 100.0;
            fd.setMatchTolerance(matchTolerance);
        }
        else if (slider == sliderVar2) {
            matchRectExpandDivisor = sliderVar2.getValue();
            fd.setMatchRectExpandDivisor(matchRectExpandDivisor);
       }
        else if (slider == sliderVar3) {
            maxTrackingDuration = sliderVar3.getValue();
            fd.setMaxTrackingDuration(maxTrackingDuration);
        }
        else if (slider == sliderVar4) {
            detectScaleFactor = sliderVar4.getValue() / 100.0;
            fd.setDetectScaleFactor(detectScaleFactor);
        }
        else if (slider == sliderVar5) {
            detectMinNeighbors = sliderVar5.getValue();
            fd.setDetectMinNeighbors(detectMinNeighbors);
        }
        else if (slider == sliderVar6) {
            detectMinFaceSize = sliderVar6.getValue();
            fd.setDetectMinFaceSize(detectMinFaceSize);
        }
        updateTextAreas(); // too lazy to write more code, not supposed to be fast anyways
    }
}

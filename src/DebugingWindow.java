import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.opencv.core.Mat;

public class DebugingWindow extends JFrame implements ActionListener, DebugWindow {

	 
//	FaceDetector fd;
	FaceDetector fd = new FaceDetector();
	
	private double matchTolerance;
	private int matchRectExpandDivisor;
	private int maxTrackingDuration;
	private double detectScaleFactor;
	private int detectMinNeighbors;
	private int detectMinFaceSize;

	JButton getVar1 = new JButton("getMatchTolerance");
	JButton getVar2 = new JButton("getMatchRectExpandDivisor");
	JButton getVar3 = new JButton("getMaxTrackingDuration");
	JButton getVar4 = new JButton("getDetectScaleFactor");
	JButton getVar5 = new JButton("getDetectMinNeighbors");
	JButton getVar6 = new JButton("getDetectMinFaceSize");

	JTextArea showVar1 = new JTextArea();
	JTextArea showVar2 = new JTextArea();
	JTextArea showVar3 = new JTextArea();
	JTextArea showVar4 = new JTextArea();
	JTextArea showVar5 = new JTextArea();
	JTextArea showVar6 = new JTextArea();

	JTextArea setTFVar1 = new JTextArea();
	JTextArea setTFVar2 = new JTextArea();
	JTextArea setTFVar3 = new JTextArea();
	JTextArea setTFVar4 = new JTextArea();
	JTextArea setTFVar5 = new JTextArea();
	JTextArea setTFVar6 = new JTextArea();

	JButton setVar1 = new JButton("setMatchTolerance");
	JButton setVar2 = new JButton("setMatchRectExpandDivisor");
	JButton setVar3 = new JButton("setMaxTrackingDuration");
	JButton setVar4 = new JButton("setDetectScaleFactor");
	JButton setVar5 = new JButton("setDetectMinNeighbors");
	JButton setVar6 = new JButton("setDetectMinFaceSize");

	JButton confirmAllChanges = new JButton("Änderungen Uebernehmen");

	public DebugingWindow(String text) {

		// JPanel hp = new JPanel(new GridLayout(1, 0));
		// JPanel p = new JPanel(new GridLayout(6, 3));

		JPanel p1 = new JPanel(new GridLayout(6, 0));
		JPanel p2 = new JPanel(new GridLayout(6, 0));
		JPanel p3 = new JPanel(new GridLayout(6, 0));
		JPanel p4 = new JPanel(new GridLayout(6, 0));
		JPanel pAll = new JPanel(new GridLayout(0, 5));

		showVar1.setName("Var1");
		showVar2.setName("Var2");
		showVar3.setName("Var3");
		showVar4.setName("Var4");
		showVar5.setName("Var5");
		showVar6.setName("Var6");

		showVar1.setBackground(new Color(211, 211, 211));
		showVar2.setBackground(new Color(211, 211, 211));
		showVar3.setBackground(new Color(211, 211, 211));
		showVar4.setBackground(new Color(211, 211, 211));
		showVar5.setBackground(new Color(211, 211, 211));
		showVar6.setBackground(new Color(211, 211, 211));

		showVar1.setEditable(false);
		showVar2.setEditable(false);
		showVar3.setEditable(false);
		showVar4.setEditable(false);
		showVar5.setEditable(false);
		showVar6.setEditable(false);

		showVar1.setBorder(BorderFactory.createLineBorder(Color.black, 0));
		showVar2.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		showVar3.setBorder(BorderFactory.createLineBorder(Color.black, 0));
		showVar4.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		showVar5.setBorder(BorderFactory.createLineBorder(Color.black, 0));
		showVar6.setBorder(BorderFactory.createLineBorder(Color.black, 1));

		setTFVar1.setName("setTFVar1");
		setTFVar2.setName("setTFVar2");
		setTFVar3.setName("setTFVar3");
		setTFVar4.setName("setTFVar4");
		setTFVar5.setName("setTFVar5");
		setTFVar6.setName("setTFVar6");

		setTFVar1.setBorder(BorderFactory.createLineBorder(Color.black, 0));
		setTFVar2.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		setTFVar3.setBorder(BorderFactory.createLineBorder(Color.black, 0));
		setTFVar4.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		setTFVar5.setBorder(BorderFactory.createLineBorder(Color.black, 0));
		setTFVar6.setBorder(BorderFactory.createLineBorder(Color.black, 1));

		p1.add(getVar1);
		p1.add(getVar2);
		p1.add(getVar3);
		p1.add(getVar4);
		p1.add(getVar5);
		p1.add(getVar6);

		p2.add(showVar1);
		p2.add(showVar2);
		p2.add(showVar3);
		p2.add(showVar4);
		p2.add(showVar5);
		p2.add(showVar6);

		p3.add(setTFVar1);
		p3.add(setTFVar2);
		p3.add(setTFVar3);
		p3.add(setTFVar4);
		p3.add(setTFVar5);
		p3.add(setTFVar6);

		p4.add(setVar1);
		p4.add(setVar2);
		p4.add(setVar3);
		p4.add(setVar4);
		p4.add(setVar5);
		p4.add(setVar6);

		getVar1.addActionListener(this);
		getVar2.addActionListener(this);
		getVar3.addActionListener(this);
		getVar4.addActionListener(this);
		getVar5.addActionListener(this);
		getVar6.addActionListener(this);

		setVar1.addActionListener(this);
		setVar2.addActionListener(this);
		setVar3.addActionListener(this);
		setVar4.addActionListener(this);
		setVar5.addActionListener(this);
		setVar6.addActionListener(this);

		p1.setBorder(BorderFactory.createLineBorder(Color.black));
		p2.setBorder(BorderFactory.createLineBorder(Color.black));
		p3.setBorder(BorderFactory.createLineBorder(Color.black));
		p4.setBorder(BorderFactory.createLineBorder(Color.black));

		pAll.add(p1);
		pAll.add(p2);
		pAll.add(p3);
		pAll.add(p4);
		pAll.add(confirmAllChanges);

		/*
		 * p.add(getVar1); p.add(showVar1); p.add(setTFVar1); p.add(setVar1);
		 * 
		 * p.add(getVar2); p.add(showVar2); p.add(setTFVar2); p.add(setVar2);
		 * 
		 * p.add(getVar3); p.add(showVar3); p.add(setTFVar3); p.add(setVar3);
		 * 
		 * p.add(getVar4); p.add(showVar4); p.add(setTFVar4); p.add(setVar4);
		 * 
		 * p.add(getVar5); p.add(showVar5); p.add(setTFVar5); p.add(setVar5);
		 * 
		 * p.add(getVar6); p.add(showVar6); p.add(setTFVar6); p.add(setVar6);
		 * 
		 * p.setBorder( BorderFactory.createLineBorder( Color.black ) );
		 * hp.setBorder( BorderFactory.createLineBorder( Color.black ) );
		 * 
		 * hp.add(p); hp.add(confirmAllChanges);
		 */

		add(pAll);

		setLocation(0, 0);
		pack();
		setVisible(true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

	}
	
	public static void main(String[] args) {

		new DebugingWindow("test");

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object btn = e.getSource();
		if (btn == getVar1) {
			showVar1.setText("" + fd.getMatchTolerance());
		}
		if (btn == getVar2) {
			showVar2.setText("" + fd.getMatchRectExpandDivisor());
		}
		if (btn == getVar3) {
			showVar3.setText("" + fd.getMaxTrackingDuration());
		}
		if (btn == getVar4) {
			showVar4.setText("" + fd.getDetectScaleFactor());
		}
		if (btn == getVar5) {
			showVar5.setText("" + fd.getDetectMinNeighbors());
		}
		if (btn == getVar6) {
			showVar6.setText("" + fd.getDetectMinFaceSize());
		}

		if (btn == setVar1) {
			matchTolerance = Double.parseDouble(setTFVar1.getText());
			fd.setMatchTolerance(matchTolerance);
		}
		if (btn == setVar2) {
			matchRectExpandDivisor = Integer.parseInt(setTFVar2.getText());
			fd.setMatchRectExpandDivisor(matchRectExpandDivisor);
			;
		}
		if (btn == setVar3) {
			// set...(setTFVar3)
			maxTrackingDuration = Integer.parseInt(setTFVar3.getText());
			fd.setMaxTrackingDuration(maxTrackingDuration);

		}
		if (btn == setVar4) {
			// set...(setTFVar4)
			detectScaleFactor = Double.parseDouble(setTFVar4.getText());
			fd.setDetectScaleFactor(detectScaleFactor);

		}
		if (btn == setVar5) {
			// set...(setTFVar5)
			detectMinNeighbors = Integer.parseInt(setTFVar5.getText());
			fd.setDetectMinNeighbors(detectMinNeighbors);

		}
		if (btn == setVar6) {
			// set...(setTFVar6)
			detectMinFaceSize = Integer.parseInt(setTFVar6.getText());
			fd.setDetectMinFaceSize(detectMinFaceSize);

		}

	}

	@Override
	public void update(FaceDetector.Phase phase, float phaseMillis, Mat image, ArrayList<Face> faces) {

	}

}

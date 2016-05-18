import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class CamAnglePixelCalibration {

	private Mat image = new Mat();
	private double width; // 640
	private double height; // 480
	private final static double angleWidth = 60;
	private final static double angleHeight = 30;

	public CamAnglePixelCalibration() {
	}

	public CamAnglePixelCalibration(Mat image) {
		this.image = image;
		width = image.width();
		height = image.height();
	}

	public Point calcPixeltoAngle(Point point) {
		double xAngleCoordinates, yAngleCoordinates;
		double xPixelCoordinates, yPixelCoordinates;
		xPixelCoordinates = point.x;
		yPixelCoordinates = point.y;

		if (xPixelCoordinates == (width / 2)) {
			xAngleCoordinates = 0;
		} else if (xPixelCoordinates > (width / 2)) {
			xAngleCoordinates = (angleWidth / 2) / (width / 2) * (xPixelCoordinates - (width / 2));
		} else { // xPixelCoordinates < width/2
			xAngleCoordinates = ((angleWidth / 2) / (width / 2) * xPixelCoordinates) - (angleWidth / 2);
		}

		if (yPixelCoordinates == (height / 2)) {
			yAngleCoordinates = 0;
		} else if (yPixelCoordinates > (height / 2)) {
			yAngleCoordinates = -((angleHeight / 2) / (height / 2) * (yPixelCoordinates - (height / 2)));
		} else { // xPixelCoordinates < height/2
			yAngleCoordinates = (angleHeight / 2) - ((angleHeight / 2) / (height / 2) * yPixelCoordinates);
		}

		return new Point(xAngleCoordinates, yAngleCoordinates);
	}

	public double calcPixeltoAngleX(double xPixelCoordinates) {
		double xAngleCoordinates;
		if (xPixelCoordinates == (width / 2)) {
			xAngleCoordinates = 0;
		} else if (xPixelCoordinates > (width / 2)) {
			xAngleCoordinates = (angleWidth / 2) / (width / 2) * (xPixelCoordinates - (width / 2));
		} else { // xPixelCoordinates < width/2
			xAngleCoordinates = ((angleWidth / 2) / (width / 2) * xPixelCoordinates) - (angleWidth / 2);
		}
		return xAngleCoordinates;
	}

	public double calcPixeltoAngleY(double yPixelCoordinates) {
		double yAngleCoordinates;
		if (yPixelCoordinates == (height / 2)) {
			yAngleCoordinates = 0;
		} else if (yPixelCoordinates > (height / 2)) {
			yAngleCoordinates = -((angleHeight / 2) / (height / 2) * (yPixelCoordinates - (height / 2)));
		} else { // xPixelCoordinates < height/2
			yAngleCoordinates = (angleHeight / 2) - ((angleHeight / 2) / (height / 2) * yPixelCoordinates);
		}

		return yAngleCoordinates;
	}

	public Point calcAngletoPixel(Point angle) {

		double xAngleCoordinates, yAngleCoordinates;
		double xPixelCoordinates, yPixelCoordinates;
		xAngleCoordinates = angle.x;
		yAngleCoordinates = angle.y;

		if (xAngleCoordinates > 0) {
			xPixelCoordinates = (width / 2) / (angleWidth / 2) * (xAngleCoordinates) + (width / 2);
		} else { // xAngleCoordinates < angleWidth/2
			xPixelCoordinates = (width / 2) / -(angleWidth / 2) * (-(angleWidth / 2) - (xAngleCoordinates));
		}

		if (yAngleCoordinates > 0) {
			yPixelCoordinates = (height / 2) / (angleHeight / 2) * ((angleHeight / 2) - (yAngleCoordinates));
		} else { // yAngleCoordinates < angleHeight/2 240/15* -(x) +240
			yPixelCoordinates = (height / 2) / (angleHeight / 2) * -(yAngleCoordinates) + (height / 2);
		}

		return new Point(xPixelCoordinates, yPixelCoordinates);
	}

	public double calcAngletoPixelX(double xAngleCoordinates) {
		double xPixelCoordinates;

		if (xAngleCoordinates > 0) {
			xPixelCoordinates = (width / 2) / (angleWidth / 2) * (xAngleCoordinates) + (width / 2);
		} else { // xAngleCoordinates < angleWidth/2
			xPixelCoordinates = (width / 2) / -(angleWidth / 2) * (-(angleWidth / 2) - (xAngleCoordinates));
		}
		return xPixelCoordinates;
	}

	public double calcAngletoPixelY(double yAngleCoordinates) {
		double yPixelCoordinates;

		if (yAngleCoordinates > 0) {
			yPixelCoordinates = (height / 2) / (angleHeight / 2) * ((angleHeight / 2) - (yAngleCoordinates));
		} else { // yAngleCoordinates < angleHeight/2 240/15* -(x) +240
			yPixelCoordinates = (height / 2) / (angleHeight / 2) * -(yAngleCoordinates) + (height / 2);
		}

		return yPixelCoordinates;
	}

	// Point p1 = new Point(0, 200);
	// Point p2 = new Point(600, 200);

	public String getXAngleText(int i) {
		String str = "";
		switch (i) {
		case 0:
			str = "" + angleWidth / (-2);
			break;
		case 1:
			str = "" + angleWidth / (-8) * 3;
			break;
		case 2:
			str = "" + angleWidth / (-4);
			break;
		case 3:
			str = "" + angleWidth / (-8);
			break;
		case 4:
			str = "0";
			break;
		case 5:
			str = "" + angleWidth / (8);
			break;
		case 6:
			str = "" + angleWidth / (4);
			break;
		case 7:
			str = "" + angleWidth / (8) * 3;
			break;
		case 8:
			str = "" + angleWidth / (2);
			break;

		default:
			break;
		}

		return str;
	}

	public String getYAngleText(int i) {
		String str = "";
		switch (i) {
		case 0:
			str = "" + angleHeight / (2);
			break;
		case 1:
			str = "" + angleHeight / (8) * 3;
			break;
		case 2:
			str = "" + angleHeight / (4);
			break;
		case 3:
			str = "" + angleHeight / (8);
			break;
		case 4:
			str = "0";
			break;
		case 5:
			str = "" + angleHeight / (-8);
			break;
		case 6:
			str = "" + angleHeight / (-4);
			break;
		case 7:
			str = "" + angleHeight / (-8) * 3;
			break;
		case 8:
			str = "" + angleHeight / (-2);
			break;

		default:
			break;
		}

		return str;
	}

	public void printHUD() {
		int kM = 5;
		int gM = 10;

		// Point Koordinaten für den Breitenskala
		Point pb1_0 = new Point(0, height / 2 - gM);
		Point pb2_0 = new Point(0, height / 2 + gM);
		Point pb_0Text = new Point(0 + 3, height / 2 + 15);

		Point pb1_1 = new Point(width / 8, height / 2 - kM);
		Point pb2_1 = new Point(width / 8, height / 2 + kM);
		Point pb_1Text = new Point(width / 8 + 3, height / 2 + 15);

		Point pb1_2 = new Point(width / 4, height / 2 - gM);
		Point pb2_2 = new Point(width / 4, height / 2 + gM);
		Point pb_2Text = new Point(width / 4 + 3, height / 2 + 15);

		Point pb1_3 = new Point(width / 8 * 3, height / 2 - kM);
		Point pb2_3 = new Point(width / 8 * 3, height / 2 + kM);
		Point pb_3Text = new Point(width / 8 * 3 + 3, height / 2 + 15);

		Point pb_4Text = new Point(width / 2 + 3, (height / 2 + 15));

		Point pb1_5 = new Point(width / 8 * 5, height / 2 - kM);
		Point pb2_5 = new Point(width / 8 * 5, height / 2 + kM);
		Point pb_5Text = new Point(width / 8 * 5 - 35, height / 2 + 15);

		Point pb1_6 = new Point(width / 4 * 3, height / 2 - gM);
		Point pb2_6 = new Point(width / 4 * 3, height / 2 + gM);
		Point pb_6Text = new Point(width / 4 * 3 - 35, height / 2 + 15);

		Point pb1_7 = new Point(width / 8 * 7, height / 2 - kM);
		Point pb2_7 = new Point(width / 8 * 7, height / 2 + kM);
		Point pb_7Text = new Point(width / 8 * 7 - 35, height / 2 + 15);

		Point pb1_8 = new Point(width, height / 2 - gM);
		Point pb2_8 = new Point(width, height / 2 + gM);
		Point pb_8Text = new Point(width - 35, height / 2 + 15);

		// Point Koordinaten für den Hoehenskala
		Point pl1_0 = new Point(width / 2 - gM, 0);
		Point pl2_0 = new Point(width / 2 + gM, 0);
		Point pl_0Text = new Point(width / 2 + 5, 0 + 10);

		Point pl1_1 = new Point(width / 2 - kM, height / 8);
		Point pl2_1 = new Point(width / 2 + kM, height / 8);
		Point pl_1Text = new Point(width / 2 + 5, height / 8 + 10);

		Point pl1_2 = new Point(width / 2 - gM, height / 4);
		Point pl2_2 = new Point(width / 2 + gM, height / 4);
		Point pl_2Text = new Point(width / 2 + 5, height / 4 + 10);

		Point pl1_3 = new Point(width / 2 - kM, height / 8 * 3);
		Point pl2_3 = new Point(width / 2 + kM, height / 8 * 3);
		Point pl_3Text = new Point(width / 2 + 5, height / 8 * 3 + 10);

		// Point pl_4Text = new Point(width / 2 + 3, (height / 2 + 15));

		Point pl1_5 = new Point(width / 2 - kM, height / 8 * 5);
		Point pl2_5 = new Point(width / 2 + kM, height / 8 * 5);
		Point pl_5Text = new Point(width / 2 + 5, height / 8 * 5 - 10);

		Point pl1_6 = new Point(width / 2 - gM, height / 4 * 3);
		Point pl2_6 = new Point(width / 2 + gM, height / 4 * 3);
		Point pl_6Text = new Point(width / 2 + 5, height / 4 * 3 - 10);

		Point pl1_7 = new Point(width / 2 - kM, height / 8 * 7);
		Point pl2_7 = new Point(width / 2 + kM, height / 8 * 7);
		Point pl_7Text = new Point(width / 2 + 5, height / 8 * 7 - 10);

		Point pl1_8 = new Point(width / 2 - gM, height);
		Point pl2_8 = new Point(width / 2 + gM, height);
		Point pl_8Text = new Point(width / 2 + 5, height - 10);

		Point pb1 = new Point(0, (height / 2));
		Point pb2 = new Point(width, (height / 2));

		Point pl1 = new Point(width / 2, 0);
		Point pl2 = new Point(width / 2, height);

		// Hoehen- und Breitenskala einzeichnen
		Imgproc.line(image, pb1, pb2, new Scalar(0, 255, 255, 0), 1);
		Imgproc.line(image, pl1, pl2, new Scalar(0, 255, 255, 0), 1);

		// Skalar Markierungen auf Breitengrad einzeichnen

		Imgproc.line(image, pb1_0, pb2_0, new Scalar(0, 255, 0, 0), 1);
		Imgproc.line(image, pb1_1, pb2_1, new Scalar(0, 255, 0, 0), 1);
		Imgproc.line(image, pb1_2, pb2_2, new Scalar(0, 255, 0, 0), 1);
		Imgproc.line(image, pb1_3, pb2_3, new Scalar(0, 255, 0, 0), 1);

		Imgproc.line(image, pb1_5, pb2_5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.line(image, pb1_6, pb2_6, new Scalar(0, 255, 0, 0), 1);
		Imgproc.line(image, pb1_7, pb2_7, new Scalar(0, 255, 0, 0), 1);
		Imgproc.line(image, pb1_8, pb2_8, new Scalar(0, 255, 0, 0), 1);

		// Skalar Beschriftung auf Breitengrad
		Imgproc.putText(image, getXAngleText(0), pb_0Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getXAngleText(1), pb_1Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getXAngleText(2), pb_2Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getXAngleText(3), pb_3Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getXAngleText(4), pb_4Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getXAngleText(5), pb_5Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getXAngleText(6), pb_6Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getXAngleText(7), pb_7Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getXAngleText(8), pb_8Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);

		// Skalar Markierungen auf Hoehengrad einzeichnen

		Imgproc.line(image, pl1_0, pl2_0, new Scalar(0, 255, 0, 0), 1);
		Imgproc.line(image, pl1_1, pl2_1, new Scalar(0, 255, 0, 0), 1);
		Imgproc.line(image, pl1_2, pl2_2, new Scalar(0, 255, 0, 0), 1);
		Imgproc.line(image, pl1_3, pl2_3, new Scalar(0, 255, 0, 0), 1);

		Imgproc.line(image, pl1_5, pl2_5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.line(image, pl1_6, pl2_6, new Scalar(0, 255, 0, 0), 1);
		Imgproc.line(image, pl1_7, pl2_7, new Scalar(0, 255, 0, 0), 1);
		Imgproc.line(image, pl1_8, pl2_8, new Scalar(0, 255, 0, 0), 1);

		// Skalar Beschriftung auf Hoehengrad
		Imgproc.putText(image, getYAngleText(0), pl_0Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getYAngleText(1), pl_1Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getYAngleText(2), pl_2Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getYAngleText(3), pl_3Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		// Imgproc.putText(image, getXAngleText(4), pl_4Text, 3, 0.5, new
		// Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getYAngleText(5), pl_5Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getYAngleText(6), pl_6Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getYAngleText(7), pl_7Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getYAngleText(8), pl_8Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
	}

}

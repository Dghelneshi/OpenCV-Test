import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class CamAngelPixelCalibration {

	Mat image = new Mat();
	double width; // 640
	double height; // 480
	double angelWidth = 60;
	double angelHeight = 30;

	public CamAngelPixelCalibration() {
	}

	public CamAngelPixelCalibration(Mat image) {
		this.image = image;
		width = image.width();
		height = image.height();
	}

	public Point calcPixeltoAngel(Point point) {
		double xAngelCoordinates, yAngelCoordinates;
		double xPixelCoordinates, yPixelCoordinates;
		xPixelCoordinates = point.x;
		yPixelCoordinates = point.y;

		if (xPixelCoordinates == (width / 2)) {
			xAngelCoordinates = 0;
		} else if (xPixelCoordinates > (width / 2)) {
			xAngelCoordinates = (angelWidth / 2) / (width / 2) * (xPixelCoordinates - (width / 2));
		} else { // xPixelCoordinates < width/2
			xAngelCoordinates = ((angelWidth / 2) / (width / 2) * xPixelCoordinates) - (angelWidth / 2);
		}

		if (yPixelCoordinates == (height / 2)) {
			yAngelCoordinates = 0;
		} else if (yPixelCoordinates > (height / 2)) {
			yAngelCoordinates = -((angelHeight / 2) / (height / 2) * (yPixelCoordinates - (height / 2)));
		} else { // xPixelCoordinates < height/2
			yAngelCoordinates = (angelHeight / 2) - ((angelHeight / 2) / (height / 2) * yPixelCoordinates);
		}

		return new Point(xAngelCoordinates, yAngelCoordinates);
	}

	public double calcPixeltoAngelX(double xPixelCoordinates) {
		double xAngelCoordinates;
		if (xPixelCoordinates == (width / 2)) {
			xAngelCoordinates = 0;
		} else if (xPixelCoordinates > (width / 2)) {
			xAngelCoordinates = (angelWidth / 2) / (width / 2) * (xPixelCoordinates - (width / 2));
		} else { // xPixelCoordinates < width/2
			xAngelCoordinates = ((angelWidth / 2) / (width / 2) * xPixelCoordinates) - (angelWidth / 2);
		}
		return xAngelCoordinates;
	}

	public double calcPixeltoAngelY(double yPixelCoordinates) {
		double yAngelCoordinates;
		if (yPixelCoordinates == (height / 2)) {
			yAngelCoordinates = 0;
		} else if (yPixelCoordinates > (height / 2)) {
			yAngelCoordinates = -((angelHeight / 2) / (height / 2) * (yPixelCoordinates - (height / 2)));
		} else { // xPixelCoordinates < height/2
			yAngelCoordinates = (angelHeight / 2) - ((angelHeight / 2) / (height / 2) * yPixelCoordinates);
		}

		return yAngelCoordinates;
	}

	public Point calcAngeltoPixel(Point angel) {

		double xAngelCoordinates, yAngelCoordinates;
		double xPixelCoordinates, yPixelCoordinates;
		xAngelCoordinates = angel.x;
		yAngelCoordinates = angel.y;

		if (xAngelCoordinates > 0) {
			xPixelCoordinates = (width / 2) / (angelWidth / 2) * (xAngelCoordinates) + (width / 2);
		} else { // xAngelCoordinates < angelWidth/2
			xPixelCoordinates = (width / 2) / -(angelWidth / 2) * (-(angelWidth / 2) - (xAngelCoordinates));
		}

		if (yAngelCoordinates > 0) {
			yPixelCoordinates = (height / 2) / (angelHeight / 2) * ((angelHeight / 2) - (yAngelCoordinates));
		} else { // yAngelCoordinates < angelHeight/2 240/15* -(x) +240
			yPixelCoordinates = (height / 2) / (angelHeight / 2) * -(yAngelCoordinates) + (height / 2);
		}

		return new Point(xPixelCoordinates, yPixelCoordinates);
	}

	public double calcAngeltoPixelX(double xAngelCoordinates) {
		double xPixelCoordinates;

		if (xAngelCoordinates > 0) {
			xPixelCoordinates = (width / 2) / (angelWidth / 2) * (xAngelCoordinates) + (width / 2);
		} else { // xAngelCoordinates < angelWidth/2
			xPixelCoordinates = (width / 2) / -(angelWidth / 2) * (-(angelWidth / 2) - (xAngelCoordinates));
		}
		return xPixelCoordinates;
	}

	public double calcAngeltoPixelY(double yAngelCoordinates) {
		double yPixelCoordinates;

		if (yAngelCoordinates > 0) {
			yPixelCoordinates = (height / 2) / (angelHeight / 2) * ((angelHeight / 2) - (yAngelCoordinates));
		} else { // yAngelCoordinates < angelHeight/2 240/15* -(x) +240
			yPixelCoordinates = (height / 2) / (angelHeight / 2) * -(yAngelCoordinates) + (height / 2);
		}

		return yPixelCoordinates;
	}

	// Point p1 = new Point(0, 200);
	// Point p2 = new Point(600, 200);

	public String getXAngelText(int i) {
		String str = "";
		switch (i) {
		case 0:
			str = "" + angelWidth / (-2);
			break;
		case 1:
			str = "" + angelWidth / (-8) * 3;
			break;
		case 2:
			str = "" + angelWidth / (-4);
			break;
		case 3:
			str = "" + angelWidth / (-8);
			break;
		case 4:
			str = "0";
			break;
		case 5:
			str = "" + angelWidth / (8);
			break;
		case 6:
			str = "" + angelWidth / (4);
			break;
		case 7:
			str = "" + angelWidth / (8) * 3;
			break;
		case 8:
			str = "" + angelWidth / (2);
			break;

		default:
			break;
		}

		return str;
	}

	public String getYAngelText(int i) {
		String str = "";
		switch (i) {
		case 0:
			str = "" + angelHeight / (2);
			break;
		case 1:
			str = "" + angelHeight / (8) * 3;
			break;
		case 2:
			str = "" + angelHeight / (4);
			break;
		case 3:
			str = "" + angelHeight / (8);
			break;
		case 4:
			str = "0";
			break;
		case 5:
			str = "" + angelHeight / (-8);
			break;
		case 6:
			str = "" + angelHeight / (-4);
			break;
		case 7:
			str = "" + angelHeight / (-8) * 3;
			break;
		case 8:
			str = "" + angelHeight / (-2);
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
		Imgproc.putText(image, getXAngelText(0), pb_0Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getXAngelText(1), pb_1Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getXAngelText(2), pb_2Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getXAngelText(3), pb_3Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getXAngelText(4), pb_4Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getXAngelText(5), pb_5Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getXAngelText(6), pb_6Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getXAngelText(7), pb_7Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getXAngelText(8), pb_8Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);

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
		Imgproc.putText(image, getYAngelText(0), pl_0Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getYAngelText(1), pl_1Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getYAngelText(2), pl_2Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getYAngelText(3), pl_3Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		// Imgproc.putText(image, getXAngelText(4), pl_4Text, 3, 0.5, new
		// Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getYAngelText(5), pl_5Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getYAngelText(6), pl_6Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getYAngelText(7), pl_7Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
		Imgproc.putText(image, getYAngelText(8), pl_8Text, 3, 0.5, new Scalar(0, 255, 0, 0), 1);
	}

}

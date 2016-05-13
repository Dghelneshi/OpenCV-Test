import org.opencv.core.Point;


public final class FDMath {
    
    private static float half_hFov;
    private static float half_vFov;
    
    private static double halfWidth;
    private static double halfHeight;
       
    public FDMath(float hFov, float vFov, int imageWidth, int imageHeight) {
        FDMath.setFov(hFov, vFov);
        FDMath.setImageSize(imageWidth, imageHeight);
    }

    public static Point normalizeCoord(Point pixelCoord) {
        double x0 = (pixelCoord.x + 0.5f - halfWidth)  /  halfWidth;
        double y0 = (pixelCoord.y + 0.5f - halfHeight) / -halfHeight;
        return new Point(x0,y0);
    }
    
    public static float hAngle(double x) {
        return (float) (x * half_hFov);
    }
    
    public static float vAngle(double y) {
        return (float) (y * half_vFov);
    }

    public static void setFov(float hFov, float vFov) {
        FDMath.half_hFov = hFov * 0.5f;
        FDMath.half_vFov = vFov * 0.5f;
    }
    
    public static void setImageSize(int imageWidth, int imageHeight) {
        FDMath.halfWidth  = imageWidth  * 0.5;
        FDMath.halfHeight = imageHeight * 0.5; 
    }
}


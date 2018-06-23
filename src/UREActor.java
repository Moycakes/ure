import java.awt.*;

public class UREActor  extends UREThing {

    URECamera camera;

    public UREActor(String thename, char theicon, Color thecolor, boolean addOutline) {
        super(thename, theicon, thecolor, addOutline);
    }

    public void attachCamera(URECamera thecamera) {
        camera = thecamera;
    }

    public void walkDir(int xdir, int ydir) {
        int destX = xdir + areaX();
        int destY = ydir + areaY();
        if (location.containerType() == UContainer.TYPE_CELL) {
            if (area().willAcceptThing(this, destX, destY)) {
                moveToCell(area(), destX, destY);
                if (camera != null)
                    camera.moveTo(area(), destX, destY);
            }
        }
    }
}
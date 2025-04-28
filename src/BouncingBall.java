import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.geometry.Primitive;
import javax.media.j3d.*;
import javax.vecmath.*;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import java.util.Enumeration;

public class BouncingBall {

    public BouncingBall() {
        // Crear el universo
        SimpleUniverse universe = new SimpleUniverse();

        // Crear la escena
        BranchGroup scene = createSceneGraph();

        // Configurar la vista
        universe.getViewingPlatform().setNominalViewingTransform();

        // Añadir la escena al universo
        universe.addBranchGraph(scene);
    }

    private BranchGroup createSceneGraph() {
        BranchGroup objRoot = new BranchGroup();

        // Iluminación
        setupLighting(objRoot);

        // Piso
        createFloor(objRoot);

        // Pelota
        createBouncingBall(objRoot);

        return objRoot;
    }

    private void createFloor(BranchGroup bg) {
        Appearance floorAppearance = new Appearance();
        floorAppearance.setMaterial(new Material(
            new Color3f(0.2f, 0.5f, 0.2f),  // Ambiental
            new Color3f(0.0f, 0.2f, 0.0f),  // Emisivo
            new Color3f(0.2f, 0.6f, 0.2f),  // Difuso
            new Color3f(0.0f, 0.7f, 0.0f),  // Especular
            1.0f                            // Brillo
        ));

        QuadArray floor = new QuadArray(4, GeometryArray.COORDINATES | GeometryArray.NORMALS);
        floor.setCoordinate(0, new Point3f(-2.0f, -1.0f, -2.0f));
        floor.setCoordinate(1, new Point3f(2.0f, -1.0f, -2.0f));
        floor.setCoordinate(2, new Point3f(2.0f, -1.0f, 2.0f));
        floor.setCoordinate(3, new Point3f(-2.0f, -1.0f, 2.0f));

        Vector3f normal = new Vector3f(0.0f, 1.0f, 0.0f);
        for (int i = 0; i < 4; i++) {
            floor.setNormal(i, normal);
        }

        Shape3D floorShape = new Shape3D(floor, floorAppearance);
        bg.addChild(floorShape);
    }

    private void createBouncingBall(BranchGroup bg) {
        TransformGroup ballTransform = new TransformGroup();
        ballTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        Appearance ballAppearance = new Appearance();
        ballAppearance.setMaterial(new Material(
            new Color3f(1.0f, 0.0f, 0.0f),  // Ambiental
            new Color3f(0.0f, 0.0f, 0.0f),  // Emisivo
            new Color3f(1.0f, 0.0f, 0.0f),  // Difuso
            new Color3f(1.0f, 1.0f, 1.0f),  // Especular
            100.0f                         // Brillo
        ));

        Sphere ball = new Sphere(0.3f, Primitive.GENERATE_NORMALS, ballAppearance);
        ballTransform.addChild(ball);

        bg.addChild(ballTransform);

        BounceBehavior bounce = new BounceBehavior(ballTransform);
        bounce.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0));
        bg.addChild(bounce);
    }

    private void setupLighting(BranchGroup bg) {
        AmbientLight ambientLight = new AmbientLight(new Color3f(0.3f, 0.3f, 0.3f));
        ambientLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0));
        bg.addChild(ambientLight);

        DirectionalLight mainLight = new DirectionalLight(
            new Color3f(0.8f, 0.8f, 0.8f),
            new Vector3f(-1.0f, -1.0f, -0.5f));
        mainLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0));
        bg.addChild(mainLight);

        DirectionalLight fillLight = new DirectionalLight(
            new Color3f(0.3f, 0.3f, 0.4f),
            new Vector3f(0.5f, -0.5f, -0.5f));
        fillLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0));
        bg.addChild(fillLight);
    }

    public static void main(String[] args) {
        System.setProperty("sun.awt.noerasebackground", "true");
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

        new BouncingBall();
    }
}

class BounceBehavior extends Behavior {

    private final TransformGroup targetTG;
    private final Transform3D transform = new Transform3D();
    private final WakeupOnElapsedFrames wakeup = new WakeupOnElapsedFrames(0);
    private long startTime;

    public BounceBehavior(TransformGroup tg) {
        this.targetTG = tg;
    }

    @Override
    public void initialize() {
        startTime = System.currentTimeMillis();
        wakeupOn(wakeup);
    }

    @Override
    public void processStimulus(Enumeration criteria) {
        long currentTime = System.currentTimeMillis();
        float t = (currentTime - startTime) / 1000.0f;

        float y = (float) Math.abs(Math.sin(t * Math.PI)) * 1.0f;

        transform.setTranslation(new Vector3f(0.0f, y, 0.0f));
        targetTG.setTransform(transform);

        wakeupOn(wakeup);
    }
}

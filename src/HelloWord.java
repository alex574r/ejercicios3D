import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.geometry.ColorCube;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;

public class HelloWord {

    public HelloWord() {
        // Crear el universo
        SimpleUniverse universe = new SimpleUniverse();
        
        // Crear el grupo raíz
        BranchGroup scene = createSceneGraph();
        
        // Configurar la vista por defecto
        universe.getViewingPlatform().setNominalViewingTransform();
        
        // Añadir el grupo raíz al universo
        universe.addBranchGraph(scene);
    }
    
    private BranchGroup createSceneGraph() {
        // Crear el grupo raíz
        BranchGroup objRoot = new BranchGroup();
        
        // Crear grupo de transformación para la rotación
        TransformGroup objRotate = new TransformGroup();
        objRotate.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        objRoot.addChild(objRotate);
        
        // Crear el cubo de colores
        objRotate.addChild(new ColorCube(0.4));
        
        // Configurar rotación automática
        setupRotation(objRotate);
        
        // Configurar iluminación
        setupLighting(objRoot);
        
        return objRoot;
    }
    
    private void setupRotation(TransformGroup tg) {
        // Crear interpolador de rotación
        Alpha rotationAlpha = new Alpha(-1, 8000); // 8 segundos por rotación completa
        
        Transform3D yAxis = new Transform3D();
        RotationInterpolator rotator = new RotationInterpolator(
            rotationAlpha, 
            tg, 
            yAxis, 
            0.0f, 
            (float) Math.PI * 2.0f);
        
        // Establecer límites de activación
        BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);
        rotator.setSchedulingBounds(bounds);
        
        tg.addChild(rotator);
    }
    
    private void setupLighting(BranchGroup bg) {
        // Crear luz ambiental (iluminación general)
        AmbientLight ambientLight = new AmbientLight(
            new Color3f(0.3f, 0.3f, 0.3f)); // Color gris suave
        //new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0
        ambientLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0,0.0,0.0),100.0));
        bg.addChild(ambientLight);
        
        // Crear luz direccional 1 (desde arriba)
        DirectionalLight directionalLight1 = new DirectionalLight(
            new Color3f(0.7f, 0.7f, 0.7f), // Color blanco
            new Vector3f(0.0f, -1.0f, -0.5f)); // Dirección de la luz
        directionalLight1.setInfluencingBounds(new BoundingSphere(new Point3d(0, 0, 0), 100));
        bg.addChild(directionalLight1);
        
        // Crear luz direccional 2 (desde un lado)
        DirectionalLight directionalLight2 = new DirectionalLight(
            new Color3f(0.5f, 0.5f, 0.5f), // Color blanco más tenue
            new Vector3f(1.0f, 0.0f, -0.5f)); // Dirección de la luz
        directionalLight2.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0));
        bg.addChild(directionalLight2);
        
        // Crear luz puntual (para resaltar detalles)
        PointLight pointLight = new PointLight(
            new Color3f(0.8f, 0.8f, 0.8f), // Color
            new Point3f(1.0f, 1.0f, 2.0f), // Posición
            new Point3f(1.0f, 0.1f, 0.0f)); // Atenuación
        pointLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0));
        bg.addChild(pointLight);
    }
    
    public static void main(String[] args) {
        System.setProperty("sun.awt.noerasebackground", "true");
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        
        ToolTipManager ttm = ToolTipManager.sharedInstance();
        ttm.setLightWeightPopupEnabled(false);
        
        new HelloWord();
    }
}
import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.geometry.Sphere;
// import com.sun.j3d.utils.image.TextureLoader; // Descomentar si usas texturas
import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.universe.ViewingPlatform;
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.GraphicsConfiguration;
import java.util.LinkedList;
import java.util.Enumeration;

public class SolarSystemWithCorrectTrailsV2 extends Applet { // Nuevo nombre

    // --- Constantes (sin cambios) ---
    private static final float SUN_RADIUS = 0.8f;
    private static final Color3f SUN_COLOR = new Color3f(1.0f, 0.9f, 0.2f);
    private static final Color3f SUN_EMISSIVE_COLOR = new Color3f(0.8f, 0.7f, 0.1f);
    private static final int MAX_TRAIL_POINTS = 150;
    private static final long TRAIL_UPDATE_INTERVAL_MS = 50;
    private static final Color3f TRAIL_COLOR = new Color3f(0.7f, 0.7f, 0.7f);
    private static final Object[][] PLANET_DATA = {
        {"Mercury", 0.10f, new Color3f(0.6f, 0.6f, 0.6f),  1.5f,  5000L,  8000L, null},
        {"Venus",   0.18f, new Color3f(0.9f, 0.7f, 0.4f),  2.2f,  8000L, 15000L, null},
        {"Earth",   0.20f, new Color3f(0.3f, 0.5f, 0.9f),  3.0f, 10000L,  3000L,
            new Object[][]{{"Moon", 0.05f, new Color3f(0.8f, 0.8f, 0.8f), 0.4f, 1000L, 1000L}}},
        {"Mars",    0.15f, new Color3f(0.8f, 0.4f, 0.2f),  4.0f, 15000L,  3500L, null},
        {"Jupiter", 0.45f, new Color3f(0.8f, 0.7f, 0.5f),  6.0f, 30000L,  1500L, null},
        {"Saturn",  0.40f, new Color3f(0.9f, 0.8f, 0.6f),  8.0f, 45000L,  1600L, null},
        {"Uranus",  0.30f, new Color3f(0.5f, 0.8f, 0.9f), 10.0f, 60000L,  2000L, null},
        {"Neptune", 0.28f, new Color3f(0.3f, 0.4f, 0.9f), 12.0f, 80000L,  2100L, null}
    };
    private final BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 200.0);
    private SimpleUniverse universe;
    private Canvas3D canvas3D;

    public SolarSystemWithCorrectTrailsV2() {
        setLayout(new BorderLayout());
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        canvas3D = new Canvas3D(config);
        add("Center", canvas3D);
        universe = new SimpleUniverse(canvas3D);
        setupCamera(); // Mover configuración de cámara a método
        BranchGroup scene = createSceneGraph();
        scene.compile();
        universe.addBranchGraph(scene);
    }

    private void setupCamera() {
        ViewingPlatform viewingPlatform = universe.getViewingPlatform();
        TransformGroup viewTransformGroup = viewingPlatform.getViewPlatformTransform();
        Transform3D viewTransform = new Transform3D();
        Point3d cameraPosition = new Point3d(0.0, 10.0, 25.0);
        Point3d sceneCenter = new Point3d(0.0, 0.0, 0.0);
        Vector3d upVector = new Vector3d(0.0, 1.0, 0.0);
        viewTransform.lookAt(cameraPosition, sceneCenter, upVector);
        viewTransform.invert();
        viewTransformGroup.setTransform(viewTransform);
        universe.getViewer().getView().setBackClipDistance(300.0);
        OrbitBehavior orbit = new OrbitBehavior(canvas3D, OrbitBehavior.REVERSE_ALL);
        orbit.setSchedulingBounds(bounds);
        viewingPlatform.setViewPlatformBehavior(orbit);
    }

    private BranchGroup createSceneGraph() {
        BranchGroup objRoot = new BranchGroup();
        // Fondo, Sol, Luces (sin cambios)
        Background background = new Background(new Color3f(0.0f, 0.0f, 0.0f));
        background.setApplicationBounds(bounds);
        objRoot.addChild(background);
        Appearance sunAppearance = new Appearance();
        Material sunMaterial = new Material();
        sunMaterial.setDiffuseColor(SUN_COLOR);
        sunMaterial.setEmissiveColor(SUN_EMISSIVE_COLOR);
        sunAppearance.setMaterial(sunMaterial);
        Sphere sun = new Sphere(SUN_RADIUS, Sphere.GENERATE_NORMALS | Sphere.GENERATE_TEXTURE_COORDS, 100, sunAppearance);
        objRoot.addChild(sun);
        PointLight sunLight = new PointLight(new Color3f(1.0f, 1.0f, 0.9f), new Point3f(0.0f, 0.0f, 0.0f), new Point3f(1.0f, 0.0f, 0.0f));
        sunLight.setInfluencingBounds(bounds);
        objRoot.addChild(sunLight);
        AmbientLight ambientLight = new AmbientLight(new Color3f(0.1f, 0.1f, 0.1f));
        ambientLight.setInfluencingBounds(bounds);
        objRoot.addChild(ambientLight);

        // --- Crear Planetas y sus Estelas ---
        for (Object[] data : PLANET_DATA) {
            // Extraer datos (sin cambios)
            String name = (String) data[0];
            float radius = (float) data[1];
            Color3f color = (Color3f) data[2];
            float orbitalRadius = (float) data[3];
            long orbitalPeriod = (long) data[4];
            long rotationPeriod = (long) data[5];
            Object[][] moonData = (Object[][]) data[6];

            Node[] planetComponents = createCelestialBodyWithTrail(
                radius, color, orbitalRadius, orbitalPeriod, rotationPeriod, false, name
            );

            if (planetComponents[0] != null) objRoot.addChild(planetComponents[0]);
            if (planetComponents[1] != null) objRoot.addChild(planetComponents[1]);
            if (planetComponents[2] != null) objRoot.addChild(planetComponents[2]);

            // --- Crear Lunas (sin cambios en la lógica) ---
            if (moonData != null && planetComponents[0] instanceof TransformGroup) {
                 TransformGroup planetSystemTG = (TransformGroup) planetComponents[0];
                 Node positionNode = planetSystemTG.getChild(0);
                 if (positionNode instanceof TransformGroup) {
                     TransformGroup planetPositionTG = (TransformGroup) positionNode;
                     for (Object[] mData : moonData) {
                        // Extraer datos luna
                        String moonName = (String) mData[0];
                        float moonRadius = (float) mData[1];
                        Color3f moonColor = (Color3f) mData[2];
                        float moonOrbitalRadius = (float) mData[3];
                        long moonOrbitalPeriod = (long) mData[4];
                        long moonRotationPeriod = (long) mData[5];
                        Node[] moonComponents = createCelestialBodyWithTrail( // Crea luna SIN estela
                             moonRadius, moonColor, moonOrbitalRadius, moonOrbitalPeriod, moonRotationPeriod, false, moonName
                         );
                        if (moonComponents[0] != null) {
                            planetPositionTG.addChild(moonComponents[0]);
                        }
                     }
                 }
            }
        }
        return objRoot;
    }

    // --- Método Helper Modificado para Crear Cuerpo Celeste CON Estela (SIN BY_REFERENCE) ---
    private Node[] createCelestialBodyWithTrail(float radius, Color3f color, float orbitalRadius,
                                                long orbitalPeriod, long rotationPeriod, boolean isSun, String name) {

        // Jerarquía de Transformación (sin cambios)
        TransformGroup orbitTG = new TransformGroup();
        orbitTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        TransformGroup positionTG = new TransformGroup();
        positionTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        Transform3D positionTransform = new Transform3D();
        positionTransform.setTranslation(new Vector3f(orbitalRadius, 0.0f, 0.0f));
        positionTG.setTransform(positionTransform);
        orbitTG.addChild(positionTG);
        TransformGroup rotationTG = new TransformGroup();
        rotationTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        positionTG.addChild(rotationTG);

        // Geometría del Cuerpo (sin cambios)
        Appearance appearance = new Appearance();
        Material material = new Material();
        material.setDiffuseColor(color);
        material.setSpecularColor(new Color3f(0.8f, 0.8f, 0.8f));
        material.setShininess(30.0f);
        if (isSun) material.setEmissiveColor(SUN_EMISSIVE_COLOR);
        else material.setAmbientColor(new Color3f(color.x * 0.2f, color.y * 0.2f, color.z * 0.2f));
        appearance.setMaterial(material);
        Sphere sphere = new Sphere(radius, Sphere.GENERATE_NORMALS | Sphere.GENERATE_TEXTURE_COORDS, 60, appearance);
        rotationTG.addChild(sphere);

        // Animaciones (sin cambios)
        if (orbitalPeriod > 0) {
            Alpha orbitAlpha = new Alpha(-1, Alpha.INCREASING_ENABLE, 0, 0, orbitalPeriod, 0, 0, 0, 0, 0);
            RotationInterpolator orbitInterpolator = new RotationInterpolator(orbitAlpha, orbitTG, new Transform3D(), 0.0f, (float) (Math.PI * 2.0));
            orbitInterpolator.setSchedulingBounds(bounds);
            orbitTG.addChild(orbitInterpolator);
        }
        if (rotationPeriod > 0) {
            Alpha rotationAlpha = new Alpha(-1, Alpha.INCREASING_ENABLE, 0, 0, rotationPeriod, 0, 0, 0, 0, 0);
            RotationInterpolator rotationInterpolator = new RotationInterpolator(rotationAlpha, rotationTG, new Transform3D(), 0.0f, (float) (Math.PI * 2.0));
            rotationInterpolator.setSchedulingBounds(bounds);
            rotationTG.addChild(rotationInterpolator);
        }

        // --- Crear la Estela (Versión sin BY_REFERENCE) ---
        Shape3D trailShape = null;
        Behavior trailBehavior = null;

        if (!isSun && orbitalRadius > 0.1f) {
            // Crear LineStripArray SIN BY_REFERENCE
            LineStripArray trailGeometry = new LineStripArray(MAX_TRAIL_POINTS,
                                                             GeometryArray.COORDINATES, // Solo coordenadas
                                                             new int[]{MAX_TRAIL_POINTS});
            // Capacidades necesarias
            trailGeometry.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
            //trailGeometry.setCapability(LineStripArray.ALLOW_VALID_VERTEX_COUNT_WRITE);

            // Apariencia (sin cambios)
            Appearance trailAppearance = new Appearance();
            ColoringAttributes trailColoring = new ColoringAttributes(TRAIL_COLOR, ColoringAttributes.SHADE_FLAT);
            LineAttributes trailLineAttribs = new LineAttributes();
            trailLineAttribs.setLineWidth(1.5f);
            trailLineAttribs.setLineAntialiasingEnable(true);
            trailAppearance.setColoringAttributes(trailColoring);
            trailAppearance.setLineAttributes(trailLineAttribs);
            trailAppearance.setMaterial(null);

            trailShape = new Shape3D(trailGeometry, trailAppearance);

            // Comportamiento (se pasa la geometría directamente)
            trailBehavior = new TrailBehavior(positionTG, trailGeometry, name);
            trailBehavior.setSchedulingBounds(bounds);
        }

        return new Node[]{orbitTG, trailShape, trailBehavior};
    }


    // --- Clase Interna para el Comportamiento de la Estela (SIN BY_REFERENCE) ---
    class TrailBehavior extends Behavior {
        private TransformGroup targetTG;
        private LineStripArray trailLine; // La geometría a actualizar
        private LinkedList<Point3f> trailPoints; // Historial de puntos
        private WakeupCondition wakeupCondition;
        private Transform3D currentTransform = new Transform3D();
        private Point3f currentPoint = new Point3f();
        private String bodyName;
        // NO SE NECESITA BUFFER: private Point3f[] pointsBuffer = new Point3f[MAX_TRAIL_POINTS];

        public TrailBehavior(TransformGroup target, LineStripArray lineStrip, String name) {
            this.targetTG = target;
            this.trailLine = lineStrip; // Guardar referencia a la geometría
            this.trailPoints = new LinkedList<>();
            this.bodyName = name;
            // NO SE NECESITA: trailLine.setCoordRefFloat(pointsBuffer);
            this.wakeupCondition = new WakeupOnElapsedTime(TRAIL_UPDATE_INTERVAL_MS);
        }

        @Override
        public void initialize() {
            wakeupOn(wakeupCondition);
        }

        @Override
        public void processStimulus(Enumeration criteria) {
            targetTG.getLocalToVworld(currentTransform);
            currentTransform.transform(currentPoint);
            trailPoints.addFirst(new Point3f(currentPoint)); // Añadir copia

            if (trailPoints.size() > MAX_TRAIL_POINTS) {
                trailPoints.removeLast();
            }

            // Actualizar la geometría directamente con setCoordinates
            if (!trailPoints.isEmpty()) {
                // Convertir la lista a un array CADA VEZ (costo bajo aquí)
                Point3f[] pointsArray = trailPoints.toArray(new Point3f[0]);

                // Rellenar los puntos restantes en el array con el último punto válido
                // para evitar usar coordenadas inválidas o (0,0,0) si trailPoints.size() < MAX_TRAIL_POINTS
                Point3f lastValidPoint = pointsArray[pointsArray.length - 1];
                Point3f[] fullPointsArray = new Point3f[MAX_TRAIL_POINTS];
                for(int i=0; i < MAX_TRAIL_POINTS; i++) {
                    if (i < pointsArray.length) {
                        fullPointsArray[i] = pointsArray[i];
                    } else {
                        // Rellenar el resto con el último punto conocido
                        // Esto evita que la línea se dibuje hacia (0,0,0)
                        fullPointsArray[i] = lastValidPoint;
                    }
                }


                // Establecer TODAS las coordenadas del LineStripArray
                // Java 3D usará setValidVertexCount para saber cuántas dibujar
                trailLine.setCoordinates(0, fullPointsArray); // Usar el array completo

                // Indicar cuántos puntos son realmente válidos para dibujar
                trailLine.setValidVertexCount(pointsArray.length); // Usar el tamaño real de trailPoints

            } else {
                trailLine.setValidVertexCount(0); // No dibujar nada si no hay puntos
            }

            wakeupOn(wakeupCondition);
        }
    } // Fin de TrailBehavior


    // --- Método main ---
    public static void main(String[] args) {
        System.setProperty("sun.awt.noerasebackground", "true");
        // Instanciar la clase corregida
        SolarSystemWithCorrectTrailsV2 solarSystemApplet = new SolarSystemWithCorrectTrailsV2();
        MainFrame mf = new MainFrame(solarSystemApplet, 1024, 768);
        mf.setTitle("Sistema Solar con Estelas (Corregido V2) - Java 3D");
    }
}
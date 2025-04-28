import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.image.TextureLoader; // Para texturas (opcional)
import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.universe.ViewingPlatform;
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Container; // Para textura de fondo
import java.awt.GraphicsConfiguration;

public class SolarSystem extends Applet {

    // --- Constantes de Configuración del Sistema Solar (Escaladas para Visualización) ---

    // Sol
    private static final float SUN_RADIUS = 0.8f;
    private static final Color3f SUN_COLOR = new Color3f(1.0f, 0.9f, 0.2f); // Amarillo brillante
    private static final Color3f SUN_EMISSIVE_COLOR = new Color3f(0.8f, 0.7f, 0.1f); // Que emita algo de luz

    // Planetas: Radio, Color, Radio Orbital, Periodo Orbital (ms), Periodo Rotación (ms)
    private static final Object[][] PLANET_DATA = {
        // Nombre (String), Radio (float), Color Difuso (Color3f), Radio Orbital (float), Periodo Orbital (long), Periodo Rotación (long), ¿Lunas? (MoonData[][])
        {"Mercury", 0.10f, new Color3f(0.6f, 0.6f, 0.6f),  1.5f,  5000L,  8000L, null},
        {"Venus",   0.18f, new Color3f(0.9f, 0.7f, 0.4f),  2.2f,  8000L, 15000L, null},
        {"Earth",   0.20f, new Color3f(0.3f, 0.5f, 0.9f),  3.0f, 10000L,  3000L, new Object[][]{{"Moon", 0.05f, new Color3f(0.8f, 0.8f, 0.8f), 0.4f, 1000L, 1000L}}}, // Tierra con Luna
        {"Mars",    0.15f, new Color3f(0.8f, 0.4f, 0.2f),  4.0f, 15000L,  3500L, null}, // Podría tener lunas pequeñas
        {"Jupiter", 0.45f, new Color3f(0.8f, 0.7f, 0.5f),  6.0f, 30000L,  1500L, null}, // Simplificado sin muchas lunas
        {"Saturn",  0.40f, new Color3f(0.9f, 0.8f, 0.6f),  8.0f, 45000L,  1600L, null}, // Simplificado sin anillos visibles (se podrían añadir como en el ejemplo anterior)
        {"Uranus",  0.30f, new Color3f(0.5f, 0.8f, 0.9f), 10.0f, 60000L,  2000L, null},
        {"Neptune", 0.28f, new Color3f(0.3f, 0.4f, 0.9f), 12.0f, 80000L,  2100L, null}
    };

    // Límites generales para las animaciones
    private final BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 200.0);

    public SolarSystem() {
        setLayout(new BorderLayout());
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        Canvas3D canvas3D = new Canvas3D(config);
        add("Center", canvas3D);

        SimpleUniverse universe = new SimpleUniverse(canvas3D);

        // Posicionar la cámara para ver el sistema solar
        ViewingPlatform viewingPlatform = universe.getViewingPlatform();
        TransformGroup viewTransformGroup = viewingPlatform.getViewPlatformTransform();
        Transform3D viewTransform = new Transform3D();
        // Vista elevada y alejada
        Point3d cameraPosition = new Point3d(0.0, 10.0, 20.0);
        Point3d sceneCenter = new Point3d(0.0, 0.0, 0.0);
        Vector3d upVector = new Vector3d(0.0, 1.0, 0.0);
        viewTransform.lookAt(cameraPosition, sceneCenter, upVector);
        viewTransform.invert();
        viewTransformGroup.setTransform(viewTransform);

        // Distancia de recorte (clip) más lejana para ver planetas distantes
        View view = universe.getViewer().getView();
        view.setBackClipDistance(300.0);

        BranchGroup scene = createSceneGraph();
        scene.compile();
        universe.addBranchGraph(scene);
    }

    private BranchGroup createSceneGraph() {
        BranchGroup objRoot = new BranchGroup();

        // --- Fondo Negro (Espacio) ---
        Background background = new Background(new Color3f(0.0f, 0.0f, 0.0f)); // Negro
        background.setApplicationBounds(bounds);
        objRoot.addChild(background);

        /* // Opcional: Fondo con Textura de Estrellas
        try {
            String texturePath = "path/to/your/starfield.jpg"; // Reemplaza con la ruta a tu imagen
            TextureLoader loader = new TextureLoader(texturePath, this);
            ImageComponent2D image = loader.getImage();
            Background bgTex = new Background();
            bgTex.setImage(image);
            bgTex.setImageScaleMode(Background.SCALE_FIT_ALL); // O SCALE_REPEAT
            bgTex.setApplicationBounds(bounds);
            objRoot.addChild(bgTex);
        } catch (Exception e) {
            System.err.println("Error cargando textura de fondo, usando color sólido: " + e.getMessage());
            Background background = new Background(new Color3f(0.0f, 0.0f, 0.0f)); // Negro
            background.setApplicationBounds(bounds);
            objRoot.addChild(background);
        }
        */


        // --- Sol ---
        Appearance sunAppearance = new Appearance();
        Material sunMaterial = new Material();
        sunMaterial.setDiffuseColor(SUN_COLOR);
        // Hacer que el sol brille por sí mismo
        sunMaterial.setEmissiveColor(SUN_EMISSIVE_COLOR);
        sunAppearance.setMaterial(sunMaterial);
        Sphere sun = new Sphere(SUN_RADIUS, Sphere.GENERATE_NORMALS | Sphere.GENERATE_TEXTURE_COORDS, 100, sunAppearance);
        objRoot.addChild(sun);

        // --- Luz Principal (emanando del Sol) ---
        PointLight sunLight = new PointLight(
            new Color3f(1.0f, 1.0f, 0.9f), // Luz blanca/amarillenta
            new Point3f(0.0f, 0.0f, 0.0f),  // Posición en el centro (Sol)
            new Point3f(1.0f, 0.0f, 0.0f)   // Atenuación (solo constante, sin atenuación por distancia para simplicidad)
        );
        sunLight.setInfluencingBounds(bounds);
        objRoot.addChild(sunLight);

        // --- Luz Ambiental Tenue ---
        AmbientLight ambientLight = new AmbientLight(new Color3f(0.1f, 0.1f, 0.1f));
        ambientLight.setInfluencingBounds(bounds);
        objRoot.addChild(ambientLight);


        // --- Crear Planetas ---
        for (Object[] data : PLANET_DATA) {
            String name = (String) data[0];
            float radius = (float) data[1];
            Color3f color = (Color3f) data[2];
            float orbitalRadius = (float) data[3];
            long orbitalPeriod = (long) data[4];
            long rotationPeriod = (long) data[5];
            Object[][] moonData = (Object[][]) data[6]; // Puede ser null

            // Crear el sistema para este planeta (órbita, posición, rotación, geometría)
            TransformGroup planetSystem = createCelestialBody(
                radius, color, orbitalRadius, orbitalPeriod, rotationPeriod, false // No es el sol
            );
            objRoot.addChild(planetSystem); // Añadir el sistema orbital del planeta a la raíz

            // --- Crear Lunas (si existen) ---
            if (moonData != null) {
                // El TransformGroup al que se añadirán las lunas es el que posiciona
                // el planeta RELATIVO a su órbita. Queremos que la luna orbite
                // alrededor de este punto.
                // Necesitamos acceder al grupo de posición/rotación del planeta.
                // La jerarquía es: planetSystem (órbita) -> planetPositionTG -> planetRotationTG -> Sphere
                 Node positionNode = planetSystem.getChild(0); // El primer hijo es planetPositionTG
                 if (positionNode instanceof TransformGroup) {
                     TransformGroup planetPositionTG = (TransformGroup) positionNode;

                     for (Object[] mData : moonData) {
                        String moonName = (String) mData[0];
                        float moonRadius = (float) mData[1];
                        Color3f moonColor = (Color3f) mData[2];
                        float moonOrbitalRadius = (float) mData[3];
                        long moonOrbitalPeriod = (long) mData[4];
                        long moonRotationPeriod = (long) mData[5]; // Las lunas también pueden rotar

                        // Crear el sistema para la luna, orbitando alrededor del planeta
                         TransformGroup moonSystem = createCelestialBody(
                             moonRadius, moonColor, moonOrbitalRadius, moonOrbitalPeriod, moonRotationPeriod, false
                         );
                         // Añadir el sistema orbital de la luna al GRUPO DE POSICIÓN del planeta
                         planetPositionTG.addChild(moonSystem);
                     }
                 }
            }
        }

        return objRoot;
    }

    // --- Método Helper para Crear un Cuerpo Celeste (Planeta o Luna) y su Sistema Orbital/Rotacional ---
    private TransformGroup createCelestialBody(float radius, Color3f color, float orbitalRadius,
                                               long orbitalPeriod, long rotationPeriod, boolean isSun) {

        // 1. Grupo para la Órbita alrededor del cuerpo padre (Sol o Planeta)
        TransformGroup orbitTG = new TransformGroup();
        orbitTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE); // Permitir rotación orbital

        // 2. Grupo para la Posición estática RELATIVA al centro de la órbita
        TransformGroup positionTG = new TransformGroup();
        Transform3D positionTransform = new Transform3D();
        positionTransform.setTranslation(new Vector3f(orbitalRadius, 0.0f, 0.0f)); // Posicionar a lo largo del eje X
        positionTG.setTransform(positionTransform);
        orbitTG.addChild(positionTG); // Añadir posición a la órbita

        // 3. Grupo para la Rotación sobre el propio eje del cuerpo
        TransformGroup rotationTG = new TransformGroup();
        rotationTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE); // Permitir rotación axial
        positionTG.addChild(rotationTG); // Añadir rotación a la posición

        // 4. Geometría del Cuerpo (Esfera)
        Appearance appearance = new Appearance();
        Material material = new Material();
        material.setDiffuseColor(color); // Color principal
        material.setSpecularColor(new Color3f(0.8f, 0.8f, 0.8f)); // Brillo blanco
        material.setShininess(30.0f);
        // Si es el sol, darle color emisivo
        if (isSun) {
            material.setEmissiveColor(new Color3f(color.x * 0.8f, color.y * 0.8f, color.z * 0.8f));
        } else {
             material.setAmbientColor(new Color3f(color.x * 0.2f, color.y * 0.2f, color.z * 0.2f)); // Que reaccione un poco a la luz ambiental
        }
        appearance.setMaterial(material);

        Sphere sphere = new Sphere(radius, Sphere.GENERATE_NORMALS | Sphere.GENERATE_TEXTURE_COORDS, 60, appearance);
        rotationTG.addChild(sphere); // Añadir esfera al grupo de rotación

        // 5. Animación de Órbita
        if (orbitalPeriod > 0) { // No aplicar órbita si el periodo es 0 o menos
            Alpha orbitAlpha = new Alpha(-1, Alpha.INCREASING_ENABLE, 0, 0, orbitalPeriod, 0, 0, 0, 0, 0);
            RotationInterpolator orbitInterpolator = new RotationInterpolator(
                orbitAlpha,
                orbitTG, // Rotar el grupo de órbita
                new Transform3D(), // Eje Y por defecto
                0.0f,
                (float) (Math.PI * 2.0)
            );
            orbitInterpolator.setSchedulingBounds(bounds);
            orbitTG.addChild(orbitInterpolator); // Añadir interpolador al grupo de órbita
        }

        // 6. Animación de Rotación Axial
        if (rotationPeriod > 0) {
            Alpha rotationAlpha = new Alpha(-1, Alpha.INCREASING_ENABLE, 0, 0, rotationPeriod, 0, 0, 0, 0, 0);
            RotationInterpolator rotationInterpolator = new RotationInterpolator(
                rotationAlpha,
                rotationTG, // Rotar el grupo de rotación
                new Transform3D(), // Eje Y por defecto
                0.0f,
                (float) (Math.PI * 2.0)
            );
            rotationInterpolator.setSchedulingBounds(bounds);
            rotationTG.addChild(rotationInterpolator); // Añadir interpolador al grupo de rotación
        }

        return orbitTG; // Devolver el grupo principal del sistema (el de órbita)
    }


    // Método main para ejecutar como aplicación
    public static void main(String[] args) {
        System.setProperty("sun.awt.noerasebackground", "true");
        // System.setProperty("j3d.rend", "D3D"); // Opcional: Forzar DirectX
        // System.setProperty("j3d.rend", "OpenGL"); // Opcional: Forzar OpenGL

        SolarSystem solarSystemApplet = new SolarSystem();
        MainFrame mf = new MainFrame(solarSystemApplet, 1024, 768); // Ventana más grande
        mf.setTitle("Sistema Solar Básico - Java 3D");
    }
}
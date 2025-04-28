import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.geometry.Sphere; // Importar Sphere
import com.sun.j3d.utils.applet.MainFrame; // Para una ventana simple
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.GraphicsConfiguration;

// Puedes ejecutar esto como una aplicación normal (main) o como un Applet si lo necesitas.
// Para simplificar, usaremos MainFrame para mostrarlo en una ventana.
public class SaturnEffect extends Applet {

    // --- Constantes de Configuración ---
    private static final int NUM_CUBES = 60; // Número de cubos en el anillo
    private static final float RING_RADIUS_X = 2.0f; // Semieje mayor de la elipse (radio en X)
    private static final float RING_RADIUS_Z = 1.5f; // Semieje menor de la elipse (radio en Z)
    private static final float CUBE_SIZE = 0.07f;  // Tamaño de los cubos orbitando
    private static final long ORBIT_DURATION = 12000; // Duración de una órbita completa (milisegundos)
    private static final long CUBE_ROTATION_DURATION = 4000; // Duración de la rotación propia del cubo
    private static final float CENTRAL_SPHERE_RADIUS = 0.6f; // Radio de la esfera central

    public SaturnEffect() {
        // Configuración básica para mostrar en una ventana
        setLayout(new BorderLayout());
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        Canvas3D canvas3D = new Canvas3D(config);
        add("Center", canvas3D);

        // Crear el universo SimpleUniverse
        SimpleUniverse universe = new SimpleUniverse(canvas3D);

        // Crear el grafo de escena
        BranchGroup scene = createSceneGraph();

        // --- Ajustar la Vista de la Cámara ---
        // Mover la cámara más arriba y atrás para una buena vista de los anillos
        Transform3D viewTransform = new Transform3D();
        viewTransform.lookAt(
            // new Point3d(0.0, 5.0, 4.0),   // Punto de vista más alto
            new Point3d(0.0, 4.0, 6.0),   // Punto de vista ligeramente elevado y más atrás
            new Point3d(0.0, 0.0, 0.0),   // Mirar al centro de la escena
            new Vector3d(0.0, 1.0, 0.0)   // El eje Y es "arriba"
        );
        viewTransform.invert(); // lookAt necesita inversión para la vista
        universe.getViewingPlatform().getViewPlatformTransform().setTransform(viewTransform);

        // Optimizar el grafo de escena (recomendado)
        scene.compile();

        // Añadir el grafo de escena al universo
        universe.addBranchGraph(scene);
    }

    private BranchGroup createSceneGraph() {
        BranchGroup objRoot = new BranchGroup();

        // --- Esfera Central (Saturno) ---
        Appearance sphereAppearance = new Appearance();
        Material yellowMaterial = new Material();
        // Color ambiental (cómo reacciona a la luz ambiental)
        yellowMaterial.setAmbientColor(new Color3f(0.3f, 0.25f, 0.1f));
        // Color difuso (el color principal bajo luz directa)
        yellowMaterial.setDiffuseColor(new Color3f(0.8f, 0.7f, 0.3f)); // Color amarillento/dorado
        // Color especular (el color del brillo)
        yellowMaterial.setSpecularColor(new Color3f(1.0f, 1.0f, 0.8f));
        // Brillo especular (qué tan concentrado es el brillo)
        yellowMaterial.setShininess(80.0f);
        sphereAppearance.setMaterial(yellowMaterial);
        // Crear la esfera con normales para iluminación y apariencia
        Sphere centralSphere = new Sphere(
            CENTRAL_SPHERE_RADIUS, // Radio
            Sphere.GENERATE_NORMALS | Sphere.GENERATE_TEXTURE_COORDS, // Generar normales para iluminación
            100, // Divisiones (mayor = más suave)
            sphereAppearance // Aplicar apariencia
        );
        objRoot.addChild(centralSphere);


        // --- Grupo de Transformación del Sistema de Anillos ---
        // Este grupo contendrá todos los cubos y rotará para crear la órbita
        TransformGroup ringOrbitGroup = new TransformGroup();
        // Permitir que este grupo sea modificado por el RotationInterpolator
        ringOrbitGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        objRoot.addChild(ringOrbitGroup);

        // Crear los cubos y posicionarlos en una elipse dentro del ringOrbitGroup
        for (int i = 0; i < NUM_CUBES; i++) {
            // Calcular el ángulo para la posición en la elipse
            double angle = 2.0 * Math.PI * i / NUM_CUBES;
            // Calcular coordenadas X y Z basadas en los radios de la elipse
            float x = (float) (RING_RADIUS_X * Math.cos(angle));
            float z = (float) (RING_RADIUS_Z * Math.sin(angle));
            // Mantener los anillos en el plano XZ (y=0)

            // --- Grupo de Transformación para la Posición Estática del Cubo ---
            // Este TG posiciona el cubo relativo al centro del ringOrbitGroup
            TransformGroup cubePositionTG = new TransformGroup();
            Transform3D cubePositionTransform = new Transform3D();
            // Establecer la traslación (posición) del cubo
            cubePositionTransform.setTranslation(new Vector3f(x, 0.0f, z));
            cubePositionTG.setTransform(cubePositionTransform);
            // Añadir este grupo de posición al grupo principal del anillo
            ringOrbitGroup.addChild(cubePositionTG);

            // --- Grupo de Transformación para la Rotación Individual del Cubo ---
            // Este TG rota el cubo sobre su propio eje
            TransformGroup cubeRotationTG = new TransformGroup();
            // Permitir que este grupo sea modificado por su propio RotationInterpolator
            cubeRotationTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
            // Añadir este grupo de rotación como hijo del grupo de posición
            cubePositionTG.addChild(cubeRotationTG);

            // Crear el cubo en sí
            ColorCube cube = new ColorCube(CUBE_SIZE);
            // Añadir la geometría del cubo al grupo de rotación
            cubeRotationTG.addChild(cube);

            // Configurar la rotación para este cubo individual
            setupIndividualCubeRotation(cubeRotationTG);
        }

        // Configurar la rotación (órbita) para todo el sistema de anillos
        setupRingOrbit(ringOrbitGroup);

        // Configurar la iluminación de la escena
        setupLighting(objRoot);

        return objRoot;
    }

    // Configura la rotación para un cubo individual alrededor de su eje Y
    private void setupIndividualCubeRotation(TransformGroup tg) {
        // Alpha define cómo progresa la animación en el tiempo
        Alpha rotationAlpha = new Alpha(
            -1, // loopCount: -1 para bucle infinito
            Alpha.INCREASING_ENABLE, // Modo: solo fase creciente
            0, // triggerTime: iniciar inmediatamente
            0, // phaseDelayDuration: sin retraso de fase
            CUBE_ROTATION_DURATION, // increasingAlphaDuration: duración de la fase creciente (una rotación)
            0, // increasingAlphaRampDuration: sin aceleración/desaceleración
            0, // alphaAtOneDuration: sin pausa en el valor máximo
            0, // decreasingAlphaDuration: no usado
            0, // decreasingAlphaRampDuration: no usado
            0  // alphaAtZeroDuration: sin pausa en el valor mínimo
        );

        // Eje de rotación (por defecto, una matriz identidad rota alrededor de Y)
        Transform3D yAxis = new Transform3D();
        // Podrías definir explícitamente el eje si quisieras rotar diferente:
        // AxisAngle4f axisAngle = new AxisAngle4f(0.0f, 1.0f, 0.0f, 0.0f); // Eje Y
        // yAxis.setRotation(axisAngle);

        // Interpolador que aplica la rotación
        RotationInterpolator rotator = new RotationInterpolator(
            rotationAlpha, // El Alpha que controla el tiempo
            tg,            // El TransformGroup objetivo a rotar
            yAxis,         // El eje y la transformación base
            0.0f,          // Ángulo mínimo (en radianes)
            (float) (Math.PI * 2.0f) // Ángulo máximo (una vuelta completa)
        );

        // Definir los límites espaciales donde la animación está activa
        BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);
        rotator.setSchedulingBounds(bounds);

        // Añadir el interpolador como hijo del TransformGroup del cubo
        tg.addChild(rotator);
    }

    // Configura la rotación orbital para todo el sistema de anillos
    private void setupRingOrbit(TransformGroup ringTG) {
        // Alpha para la órbita
        Alpha orbitAlpha = new Alpha(
            -1, // Bucle infinito
            Alpha.INCREASING_ENABLE,
            0, 0,
            ORBIT_DURATION, // Duración para una órbita completa
            0, 0, 0, 0, 0
        );

        // Eje de rotación para la órbita (alrededor del eje Y del mundo)
        Transform3D yAxis = new Transform3D();

        // Interpolador de rotación para la órbita
        RotationInterpolator orbitInterpolator = new RotationInterpolator(
            orbitAlpha, // Alpha de la órbita
            ringTG,     // Grupo objetivo (el que contiene todos los cubos)
            yAxis,      // Eje de rotación
            0.0f,       // Ángulo inicial
            (float) (Math.PI * 2.0f) // Ángulo final (una vuelta completa)
        );

        // Límites de activación
        BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);
        orbitInterpolator.setSchedulingBounds(bounds);

        // Añadir el interpolador al grupo del anillo
        ringTG.addChild(orbitInterpolator);
    }

    // Configuración de la iluminación (ajustada ligeramente)
    private void setupLighting(BranchGroup bg) {
        // Límites de influencia para las luces
        BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);

        // Luz Ambiental (iluminación base general)
        AmbientLight ambientLight = new AmbientLight(new Color3f(0.2f, 0.2f, 0.2f)); // Luz ambiental suave
        ambientLight.setInfluencingBounds(bounds);
        bg.addChild(ambientLight);

        // Luz Direccional 1 (simula una fuente lejana como el sol)
        DirectionalLight directionalLight1 = new DirectionalLight(
            new Color3f(0.8f, 0.8f, 0.7f),     // Color de la luz (ligeramente cálida)
            new Vector3f(-1.0f, -1.0f, -1.0f) // Dirección de la luz (desde arriba-izquierda-atrás)
        );
        directionalLight1.setInfluencingBounds(bounds);
        bg.addChild(directionalLight1);

        // Luz Direccional 2 (luz de relleno desde otra dirección)
        DirectionalLight directionalLight2 = new DirectionalLight(
            new Color3f(0.4f, 0.4f, 0.5f),    // Color de la luz (ligeramente fría)
            new Vector3f(1.0f, -0.5f, 1.0f)  // Dirección (desde abajo-derecha-adelante)
        );
        directionalLight2.setInfluencingBounds(bounds);
        bg.addChild(directionalLight2);

        /* Opcional: Luz Puntual si quieres un brillo localizado
        PointLight pointLight = new PointLight(
            new Color3f(0.7f, 0.7f, 0.7f),
            new Point3f(0.0f, 3.0f, 3.0f), // Posición de la luz
            new Point3f(1.0f, 0.1f, 0.0f)); // Atenuación (constante, lineal, cuadrática)
        pointLight.setInfluencingBounds(bounds);
        bg.addChild(pointLight);
        */
    }

    // Método main para ejecutar como aplicación
    public static void main(String[] args) {
        // Asegura que Java 3D funcione bien en entornos Swing/AWT
        System.setProperty("sun.awt.noerasebackground", "true");
        // System.setProperty("j3d.rend", "D3D"); // Opcional: Forzar DirectX en Windows
        // System.setProperty("j3d.rend", "OpenGL"); // Opcional: Forzar OpenGL

        // Crear la ventana principal y mostrar el applet dentro
        SaturnEffect saturnApplet = new SaturnEffect();
        MainFrame mf = new MainFrame(saturnApplet, 800, 600); // Ancho y alto de la ventana
        mf.setTitle("Efecto Saturno - Java 3D");
    }
}
import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.image.TextureLoader; // Para cargar texturas
import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.universe.ViewingPlatform;
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GraphicsConfiguration;
import java.net.URL; // Para cargar recursos (más robusto)

public class TexturedSolarSystem extends Applet {

    // --- Constantes de Configuración ---
    private static final float SUN_RADIUS = 0.8f;
    private static final String SUN_TEXTURE = "mars.jpg"; // Nombre archivo textura Sol

    // Planetas: Nombre, Radio, Textura Archivo, Radio Orbital, Periodo Orbital (ms), Periodo Rotación (ms), Lunas?
    private static final Object[][] PLANET_DATA = {
        // Nombre (String), Radio (float), Textura (String), Radio Orbital (float), Periodo Orbital (long), Periodo Rotación (long), Lunas (Object[][])
        {"Mercury", 0.10f, "mars.jpg",  1.5f,  5000L,  8000L, null},
        {"Venus",   0.18f, "mars.jpg",    2.2f,  8000L, 15000L, null},
        {"Earth",   0.20f, "mars.jpg",    3.0f, 10000L,  3000L, new Object[][]{{"Moon", 0.05f, "moon.jpg", 0.4f, 1000L, 2000L}}}, // Tierra con Luna texturizada
        {"Mars",    0.15f, "mars.jpg",     4.0f, 15000L,  3500L, null},
        {"Jupiter", 0.45f, "mars.jpg",  6.0f, 30000L,  1500L, null},
        {"Saturn",  0.40f, "mars.jpg",   8.0f, 45000L,  1600L, null}, // Añadir anillos sería un paso extra complejo
        {"Uranus",  0.30f, "mars.jpg",  10.0f, 60000L,  2000L, null},
        {"Neptune", 0.28f, "mars.jpg", 12.0f, 80000L,  2100L, null}
    };

    private static final String BACKGROUND_TEXTURE = "mars.jpg"; // Nombre archivo textura fondo

    // Límites generales para las animaciones y el fondo
    private final BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 300.0); // Aumentar bounds si es necesario

    public TexturedSolarSystem() {
        setLayout(new BorderLayout());
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        Canvas3D canvas3D = new Canvas3D(config);
        add("Center", canvas3D);

        SimpleUniverse universe = new SimpleUniverse(canvas3D);

        ViewingPlatform viewingPlatform = universe.getViewingPlatform();
        TransformGroup viewTransformGroup = viewingPlatform.getViewPlatformTransform();
        Transform3D viewTransform = new Transform3D();
        Point3d cameraPosition = new Point3d(0.0, 8.0, 18.0); // Ajustar cámara para mejor vista
        Point3d sceneCenter = new Point3d(0.0, 0.0, 0.0);
        Vector3d upVector = new Vector3d(0.0, 1.0, 0.0);
        viewTransform.lookAt(cameraPosition, sceneCenter, upVector);
        viewTransform.invert();
        viewTransformGroup.setTransform(viewTransform);

        View view = universe.getViewer().getView();
        view.setBackClipDistance(bounds.getRadius() * 2); // Ajustar clip distance a los bounds

        BranchGroup scene = createSceneGraph();
        scene.compile();
        universe.addBranchGraph(scene);
    }

    private BranchGroup createSceneGraph() {
        BranchGroup objRoot = new BranchGroup();

        // --- Fondo con Textura (Vía Láctea) ---
        try {
            // Intenta cargar como recurso primero (más robusto si está en el classpath)
            URL textureUrl = getClass().getClassLoader().getResource(BACKGROUND_TEXTURE);
            TextureLoader loader;
            if (textureUrl != null) {
                loader = new TextureLoader(textureUrl, this);
            } else {
                // Si no es recurso, intenta cargar como archivo local
                loader = new TextureLoader(BACKGROUND_TEXTURE, this);
            }

            ImageComponent2D image = loader.getImage();
             if (image == null) {
                System.err.println("Error: No se pudo cargar la imagen de fondo: " + BACKGROUND_TEXTURE);
                addSolidBackground(objRoot); // Fallback a fondo sólido
            } else {
                Background bgTex = new Background();
                bgTex.setImage(image);
                // Escalar para cubrir toda la esfera de fondo
                bgTex.setImageScaleMode(Background.SCALE_FIT_MAX);
                bgTex.setApplicationBounds(bounds);
                objRoot.addChild(bgTex);
            }
        } catch (Exception e) {
            System.err.println("Error cargando textura de fondo '" + BACKGROUND_TEXTURE + "', usando color sólido: " + e);
            addSolidBackground(objRoot); // Fallback a fondo sólido
        }

        // --- Sol ---
        Appearance sunAppearance = createAppearance(SUN_TEXTURE, true); // Crear apariencia con textura, es emisivo
        Sphere sun = new Sphere(SUN_RADIUS,
                                Sphere.GENERATE_NORMALS | Sphere.GENERATE_TEXTURE_COORDS | Sphere.GENERATE_NORMALS_INWARD, // Coords de textura! inward normals for background? no needed for sun
                                100, sunAppearance);
        objRoot.addChild(sun);

        // --- Luz Principal (emanando del Sol) ---
        // Más intensa para compensar texturas oscuras
        PointLight sunLight = new PointLight(
            new Color3f(1.0f, 1.0f, 0.95f),
            new Point3f(0.0f, 0.0f, 0.0f),
            new Point3f(1.0f, 0.005f, 0.0f) // Atenuación muy ligera para alcance lejano
        );
        sunLight.setInfluencingBounds(bounds);
        objRoot.addChild(sunLight);

        // --- Luz Ambiental Tenue ---
        AmbientLight ambientLight = new AmbientLight(new Color3f(0.15f, 0.15f, 0.15f)); // Ligeramente más brillante
        ambientLight.setInfluencingBounds(bounds);
        objRoot.addChild(ambientLight);

        // --- Crear Planetas ---
        for (Object[] data : PLANET_DATA) {
            String name = (String) data[0];
            float radius = (float) data[1];
            String textureFile = (String) data[2];
            float orbitalRadius = (float) data[3];
            long orbitalPeriod = (long) data[4];
            long rotationPeriod = (long) data[5];
            Object[][] moonData = (Object[][]) data[6];

            TransformGroup planetSystem = createCelestialBody(
                radius, textureFile, orbitalRadius, orbitalPeriod, rotationPeriod, false // No es el sol
            );
            objRoot.addChild(planetSystem);

            // --- Crear Lunas (si existen) ---
            if (moonData != null) {
                 Node positionNode = planetSystem.getChild(0);
                 if (positionNode instanceof TransformGroup) {
                     TransformGroup planetPositionTG = (TransformGroup) positionNode;

                     for (Object[] mData : moonData) {
                        String moonName = (String) mData[0];
                        float moonRadius = (float) mData[1];
                        String moonTextureFile = (String) mData[2];
                        float moonOrbitalRadius = (float) mData[3];
                        long moonOrbitalPeriod = (long) mData[4];
                        long moonRotationPeriod = (long) mData[5];

                         TransformGroup moonSystem = createCelestialBody(
                             moonRadius, moonTextureFile, moonOrbitalRadius, moonOrbitalPeriod, moonRotationPeriod, false
                         );
                         planetPositionTG.addChild(moonSystem);
                     }
                 }
            }
        }

        return objRoot;
    }

    // --- Método Helper para Crear Apariencia con Textura ---
    private Appearance createAppearance(String textureFileName, boolean isEmissive) {
        Appearance appearance = new Appearance();
        Material material = new Material();

        // Cargar Textura
        try {
             // Intenta cargar como recurso primero
            URL textureUrl = getClass().getClassLoader().getResource(textureFileName);
            TextureLoader loader;
             if (textureUrl != null) {
                loader = new TextureLoader(textureUrl, "LUMINANCE_ALPHA", this); // Usar LUMINANCE_ALPHA puede ser mejor para texturas no RGB
            } else {
                // Si no es recurso, intenta cargar como archivo local
                loader = new TextureLoader(textureFileName, "LUMINANCE_ALPHA", this);
            }

            Texture texture = loader.getTexture();
            if (texture == null) {
                 System.err.println("Advertencia: No se pudo cargar la textura: " + textureFileName + ". Usando color por defecto.");
                 // Color por defecto si falla la textura
                 material.setDiffuseColor(new Color3f(0.5f, 0.5f, 0.5f));
            } else {
                texture.setBoundaryModeS(Texture.WRAP); // Repetir textura si es necesario
                texture.setBoundaryModeT(Texture.WRAP);
                texture.setEnable(true); // Habilitar mipmapping
                texture.setMinFilter(Texture.MULTI_LEVEL_LINEAR); // Filtro para mipmapping
                texture.setMagFilter(Texture.BASE_LEVEL_LINEAR); // Filtro magnificacion

                appearance.setTexture(texture);

                // Atributos de Textura para combinar con iluminación
                TextureAttributes texAttr = new TextureAttributes();
                texAttr.setTextureMode(TextureAttributes.MODULATE); // Combinar textura con color de iluminación/material
                appearance.setTextureAttributes(texAttr);

                // Poner el color difuso del material en blanco para no teñir la textura
                material.setDiffuseColor(new Color3f(1.0f, 1.0f, 1.0f));
            }

        } catch (Exception e) {
            System.err.println("Error cargando textura '" + textureFileName + "': " + e);
             material.setDiffuseColor(new Color3f(0.5f, 0.5f, 0.5f)); // Color por defecto
        }

        // Propiedades del material (comunes con o sin textura)
        material.setSpecularColor(new Color3f(0.1f, 0.1f, 0.1f)); // Menos brillo especular en planetas texturizados
        material.setShininess(15.0f);
        if (isEmissive) {
            // Hacer que el sol brille (incluso con textura)
             material.setEmissiveColor(new Color3f(0.8f, 0.8f, 0.6f));
        } else {
             material.setAmbientColor(new Color3f(0.1f, 0.1f, 0.1f)); // Reacción a luz ambiental
        }
        appearance.setMaterial(material);


        // Habilitar transparencia si la textura tiene canal alfa (PNG)
        // TransparencyAttributes transAttr = new TransparencyAttributes();
        // transAttr.setTransparencyMode(TransparencyAttributes.BLENDED);
        // transAttr.setTransparency(0.0f); // 0 = opaco, 1 = invisible
        // appearance.setTransparencyAttributes(transAttr);


        return appearance;
    }


    // --- Método Helper para Crear Cuerpo Celeste con Textura ---
    private TransformGroup createCelestialBody(float radius, String textureFile, float orbitalRadius,
                                               long orbitalPeriod, long rotationPeriod, boolean isSun) {

        TransformGroup orbitTG = new TransformGroup();
        orbitTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        TransformGroup positionTG = new TransformGroup();
        Transform3D positionTransform = new Transform3D();
        positionTransform.setTranslation(new Vector3f(orbitalRadius, 0.0f, 0.0f));
        positionTG.setTransform(positionTransform);
        orbitTG.addChild(positionTG);

        TransformGroup rotationTG = new TransformGroup();
        rotationTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        positionTG.addChild(rotationTG);

        // Crear Apariencia usando el método helper
        Appearance appearance = createAppearance(textureFile, isSun);

        // Crear Esfera CON COORDENADAS DE TEXTURA
        Sphere sphere = new Sphere(radius,
                                   Sphere.GENERATE_NORMALS | Sphere.GENERATE_TEXTURE_COORDS, // ¡Importante!
                                   80, // Más detalle para texturas
                                   appearance);
        rotationTG.addChild(sphere);

        // Animación de Órbita
        if (orbitalPeriod > 0) {
            Alpha orbitAlpha = new Alpha(-1, Alpha.INCREASING_ENABLE, 0, 0, orbitalPeriod, 0, 0, 0, 0, 0);
            RotationInterpolator orbitInterpolator = new RotationInterpolator(
                orbitAlpha, orbitTG, new Transform3D(), 0.0f, (float) (Math.PI * 2.0));
            orbitInterpolator.setSchedulingBounds(bounds);
            orbitTG.addChild(orbitInterpolator);
        }

        // Animación de Rotación Axial
        if (rotationPeriod > 0) {
            Alpha rotationAlpha = new Alpha(-1, Alpha.INCREASING_ENABLE, 0, 0, rotationPeriod, 0, 0, 0, 0, 0);
            RotationInterpolator rotationInterpolator = new RotationInterpolator(
                rotationAlpha, rotationTG, new Transform3D(), 0.0f, (float) (Math.PI * 2.0));
            rotationInterpolator.setSchedulingBounds(bounds);
            rotationTG.addChild(rotationInterpolator);
        }

        return orbitTG;
    }

     // --- Método para añadir fondo sólido si falla la textura ---
    private void addSolidBackground(BranchGroup root) {
        Background background = new Background(new Color3f(0.05f, 0.05f, 0.15f)); // Azul muy oscuro
        background.setApplicationBounds(bounds);
        root.addChild(background);
    }


    // --- Método main ---
    public static void main(String[] args) {
        System.setProperty("sun.awt.noerasebackground", "true");

        // Intenta crear la ventana en el hilo de despacho de eventos de Swing/AWT
        javax.swing.SwingUtilities.invokeLater(() -> {
             TexturedSolarSystem solarSystemApplet = new TexturedSolarSystem();
             MainFrame mf = new MainFrame(solarSystemApplet, 1280, 800); // Ventana más grande
             mf.setTitle("Sistema Solar Texturizado - Java 3D");
             // Centrar la ventana (opcional)
             mf.setLocationRelativeTo(null);
        });
    }
}
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package newpackage.newpackage;

import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.geometry.ColorCube;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;

public class HelloWord extends JFrame {

    private TransformGroup viewTransformGroup;

    public HelloWord() {
        // Configuración ventana principal
        super("3D Viewer con Sliders");
        setLayout(new BorderLayout());
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Crear lienzo 3D
        Canvas3D canvas = new Canvas3D(SimpleUniverse.getPreferredConfiguration());
        add("Center", canvas);

        // Universo y escena
        SimpleUniverse universe = new SimpleUniverse(canvas);
        BranchGroup scene = createSceneGraph();
        universe.addBranchGraph(scene);

        // Obtener el grupo de transformación de la cámara
        viewTransformGroup = universe.getViewingPlatform().getViewPlatformTransform();

        // Panel de sliders
        add("South", createSlidersPanel());

        // Posición inicial de cámara
        updateViewTransform(30, 30, 0);

        setVisible(true);
    }

    private JPanel createSlidersPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1));

        JSlider sliderX = createSlider("Rotar X");
        JSlider sliderY = createSlider("Rotar Y");
        JSlider sliderZ = createSlider("Rotar Z");

        ChangeListener listener = e -> {
            int rotX = sliderX.getValue();
            int rotY = sliderY.getValue();
            int rotZ = sliderZ.getValue();
            updateViewTransform(rotX, rotY, rotZ);
        };

        sliderX.addChangeListener(listener);
        sliderY.addChangeListener(listener);
        sliderZ.addChangeListener(listener);

        panel.add(wrapSlider("Rotación X", sliderX));
        panel.add(wrapSlider("Rotación Y", sliderY));
        panel.add(wrapSlider("Rotación Z", sliderZ));

        return panel;
    }

    private JPanel wrapSlider(String label, JSlider slider) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(label), BorderLayout.WEST);
        panel.add(slider, BorderLayout.CENTER);
        return panel;
    }

    private JSlider createSlider(String name) {
        JSlider slider = new JSlider(-180, 180, 0);
        slider.setMajorTickSpacing(90);
        slider.setMinorTickSpacing(15);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setName(name);
        return slider;
    }

    private void updateViewTransform(int angleX, int angleY, int angleZ) {
    // Rotaciones
    Transform3D rotX = new Transform3D();
    rotX.rotX(Math.toRadians(angleX));

    Transform3D rotY = new Transform3D();
    rotY.rotY(Math.toRadians(angleY));

    Transform3D rotZ = new Transform3D();
    rotZ.rotZ(Math.toRadians(angleZ));

    // Combinación de rotaciones
    Transform3D combined = new Transform3D();
    combined.mul(rotX);
    combined.mul(rotY);
    combined.mul(rotZ);

    // Trasladar la cámara hacia atrás
    Transform3D translateBack = new Transform3D();
    translateBack.setTranslation(new Vector3f(0.0f, 0.0f, 5.0f)); // Alejar 5 unidades

    // Aplicar rotaciones y luego la traslación
    translateBack.mul(combined); // rotaciones después de trasladar
    viewTransformGroup.setTransform(translateBack);
}


    private BranchGroup createSceneGraph() {
        BranchGroup objRoot = new BranchGroup();

        TransformGroup movingGroup = new TransformGroup();
        movingGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        objRoot.addChild(movingGroup);

        TransformGroup rotatingGroup = new TransformGroup();
        rotatingGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        movingGroup.addChild(rotatingGroup);

        rotatingGroup.addChild(new ColorCube(0.3));

        setupRotation(rotatingGroup);
        setupMovement(movingGroup);
        setupLighting(objRoot);

        return objRoot;
    }

    private void setupRotation(TransformGroup tg) {
        Alpha rotationAlpha = new Alpha(-1, 8000);
        Transform3D yAxis = new Transform3D();
        RotationInterpolator rotator = new RotationInterpolator(
            rotationAlpha, tg, yAxis, 0.0f, (float) Math.PI * 2.0f);

        BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);
        rotator.setSchedulingBounds(bounds);
        tg.addChild(rotator);
    }

    private void setupMovement(TransformGroup tg) {
        Alpha movementAlpha = new Alpha(-1, Alpha.INCREASING_ENABLE |
                                              Alpha.DECREASING_ENABLE,
                                              0, 0,
                                              4000, 0, 0,
                                              4000, 0, 0);

        Transform3D movementAxis = new Transform3D();
        PositionInterpolator mover = new PositionInterpolator(
            movementAlpha, tg, movementAxis, -1.5f, 1.5f);
        mover.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0));
        tg.addChild(mover);
    }

    private void setupLighting(BranchGroup bg) {
        AmbientLight ambientLight = new AmbientLight(new Color3f(0.3f, 0.3f, 0.3f));
        ambientLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0));
        bg.addChild(ambientLight);

        DirectionalLight directionalLight1 = new DirectionalLight(
            new Color3f(0.7f, 0.7f, 0.7f), new Vector3f(0.0f, -1.0f, -0.5f));
        directionalLight1.setInfluencingBounds(new BoundingSphere(new Point3d(0, 0, 0), 100));
        bg.addChild(directionalLight1);

        DirectionalLight directionalLight2 = new DirectionalLight(
            new Color3f(0.5f, 0.5f, 0.5f), new Vector3f(1.0f, 0.0f, -0.5f));
        directionalLight2.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0));
        bg.addChild(directionalLight2);

        PointLight pointLight = new PointLight(
            new Color3f(0.8f, 0.8f, 0.8f),
            new Point3f(1.0f, 1.0f, 2.0f),
            new Point3f(1.0f, 0.1f, 0.0f));
        pointLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0));
        bg.addChild(pointLight);
    }

    public static void main(String[] args) {
        System.setProperty("sun.awt.noerasebackground", "true");
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

        new HelloWord();
    }
}

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This
 * Created by shenyuan on 16/5/27.
 */
public class LowPolyApplet extends JApplet {


    int width = 0, height = 0;
    float minThreshold = 2.5f, maxThreshold = 5.0f;
    int pointCount = 2000;

    public int orig[] = null;

    Image sourceImage;
    JPanel controlPanel, imagePanel;
    JLabel origLabel, finalLabel;
    JSlider pointSlider;

    // Applet init function
    public void init() {
        try {
            URL url = getClass().getClassLoader().getResource("lowpoly.jpg");
            sourceImage = ImageIO.read(new File(url.getFile()));
            //sourceImage = sourceImage.getScaledInstance(500, 500, Image.SCALE_SMOOTH);
            width = sourceImage.getWidth(null);
            height = sourceImage.getHeight(null);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        // Controlling panel
        controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout());
        controlPanel.setBackground(new Color(192,204,226));

        // Image panel
        imagePanel = new JPanel();
        imagePanel.setLayout(new GridLayout(1, 2, 15, 15));
        imagePanel.setBackground(Color.BLACK);

        // Show origin image
        origLabel = new JLabel("Original Image", new ImageIcon(sourceImage), JLabel.CENTER);
        origLabel.setVerticalTextPosition(JLabel.BOTTOM);
        origLabel.setHorizontalTextPosition(JLabel.CENTER);
        origLabel.setForeground(Color.blue);
        imagePanel.add(origLabel);

        // Show the low-poly result
        finalLabel = new JLabel("Final result", new ImageIcon(sourceImage), JLabel.CENTER);
        finalLabel.setVerticalTextPosition(JLabel.BOTTOM);
        finalLabel.setHorizontalTextPosition(JLabel.CENTER);
        finalLabel.setForeground(Color.blue);
        imagePanel.add(finalLabel);

        // Control panel
        JLabel txtLabel = new JLabel("Points percentage");
        txtLabel.setHorizontalAlignment(JLabel.CENTER);
        controlPanel.add(txtLabel, BorderLayout.NORTH);

        pointSlider = new JSlider(JSlider.HORIZONTAL, 0, 5000, pointCount);
        pointSlider.setMajorTickSpacing(500);
        pointSlider.setMinorTickSpacing(50);
        pointSlider.setPaintTicks(true);
        pointSlider.setPaintLabels(true);
        pointSlider.setBackground(new Color(192,204,226));
        pointSlider.addChangeListener(new ThresholdListener());
        controlPanel.add(pointSlider);


        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(imagePanel, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        Container cont = getContentPane();
        cont.removeAll();
        cont.setLayout(new BorderLayout());
        cont.setPreferredSize(new Dimension(500, 500));
        cont.add(scrollPane, BorderLayout.CENTER);
        //cont.setVisible(true);

        processImage();

    }

    public double[][] chooseTrianglePoint(int[] original) {
        Set<Point> pointSet = new HashSet<Point>();

        int totalPoints = width * height;
        int totalEdgePoints = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (original[i*width+j] == 0xffffffff) {
                    totalEdgePoints++;
                }
            }
        }
        double pEdge = (double)pointCount * 0.95 / totalEdgePoints;
        double pNonEdge = (double)pointCount * 0.05 / (totalPoints - totalEdgePoints);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (original[i*width+j] == 0xffffffff) {
                    if (Math.random() < pEdge)
                        pointSet.add(new Point(j, i));
                } else {
                    if (Math.random() < pNonEdge)
                        pointSet.add(new Point(j, i));
                }
            }
        }

        // Add four corner points.
        pointSet.add(new Point(0, 0));
        pointSet.add(new Point(width-1, 0));
        pointSet.add(new Point(0, height-1));
        pointSet.add(new Point(width-1, height-1));

        double[][] points = new double[pointSet.size()][2];
        int i = 0;
        for (Point p : pointSet) {
            points[i][0] = p.x;
            points[i][1] = p.y;
            i++;
        }

        return points;
    }

    private void processImage(){

        orig=new int[width*height];
        PixelGrabber grabber = new PixelGrabber(sourceImage, 0, 0, width, height, orig, 0, width);
        try {
            grabber.grabPixels();
        }
        catch(InterruptedException e2) {
            System.out.println("error: " + e2);
        }

        new Thread(){
            public void run(){
                long start, end;

                pointSlider.setEnabled(false);

                /*
                 * Detect edges
                 */
                start = System.currentTimeMillis();
                System.out.println("Detecting image edges...");
                int[] res = new int[width*height];
                CannyEdgeDetector detector = new CannyEdgeDetector();
                detector.setSourceImage(toBufferedImage(sourceImage));
                detector.setLowThreshold(minThreshold);
                detector.setHighThreshold(maxThreshold);
                detector.process();
                final BufferedImage edgeImage = detector.getEdgesImage();
                PixelGrabber grabber = new PixelGrabber(edgeImage, 0, 0, width, height, res, 0, width);
                try {
                    grabber.grabPixels();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.exit(1);
                }
                end = System.currentTimeMillis();
                System.out.println("Finish detecting image edges, using " + (end-start) + " ms.");

                /*
                 * Choose points
                 */
                System.out.println("Choosing points...");
                start = System.currentTimeMillis();
                double[][] points = chooseTrianglePoint(res);
                System.out.println("Point count: " + points.length);
                int[] pointsSelected = new int[width*height];
                for (int i = 0; i < pointsSelected.length; i++)
                    pointsSelected[i] = 0xff000000;
                for (int i = 0; i < points.length; i++) {
                    int x = (int)(points[i][0]);
                    int y = (int)(points[i][1]);
                    pointsSelected[y*width+x] = 0xffffffff;
                }
                end = System.currentTimeMillis();
                System.out.println("Finish choosing points, using " + (end-start) + " ms.");

                /*
                 * Triangulation
                 */
                System.out.println("Triangulating...");
                start = System.currentTimeMillis();
                List<Integer> trianglePoints = null;
                try {
                    trianglePoints = Delaunay.triangulate(points);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.exit(1);
                }
                List<Triangle> triangles = new ArrayList<Triangle>();
                for (int i = trianglePoints.size() - 1; i >= 0; i -= 3) {
                    Triangle triangle = new Triangle(trianglePoints.get(i), trianglePoints.get(i-1), trianglePoints.get(i-2));
                    triangles.add(triangle);
                }
                end = System.currentTimeMillis();
                System.out.println("Finish triangulation, using " + (end-start) + " ms.");
                /*
                 * Filling color and show final result
                 */
                final BufferedImage resultImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = resultImage.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                for (int i = 0; i < triangles.size(); i++) {
                    Point p1 = new Point(points[triangles.get(i).p1][0], points[triangles.get(i).p1][1]);
                    Point p2 = new Point(points[triangles.get(i).p2][0], points[triangles.get(i).p2][1]);
                    Point p3 = new Point(points[triangles.get(i).p3][0], points[triangles.get(i).p3][1]);
                    int middleX = (int)((p1.x + p2.x + p3.x) / 3);
                    int middleY = (int)((p1.y + p2.y + p3.y) / 3);
                    int[] x = new int[3];
                    int[] y = new int[3];
                    x[0] = (int)p1.x;
                    x[1] = (int)p2.x;
                    x[2] = (int)p3.x;
                    y[0] = (int)p1.y;
                    y[1] = (int)p2.y;
                    y[2] = (int)p3.y;

                    Polygon p = new Polygon(x, y, 3);
                    g2d.setColor(new Color(orig[middleY*width+middleX]));
                    g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawPolygon(p);
                    g2d.fill(p);
                }
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        finalLabel.setIcon(new ImageIcon(resultImage));
                    }
                });
                end = System.currentTimeMillis();
                System.out.println("Finish filling color, using " + (end-start) + " ms.");


                pointSlider.setEnabled(true);
            }
        }.start();
    }

    public static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }

        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        return bimage;
    }

    class ThresholdListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider)e.getSource();
            if (!source.getValueIsAdjusting()) {
                System.out.println("Use " + source.getValue() + "% of points");
                pointCount = source.getValue();
                processImage();
            }
        }
    }

    class Point implements Comparable<Point> {
        double precision = 0.000001;
        double x, y;
        Point( double x, double y) {
            this.x = x; this.y = y;
        }

        public int compareTo(Point o) {
            if (this == o || Math.abs(x - o.x) < precision && Math.abs(y - o.y) < precision)
                return 0;
            if (Math.abs(y - o.y) < precision) {
                if (x - o.x < 0)
                    return -1;
                else
                    return 1;
            } else {
                if (y - o.y < 0)
                    return -1;
                else
                    return 1;
            }
        }
    }

    static class Triangle {
        int p1, p2, p3;
        Triangle(int p1, int p2, int p3) {
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
        }
    }

}

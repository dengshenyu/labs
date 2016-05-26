import java.awt.*;
import java.awt.image.*;
import java.applet.*;
import java.net.*;
import java.io.*;
import java.lang.Math;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.JApplet;
import javax.imageio.*;
import javax.swing.event.*;



public class sobelDemo extends JApplet {

    Image edgeImage, accImage, outputImage;
    MediaTracker tracker = null;
    PixelGrabber grabber = null;
    int width = 0, height = 0;
    String fileNames[] = {"test.png"};

    javax.swing.Timer timer;
    //slider constraints
    static final int TH_MIN = 0;
    static final int TH_MAX = 255;
    static final int TH_INIT = 60;
    int threshold=TH_INIT;
    boolean thresholdActive=false;

    int imageNumber=0;
    static int progress=0;
    public int orig[] = null;

    Image image[] = new Image[fileNames.length];

    JProgressBar progressBar;
    JPanel controlPanel, imagePanel, progressPanel;
    JLabel origLabel, outputLabel,comboLabel,sigmaLabel,thresholdLabel,processing;
    JSlider thresholdSlider;
    JButton thresholding;
    JComboBox imSel;
    static sobel edgedetector;

    public Image getImage(String fileName) {
        try {
            return ImageIO.read(new File("/tmp/" + fileName));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // Applet init function
    public void init() {

        tracker = new MediaTracker(this);
        for(int i = 0; i < fileNames.length; i++) {
            //System.out.println(this.getCodeBase());
            image[i] = getImage(fileNames[i]);
            //image[i] = getImage(this.getCodeBase(),fileNames[i]);
            image[i] = image[i].getScaledInstance(256, 256, Image.SCALE_SMOOTH);
            tracker.addImage(image[i], i);
        }
        try {
            tracker.waitForAll();
        }
        catch(InterruptedException e) {
            System.out.println("error: " + e);
        }

        Container cont = getContentPane();
        cont.removeAll();
        cont.setBackground(Color.black);
        cont.setLayout(new BorderLayout());

        controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(2,3,15,0));
        controlPanel.setBackground(new Color(192,204,226));
        imagePanel = new JPanel();
        imagePanel.setBackground(new Color(192,204,226));
        progressPanel = new JPanel();
        progressPanel.setBackground(new Color(192,204,226));
        progressPanel.setLayout(new GridLayout(2,1));

        comboLabel = new JLabel("IMAGE");
        comboLabel.setHorizontalAlignment(JLabel.CENTER);
        controlPanel.add(comboLabel);
        sigmaLabel = new JLabel("");
        sigmaLabel.setHorizontalAlignment(JLabel.CENTER);
        controlPanel.add(sigmaLabel);
        thresholdLabel = new JLabel("Threshold Value = "+TH_INIT);
        thresholdLabel.setHorizontalAlignment(JLabel.CENTER);
        controlPanel.add(thresholdLabel);


        processing = new JLabel("Processing...");
        processing.setHorizontalAlignment(JLabel.LEFT);
        progressBar = new JProgressBar(0,100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true); //get space for the string
        progressBar.setString("");          //but don't paint it
        progressPanel.add(processing);
        progressPanel.add(progressBar);

        width = image[imageNumber].getWidth(null);
        height = image[imageNumber].getHeight(null);

        imSel = new JComboBox(fileNames);
        imageNumber = imSel.getSelectedIndex();
        imSel.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        imageNumber = imSel.getSelectedIndex();
                        origLabel.setIcon(new ImageIcon(image[imageNumber]));
                        processImage();
                    }
                }
        );
        controlPanel.add(imSel, BorderLayout.PAGE_START);

        timer = new javax.swing.Timer(100, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                progressBar.setValue(edgedetector.getProgress());
            }
        });

        origLabel = new JLabel("Original Image", new ImageIcon(image[imageNumber]), JLabel.CENTER);
        origLabel.setVerticalTextPosition(JLabel.BOTTOM);
        origLabel.setHorizontalTextPosition(JLabel.CENTER);
        origLabel.setForeground(Color.blue);
        imagePanel.add(origLabel);

        outputLabel = new JLabel("Edge Detected", new ImageIcon(image[imageNumber]), JLabel.CENTER);
        outputLabel.setVerticalTextPosition(JLabel.BOTTOM);
        outputLabel.setHorizontalTextPosition(JLabel.CENTER);
        outputLabel.setForeground(Color.blue);
        imagePanel.add(outputLabel);


        thresholding = new JButton("Thresholding Off");
        //thresholding.setVerticalTextPosition(AbstractButton.BOTTOM);
        //thresholding.setHorizontalTextPosition(AbstractButton.CENTER);
        thresholding.setBackground(Color.RED);
        thresholding.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if(thresholdActive==true){
                            thresholdActive=false;
                            thresholding.setBackground(Color.RED);
                            thresholding.setText("Thresholding Off");
                            thresholdSlider.setEnabled(false);
                        }
                        else{
                            thresholdActive=true;
                            thresholding.setBackground(Color.GREEN);
                            thresholding.setText("Thresholding ON");
                            thresholdSlider.setEnabled(true);
                        }
                        processImage();
                    }
                }
        );
        controlPanel.add(thresholding);

        thresholdSlider = new JSlider(JSlider.HORIZONTAL, TH_MIN, TH_MAX, TH_INIT);
        thresholdSlider.addChangeListener(new thresholdListener());
        thresholdSlider.setMajorTickSpacing(40);
        thresholdSlider.setMinorTickSpacing(10);
        thresholdSlider.setPaintTicks(true);
        thresholdSlider.setPaintLabels(true);
        thresholdSlider.setBackground(new Color(192,204,226));
        controlPanel.add(thresholdSlider);



        cont.add(controlPanel, BorderLayout.NORTH);
        cont.add(imagePanel, BorderLayout.CENTER);
        cont.add(progressPanel, BorderLayout.SOUTH);

        processImage();

    }
    class thresholdListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider)e.getSource();
            if (!source.getValueIsAdjusting()) {
                System.out.println("threshold="+source.getValue());
                threshold=source.getValue();
                thresholdLabel.setText("Threshold Value = "+source.getValue());
                processImage();
            }
        }
    }
    public int[] threshold(int[] original, int value) {
        for(int x=0; x<original.length; x++) {
            if((original[x] & 0xff)>=value)
                original[x]=0xffffffff;
            else
                original[x]=0xff000000;
        }
        return original;
    }
    private void processImage(){
        orig=new int[width*height];
        PixelGrabber grabber = new PixelGrabber(image[imageNumber], 0, 0, width, height, orig, 0, width);
        try {
            grabber.grabPixels();
        }
        catch(InterruptedException e2) {
            System.out.println("error: " + e2);
        }
        progressBar.setMaximum(width-4);

        processing.setText("Processing...");
        thresholdSlider.setEnabled(false);
        thresholding.setEnabled(false);
        imSel.setEnabled(false);
        edgedetector = new sobel();
        timer.start();

        new Thread(){
            public void run(){
                edgedetector.init(orig,width,height);
                int[] res = edgedetector.process();
                //for (int i = 0; i < res.length; i++)
                    //CommonUtils.printBits(res[i]);
                if(thresholdActive==true)
                    res=threshold(res, threshold);
                final Image output = createImage(new MemoryImageSource(width, height, res, 0, width));
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        outputLabel.setIcon(new ImageIcon(output));
                        processing.setText("Done");
                        if(thresholdActive==true){
                            thresholdSlider.setEnabled(true);
                        }
                        thresholding.setEnabled(true);
                        imSel.setEnabled(true);
                    }
                });
            }
        }.start();
    }

}
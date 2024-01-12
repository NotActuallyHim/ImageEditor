import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class ImageEditorPanel extends JPanel implements KeyListener, MouseListener {
    private Color[][] pixels;
    private static final int DISPLAY_HEIGHT = 100;
    private static final int DISPLAY_WIDTH = 255;
    private static final int BUFFER_SPACE = 25;
    private static final int BLUR_RADIUS = 5;
    private static final double CONTRAST_FACTOR = 0.1;
    
    public ImageEditorPanel() {
        BufferedImage imageIn = null;
        try {
            // the image should be in the main project folder, not in \src or \bin
            imageIn = ImageIO.read(new File("Unit5image.PNG"));
        } catch (IOException e) {
            System.out.println(e);
            System.exit(1);
        }
        pixels = makeColorArray(imageIn);
        setPreferredSize(new Dimension(Math.max(pixels.length, pixels[0].length) + BUFFER_SPACE + DISPLAY_WIDTH,
                Math.max(BUFFER_SPACE + DISPLAY_HEIGHT, Math.max(pixels.length, pixels[0].length))));
        setBackground(Color.WHITE);
        addKeyListener(this);
        addMouseListener(this);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawPixels(g);
        drawHistogram(g);
    }

    private void drawPixels(Graphics g) {
        for (int row = 0; row < pixels.length; row++) {
            for (int col = 0; col < pixels[0].length; col++) {
                g.setColor(pixels[row][col]);
                g.fillRect(col, row, 1, 1);
            }
        }
    }

    private void drawHistogram(Graphics g) {
        int[] hist = brightnessHistogram(pixels);
        int max = 0;
        g.setColor(Color.GRAY);
        g.fillRect(this.getWidth() - DISPLAY_WIDTH, BUFFER_SPACE, DISPLAY_WIDTH, DISPLAY_HEIGHT);
        for (int i = 0; i < hist.length; i++) {
            max = Math.max(max, hist[i]);
        }
        g.setColor(Color.BLACK);
        for (int i = 0; i < hist.length; i++) {
            int barHeight = barHeight(max, hist[i], DISPLAY_HEIGHT);
            int yCoordinate = BUFFER_SPACE + DISPLAY_HEIGHT - barHeight;
            g.fillRect(this.getWidth() - DISPLAY_WIDTH + i, yCoordinate, 1, barHeight);
        }
    }

    public Color[][] applyVintageFilter(Color[][] originalPixels) {
        pixels = grayScale(originalPixels);
        Color[][] vintagePixels = new Color[pixels.length][pixels[0].length];
        for (int i = 0; i < pixels.length; i++)
            for (int j = 0; j < pixels[0].length; j++) {
                Color originalColor = pixels[i][j];
                int red = clamp((int) (originalColor.getRed() * 1.2), 0, 255);
                int green = clamp((int) (originalColor.getGreen() * 0.9), 0, 255);
                int blue = clamp((int) (originalColor.getBlue() * 0.8), 0, 255);
                vintagePixels[i][j] = new Color(red, green, blue);
            }
        return vintagePixels;
    }

    private int clamp(int value, int min, int max) {
        return Math.min(max, Math.max(min, value));
    }

    public int barHeight(int max, int value, int displayHeight) {
        return (int) (((double) value / max) * displayHeight);
    }

    public Color[][] makeColorArray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Color[][] result = new Color[height][width];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                Color c = new Color(image.getRGB(col, row), true);
                result[row][col] = c;
            }
        }
        return result;
    }

    public Color[][] rotate(Color[][] pixels) {
        Color[][] result = new Color[pixels[0].length][pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[0].length; j++) {
                result[j][i] = pixels[i][j];
            }
        }
        result = flipHorizontal(result);
        return result;
    }

    public Color[][] flipHorizontal(Color[][] pixels) {
        Color[][] result = new Color[pixels.length][pixels[0].length];
        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < result[0].length; j++) {
                result[i][j] = pixels[i][pixels[i].length - 1 - j];
            }
        }
        return result;
    }

    public Color[][] flipVertical(Color[][] pixels) {
        Color[][] result = new Color[pixels.length][pixels[0].length];
        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < result[0].length; j++) {
                result[i][j] = pixels[pixels.length - 1 - i][j];
            }
        }
        return result;
    }

    public Color[][] grayScale(Color[][] pixels) {
        Color[][] result = new Color[pixels.length][pixels[0].length];
        Color temp;
        int gray;
        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[0].length; j++) {
                temp = pixels[i][j];
                gray = (int) (temp.getRed() * 0.2126 + temp.getGreen() * 0.7125 + temp.getBlue() * 0.0722);
                result[i][j] = new Color(gray, gray, gray);
            }
        }
        return result;
    }

    public Color[][] gaussianBlur(Color[][] pixels) {
        Color[][] result = new Color[pixels.length][pixels[0].length];
        double[][] kernel = gaussianKernel();

        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[0].length; j++) {
                double red = 0, blue = 0, green = 0;

                for (int p = -BLUR_RADIUS; p <= BLUR_RADIUS; p++) {
                    for (int k = -BLUR_RADIUS; k <= BLUR_RADIUS; k++) {
                        int x = Math.min(Math.max(i + p, 0), pixels.length - 1);
                        int y = Math.min(Math.max(j + k, 0), pixels[0].length - 1);

                        double weight = kernel[p + BLUR_RADIUS][k + BLUR_RADIUS];
                        red += pixels[x][y].getRed() * weight;
                        blue += pixels[x][y].getBlue() * weight;
                        green += pixels[x][y].getGreen() * weight;
                    }
                }

                result[i][j] = new Color((int) Math.round(red), (int) Math.round(green), (int) Math.round(blue));
            }
        }

        return result;
    }

    private double[][] gaussianKernel() {
        int diameter = 2 * BLUR_RADIUS + 1;
        double[][] kernel = new double[diameter][diameter];
        double sigma = BLUR_RADIUS / 3.0;

        double sum = 0;

        // based off of the equation
        for (int i = -BLUR_RADIUS; i <= BLUR_RADIUS; i++) {
            for (int j = -BLUR_RADIUS; j <= BLUR_RADIUS; j++) {
                double exponent = -(i * i + j * j) / (2 * sigma * sigma);
                kernel[i + BLUR_RADIUS][j + BLUR_RADIUS] = Math.exp(exponent) / (2 * Math.PI * sigma * sigma);
                sum += kernel[i + BLUR_RADIUS][j + BLUR_RADIUS];
            }
        }

        // Normalize the kernel so that they all add up to be 1
        for (int i = 0; i < diameter; i++) {
            for (int j = 0; j < diameter; j++) {
                kernel[i][j] /= sum;
            }
        }

        return kernel;
    }

    public Color[][] contrast(Color[][] pixels) {
        Color[][] result = new Color[pixels.length][pixels[0].length];
        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < result[0].length; j++) {
                int red = pixels[i][j].getRed(), green = pixels[i][j].getGreen(), blue = pixels[i][j].getBlue();
                int newRed = (int) Math.max(0, Math.min(255, red * contrastWeight(red)));
                int newGreen = (int) Math.max(0, Math.min(255, green * contrastWeight(green)));
                int newBlue = (int) Math.max(0, Math.min(255, blue * contrastWeight(blue)));
                result[i][j] = new Color(newRed, newGreen, newBlue);
            }
        }
        return result;
    }

    public double contrastWeight(int value) {
        double error = Math.abs(((255 / 2.0) - value) / (225 / 2.0));
        if (value > 128) {
            return 1 - error * CONTRAST_FACTOR;
        } else {
            return 1 + error * CONTRAST_FACTOR;
        }

    }

    public int[] brightnessHistogram(Color[][] pixels) {
        int[] hist = new int[255];
        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[0].length; j++) {
                int bin = (int) (pixels[i][j].getRed() * 0.21 + pixels[i][j].getGreen() * 0.72
                        + pixels[i][j].getBlue() * 0.07);
                bin = Math.max(0, Math.min(254, bin));
                hist[bin]++;
            }
        }
        return hist;
    }

    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_1 -> pixels = rotate(pixels);
            case KeyEvent.VK_2 -> pixels = grayScale(pixels);
            case KeyEvent.VK_3 -> pixels = flipHorizontal(pixels);
            case KeyEvent.VK_4 -> pixels = flipVertical(pixels);
            case KeyEvent.VK_5 -> pixels = gaussianBlur(pixels);
            case KeyEvent.VK_6 -> pixels = contrast(pixels);
            case KeyEvent.VK_7 -> pixels = applyVintageFilter(pixels);
        }
        repaint();
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

}
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.*;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class ImageEditorPanel extends JPanel implements KeyListener, MouseListener {

    Color[][] pixels;
    int fps = 30;
    final int DISPLAY_HEIGHT = 100;
    final int DISPLAY_WIDTH = 255;
    final int BUFFER_SPACE = 25;

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
        for (int row = 0; row < pixels.length; row++) {
            for (int col = 0; col < pixels[0].length; col++) {
                g.setColor(pixels[row][col]);
                g.fillRect(col, row, 1, 1);
            }
        }
        displayHist(g);
    }

    public void displayHist(Graphics g) {
        int[] hist = brightnessHist(pixels);
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

    public Color[][] applyVintageFilter(Color[][] pixels) {
        Color[][] vintagePixels = new Color[pixels.length][pixels[0].length];

        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[0].length; j++) {
                Color originalColor = pixels[i][j];

                int red = (int) (originalColor.getRed() * 1.2);
                int green = (int) (originalColor.getGreen() * 0.9);
                int blue = (int) (originalColor.getBlue() * 0.8);

                red = Math.min(255, Math.max(0, red));
                green = Math.min(255, Math.max(0, green));
                blue = Math.min(255, Math.max(0, blue));

                vintagePixels[i][j] = new Color(red, green, blue);
            }
        }

        return vintagePixels;
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

    public Color[][] gaussianBlur(Color[][] pixels, int blurRadius) {
        Color[][] result = new Color[pixels.length][pixels[0].length];
        double[][] kernel = gaussianKernel(blurRadius);

        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[0].length; j++) {
                double red = 0, blue = 0, green = 0;

                for (int p = -blurRadius; p <= blurRadius; p++) {
                    for (int k = -blurRadius; k <= blurRadius; k++) {
                        int x = Math.min(Math.max(i + p, 0), pixels.length - 1);
                        int y = Math.min(Math.max(j + k, 0), pixels[0].length - 1);

                        double weight = kernel[p + blurRadius][k + blurRadius];
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

    private double[][] gaussianKernel(int blurRadius) {
        int diameter = 2 * blurRadius + 1;
        double[][] kernel = new double[diameter][diameter];
        double sigma = blurRadius / 3.0;

        double sum = 0;

        // based off of the equation
        for (int i = -blurRadius; i <= blurRadius; i++) {
            for (int j = -blurRadius; j <= blurRadius; j++) {
                double exponent = -(i * i + j * j) / (2 * sigma * sigma);
                kernel[i + blurRadius][j + blurRadius] = Math.exp(exponent) / (2 * Math.PI * sigma * sigma);
                sum += kernel[i + blurRadius][j + blurRadius];
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

    public Color[][] contrast(Color[][] pixels, double contrastFactor) {
        Color[][] result = new Color[pixels.length][pixels[0].length];
        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < result[0].length; j++) {
                int red = pixels[i][j].getRed(), green = pixels[i][j].getGreen(), blue = pixels[i][j].getBlue();
                int newRed = (int) Math.max(0, Math.min(255, red * conWeight(red, contrastFactor)));
                int newGreen = (int) Math.max(0, Math.min(255, green * conWeight(green, contrastFactor)));
                int newBlue = (int) Math.max(0, Math.min(255, blue * conWeight(blue, contrastFactor)));
                result[i][j] = new Color(newRed, newGreen, newBlue);
            }
        }
        return result;
    }

    public double conWeight(int value, double contrastFactor) {
        double error = Math.abs(((255 / 2.0) - value) / (225 / 2.0));
        if (value > 128) {
            return 1 - error * contrastFactor;
        } else {
            return 1 + error * contrastFactor;
        }

    }

    public int[] brightnessHist(Color[][] pixels) {
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
        if (e.getKeyCode() == KeyEvent.VK_R) {
            pixels = rotate(pixels);
        }
        if (e.getKeyCode() == KeyEvent.VK_1) {
            pixels = grayScale(pixels);
        }
        if (e.getKeyCode() == KeyEvent.VK_2) {
            pixels = flipHorizontal(pixels);
        }
        if (e.getKeyCode() == KeyEvent.VK_3) {
            pixels = gaussianBlur(pixels, 5);
        }
        if (e.getKeyCode() == KeyEvent.VK_4) {
            pixels = contrast(pixels, 0.1);
        }
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

    public void run() {
        while (true) {
            repaint();
            delay(1000 / fps);
        }

    }

    public void delay(int n) {
        try {
            Thread.sleep(n);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
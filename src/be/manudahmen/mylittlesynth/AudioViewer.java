/*
 * This file is part of Plants-Growth-2
 *     Plants-Growth-2 is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Plants-Growth-2 is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Plants-Growth-2.  If not, see <http://www.gnu.org/licenses/>.
 */

package be.manudahmen.mylittlesynth;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.*;
import java.util.stream.Stream;

public class AudioViewer {
    private int numberOfSamplesPerPixel = 1;
    private final LinkedList<Double> doubles = new LinkedList<>();
    private Map<Object, ArrayList<Double>> volumeEnvelopes = Collections.synchronizedMap(new HashMap<>());
    private LinkedList<Double> getDoubles() {
        return doubles;
    }

    private void addDouble(Double value) {
        this.doubles.add(value);
    }

    public void sendEnvelopeVolume(int tone, double volume) {
        if (!volumeEnvelopes.containsKey(tone))
            volumeEnvelopes.put(tone, new ArrayList<>());
        this.volumeEnvelopes.get(tone).add(volume);
    }

    private final int channels;
    private final float sampleRate;
    private final Canvas canvas;
    private double[] oldValues;
    private double min = 0;
    private double max = Short.MAX_VALUE;
    private double position;
    private final LinkedList<Double> values;
    private boolean running;

    public AudioViewer(float sampleRate, int channels, Canvas canvas) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.canvas = canvas;
        oldValues = new double[channels];
        running = true;
        values = new LinkedList<>();

        new CanvasRedrawTask().start();

    }

    void render(long timeElapsed, int numberOfSamples) {

        GraphicsContext context2D = canvas.getGraphicsContext2D();
        int maxWidth = (int) canvas.getWidth();
        drawBorder(canvas.getGraphicsContext2D(), Color.RED);
        Enveloppe enveloppe = new Enveloppe(1);
        final double[] yPoints = new double[enveloppe.points.length];
        final double[] xPoints = new double[enveloppe.points.length];
        final double[] xPoints2 = new double[maxWidth];
        final double[] yPoints2 = new double[maxWidth];
        final double maxHeight = canvas.getHeight() / 2;
        //System.out.println("maxHeight:" + maxHeight);
        for (int i = 0; i < enveloppe.points.length; i++) {

            double y = enveloppe.points[i].getY() * maxHeight;
            double z = enveloppe.points[i].getZ() * canvas.getWidth();

            xPoints[i] = z;
            yPoints[i] = -y + maxHeight;


        }
        for (int i = 0; i < xPoints2.length; i++) {

            double v = enveloppe.getForm(1.0 * i / maxWidth);
            xPoints2[i] = i;
            yPoints2[i] = -v * maxHeight + maxHeight;


        }
        LinkedList<Double> doubles2 = getDoubles();
        if (doubles2.size() >= channels) {
            //System.out.println("DOUBLES:" + doubles2.getFirst());
            int size = (int) Math.min(canvas.getWidth() * 2, doubles2.size() / numberOfSamplesPerPixel);

            final double[] samples = new double[size];

            int total = 0;

            do {
                int i = 0;
                try {
                    while ((doubles2.size() > 0) && (doubles2.getFirst() != null) && (i < size)) {
                        samples[i++] = doubles2.getFirst();
                        doubles2.removeFirst();
                        total++;
                    }
                } catch (NoSuchElementException ex) {
                }
            } while ((sampleSpeed(timeElapsed, numberOfSamples) > treatmentSpeed(timeElapsed, total)));

            final double[] xpoints = new double[size / 2];
            final double[] xs = new double[size / 2];
            final double[] ys1 = new double[size / 2];
            final double[] ys2 = new double[size / 2];
            final double[][] env = new double[volumeEnvelopes.size()][size / 2];


            int halfSize = size / 2;

            for (int i = 0; i < halfSize; ) {

                xs[i] = position++;

                xpoints[i] = xs[i];

                int k = 0;

                ys1[i] = 0;
                ys2[i] = 0;

                while (k < numberOfSamplesPerPixel * 2) {
                    ys1[i] += samples[i + k++] * maxHeight / max + maxHeight;

                    ys2[i] += samples[i + k++] * maxHeight / max + maxHeight;


                    if (position >= canvas.getWidth()) {
                        position = 0;
                    }
                }


                ys1[i] /= numberOfSamplesPerPixel;
                ys2[i] /= numberOfSamplesPerPixel;
                i += 1;

            }

            for (int i = 0; i < volumeEnvelopes.size(); i++) {
                double[] envi = env[i];
                volumeEnvelopes.forEach((integer, doubles) -> {
                    Object[] doubles1 = doubles.toArray();
                    double[] unboxed = Stream.of(doubles1).mapToDouble(value -> (double) value).toArray();
                    context2D.setFill(Color.BLACK);
                    context2D.setStroke(Color.GREEN);
                    // context2D.strokePolyline(xPoints2, unboxed, unboxed.length);

                });
            }
            context2D.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            context2D.strokePolyline(xPoints, yPoints, xPoints.length);
            context2D.strokePolyline(xPoints2, yPoints2, xPoints2.length);
            for (int d = 0; d < env.length; d++) {
            }
            context2D.setFill(Color.BLACK);
            context2D.setStroke(Color.BLUE);
            context2D.setLineWidth(4.0);
            context2D.strokePolyline(xpoints, ys1, size / 2);
            context2D.strokePolyline(xpoints, ys2, size / 2);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //System.out.println("Points: " + size / 2 + "(" + xpoints[0] + ", " + ys1[0] + ")");

        }
    }

    private double treatmentSpeed(long time, int sample) {
        return 1.0 * sample / time;
    }


    private double sampleSpeed(long time, int sample) {

        return 1.0 * sample / time;
    }

    private void drawBorder(GraphicsContext g, Color selectedColor) {
        final double canvasWidth = g.getCanvas().getWidth();
        final double canvasHeight = g.getCanvas().getHeight();

        g.setStroke(selectedColor);
        g.setLineWidth(4);
        g.strokeRect(0, 0, canvasWidth, canvasHeight);

        //sets the color back to the currently selected ColorPicker color

    }

    public void sendDouble(Double value) {
        addDouble(value);
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    class CanvasRedrawTask extends AnimationTimer {
        long time = System.nanoTime();
        long f = System.nanoTime();

        @Override
        public void handle(long now) {

            render(f, getDoubles().size() / 2);
            f = (System.nanoTime() - time) / 1000 / 1000;
            System.out.println("Time since last redraw " + f + " ms");
            time = System.nanoTime();
        }
    }
}

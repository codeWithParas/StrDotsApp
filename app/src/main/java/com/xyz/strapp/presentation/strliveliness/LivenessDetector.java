package com.xyz.strapp.presentation.strliveliness;

import android.content.Context;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

public class LivenessDetector {

    private Interpreter interpreter;
    private final float spoofThreshold;
    private static final int INPUT_SIZE = 224;

    public LivenessDetector(Context context, String modelPath, float spoofThreshold) throws IOException {
        this.spoofThreshold = spoofThreshold;
        loadModel(context, modelPath);
    }

    private void loadModel(Context context, String modelPath) throws IOException {
        ByteBuffer modelBuffer = FileUtil.loadMappedFile(context, modelPath);
        interpreter = new Interpreter(modelBuffer);
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }

    /**
     * Run liveness detection.
     * @param faceBitmap Input image must be RGB 224x224
     * @return true if live, false if spoof.
     */
    public boolean isLive(Bitmap faceBitmap) {
        if (faceBitmap.getWidth() != INPUT_SIZE || faceBitmap.getHeight() != INPUT_SIZE) {
            throw new IllegalArgumentException("Input bitmap must be 224x224");
        }

        // Prepare input tensor
        float[][][][] input = new float[1][INPUT_SIZE][INPUT_SIZE][3];

        for (int y = 0; y < INPUT_SIZE; y++) {
            for (int x = 0; x < INPUT_SIZE; x++) {
                int pixel = faceBitmap.getPixel(x, y);
                // Extract RGB channels
                float r = ((pixel >> 16) & 0xFF) / 255.0f;
                float g = ((pixel >> 8) & 0xFF) / 255.0f;
                float b = (pixel & 0xFF) / 255.0f;
                input[0][y][x][0] = r;
                input[0][y][x][1] = g;
                input[0][y][x][2] = b;
            }
        }

        // Prepare output tensor
        float[][] output = new float[1][1];
        interpreter.run(input, output);

        float score = output[0][0];
        System.out.println("Spoof score → " + score);

        return score < spoofThreshold;
    }
}


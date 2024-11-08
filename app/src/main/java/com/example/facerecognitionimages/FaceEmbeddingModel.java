package com.example.facerecognitionimages;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FaceEmbeddingModel {
    private Interpreter tflite;

    public FaceEmbeddingModel(Context context) {
        try {
            tflite = new Interpreter(loadModelFile(context));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        // Open the model file from assets
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd("mobile_face_net.tflite");

        // Get the start offset and declared length from AssetFileDescriptor
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();

        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        // Map the model file into memory
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


    public float[] getEmbedding(Bitmap faceBitmap) {
        // Resize faceBitmap to the model's expected input size
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(faceBitmap, 112, 112, true);
        // Normalize the bitmap for the model's input format [-1, 1]
        float[][][][] input = new float[1][112][112][3];
        for (int x = 0; x < 112; x++) {
            for (int y = 0; y < 112; y++) {
                int pixel = resizedBitmap.getPixel(x, y);
                input[0][x][y][0] = (pixel >> 16 & 0xFF) / 255.0f * 2 - 1;
                input[0][x][y][1] = (pixel >> 8 & 0xFF) / 255.0f * 2 - 1;
                input[0][x][y][2] = (pixel & 0xFF) / 255.0f * 2 - 1;
            }
        }
        // Output array to store the embeddings
        float[][] embeddings = new float[1][192];
        tflite.run(input, embeddings);

        return embeddings[0];
    }
}

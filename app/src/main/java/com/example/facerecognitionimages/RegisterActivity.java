package com.example.facerecognitionimages;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class RegisterActivity extends AppCompatActivity {
    CardView galleryCard,cameraCard;
    ImageView imageView;
    Uri image_uri, cameraImageUri;
    InputImage inputImage;
    private TextToSpeech textToSpeech;
    private static final int REQUEST_PICK_IMAGE = 102;
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_CAMERA_PERMISSION = 123;
    FaceDetectorOptions highAccuracyOpts =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                    .build();

    FaceDetector detector;
    private FaceEmbeddingModel faceEmbeddingModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        imageView = findViewById(R.id.imageView2);
        galleryCard = findViewById(R.id.gallerycard);
        cameraCard = findViewById(R.id.cameracard);


        galleryCard.setOnClickListener(v -> choosePicture(REQUEST_PICK_IMAGE));

        cameraCard.setOnClickListener(v-> takePicture(REQUEST_CAMERA_PERMISSION));

        detector = FaceDetection.getClient(highAccuracyOpts);
        faceEmbeddingModel = new FaceEmbeddingModel(this);

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int langResult = textToSpeech.setLanguage(Locale.getDefault());
                    if (langResult == TextToSpeech.LANG_MISSING_DATA
                            || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(RegisterActivity.this, "Language not supported", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, "TTS initialization failed", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    private void choosePicture(int reqCode) {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, reqCode);
    }

    private void takePicture(int reqCode) {
        // Request camera permission if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            } else {
                openCamera();
            }
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Create a file to store the image
        File photoFile = createImageFile();
        if (photoFile != null) {
            cameraImageUri = FileProvider.getUriForFile(this, "your.package.name.fileprovider", photoFile);
            takePicture.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            startActivityForResult(takePicture, REQUEST_IMAGE_CAPTURE);
        }
    }

    // Helper method to create an image file
    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to use the camera", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            image_uri = data.getData();
            displayImage(image_uri);
            Bitmap inputImage = getBitmapFromUri(image_uri);
            performFaceDetection(inputImage);
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Use the cameraImageUri to get the full-resolution image
            if (cameraImageUri != null) {
                image_uri = cameraImageUri;
                Bitmap inputImage = getBitmapFromUri(cameraImageUri);
                if (inputImage != null) {
                    displayImage(cameraImageUri);
                    performFaceDetection(inputImage);
                }
            }
        }
    }


    private void displayImage(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            imageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Uri getImageUri(Bitmap bitmap) {
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "CapturedImage", null);
        return Uri.parse(path);
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            return MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void performFaceDetection(Bitmap input) {
        Bitmap mutableBmp = input.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBmp);
        InputImage image = InputImage.fromBitmap(input, 0);
        Task<List<Face>> result =
                detector.process(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<Face>>() {
                                    @Override
                                    public void onSuccess(List<Face> faces) {
                                        // Task completed successfully
                                        Log.d("tryFace", "Len = " + faces.size());
                                        if (faces.isEmpty()) {
                                            // No faces detected, trigger TTS
                                            speak("No faces detected in the image.");
                                        } else {
                                            for (Face face : faces) {
                                                Rect bounds = face.getBoundingBox();
                                                Paint p1 = new Paint();
                                                p1.setColor(Color.RED);
                                                p1.setStyle(Paint.Style.STROKE);
                                                performFaceRecognition(bounds, input);
                                                canvas.drawRect(bounds, p1);
                                            }
                                        }
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                    }
                                });
    }

    private void speak(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    public void performFaceRecognition(Rect bound, Bitmap input){
        if(bound.top<0){
            bound.top = 0;
        }
        if (bound.left < 0) {
            bound.left = 0;
        }
        if(bound.right>input.getWidth()){
            bound.right = input.getWidth() -1;
        }
        if(bound.bottom>input.getHeight()){
            bound.bottom = input.getHeight()-1;
        }

        Bitmap croppedFace = Bitmap.createBitmap(input,bound.left, bound.top, bound.width(), bound.height());
        imageView.setImageBitmap(croppedFace);

        float[] embedding = faceEmbeddingModel.getEmbedding(croppedFace);
        Log.d("tryFace", "Len = "+embedding.length);
        for (int i = 0;i<embedding.length;i++){
            Log.d("tryFace", "embedding["+i+"] = "+embedding[i]);
        }

        // Save or compare the embedding as needed
        saveOrCompareEmbedding(embedding);

    }

    private void saveOrCompareEmbedding(float[] embedding) {
        // Logic to store or compare the embedding with stored embeddings
        // For example, you can store it in a local Room or Firebase database
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            super.onDestroy();
            if (textToSpeech != null) {
                textToSpeech.stop();
                textToSpeech.shutdown();
            }
        }}


}
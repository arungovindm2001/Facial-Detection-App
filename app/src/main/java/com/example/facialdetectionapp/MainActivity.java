package com.example.facialdetectionapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.facialdetectionapp.Helper.GraphicOverlay;
import com.example.facialdetectionapp.Helper.RectOverlay;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.util.List;

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {

    // initialising the button, graphic overlay, alert dialog and camera view variables
    private Button faceDetectButton,retakeButton,switchButton;
    private GraphicOverlay graphicOverlay, graphicOverlayRetake;
    private CameraView cameraView;
    AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // assigning values to all the variables
        faceDetectButton = findViewById(R.id.detect_face_button);
        retakeButton = findViewById (R.id.retake_button);
        switchButton = findViewById(R.id.switch_camera);
        graphicOverlay = findViewById(R.id.graphic_overlay);
        graphicOverlayRetake = findViewById((R.id.graphic_overlay_retake));
        cameraView = findViewById(R.id.camera_view);



        //initializing alert dialog to display after clicking detect face button
        alertDialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Please Wait. Loading...")
                .setCancelable(false)
                .build();

        // adding functionality to the face detect button
        faceDetectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.start();                // starts the cameraView
                cameraView.captureImage();         // captures the image
                graphicOverlay.clear();            // clears the graphic overlay

            }
        });

        // adding functionality to the switch button
        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.toggleFacing();      // toggles camera
            }
        });

        // adding functionality to the retake button
        retakeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                graphicOverlayRetake.clear();
                retakeButton.setVisibility(View.INVISIBLE);

                faceDetectButton.setVisibility(View.VISIBLE);
                switchButton.setVisibility(View.VISIBLE);
            }
        });

        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }
            // If error occurs, the block of code is executed here
            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            // While taking the image, the block of code is executed here
            @Override
            public void onImage(CameraKitImage cameraKitImage) {
                alertDialog.show();
                Bitmap bitmap = cameraKitImage.getBitmap();
                bitmap = Bitmap.createScaledBitmap(bitmap, cameraView.getWidth(), cameraView.getHeight(), false);
                cameraView.stop();

                processFacedetection(bitmap);

                faceDetectButton.setVisibility (View.INVISIBLE);
                switchButton.setVisibility(View.VISIBLE);
            }

            // If onVideo mode, the block of code is executed here
            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });

    }

    private void processFacedetection(Bitmap bitmap) {
        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);

        FirebaseVisionFaceDetectorOptions firebaseVisionFaceDetectorOptions = new FirebaseVisionFaceDetectorOptions.Builder().build();

        FirebaseVisionFaceDetector firebaseVisionFaceDetector = FirebaseVision.getInstance()
                .getVisionFaceDetector(firebaseVisionFaceDetectorOptions);

        firebaseVisionFaceDetector.detectInImage(firebaseVisionImage)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                        getFaceResults(firebaseVisionFaces);

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Error"+e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });
    }

    // call firebaseVisionFaces to detect faces
    private void getFaceResults(List<FirebaseVisionFace> firebaseVisionFaces) {
        switchButton.setVisibility(View.INVISIBLE);
        int counter = 0;
        for (FirebaseVisionFace face: firebaseVisionFaces) {
            Rect rect = face.getBoundingBox();
            RectOverlay rectOverlay = new RectOverlay(graphicOverlayRetake, rect);

            // Add the rectangular outline over the face
            graphicOverlayRetake.add(rectOverlay);
            counter = counter + 1;
        }

        retakeButton.setVisibility (View.VISIBLE);

        if (counter == 0) {
            // display toast message if no faces are found
            Toast.makeText(MainActivity.this, "No faces found",Toast.LENGTH_SHORT).show();
        } else if (counter == 1) {
            // display toast message if 1 face are found
            Toast.makeText(MainActivity.this, "1 face found",Toast.LENGTH_SHORT).show();
        } else {
            // display toast message if more than 1 face is found
            Toast.makeText(MainActivity.this, counter +" faces found",Toast.LENGTH_SHORT).show();
        }

        alertDialog.dismiss();

        runFaceRecog(firebaseVisionFaces);
    }

    @Override
    protected void onPause() {
        super.onPause();

        cameraView.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        cameraView.start();
    }
    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setMessage("Do you want to exit ?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //if user pressed "yes", then he is allowed to exit from application
                finish();
            }
        });
        builder.setNegativeButton("No",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //if user select "No", just cancel this dialog and continue with app
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void runFaceRecog(List<FirebaseVisionFace> faces) {
        StringBuilder result = new StringBuilder();
        float smilingProbability = 0;
        float rightEyeOpenProbability = 0;
        float leftEyeOpenProbability = 0;

        for (FirebaseVisionFace face : faces) {
            if (face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                smilingProbability = face.getSmilingProbability();
            }
            if (face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                rightEyeOpenProbability = face.getRightEyeOpenProbability ();
            }
            if (face.getLeftEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                leftEyeOpenProbability = face.getLeftEyeOpenProbability();
            }


            result.append("Smile: ");
            if (smilingProbability > 0.5) {
                result.append("Yes \nProbability: ").append(smilingProbability);
            } else {
                result.append("No");
            }
            result.append("\n\nRight eye: ");
            if (rightEyeOpenProbability > 0.5) {
                result.append("Open \nProbability: ").append(rightEyeOpenProbability);
            } else {
                result.append("Close");
            }
            result.append("\n\nLeft eye: ");
            if (leftEyeOpenProbability > 0.5) {
                result.append("Open \nProbability: ").append(leftEyeOpenProbability);
            } else {
                result.append("Close");
            }
            result.append("\n\n");
        }
        AlertBox(result);

    }

    private void AlertBox(StringBuilder result) {
        AlertDialog.Builder probability = new AlertDialog.Builder(this);
        probability.setCancelable(true);
        probability.setMessage(result);

        AlertDialog probabilityAlert = probability.create();
        probabilityAlert.show();
    }

}
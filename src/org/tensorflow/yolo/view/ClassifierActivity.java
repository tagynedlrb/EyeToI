package org.tensorflow.yolo.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import static android.speech.tts.TextToSpeech.ERROR;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import org.tensorflow.yolo.R;
import org.tensorflow.yolo.TensorFlowImageRecognizer;
import org.tensorflow.yolo.model.PostProcessingOutcome;
import org.tensorflow.yolo.model.Recognition;
import org.tensorflow.yolo.util.ImageUtils;
import org.tensorflow.yolo.view.components.BorderedText;

import java.util.List;
import java.util.Locale;
import java.util.Vector;

import static org.tensorflow.yolo.Config.INPUT_SIZE;
import static org.tensorflow.yolo.Config.LOGGING_TAG;

/**
 * Classifier activity class
 * Modified by Zoltan Szabo
 */
public class ClassifierActivity extends TextToSpeechActivity implements OnImageAvailableListener, SensorEventListener {
    private boolean MAINTAIN_ASPECT = true;
    private float TEXT_SIZE_DIP = 10;

    private TensorFlowImageRecognizer recognizer;
    private Integer sensorOrientation;
    private int previewWidth = 0;
    private int previewHeight = 0;
    private Bitmap croppedBitmap = null;
    private boolean computing = false;
    private Matrix frameToCropTransform;

    private OverlayView overlayView;
    private BorderedText borderedText;
    private long lastProcessingTimeMs;
    private long lastPostProcessingTimeMs;

    private double oldLong = 0;
    private double oldLat = 0;
    private double curLatitude = 0;
    private double curLongitude = 0;
    private Location oldLocation;
    private Location curLocation;

    //현재 걸음 수
    private int mSteps = 0;
    //리스너가 등록되고 난 후의 step count
    private int mCounterSteps = 0;

    private TextToSpeech textToSpeech;

    //센서 연결을 위한 변수
    private SensorManager sensorManager;
    private Sensor stepCountSensor;

    private int oldStep = 0;
    private int curStep = 0;
    private int movedStep = 0;

    private int distanceFromBollard = 0;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(null);

        // TTS settings
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener(){
            @Override
            public void onInit(int status){
                if(status != ERROR)
                    textToSpeech.setLanguage(Locale.KOREAN);
            }
        });

//        textToSpeech = new TextToSpeech(this, this);
//        textToSpeech.setLanguage(Locale.KOREAN);

        //센서 연결[걸음수 센서를 이용한 흔듬 감지]
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if(stepCountSensor == null){
            // Toast.makeText(this,"No Step Detect Sensor",Toast.LENGTH_SHORT).show();
        }
    }
    public void onStart() {
        super.onStart();
        if(stepCountSensor !=null){
            //센서의 속도 설정
            sensorManager.registerListener((SensorEventListener)this,stepCountSensor,SensorManager.SENSOR_DELAY_GAME);
        }
    }
    public void onStop(){
        super.onStop();
        if(sensorManager!=null){
            sensorManager.unregisterListener((SensorEventListener)this);
        }
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        recognizer = TensorFlowImageRecognizer.create(getAssets());

        overlayView = (OverlayView) findViewById(R.id.overlay);
        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        final int screenOrientation = getWindowManager().getDefaultDisplay().getRotation();

        Log.i(LOGGING_TAG, String.format("Sensor orientation: %d, Screen orientation: %d",
                rotation, screenOrientation));

        sensorOrientation = rotation + screenOrientation;

        Log.i(LOGGING_TAG, String.format("Initializing at size %dx%d", previewWidth, previewHeight));

        croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Config.ARGB_8888);

        frameToCropTransform = ImageUtils.getTransformationMatrix(previewWidth, previewHeight,
                INPUT_SIZE, INPUT_SIZE, sensorOrientation, MAINTAIN_ASPECT);
        frameToCropTransform.invert(new Matrix());

        addCallback((final Canvas canvas) -> renderAdditionalInformation(canvas));
    }

    @Override
    public void onImageAvailable(final ImageReader reader) {
        Image image = null;

        try {
            image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            if (computing) {
                image.close();
                return;
            }

            computing = true;
            fillCroppedBitmap(image);
            image.close();
        } catch (final Exception ex) {
            if (image != null) {
                image.close();
            }
            Log.e(LOGGING_TAG, ex.getMessage());
        }

        oldLocation = new Location("old location");
        curLocation = new Location("current location");



        runInBackground(() -> {

//            try {
//                Thread.sleep(500); //초 대기
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

            final long startTime = SystemClock.uptimeMillis();
            final PostProcessingOutcome recognitionOutput = recognizer.recognizeImage(croppedBitmap);
            List<Recognition> results = recognitionOutput.getRecognitions();
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
            lastPostProcessingTimeMs = recognitionOutput.getPostProcessingTime();
            overlayView.setResults(results);

            if (oldStep == 0) {
                oldStep = mSteps;
                curStep = mSteps;
            }

            // 이전/현재 비율 구하기
            double ratio = recognizer.sizeCompare(recognitionOutput.getRecognitions());
//
//            Toast myToast6 = Toast.makeText(this.getApplicationContext(),
//                            String.format("RATIO: %f", ratio), Toast.LENGTH_SHORT);
//                    myToast6.show();
            if(ratio != 0){
                // update curStep
                curStep = mSteps;

                // 현재 보수 받아오기
                if (oldStep != curStep)
                    movedStep = curStep - oldStep;
                int oneStep = 30;
                double movedDistance = 0;
                movedDistance = oneStep * movedStep * 0.01;

                // update oldStep
                oldStep = curStep;

//                if (movedDistance != 0) {
//                    Toast myToast = Toast.makeText(this.getApplicationContext(),
//                            String.format("Step: %d  moved : %f", movedStep, movedDistance), Toast.LENGTH_SHORT);
//                    myToast.show();
//                }

                // 물체로부터의 거리 계산
                double distanceFrom = 0;
                // 멀어질 경우 : 3/5 비교 후, => 보폭 * 5/2 => 보폭 * pow((1 - 3/5), -1)    (넘겨줄 때 음수 처리)
                // 가까워질 경우 : 3/5 비교 후, => 보폭 * 3/2 => 보폭 * 3/5 * pow((1 - 3/5), -1)
                // 가까워질 때
                if(ratio > 0)
                    distanceFrom = movedDistance * ratio * Math.pow((1 - ratio), -1);
                    // 멀어질 때
                else if (ratio < 0){
                    ratio = -1 * ratio;
                    distanceFrom = movedDistance * Math.pow((1 - ratio), -1);
                }

                if(movedStep != 0) {
                    Toast myToast3 = Toast.makeText(this.getApplicationContext(),
                            String.format("moved : %f, ratio : %f, distanceFrom : %f", movedDistance, ratio, distanceFrom), Toast.LENGTH_SHORT);
                    myToast3.show();

                    distanceFromBollard = (int)distanceFrom + 1;    // 올림

                }
//                else{
//                    Toast myToast4 = Toast.makeText(this.getApplicationContext(),"NO", Toast.LENGTH_SHORT);
//                    myToast4.show();
//                }
                movedStep = 0;
            }

            if(distanceFromBollard != 0 && distanceFromBollard < 7) {
                textToSpeech.speak(String.format("볼라드 %d미터", distanceFromBollard), TextToSpeech.QUEUE_ADD, null, null);
                distanceFromBollard = 0;
            }
//              speak(results);


//            // save old Long, Lat
//            oldLat = curLatitude;
//            oldLong = curLongitude;
//            oldLocation.setLatitude(curLatitude);
//            oldLocation.setLongitude(curLongitude);
//
//            //get Current Long, Lat
//
//            Location currLocation1 = gpsTracker.getLocation();
//            curLatitude = currLocation1.getLatitude();  //gpsTracker.getLatitude(); // 위도
//            curLongitude = currLocation1.getLongitude();    //gpsTracker.getLongitude(); //경도
//            curLocation.setLatitude(curLatitude);
//            curLocation.setLongitude(curLongitude);
//
//
//            if(curLocation.getLongitude() != oldLocation.getLongitude() && curLocation.getLatitude() != oldLocation.getLatitude()) {
//                Log.i(LOGGING_TAG, String.format("latitude : %f, longitude : %f", curLatitude, curLongitude));
//                Toast myToast = Toast.makeText(this.getApplicationContext(),
//                        String.format("latitude : %f, longitude : %f", curLatitude, curLongitude), Toast.LENGTH_SHORT);
//                myToast.show();
//
//            }
//
//            if((oldLat != 0 && oldLong != 0) && (oldLong != curLongitude && oldLat != curLatitude)) {
//                double moved =  oldLocation.distanceTo(curLocation);//Location.distanceBetween(oldLat, oldLong, curLatitude, curLongitude); //gpsTracker.measure(oldLat, oldLong, curLatitude, curLongitude);
//                Toast myToast2 = Toast.makeText(this.getApplicationContext(),
//                        String.valueOf(moved), Toast.LENGTH_SHORT);
//                myToast2.show();
//            }

            requestRender();
            computing = false;
        });
    }

    private void fillCroppedBitmap(final Image image) {
        Bitmap rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        rgbFrameBitmap.setPixels(ImageUtils.convertYUVToARGB(image, previewWidth, previewHeight),
                0, previewWidth, 0, 0, previewWidth, previewHeight);
        new Canvas(croppedBitmap).drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recognizer != null) {
            recognizer.close();
        }
    }

    private void renderAdditionalInformation(final Canvas canvas) {
        final Vector<String> lines = new Vector();
        if (recognizer != null) {
            for (String line : recognizer.getStatString().split("\n")) {
                lines.add(line);
            }
        }

        lines.add("Frame: " + previewWidth + "x" + previewHeight);
        lines.add("View: " + canvas.getWidth() + "x" + canvas.getHeight());
        lines.add("Rotation: " + sensorOrientation);
        lines.add("Inference time: " + lastProcessingTimeMs + "ms");
        lines.add(">> Post-processing time: " + lastPostProcessingTimeMs + "ms");

        borderedText.drawLines(canvas, 10, 10, lines);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_STEP_COUNTER){

            //stepcountsenersor는 앱이 꺼지더라도 초기화 되지않는다. 그러므로 우리는 초기값을 가지고 있어야한다.
            if (mCounterSteps < 1) {
                // initial value
                mCounterSteps = (int) event.values[0];
            }
            //리셋 안된 값 + 현재값 - 리셋 안된 값
            mSteps = (int) event.values[0] - mCounterSteps;
//            mwalknum.setText(Integer.toString(mSteps));
            Log.i("log: ", "New step detected by STEP_COUNTER sensor. Total step count: " + mSteps );
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
package org.tensorflow.yolo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.tensorflow.yolo.view.CameraActivity;
import org.tensorflow.yolo.view.ClassifierActivity;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class MenuActivity extends Activity implements TextToSpeech.OnInitListener {
    private Button naviButton;
    private Button objectButton;
    private Button btnStt;
    TextView txtInMsg;
    public static Context mContext;
    Intent sttIntent;
    SpeechRecognizer mRecognizer;
    TextToSpeech tts;
    final int PERMISSION = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        if ( Build.VERSION.SDK_INT >= 23 ){
            // 퍼미션 체크
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET,
                    Manifest.permission.RECORD_AUDIO},PERMISSION);
        }
        mContext = this;
        btnStt = (Button) findViewById(R.id.btn_stt_start);
        txtInMsg = (TextView) findViewById(R.id.txtInMsg);


        //STT,TTS
        speechInit();
        btnStt.setOnClickListener(v -> {
            mRecognizer = SpeechRecognizer.createSpeechRecognizer(this); // 음성인식 객체
            mRecognizer.setRecognitionListener(listener); // 음성인식 리스너 등록
            mRecognizer.startListening(sttIntent);
        });

        naviButton = (Button) findViewById(R.id.naviButton);
        objectButton = (Button) findViewById(R.id.dtt_btn);
        naviButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(MenuActivity.this, MapFragmentActivity.class);
                startActivity(intent1);
            }
        });
        objectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent2 = new Intent(MenuActivity.this, ClassifierActivity.class);
                startActivity(intent2);
            }
        });
    }
        private void speechInit() {
            // stt 객체 생성, 초기화
            sttIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            sttIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
            sttIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR");

            // tts 객체 생성, 초기화
            tts = new TextToSpeech(MenuActivity.this, this);
        }

        private RecognitionListener listener = new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Toast.makeText(getApplicationContext(), "음성인식을 시작합니다.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onError(int error) {
                String message;

                switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO:
                        message = "오디오 에러";
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        message = "클라이언트 에러";
                        break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        message = "퍼미션 없음";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        message = "네트워크 에러";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        message = "네트웍 타임아웃";
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        message = "찾을 수 없음";
                        break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        message = "RECOGNIZER가 바쁨";
                        break;
                    case SpeechRecognizer.ERROR_SERVER:
                        message = "서버가 이상함";
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        message = "말하는 시간초과";
                        break;
                    default:
                        message = "알 수 없는 오류임";
                        break;
                }
                String guideStr = "에러가 발생하였습니다.";
                Toast.makeText(getApplicationContext(), guideStr + message, Toast.LENGTH_SHORT).show();
                funcVoiceOut(guideStr);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                String resultStr = "";

                for (int i = 0; i < matches.size(); i++) {
                    txtInMsg.setText(matches.get(i));
                    resultStr += matches.get(i);
                }

                if(resultStr.length() < 1) return;
                resultStr = resultStr.replace(" ", "");

                moveActivity(resultStr);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        };

        public void moveActivity(String resultStr) {
            if(resultStr.indexOf("경로안내") > -1) {
                String guideStr = "경로안내로 넘어갑니다";
                Toast.makeText(getApplicationContext(), guideStr, Toast.LENGTH_SHORT).show();
                funcVoiceOut(guideStr);

                Intent intent1 = new Intent(getApplicationContext(), MapFragmentActivity.class);
                startActivity(intent1);
            }
            if(resultStr.indexOf("사물인식") > -1) {
                String guideStr = "사물인식으로 넘어갑니다";
                Toast.makeText(getApplicationContext(), guideStr, Toast.LENGTH_SHORT).show();
                funcVoiceOut(guideStr);

                Intent intent2 = new Intent(getApplicationContext(), ClassifierActivity.class);
                startActivity(intent2);
            }
        }

        public void funcVoiceOut(String OutMsg){
            if(OutMsg.length()<1)return;
            if(!tts.isSpeaking()) {
                tts.speak(OutMsg, TextToSpeech.QUEUE_FLUSH, null);
            }
        }

        @Override
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.KOREAN);
                tts.setPitch(1);
            } else {
                Log.e("TTS", "초기화 실패");
            }
        }

        @Override
        protected void onDestroy() {
            if (tts != null) {
                tts.stop();
                tts.shutdown();
            }
            if(mRecognizer!=null){
                mRecognizer.destroy();
                mRecognizer.cancel();
                mRecognizer=null;
            }
            super.onDestroy();
        }
    }



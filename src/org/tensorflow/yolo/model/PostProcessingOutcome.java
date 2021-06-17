package org.tensorflow.yolo.model;

import android.util.Log;

import java.util.List;

import static org.tensorflow.yolo.Config.LOGGING_TAG;

/**
 * Model to store the outcome from post-processing
 *
 * Created by Chew Jing Wei on 21/01/21.
 */
public class PostProcessingOutcome {
    private long postProcessingTime;
    private List<Recognition> recognitions;
    private double sqrSizeOld;

    public PostProcessingOutcome(long postProcessingTime, List<Recognition> recognitions) {
        this.setPostProcessingTime(postProcessingTime);
        this.setRecognitions(recognitions);
    }

    public long getPostProcessingTime() {
        return this.postProcessingTime;
    }

    public void setPostProcessingTime(long postProcessingTime) {
        this.postProcessingTime = postProcessingTime;
    }

    public List<Recognition> getRecognitions() {
        return this.recognitions;
    }

    public void setRecognitions(List<Recognition> recognitions) { // class Recognition으로 연결
        this.recognitions = recognitions;
    }

    public synchronized void sizeCompare() {

        double ratio;
        sqrSizeOld = 0;

        if (recognitions != null) {
            for (Recognition r : recognitions) {
                double squareSize = Math.sqrt(r.getLocation().getWidth() * r.getLocation().getHeight()); // 상크기 루트 씌운거
                if (sqrSizeOld == 0) { // 인식 개체가 소멸했을 시 초기화 방도 강구할 것
                    sqrSizeOld = squareSize;

                }

                ratio = sqrSizeOld / squareSize;
                if (ratio >= 1.3) { // 4 -> 3 meter 를 가정할 때, 4/3 인 1.3을 최소치로 설정
                    Log.i(LOGGING_TAG, String.format("Alert! => old : %f, size : %f ratio :%f", sqrSizeOld, squareSize, ratio));
                    sqrSizeOld = squareSize;
                }
            }
        }
    }
}

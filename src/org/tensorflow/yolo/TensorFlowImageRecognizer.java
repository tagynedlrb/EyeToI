package org.tensorflow.yolo;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import org.tensorflow.yolo.model.PostProcessingOutcome;
import org.tensorflow.yolo.model.Recognition;
import org.tensorflow.yolo.util.ClassAttrProvider;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import static org.tensorflow.yolo.Config.IMAGE_MEAN;
import static org.tensorflow.yolo.Config.IMAGE_STD;
import static org.tensorflow.yolo.Config.INPUT_NAME;
import static org.tensorflow.yolo.Config.INPUT_SIZE;
import static org.tensorflow.yolo.Config.LOGGING_TAG;
import static org.tensorflow.yolo.Config.MODEL_FILE;
import static org.tensorflow.yolo.Config.OUTPUT_NAME;

/**
 * A classifier specialized to label images using TensorFlow.
 * Modified by Zoltan Szabo
 */
public class TensorFlowImageRecognizer {
    private int outputSize;
    private Vector<String> labels;
    private TensorFlowInferenceInterface inferenceInterface;
    private double oldSqrSize = 0;
    private double ratio = 0;
    private double tempRatio = 0;
    private String oldTitle;

    private TensorFlowImageRecognizer() {
    }

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param assetManager The asset manager to be used to load assets.
     * @throws IOException
     */
    public static TensorFlowImageRecognizer create(AssetManager assetManager) {
        TensorFlowImageRecognizer recognizer = new TensorFlowImageRecognizer();
        recognizer.labels = ClassAttrProvider.newInstance(assetManager).getLabels();
        recognizer.inferenceInterface = new TensorFlowInferenceInterface(assetManager,
                "file:///android_asset/" + MODEL_FILE);
        recognizer.outputSize = YOLOClassifier.getInstance()
                .getOutputSizeByShape(recognizer.inferenceInterface.graphOperation(OUTPUT_NAME));
        return recognizer;
    }

    public PostProcessingOutcome recognizeImage(final Bitmap bitmap) {
        return YOLOClassifier.getInstance().classifyImage(runTensorFlow(bitmap), labels);
    }

    public String getStatString() {
        return inferenceInterface.getStatString();
    }

    public void close() {
        inferenceInterface.close();
    }

    private float[] runTensorFlow(final Bitmap bitmap) {
        final float[] tfOutput = new float[outputSize];
        // Copy the input data into TensorFlow.
        inferenceInterface.feed(INPUT_NAME, processBitmap(bitmap), 1, INPUT_SIZE, INPUT_SIZE, 3);

        // Run the inference call.
        inferenceInterface.run(new String[]{OUTPUT_NAME});

        // Copy the output Tensor back into the output array.
        inferenceInterface.fetch(OUTPUT_NAME, tfOutput);

        return tfOutput;
    }

    /**
     * Preprocess the image data from 0-255 int to normalized float based
     * on the provided parameters.
     *
     * @param bitmap
     */
    private float[] processBitmap(final Bitmap bitmap) {
        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        float[] floatValues = new float[INPUT_SIZE * INPUT_SIZE * 3];

        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3 + 0] = (((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            floatValues[i * 3 + 1] = (((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            floatValues[i * 3 + 2] = ((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
        }
        return floatValues;
    }
    public synchronized double sizeCompare(List<Recognition> recognitions) {

        Vector v = new Vector();

        if (recognitions != null) {
            // ????????? ?????? ??????, sorting ???
            for (Recognition r : recognitions) {
                SizeNType temp = new SizeNType();
                temp.size = Math.sqrt(r.getLocation().getWidth() * r.getLocation().getHeight()); // ????????? ?????? ?????????
                temp.type = r.getTitle();
                // ?????? class ??? ????????? ??????
                if (oldTitle != null) {
                    if (temp.type.equals(oldTitle))
                        v.add(temp);
                } else{ // ??? ???????????? oldTitle??? null ??? ???
                    oldTitle = temp.type;
                }
            }
//            Collections.sort(v, new Comparator() {
//                @Override
//                public int compare(Object arg0, Object arg1) {
//                    return ((SizeNType) arg0).size > ((SizeNType) arg1).size ? 0 : 1;
//                }
//            });
        }
        // ????????? ????????? curr_squareSize, oldSquareSize??? ????????? ??????
        double temp_squareSize = 0;
//        Log.i(LOGGING_TAG, String.format("Old : %f", oldSqrSize));
        for (int i = 0; i < v.size(); i++) {
            SizeNType curr = (SizeNType) v.get(i);
            double curr_squareSize = curr.size;

            // ?????? ????????? ????????? ???????????? ??????
            if (oldSqrSize == 0) {
                oldSqrSize = curr_squareSize;
                oldTitle = curr.type;
                ratio = 0;
                Log.i(LOGGING_TAG, "NO...");
                return ratio;
            }

            Log.i(LOGGING_TAG, "YES!");

            // ratio??? 1??? ?????? ?????????(???) ????????? ?????? ????????? ?????? ????????? ??????  ??????
            // ????????? ?????? : 3/5 ?????? ???, => ?????? * 5/2 => ?????? * pow((1 - 3/5), -1)    (????????? ??? ?????? ??????)
            // ???????????? ?????? : 3/5 ?????? ???, => ?????? * 3/2 => ?????? * 3/5 * pow((1 - 3/5), -1)
            // ??????
            if(oldSqrSize < curr_squareSize)
                tempRatio = oldSqrSize * Math.pow(curr_squareSize, -1);
            else if(oldSqrSize > curr_squareSize)
                tempRatio = -1 * curr_squareSize * Math.pow(oldSqrSize, -1);

            Log.i(LOGGING_TAG, String.format("tempRatio : %f ratio : %f", tempRatio, ratio));
            // ??????
            // tempRatio??? ??? ??? ?????? => ??????
            if(i == 0 || Math.abs(ratio) < Math.abs(tempRatio)) {
                Log.i(LOGGING_TAG, String.format("Yes"));
                ratio = tempRatio;
                temp_squareSize = curr_squareSize;
            }
        } // end for
        oldSqrSize = temp_squareSize;
        return ratio;
    }
}



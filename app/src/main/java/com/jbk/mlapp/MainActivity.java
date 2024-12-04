package com.jbk.mlapp;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import org.tensorflow.lite.Interpreter;

import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.nex3z.fingerpaintview.FingerPaintView;
import android.os.Handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    // Logging and file name constants
    private static final String TAG = "MappedByteBuffer";

    // This makes it so it can handle the .module file
    private MappedByteBuffer mappedByteBuffer;
    //delay to submit images
    private Handler handler = new Handler();
    private Runnable runnable;
    private long delay = 1000;
    private Interpreter interpreter;


    //public Timer time = new Timer();
    public FingerPaintView finger;
    //This maps the predicted class (a number from 0-61 to the correct character
    public String predMap="0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        //initialize and get references to layout objects
        Paint p = new Paint();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Button bks = findViewById(R.id.Clear);
        Button space = findViewById(R.id.space);
        Button period = findViewById(R.id.Period);
        Button comma = findViewById(R.id.comma);
        Button clearall = findViewById(R.id.ClearAll);
        finger = findViewById(R.id.fingerPaintView);
        TextView message = findViewById(R.id.Message);
        TextView prob = findViewById(R.id.Prob);

        //set initial content and onclick/ontouch listeners

        prob.setText("0.0");

        //undo previous action iff message!=null
        bks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = message.getText().toString();
                if (!text.isEmpty()) {
                    text = text.substring(0, text.length() - 1);
                    message.setText(text);
                }
            }
        });
        //add a space to the message
        space.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = message.getText().toString();
                text = text+" ";
                message.setText(text);
            }
        });

        //set a delay after the finger lifts and preform prediction if finger does
        //not retouch the screen within a second.
        finger.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() ==MotionEvent.ACTION_UP) {
                    //ClearIn500ms=true;
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            String text = message.getText().toString();
                            //save image to bitmap
                            Bitmap img= Bitmap.createBitmap(finger.getWidth(),finger.getHeight(),Bitmap.Config.ARGB_8888);
                            img = finger.exportToBitmap(28,28);
                            int[] pixels= new int[28*28];

                            //convert img to byte data
                            img.getPixels(pixels,0,img.getWidth(),0,0,img.getWidth(),img.getHeight());
                            ByteBuffer buffer = ByteBuffer.allocateDirect(pixels.length*4);
                            buffer.order(ByteOrder.nativeOrder());


                            for (int i=0;i<pixels.length;i++) {
                                int pixel = pixels[i];
                                int red = (pixel >> 16) & 0xFF;
                                int green = (pixel >> 8) & 0xFF;
                                int blue = pixel & 0xFF;
                                float gray = (red + green + blue)/3;
                                buffer.putFloat((255 - gray) / 255.0f);
                                //buffer.putFloat(gray);

                            }
                            buffer.rewind(); // Reset position to the beginning

                            //call model to get output
                            float[] probs = doInference(buffer);

                            int prediction = getPredictions(probs);
                            //add output to message
                            prob.setText(String.valueOf(probs[prediction]));
                            text=text+predMap.charAt(prediction);
                            //text=text+predMap.charAt(12);
                            message.setText(text);

                            finger.clear();
                        }
                    };
                    handler.postDelayed(runnable, delay);
                } else if (motionEvent.getAction() ==MotionEvent.ACTION_DOWN) {
                    handler.removeCallbacks(runnable);
                }
                return false;
            }
        });

        //add a period to the message
        period.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = message.getText().toString();
                text = text+".";
                message.setText(text);
            }
        });

        //clear all the message
        clearall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                message.setText("");
            }
        });
        //add a comma to the message
        comma.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = message.getText().toString();
                text = text+",";
                message.setText(text);
            }
        });
        //set initial text to nothing
        message.setText("");

        //set mapped byte buffer

        try {
            interpreter = new Interpreter(loadModelFile());
            Log.d(TAG,"loaded");
        } catch (IOException e) {
            Log.d(TAG,"Didnt");
            throw new RuntimeException(e);
        }



        //set finger paint pen values.
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(45);
        p.setColor(Color.BLACK);
        finger.setPen(p);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public float[] doInference(ByteBuffer input)
    {
        float[][] output = new float[1][62];
        interpreter.run(input,output);
        //interpreter.invoke();
        Log.d(TAG, "Output tensor shape: " + Arrays.toString(interpreter.getOutputTensor(0).shape()));
        return output[0];
    }
    public int getPredictions(float[] probs){
        int max=0;
        for(int i=0;i<probs.length;i++){
            if(probs[i]>probs[max]){
                max=i;
            }
        }
        return max;
    }
    private MappedByteBuffer loadModelFile() throws IOException
    {
        AssetFileDescriptor assetFileDescriptor =  this.getAssets().openFd("emnist_model.tflite");
        FileInputStream fileInputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();
        long startOffset = assetFileDescriptor.getStartOffset();
        long length = assetFileDescriptor.getLength();

        //return the mapped byte buffer
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,length);
    }


}
package com.jbk.mlapp;

import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;

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
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    // Logging and file name constants
    private static final String TAG = "MappedByteBuffer";
    private static final String MODULE_FILE_NAME = "develocity-gradle-plugin-3.17.4.module";

    // This makes it so it can handle the .module file
    private MappedByteBuffer mappedByteBuffer;
    //delay to submit images
    private Handler handler = new Handler();
    private Runnable runnable;
    private long delay = 1000;

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
                            //text=text+predMap.charAt(getPredictions());
                            text=text+predMap.charAt(12);
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

        //clear all of the message
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
            // Load the .module file into memory
            mappedByteBuffer = loadMappedByteBufferFromFile(MODULE_FILE_NAME);
            Log.d(TAG, "MappedByteBuffer is loaded: " + mappedByteBuffer.isLoaded());
        } catch (IOException e) {
            // If it fails error message pops up
            Log.e(TAG, "Error loading .module file", e);
        }


        //set finger paint pen values.
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(20);
        p.setColor(Color.BLACK);
        finger.setPen(p);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private MappedByteBuffer loadMappedByteBufferFromFile(String fileName) throws IOException {
        // Finds the file in downloads
        File downloadsDir = null;
        downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File moduleFile = new File(downloadsDir, fileName);

        if (!moduleFile.exists()) {
            // Throws exception if the file isnt found
            throw new IOException("File not found: " + moduleFile.getAbsolutePath());
        }

        // Opens the file and map it into memory
        FileInputStream inputStream = new FileInputStream(moduleFile);
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
    }

}
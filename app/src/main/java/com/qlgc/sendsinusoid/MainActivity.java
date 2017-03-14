package com.qlgc.sendsinusoid;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    final private static float Frequency  = 440.0f;// generated sinusoidal signal
    final private static String IP_ADDRESS = "10.64.16.12";
    private boolean status = false;

    private TextView textView;
    private EditText myEditTextHz, myEditTextIP;

    private double sampleRate = 48000.0;
    private int startTimeIndex = 0;
    private int N = (int)(sampleRate*0.04); // Each frame have this many samples
    private int NFrameTimer,NFrameSocket;


    Timer timer;
    TimerTask timerTask;

    //we are going to use a handler to be able to run in our TimerTask
    final Handler handler = new Handler();

    final byte[] buffer = new byte[N*2];
    final double[] piftIndex = new double[N];
    final double step = Frequency*N/sampleRate*Math.PI*2;
    final short[] shortBuffer = new short[N];



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn = (Button) findViewById(R.id.finishButton);
        btn.setEnabled(false);


        textView = (TextView) findViewById(R.id.textView);
    }

    /**
     * Called when the user clicks the Start button
     */
    public void startRecording(View view) {
        // Do something in response
        textView.setText("Start ......");

        status = true;
        NFrameTimer = 0;
        NFrameSocket = 0;
        startTimer();
        startStreaming();

        Button startButton = (Button) findViewById(R.id.startButton);
        startButton.setEnabled(false);
        EditText EditTextField = (EditText) findViewById(R.id.ipAddress);
        EditTextField.setEnabled(false);
        Button finishButton = (Button) findViewById(R.id.finishButton);
        finishButton.setEnabled(true);

    }

    public void finishRecording(View view) {
        // Do something in response
        textView.setText("Finish ......");

        status = false;


        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

//        socket.close();
//        Log.d("VS","Socket released");

        Button startButton = (Button) findViewById(R.id.startButton);
        startButton.setEnabled(true);
        EditText EditTextField = (EditText) findViewById(R.id.ipAddress);
        EditTextField.setEnabled(true);
        Button finishButton = (Button) findViewById(R.id.finishButton);
        finishButton.setEnabled(false);
    }

    /*Exit the Apps*/
    public void exitApp(View view) {
        finish();
        System.exit(0);
    }


    public void startStreaming() {


        for(int i=0;i<N;i++){
            piftIndex[i] = Frequency*i/sampleRate*Math.PI*2;
        }
        Thread udpSendThread = new Thread(new Runnable() {
            @Override
            public void run() {

                try{
                    Thread.sleep(1000);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }

                try {
                    // create new UDP socket
                    final DatagramSocket socket  = new DatagramSocket();// <-- create an unbound socket first


                    Log.d("UDP", "Socket Created");

                    // First get the Frequency and IP address from the text field 10.64.8.78
                    handler.post(new Runnable(){
                        public void run() {
//                            String tmp = myEditTextHz.getText().toString();
//                            Pattern p = Pattern.compile("[0-9]*\\.?[0-9]+");
//                            Matcher m = p.matcher(tmp);
//                            if (m.find()) {
//                                float Frequency = Float.parseFloat(m.group());
//                            }
//                            IP_ADDRESS = myEditTextIP.getText().toString();
                            Toast.makeText(getApplicationContext(), "The frequency is: " + Float.toString(Frequency), Toast.LENGTH_LONG).show();
                            // TODO add some grammar check to the IP_ADDRESS
                            Log.w("UDP","IP address " + IP_ADDRESS);
                        }
                    });

                    // get server name

                    final InetAddress serverAddr = InetAddress.getByName(IP_ADDRESS);
                    Log.w("UDP", "Connecting "+IP_ADDRESS);

                    while (status) {
                        while(NFrameSocket<NFrameTimer){
                            NFrameSocket = NFrameTimer;

                            for(int i=0;i<N;i++){
                                double tmp = 0.05*Math.sin(piftIndex[i]);
                                shortBuffer[i] = (short)(tmp*32768);
                                piftIndex[i] += step;
                            }

                            // We did some changes here
                            // The first one will be the Frame Number
                            shortBuffer[0]= (short) NFrameSocket;

                            // short to byte
                            byte byte1, byte2;
                            for (int i=0;i<N;i++) {
                                byte1 = (byte) (shortBuffer[i]&0xFF); // the low byte
                                byte2 = (byte) ((shortBuffer[i]>>8)&0xFF); // the high byte
                                buffer[i*2] = byte1;
                                buffer[i*2+1] = byte2;
                            }


                            // create a UDP packet with data and its destination ip & port
                            DatagramPacket packet = new DatagramPacket(buffer, 2*N, serverAddr, 5001);
                            Log.w("UDP", "C: Sending the current frame");

                            try {
                                // send the UDP packet
                                socket.send(packet);
                                //Toast.makeText(getApplicationContext(),"MeawMeaw",Toast.LENGTH_LONG).show();
                                handler.post(new Runnable(){
                                    @Override
                                    public void run() {
                                    //textView.setText(Arrays.toString(EngeryBuffer));
                                    textView.setText(Integer.toString(NFrameSocket));
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.w("UDP", "C: Sending just failed");
                            }

                        }

                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e){
                            e.printStackTrace();
                        }
                    }

                } catch (Exception e) {
                    Log.w("UDP", "C: Error", e);
                    //e.printStackTrace();
                }

            }
        });

        // start the streaming thread
        udpSendThread.start();

    }



    public void startTimer() {
            //set a new Timer
            timer = new Timer();
            //initialize the TimerTask's job
            timerTask = new TimerTask() {
                public void run() {
                    //use a handler to run a toast that shows the current timestamp
                    handler.post(new Runnable() {
                        public void run() {

                            handler.post(new Runnable(){
                                @Override
                                public void run() {
                                    //textView.setText(Integer.toString(NFrameTimer));
                                }
                            });

                            NFrameTimer++;

                        }
                    });
                }
            };

            //schedule the timer, after the first 1000ms the TimerTask will run every 40ms
            timer.schedule(timerTask, 3000, 40); //

    }



}

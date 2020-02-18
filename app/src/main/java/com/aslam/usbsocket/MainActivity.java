package com.aslam.usbsocket;

import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.aslam.usbsocket.databinding.ActivityMainBinding;

import java.io.EOFException;

public class MainActivity extends AppCompatActivity {

    String TAG = "USB_SOCKET";

    ActivityMainBinding binding;

    private SocketServer socketServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        socketServer = new SocketServer(new SocketServer.SocketServerListener() {
            @Override
            public void onConnected(final String message) {
                Log.e(TAG, "onConnected => " + message);
                updateUI("onConnected => " + message);
            }

            @Override
            public void onDisconnected(EOFException exception) {
                Log.e(TAG, "onDisconnected");
                updateUI("onDisconnected");
            }

            @Override
            public void onFailed(Exception exception) {
                Log.e(TAG, "onFailed");
                updateUI("onFailed");
            }

            @Override
            public void onDataReceived(String data) {
                Log.e(TAG, "onDataReceived => " + data);
                updateUI("onDataReceived => " + data);
            }
        });

        socketServer.start();

        binding.btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                socketServer.sendData("Hello from Android");
            }
        });
    }

    void updateUI(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.txtResult.setText("\n" + message + binding.txtResult.getText());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socketServer.stop();
    }
}

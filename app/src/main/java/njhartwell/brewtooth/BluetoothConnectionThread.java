package njhartwell.brewtooth;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Created by z001yk4 on 11/22/16.
 */

public class BluetoothConnectionThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private Handler handler;
    private static final String TAG = "BluetoothIOThread";

    public BluetoothConnectionThread(BluetoothSocket socket, Handler h) {
        handler = h;
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run() {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()
        BufferedReader bufferedReader;

        try {
            bufferedReader = new BufferedReader(
                    new InputStreamReader(mmInStream, "UTF-8")
            );
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Failed to create BufferedReader", e);
            return;
        }

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                // bytes = mmInStream.read(buffer);
                // Send the obtained bytes to the UI activity
                //String text = new String(buffer, 0, bytes - 1);
                String l = bufferedReader.readLine();
                Log.d("BluetoothData", l);
                sendLine(l);
            } catch (IOException e) {
                Log.d(TAG, "quitting loop");
                break;
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) { }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }

    private void sendLine(String line) {
        Message m = new Message();
        m.what = Constants.BLUETOOTH_DATA;
        m.obj = line;
        handler.sendMessage(m);
    }
}

package njhartwell.brewtooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothConnectThread bluetoothConnectThread;
    private BluetoothAdapter bluetoothAdapter;
    private LineChart lineChart;
    private LineDataSet dataSet;
    private long firstDatapointTimestamp = 0;

    static final private String TAG = "MainActivity";
    static final private String LAST_DEVICE = "last_device";
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.BLUETOOTH_DATA:
                    handleDataPoint((String) msg.obj);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeGraph();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            return;
        };

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        findViewById(R.id.config_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("views", "Starting bluetooth activity");
                Intent i = new Intent(MainActivity.this, BluetoothDeviceListActivity.class);
                startActivityForResult(i, Constants.BLUETOOTH_DEVICE_SELECT);
            }
        });

        String lastDevice = getLastDevice();
        Log.d(TAG, "Using saved device:" + lastDevice);
        if (lastDevice != null) {
            connect(lastDevice);
        }
    }

    protected String getLastDevice() {
        SharedPreferences sharedPreferences = this.getPreferences(Context.MODE_PRIVATE);
        return sharedPreferences.getString(LAST_DEVICE, null);
    }

    protected void saveLastDevice(String deviceAddress) {
        SharedPreferences sharedPreferences = this.getPreferences(Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(LAST_DEVICE, deviceAddress).commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothConnectThread != null) {
            Log.d(TAG, "Cancelling Bluetooth Connect thread");
            bluetoothConnectThread.cancel();
        }
    }

    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    return;
                }
                break;
            case Constants.BLUETOOTH_DEVICE_SELECT:
                String deviceAddress = data.getStringExtra(BluetoothDeviceListActivity.EXTRA_DEVICE_ADDRESS);
                Log.d(TAG, "Device selected" + deviceAddress);
                saveLastDevice(deviceAddress);
                connect(deviceAddress);
                break;
        }
    }

    protected void connect(String deviceAddress) {
        Log.d(TAG, "Attempting connection to " + deviceAddress);
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        bluetoothConnectThread = new BluetoothConnectThread(device, handler);
        bluetoothConnectThread.start();
    }


    private void initializeGraph() {
        lineChart = (LineChart) findViewById(R.id.chart1);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(300f);
        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(105f);
        List<Entry> entries = new ArrayList<>();
        dataSet = new LineDataSet(entries, "MT");
        dataSet.addEntry(new Entry(0.0f, 0.0f));
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }

    private void handleDataPoint(String datum) {
        String[] parts = datum.split(" ");
        if (parts.length < 2) {
            return;
        }

        TextView receiver = null;
        switch (parts[0]) {
            case ("/temp/a14"):
                receiver = (TextView) findViewById(R.id.temp_hlt);
               break;
            case ("/temp/a15"):
                Long now = (new Date()).getTime() / 1000;
                if (firstDatapointTimestamp == 0) {
                    firstDatapointTimestamp = now;
                }
                Long timeElapsed = now - firstDatapointTimestamp;
                Entry entry = new Entry((float) timeElapsed, Float.parseFloat(parts[1]));
                dataSet.addEntry(entry);
                lineChart.invalidate();
                receiver = (TextView) findViewById(R.id.temp_mt);
                break;
        }

        if (receiver != null) {
            receiver.setText(parts[1]);
        }
    }
}

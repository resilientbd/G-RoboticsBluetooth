package bubt.faisal.com.g_roboticsbluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;

import bubt.faisal.com.g_roboticsbluetooth.activity.BaseActivity;
import bubt.faisal.com.g_roboticsbluetooth.bluetooth.DeviceConnector;
import bubt.faisal.com.g_roboticsbluetooth.bluetooth.DeviceListActivity;

import static bubt.faisal.com.g_roboticsbluetooth.activity.BaseActivity.MESSAGE_DEVICE_NAME;
import static bubt.faisal.com.g_roboticsbluetooth.activity.BaseActivity.MESSAGE_READ;
import static bubt.faisal.com.g_roboticsbluetooth.activity.BaseActivity.MESSAGE_STATE_CHANGE;
import static bubt.faisal.com.g_roboticsbluetooth.activity.BaseActivity.MESSAGE_TOAST;
import static bubt.faisal.com.g_roboticsbluetooth.activity.BaseActivity.MESSAGE_WRITE;

public class MainActivity extends BaseActivity {

    private static final String DEVICE_NAME = "DEVICE_NAME";
    private static final String LOG = "LOG";
    private String deviceName;
    // Подсветка crc
    private static final String CRC_OK = "#FFFF00";
    private static final String CRC_BAD = "#FF0000";

    private static final SimpleDateFormat timeformat = new SimpleDateFormat("HH:mm:ss.SSS");

//    private static String MSG_NOT_CONNECTED;
//    private static String MSG_CONNECTING;
//    private static String MSG_CONNECTED;

    private static DeviceConnector connector;
    private static BluetoothResponseHandler mHandler;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        if (mHandler == null) mHandler = new BluetoothResponseHandler(this);
        else mHandler.setTarget(this);
//        MSG_NOT_CONNECTED = getString(R.string.msg_not_connected);
//        MSG_CONNECTING = getString(R.string.msg_connecting);
//        MSG_CONNECTED = getString(R.string.msg_connected);

        if (isConnected() && (savedInstanceState != null)) {
            setDeviceName(savedInstanceState.getString(DEVICE_NAME));
        }
        //else getSupportActionBar().setSubtitle(MSG_NOT_CONNECTED);
    }
    private void stopConnection() {
        if (connector != null) {
            connector.stop();
            connector = null;
            deviceName = null;
        }
    }
    private boolean isConnected() {
        return (connector != null) && (connector.getState() == DeviceConnector.STATE_CONNECTED);
    }
    void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        getSupportActionBar().setSubtitle(deviceName);
    }
    private void startDeviceListActivity() {
        stopConnection();
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = btAdapter.getRemoteDevice(address);
                    if (super.isAdapterReady() && (connector == null)) setupConnector(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                super.pendingRequestEnableBt = false;
                if (resultCode != Activity.RESULT_OK) {
                    Utils.log("BT not enabled");
                }
                break;
        }
    }
    // ==========================================================================
    private void setupConnector(BluetoothDevice connectedDevice) {
        stopConnection();
        try {
            String emptyName = getString(R.string.empty_device_name);
            DeviceData data = new DeviceData(connectedDevice, emptyName);
            connector = new DeviceConnector(data, mHandler);
            connector.connect();
        } catch (IllegalArgumentException e) {
            Utils.log("setupConnector failed: " + e.getMessage());
        }
    }
    public class BluetoothResponseHandler extends Handler {
        private WeakReference<MainActivity> mActivity;

        public BluetoothResponseHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        public void setTarget(MainActivity target) {
            mActivity.clear();
            mActivity = new WeakReference<MainActivity>(target);
        }

        private String getCommandEnding() {
//            String result = Utils.getPrefence(this, getString(R.string.pref_commands_ending));
//            if (result.equals("\\r\\n")) result = "\r\n";
//            else if (result.equals("\\n")) result = "\n";
//            else if (result.equals("\\r")) result = "\r";
//            else result = "";
            String result=null;
            return result;
        }
        // ============================================================================

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case MESSAGE_STATE_CHANGE:

                        final ActionBar bar = activity.getSupportActionBar();
                        switch (msg.arg1) {
                            case DeviceConnector.STATE_CONNECTED:
                                //bar.setSubtitle(MSG_CONNECTED);
                                break;
                            case DeviceConnector.STATE_CONNECTING:
                              //  bar.setSubtitle(MSG_CONNECTING);
                                break;
                            case DeviceConnector.STATE_NONE:
                              //  bar.setSubtitle(MSG_NOT_CONNECTED);
                                break;
                        }
                        break;
                    // Here will be bluetooth recieved data.
                    case MESSAGE_READ:
                        final String readMessage = (String) msg.obj;

                        break;

                    case MESSAGE_DEVICE_NAME:
                        //activity.setDeviceName((String) msg.obj);
                        break;

                    case MESSAGE_WRITE:
                        // stub
                        break;

                    case MESSAGE_TOAST:
                        // stub
                        break;
                }
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.device_control_activity, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_search:
                try{
                    if (super.isAdapterReady()) {
                        if (isConnected()) stopConnection();
                        else startDeviceListActivity();
                    } else {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }
                }catch (Exception e)
                {
                    Toast.makeText(getBaseContext(),"Error: "+e,Toast.LENGTH_LONG).show();
                }

                return true;

            case R.id.menu_clear:
//                if (logTextView != null) logTextView.setText("");
                return true;

            case R.id.menu_send:
//                if (logTextView != null) {
//                    final String msg = logTextView.getText().toString();
//                    final Intent intent = new Intent(Intent.ACTION_SEND);
//                    intent.setType("text/plain");
//                    intent.putExtra(Intent.EXTRA_TEXT, msg);
//                    startActivity(Intent.createChooser(intent, getString(R.string.menu_send)));
//                }
                return true;

            case R.id.menu_settings:
//                final Intent intent = new Intent(this, SettingsActivity.class);
//                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    // ============================================================================

    public void sendCommand(View view)
    {
      String commandString = "abc";
        byte[] cm=commandString.getBytes();
        try{
            connector.write(cm);
        }catch (Exception e)
        {
           Toast.makeText(getBaseContext(),"Error: "+e,Toast.LENGTH_LONG).show();
        }



    }
}

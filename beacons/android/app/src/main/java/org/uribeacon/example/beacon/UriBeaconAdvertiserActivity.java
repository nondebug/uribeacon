package org.uribeacon.example.beacon;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * Advertise an Eddystone Beacon
 * See http://physical-web.org & https://github.com/google/eddystone/ for more info
 *
 * @author Don Coleman
 */
public class UriBeaconAdvertiserActivity extends Activity {

    private static final String TAG = "UriBeaconAdvertiser";
    private static final int ENABLE_BLUETOOTH_REQUEST = 17;
    // Really 0xFEAA, but Android seems to prefer the expanded 128-bit UUID version
    private static final ParcelUuid URI_BEACON_UUID = ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uri_beacon);

        setupBluetooth();
    }

    private void advertiseUriBeacon() {

        BluetoothLeAdvertiser bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        AdvertiseData advertisementData = getAdvertisementData();
        AdvertiseSettings advertiseSettings = getAdvertiseSettings();

        bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertisementData, advertiseCallback);
    }

    private AdvertiseData getAdvertisementData() {
        AdvertiseData.Builder builder = new AdvertiseData.Builder();
        builder.setIncludeTxPowerLevel(false);

        // Manually build the advertising info to send the URL http://www.eff.org
        // See https://github.com/google/eddystone/tree/master/eddystone-url
        byte[] beaconData = new byte[7];
        beaconData[0] = 0x10; // frame type: url
        beaconData[1] = (byte) 0xBA; // calibrated tx power at 0 m
        beaconData[2] = 0x00; // http://www.
        beaconData[3] = 0x65; // e
        beaconData[4] = 0x66; // f
        beaconData[5] = 0x66; // f
        beaconData[6] = 0x08; // .org

        builder.addServiceData(URI_BEACON_UUID, beaconData);

        // Adding 0xFEAA to the "Service Complete List UUID 16" (0x3) for iOS compatibility
        builder.addServiceUuid(URI_BEACON_UUID);

        return builder.build();
    }

    private AdvertiseSettings getAdvertiseSettings() {
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        builder.setConnectable(false);

        return builder.build();
    }

    private void setupBluetooth() {

        BluetoothManager bluetoothManager = (BluetoothManager) this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.bluetooth_error);
            builder.setMessage(R.string.no_bluetooth).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
            builder.show();

        } else if (!bluetoothAdapter.isEnabled()) {

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST);

        } else if (!bluetoothAdapter.isMultipleAdvertisementSupported()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.bluetooth_error);
            builder.setMessage(R.string.no_bluetooth_advertise)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
            builder.show();

        } else {
            advertiseUriBeacon();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.uri_beacon, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ENABLE_BLUETOOTH_REQUEST) {
            if (resultCode == RESULT_OK) {
                advertiseUriBeacon();
            } else {
                finish();
            }
        }
    }

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @SuppressLint("Override")
        @Override
        public void onStartSuccess(AdvertiseSettings advertiseSettings) {
            final String message = "Advertisement successful";
            Log.d(TAG, message);
            UriBeaconAdvertiserActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(UriBeaconAdvertiserActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        }

        @SuppressLint("Override")
        @Override
        public void onStartFailure(int i) {
            final String message = "Advertisement failed error code: " + i;
            Log.e(TAG, message);
            UriBeaconAdvertiserActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(UriBeaconAdvertiserActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        }

    };

}

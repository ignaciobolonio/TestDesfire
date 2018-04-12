package com.example.ignacio.testdesfire;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;
import android.widget.Toolbar;


import com.nxp.nfclib.CardType;
import com.nxp.nfclib.KeyType;
import com.nxp.nfclib.NxpNfcLib;
import com.nxp.nfclib.defaultimpl.KeyData;
import com.nxp.nfclib.desfire.DESFireFactory;
import com.nxp.nfclib.desfire.DESFireFile;
import com.nxp.nfclib.desfire.EV1ApplicationKeySettings;
import com.nxp.nfclib.desfire.IDESFireEV1;
import com.nxp.nfclib.utils.NxpLogUtils;

import java.io.File;
import java.security.Key;

import javax.crypto.spec.SecretKeySpec;


public class MainActivity extends AppCompatActivity {
    public static final String TAG = "DEBUGGING";
    // The package key you will get from the registration server
    private static String m_strPackageKey = "7b25332c391aea103047374396574019";
    public Button write_button;
    public Button read_button;
    private DrawerLayout mDrawerLayout;
    // The TapLinX library instance
    private NxpNfcLib m_libInstance = null;

    private IDESFireEV1 m_objDESFireEV1 = null;
    private CardType m_cardType = CardType.UnknownCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeLibrary();

        write_button = (Button) findViewById(R.id.write_icon);
        read_button = (Button) findViewById(R.id.read_icon);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);


        write_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Write button clicked");
                writeMode();
            }
        });

        read_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Read button clicked");
                readMode();
            }
        });

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                        menuItem.setChecked(true);
                        // close drawer when item is tapped
                        mDrawerLayout.closeDrawers();

                        // Add code here to update the UI based on the item selected
                        // For example, swap UI fragments here

                        return true;
                    }
                });
    }


    /**
     * Initialize the library and register to this activity.
     */
    @TargetApi(19)
    private void initializeLibrary() {
        m_libInstance = NxpNfcLib.getInstance();
        m_libInstance.registerActivity(this, m_strPackageKey);
    }

    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected void onResume() {
        m_libInstance.startForeGroundDispatch();
        super.onResume();
    }

    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected void onPause() {
        m_libInstance.stopForeGroundDispatch();
        super.onPause();
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * @param intent NFC intent from the android framework.
     * @see android.app.Activity#onNewIntent(android.content.Intent)
     */
    @Override
    public void onNewIntent(final Intent intent) {
        Log.d(TAG, "onNewIntent");
        Toast.makeText(getApplicationContext(), "Card detected", Toast.LENGTH_LONG).show();
        cardLogic(intent);
        super.onNewIntent(intent);
    }

    private void cardLogic(final Intent intent) {

        m_cardType = m_libInstance.getCardType(intent);
        if (CardType.DESFireEV1 == m_cardType) {
            Log.d(TAG, "DESFireEV1 found");
            m_objDESFireEV1 = DESFireFactory.getInstance()
                    .getDESFire(m_libInstance.getCustomModules());
            try {
                m_objDESFireEV1.getReader().connect();
                // Timeout to prevent exceptions in authenticate
                m_objDESFireEV1.getReader().setTimeout(2000);
                // Select root app
                m_objDESFireEV1.selectApplication(0);
                Log.d(TAG, "AID 000000 selected");
                // DEFAULT_KEY_2KTDES is a byte array of 24 zero bytes
                Key key = new SecretKeySpec(SampleAppKeys.KEY_2KTDES, "DESede");
                KeyData keyData = new KeyData();
                keyData.setKey(key);
                // Authenticate to PICC Master Key
                m_objDESFireEV1.authenticate(0, IDESFireEV1.AuthType.Native,
                        KeyType.TWO_KEY_THREEDES, keyData);
                Log.d(TAG, "DESFireEV1 authenticated");

            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }


    private void writeMode() {
        byte[] appId = new byte[]{0x01, 0x00, 0x00};
        int fileSize = 4;
        byte[] data = new byte[]{0x48, 0x6f, 0x6c, 0x61};
        int timeOut = 2000;
        int fileNo = 0;

        Key key0 = new SecretKeySpec(SampleAppKeys.KEY_AES128_ZEROS, "Master");
        KeyData keyData0 = new KeyData();
        keyData0.setKey(key0);
        Key key1 = new SecretKeySpec(SampleAppKeys.KEY_AES128_ONES, "KEY1");
        KeyData keyData1 = new KeyData();
        keyData1.setKey(key1);
        Key key2 = new SecretKeySpec(SampleAppKeys.KEY_AES128_TWOS, "KEY2");
        KeyData keyData2 = new KeyData();
        keyData2.setKey(key2);


        Log.d(TAG, "writeMode");

        try {
            m_objDESFireEV1.getReader().setTimeout(timeOut);

            m_objDESFireEV1.format();

            EV1ApplicationKeySettings.Builder appsetbuilder = new EV1ApplicationKeySettings.Builder();

            EV1ApplicationKeySettings appsettings = appsetbuilder.setAppKeySettingsChangeable(true)
                    .setAppMasterKeyChangeable(true)
                    .setAuthenticationRequiredForDirectoryConfigurationData(false)
                    .setMaxNumberOfApplicationKeys(3)
                    .setKeyTypeOfApplicationKeys(KeyType.AES128).build();

            m_objDESFireEV1.createApplication(appId, appsettings);
            m_objDESFireEV1.selectApplication(appId);
            //Log.i(TAG,"Stop");

            try {
                m_objDESFireEV1.authenticate(0, IDESFireEV1.AuthType.AES, KeyType.AES128,
                        keyData0);
                m_objDESFireEV1.changeKey(1, KeyType.AES128, SampleAppKeys.KEY_AES128_ZEROS,
                        SampleAppKeys.KEY_AES128_ONES, (byte) 0);
                m_objDESFireEV1.changeKey(2, KeyType.AES128, SampleAppKeys.KEY_AES128_ZEROS,
                        SampleAppKeys.KEY_AES128_TWOS, (byte) 0);
            } catch (Exception e) {
                Log.i(TAG, "writeMode: Could not change keys");
                Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_LONG).show();
            }


            m_objDESFireEV1.createFile(fileNo, new DESFireFile.StdDataFileSettings(
                    IDESFireEV1.CommunicationType.Enciphered, (byte) 0x2, (byte) 0x1, (byte) 0, (byte) 0, fileSize));

            m_objDESFireEV1.authenticate(1, IDESFireEV1.AuthType.AES, KeyType.AES128, keyData1);
            m_objDESFireEV1.writeData(0, 0, data, IDESFireEV1.CommunicationType.Enciphered);

            m_objDESFireEV1.getReader().close();
            Toast.makeText(getApplicationContext(), "Write Success!", Toast.LENGTH_LONG).show();
            Log.i(TAG, "Write success");

            // Set the custom path where logs will get stored, here we are setting the log folder DESFireLogs under
            // external storage.
            String spath = Environment.getExternalStorageDirectory().getPath() + File.separator + "DESFireLogs";
            NxpLogUtils.setLogFilePath(spath);
            // if you don't call save as below , logs will not be saved.
            NxpLogUtils.save();

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Write Failure", Toast.LENGTH_LONG).show();
            Log.i(TAG, "writeMode: Write data failed");
            e.printStackTrace();
        }
    }


    private void readMode() {
        byte[] appId = new byte[]{0x01, 0x00, 0x00};
        int fileSize = 4;
        byte[] readData = new byte[fileSize];
        int timeOut = 2000;
        int fileNo = 0;

        Key key0 = new SecretKeySpec(SampleAppKeys.KEY_AES128_ZEROS, "Master");
        KeyData keyData0 = new KeyData();
        keyData0.setKey(key0);
        Key key1 = new SecretKeySpec(SampleAppKeys.KEY_AES128_ONES, "KEY1");
        KeyData keyData1 = new KeyData();
        keyData1.setKey(key1);
        Key key2 = new SecretKeySpec(SampleAppKeys.KEY_AES128_TWOS, "KEY2");
        KeyData keyData2 = new KeyData();
        keyData2.setKey(key2);


        Log.d(TAG, "readMode");

        try {
            m_objDESFireEV1.getReader().setTimeout(timeOut);

            try {
                m_objDESFireEV1.selectApplication(appId);
                m_objDESFireEV1.authenticate(2, IDESFireEV1.AuthType.AES, KeyType.AES128, keyData2);
            } catch (Exception e) {
                Log.i(TAG, "readMode: Could not authenticate to read");
                Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_LONG).show();
            }


            readData = m_objDESFireEV1.readData(fileNo, 0, 0, IDESFireEV1.CommunicationType.Enciphered, fileSize);
            String result = new String(readData);

            m_objDESFireEV1.getReader().close();
            Log.i(TAG, "Read Success. Data:" + result);
            Toast.makeText(getApplicationContext(), "Read Success! Data:" + result, Toast.LENGTH_LONG).show();


            // Set the custom path where logs will get stored, here we are setting the log folder DESFireLogs under
            // external storage.
            String spath = Environment.getExternalStorageDirectory().getPath() + File.separator + "DESFireLogs";
            NxpLogUtils.setLogFilePath(spath);
            // if you don't call save as below , logs will not be saved.
            NxpLogUtils.save();

        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "readMode: Read data failed");
            Toast.makeText(getApplicationContext(), "Read failed", Toast.LENGTH_LONG).show();
        }
    }

}
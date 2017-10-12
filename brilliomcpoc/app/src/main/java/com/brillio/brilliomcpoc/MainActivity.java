package com.brillio.brilliomcpoc;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity implements FingerprintUiHelper.Callback {

    private static final String DEFAULT_KEY_NAME = "mc_key";
    private static final String DIALOG_FRAGMENT_TAG = "scgFragment";
    private static final long ERROR_FP = 30000;
    android.app.FragmentManager fm = getFragmentManager();
    private KeyguardManager keyguardManager;
    private FingerprintManager fingerprintManager;
    private KeyStore mKeyStore;
    private KeyGenerator mKeyGenerator;
    private Cipher defaultCipher;
    private SharedPreferences mSharedPreferences;
    private FingerprintUiHelper mFingerprintUiHelper;
    TextView messageText;
    TextView merchantText;
    TextView registerText;
    ProgressBar mprogressbar;
    private static final long ERROR_TIMEOUT_MILLIS = 2000;
    private static final long PRG_TIME = 8000;
    RelativeLayout fingerprintcontainer , progresscontainer;

    ImageView mFingerPrintIcon;
    private MPreference mPreference;
    private View mBottomSheetView;
    private BottomSheetBehavior mBottomSheetBehavior;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFingerPrintIcon = (ImageView) findViewById(R.id.fingerprint_icon);
        fingerprintcontainer = (RelativeLayout)findViewById(R.id.rl_fingerprintcontainer);
        progresscontainer = (RelativeLayout)findViewById(R.id.rl_progressbar);
        progresscontainer.setVisibility(View.GONE);
        mprogressbar = (ProgressBar)findViewById(R.id.progressbar);
        mBottomSheetView =  findViewById(R.id.bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(mBottomSheetView);
        messageText = (TextView) findViewById(R.id.fingerprint_status);
        merchantText = (TextView) findViewById(R.id.airbnbText);
        registerText = (TextView)findViewById(R.id.registerFPText);
        mPreference = new MPreference(getApplicationContext());

        if(mPreference.getTimeStamp()==0) {
            activeFingerPrint();

        }else{
            Long finerprintLastTime = mPreference.getTimeStamp();
            Long fingerprintCurrentTime = Calendar.getInstance().getTimeInMillis();
            long diff = fingerprintCurrentTime - finerprintLastTime;
            if(diff > ERROR_FP){
                mPreference.setTimeStamp(0);
                MainActivity.this.activeFingerPrint();
            }else{
                Toast.makeText(getApplicationContext(),"Please try to make payment after 30 seconds", Toast.LENGTH_SHORT).show();
                Handler mhandler = new Handler();
                mhandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.activeFingerPrint();
                        Toast.makeText(getApplicationContext(),"Please use your registered fingerprint to authenticate payment", Toast.LENGTH_SHORT).show();
                                            }
                }, ERROR_FP);
            }
        }

        Intent intent = getIntent();

        String total = intent.getStringExtra("total");
        String merchantName = intent.getStringExtra("data");
        if (TextUtils.isEmpty(total))
            return;

        try {
            JSONObject totalObject = new JSONObject(total);
            JSONObject customdataObject = new JSONObject(merchantName);
            total = totalObject.getString("value");
            merchantName = customdataObject.getString("merchantName");
            TextView v = (TextView) findViewById(R.id.json_content);
            v.setText(
                    //"merchantName: " + merchantName + "\n" +
                    "Total: " + "$" + total
            );
            merchantText.setText(merchantName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean activeFingerPrint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            keyguardManager = getSystemService(KeyguardManager.class);
            fingerprintManager = getSystemService(FingerprintManager.class);
            initFingerPrint();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return true;
            }
            if (!fingerprintManager.hasEnrolledFingerprints()) {
                mFingerPrintIcon.setColorFilter(ContextCompat.getColor(this, R.color.grey));
                // This happens when no fingerprints are registered.
         /*   Toast.makeText(this,
                    "Go to 'Settings -> Security -> Fingerprint' and register at least one fingerprint",
                    Toast.LENGTH_SHORT).show();*/
                registerText.setText("Please register atleast one fingerprint");
                registerText.setPaintFlags(registerText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                registerText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivityForResult(new Intent(Settings.ACTION_SECURITY_SETTINGS), 101);
                    }
                });
            }
            if (initCipher(defaultCipher)) {
                mFingerprintUiHelper = new FingerprintUiHelper(
                        getSystemService(FingerprintManager.class),
                        MainActivity.this);
                mFingerprintUiHelper.startListening(new FingerprintManager.CryptoObject(defaultCipher));
            }
            return true;
        }
        return false;
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onPause() {
        super.onPause();
        if (mFingerprintUiHelper != null)
            mFingerprintUiHelper.stopListening();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onStop() {
        super.onStop();
        if (mFingerprintUiHelper != null)
            mFingerprintUiHelper.stopListening();
    }

    private void initFingerPrint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mKeyStore = KeyStore.getInstance("AndroidKeyStore");
            } catch (KeyStoreException e) {
                //throw new RuntimeException("Failed to get an instance of KeyStore", e);
                System.out.println("Failed to get an instance of KeyStore");
            }
            try {
                mKeyGenerator = KeyGenerator
                        .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                // throw new RuntimeException("Failed to get an instance of KeyGenerator", e);
                System.out.println("Failed to get an instance of KeyGenerator");
            }
            defaultCipher = null;
            try {
                defaultCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                        + KeyProperties.BLOCK_MODE_CBC + "/"
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                //throw new RuntimeException("Failed to get an instance of Cipher", e);
                System.out.println("Failed to get an instance of Cipher");
            }
            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            createKey();
        }
    }

    private void createKey() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mKeyStore.load(null);
                KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(DEFAULT_KEY_NAME,
                        KeyProperties.PURPOSE_ENCRYPT |
                                KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        // Require the user to authenticate with a fingerprint to authorize every use
                        // of the key
                        .setUserAuthenticationRequired(true)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    builder.setInvalidatedByBiometricEnrollment(true);
                }
                mKeyGenerator.init(builder.build());
                mKeyGenerator.generateKey();
            } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException
                    | CertificateException | IOException e) {
                //throw new RuntimeException(e);
                System.out.println("Error Generate");
            }
        }
    }

    private boolean initCipher(Cipher cipher) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mKeyStore.load(null);
                SecretKey key = (SecretKey) mKeyStore.getKey(DEFAULT_KEY_NAME, null);
                cipher.init(Cipher.ENCRYPT_MODE, key);
                return true;
            } catch (KeyPermanentlyInvalidatedException e) {
                return false;
            } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                    | NoSuchAlgorithmException | InvalidKeyException e) {
                //throw new RuntimeException("Failed to init Cipher", e);
                System.out.println("Error Generate");
                return false;
            }
        } else {
            return false;
        }
    }

    public void onFingerPrintAuthSuccess(boolean status) {
        JSONObject jsonObject = new JSONObject();
        JSONObject rootJsonObject = new JSONObject();
        //JSONObject addressjsonObject = new JSONObject();
        try {
            // use if for all attriburte and match json string and our jsonobject is same or not
            jsonObject.put("cardholderName", "First Last");
            jsonObject.put("cardToken", "1234567890123456");
            jsonObject.put("tokenProviderURL", "https://www.masterpass.com/masterpass");
            jsonObject.put("tokenExpiryDate", "12-22");
            jsonObject.put("cryptogram", "0064F1DEAB336112C600048DE908B602005514");
            jsonObject.put("lastFourOfFPAN", "1234");
            jsonObject.put("trid", "50100000000");
            jsonObject.put("typeOfCryptogram", "UCAF");

            /*addressjsonObject.put("country","");
            addressjsonObject.put("addressLine","2200 MasterCard Blvd\n" +
                    "O'fallon MO, 63368");
            addressjsonObject.put("region","");
            addressjsonObject.put("city","");
            addressjsonObject.put("dependentLocality","");
            addressjsonObject.put("postalCode","63368");
            addressjsonObject.put("sortingCode","");
            addressjsonObject.put("languageCode","");
            addressjsonObject.put("organization","");
            addressjsonObject.put("recipient","");
            jsonObject.put("cardNumber", "2226470000067784");
            jsonObject.put("cardholderName", "Mastercard 2nd Series Bin");
            jsonObject.put("cardSecurityCode", "566");
            jsonObject.put("expiryMonth", "10");
            jsonObject.put("expiryYear", "21");
            jsonObject.put("paymentAddress",addressjsonObject);*/
            if(status)
                jsonObject.put("status", "success");
            else
                jsonObject.put("status", "fail");
            //rootJsonObject.put("basicCardResponse", jsonObject);
            rootJsonObject.put("networkTokenizedCardResponse", jsonObject);
            // rootJSonObject.toString() value is as similar as jsonString

              //cardToken

            Intent result = new Intent();
            Bundle extras = new Bundle();
            extras.putString("methodName", "masterpass");
            // here instead of jsoString use rootJsonObject.toString()
            extras.putString("details", rootJsonObject.toString());

            result.putExtras(extras);
            setResult(RESULT_OK, result);
            finish();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onAuthenticated() {
        // Toast.makeText(getApplicationContext(),"Auth success", Toast.LENGTH_SHORT).show();
        // show fingerprint green icon for 1300 seconds then call onFingerPrintAuthSuccess()
        messageText.setText("Success");
        fingerprintcontainer.setVisibility(View.GONE);
        progresscontainer.setVisibility(View.VISIBLE);
        messageText.setTextColor(getResources().getColor(R.color.green));
        mFingerPrintIcon.setColorFilter(ContextCompat.getColor(this, R.color.green));
        mPreference.setTimeStamp(0);
        final String successtext = messageText.getText().toString();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                    mprogressbar.setVisibility(View.GONE);
                    MainActivity.this.onFingerPrintAuthSuccess(true);
            }
        }, ERROR_TIMEOUT_MILLIS);
    }

    @Override
    public void onError(String messageContent) {


        // update error messgae content below authorization of fingerprint textview.

        messageText.setText(messageContent);
        messageText.setTextColor(getResources().getColor(R.color.red));
        //Toast.makeText(getApplicationContext(), messageContent, Toast.LENGTH_SHORT).show();
        mFingerPrintIcon.setColorFilter(ContextCompat.getColor(this, R.color.red));
        if(messageContent.toLowerCase().contains("many")){
            mPreference.setTimeStamp(Calendar.getInstance().getTimeInMillis());
            onFingerPrintAuthSuccess(false);
        }


    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void fingerPrintNotEnrollerd() {
        // show light grey icon and hyperlink text view whill navigate to sectuiry settings of device.
        // then onActivityResult check fingerprint is registered or not.
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 101) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            if (!fingerprintManager.hasEnrolledFingerprints()) {

                mFingerPrintIcon.setColorFilter(ContextCompat.getColor(this, R.color.grey));
                // This happens when no fingerprints are registered.
             /*   Toast.makeText(this,
                        "Go to 'Settings -> Security -> Fingerprint' and register at least one fingerprint",
                        Toast.LENGTH_SHORT).show();*/
                registerText.setText("Please register atleast one fingerprint");
                registerText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivityForResult(new Intent(Settings.ACTION_SECURITY_SETTINGS), 101);
                    }
                });

                return;

            }else{
                mFingerPrintIcon.setColorFilter(ContextCompat.getColor(this, R.color.black));
                registerText.setVisibility(View.GONE);
                initFingerPrint();
                if (initCipher(defaultCipher)) {
                    mFingerprintUiHelper = new FingerprintUiHelper(
                            getSystemService(FingerprintManager.class),
                            MainActivity.this);
                    mFingerprintUiHelper.startListening(new FingerprintManager.CryptoObject(defaultCipher));
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onFingerPrintAuthSuccess() {
    }
}

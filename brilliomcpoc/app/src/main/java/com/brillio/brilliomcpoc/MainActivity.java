package com.brillio.brilliomcpoc;

import android.app.KeyguardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

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

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {

    private static final String DEFAULT_KEY_NAME = "socalgas_key";
    private static final String DIALOG_FRAGMENT_TAG = "scgFragment";
    android.app.FragmentManager fm = getFragmentManager();
    private KeyguardManager keyguardManager;
    private FingerprintManager fingerprintManager;
    private KeyStore mKeyStore;
    private KeyGenerator mKeyGenerator;
    private Cipher defaultCipher;
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            keyguardManager = getSystemService(KeyguardManager.class);
            fingerprintManager = getSystemService(FingerprintManager.class);
            initFingerPrint();
        }

        findViewById(R.id.back_to_browser).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {

                if (initCipher(defaultCipher)) {
                    // Show the fingerprint dialog. The user has the option to use the fingerprint with
                    // crypto, or you can fall back to using a server-side verified password.
                    FingerprintAuthenticationDialogFragment fragment
                            = new FingerprintAuthenticationDialogFragment();
                    fragment.setCryptoObject(new FingerprintManager.CryptoObject(defaultCipher));

                    boolean useFingerprintPreference = mSharedPreferences
                            .getBoolean(getString(R.string.use_fingerprint_to_authenticate_key),
                                    true);
                    if (useFingerprintPreference) {
                        fragment.setStage(
                                FingerprintAuthenticationDialogFragment.Stage.FINGERPRINT);
                    }
                    fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
                }
            }
        });

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
                    "merchantName: " + merchantName + "\n" +
                            "total: " + total
            );

        } catch (JSONException e) {
            e.printStackTrace();
        }
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

    public void onError(String s) {

    }

    public void onFingerPrintAuthSuccess() {
        String jsonString = "{\n" +
                "\t\"networkTokenizedCardResponse\": {\n" +
                "\t\t\"cardholderName\": \"First Last\",\n" +
                "\t\t\"cardToken\": \"1234567890123456\",\n" +
                "\t\t\"tokenProviderURL\": \"https://www.masterpass.com/masterpass\",\n" +
                "\t\t\"tokenExpiryDate\": \"12-22\",\n" +
                "\t\t\"cryptogram\": \"0064F1DEAB336112C600048DE908B602005514\",\n" +
                "\t\t\"lastFourOfFPAN\": \"1234\",\n" +
                "\t\t\"trid\": \"50100000000\",\n" +
                "\t\t\"typeOfCryptogram\": \"UCAF\"\n" +
                "\t}\n" +
                "}";
        Intent result = new Intent();
        Bundle extras = new Bundle();
        extras.putString("methodName", "bankoo");
        extras.putString("details", jsonString);

        result.putExtras(extras);
        setResult(RESULT_OK, result);
        finish();
    }
}

package amiin.bazouk.application.com.demo_bytes_android.activities.firsttimesactivities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.NetworkOnMainThreadException;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;

import amiin.bazouk.application.com.demo_bytes_android.Constants;
import amiin.bazouk.application.com.demo_bytes_android.R;
import amiin.bazouk.application.com.demo_bytes_android.activities.MainActivity;
import amiin.bazouk.application.com.demo_bytes_android.activities.RootDetectedDialog;
import amiin.bazouk.application.com.demo_bytes_android.iota.Seed;
import amiin.bazouk.application.com.demo_bytes_android.iota.SeedValidator;
import amiin.bazouk.application.com.demo_bytes_android.utils.DetectRoot;
import amiin.bazouk.application.com.demo_bytes_android.utils.Email;
import amiin.bazouk.application.com.demo_bytes_android.utils.EmailValidator;
import amiin.bazouk.application.com.demo_bytes_android.utils.InternetConn;
import amiin.bazouk.application.com.demo_bytes_android.utils.RandomDigits;
import jota.utils.SeedRandomGenerator;

public class JoinActivity extends AppCompatActivity {

    private TextInputEditText seedEditText;
    private TextInputLayout seedEditTextLayout;
    private TextInputEditText emailEditText;
    private TextInputLayout emailEditTextLayout;
    private Button loginButton;

    private static SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.getBoolean(Constants.PREF_RUN_WITH_ROOT, false)) {
            if (DetectRoot.isDeviceRooted()) {
                RootDetectedDialog dialog = new RootDetectedDialog();
                dialog.show(this.getFragmentManager(), null);
            }
        }


        loginButton = findViewById(R.id.go_to_code);
        disableLoginButton();

        seedEditText = findViewById(R.id.seed_login_seed_input);
        seedEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                validateInputs();
                seedEditTextLayout.setError(null);
            }
        });
        seedEditTextLayout = findViewById(R.id.seed_login_seed_text_input_layout);

        emailEditText = findViewById(R.id.email_input);
        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                emailEditTextLayout.setError(null);
                validateInputs();
            }
        });
        emailEditTextLayout = findViewById(R.id.email_login_text_input_layout);


        findViewById(R.id.generate_seed_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String generatedSeed = SeedRandomGenerator.generateNewSeed();
                seedEditText.setText(generatedSeed);
                Bundle bundle = new Bundle();
                bundle.putString("generatedSeed", generatedSeed);
                GenerateSeedDialog dialog = new GenerateSeedDialog();
                dialog.setArguments(bundle);
                dialog.show(getFragmentManager(), null);
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginDialog();
            }
        });
    }

    private void validateInputs() {
        String seed = seedEditText.getText().toString();
        String email = emailEditText.getText().toString();
        if(seed.isEmpty() || email.isEmpty()) {
            disableLoginButton();
        }
        else{
            enableLoginButton();
        }
    }

    private void enableLoginButton() {
        loginButton.setEnabled(true);
        loginButton.setBackgroundColor(Color.parseColor("#FFAD6BEF"));
        loginButton.setAlpha(1);
    }

    private void disableLoginButton() {
        loginButton.setEnabled(false);
        loginButton.setBackgroundColor(Color.GRAY);
        loginButton.setAlpha(.5f);
    }

    private void sendEmailWithCode(String code) throws IOException {
        final String recipient = ((TextInputEditText)findViewById(R.id.email_input)).getText().toString();
        Thread sendEmailThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    Email email = new Email(
                            getResources().getString(R.string.mailgun_domain_name),
                            getResources().getString(R.string.mailgun_api_key)
                    );
                    email.sendAuthEmail(recipient, code);

                } catch (Exception e) {
                    Log.e("SendMail", e.getMessage(), e);
                }
            }
        });
        sendEmailThread.start();
    }

    private void loginDialog() {
        String seed = seedEditText.getText().toString();
        String email = emailEditText.getText().toString();

        if(!EmailValidator.isValid(email)) {
            emailEditTextLayout.setError(getString(R.string.messages_invalid_email));
            if (emailEditTextLayout.getError() != null)
                return;
        }

        if (SeedValidator.isSeedValid(this, seed) != null) {
            seedEditTextLayout.setError(getString(R.string.messages_invalid_seed));
            if (seedEditTextLayout.getError() != null)
                return;
        }

        if (!InternetConn.isConnected(getApplicationContext())) {
            showAlertDialog("Email cannot be sent. Not connected to internet.");
            return;
        }

        String code = RandomDigits.getRandom6();

        try {
            sendEmailWithCode(code);
            Seed.saveSeed(getApplicationContext(), seed);

            Intent intent = new Intent(JoinActivity.this, CodeActivity.class);
            intent.putExtra(Constants.CODE, code);
            startActivity(intent);

        } catch (NetworkOnMainThreadException e) {
            e.printStackTrace();
            showAlertDialog("Email cannot be sent. Not connected to internet.");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("LOGIN", e.toString(), e);
            showAlertDialog(e.getMessage() != null ? e.getMessage() : "Internal Error occurred.");
        }
    }

    private void showAlertDialog(String errMsg) {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("Unable to proceed")
                .setMessage(errMsg)
                .setCancelable(false)
                .setNegativeButton(R.string.buttons_ok, null)
                .create();

        alertDialog.show();
    }

    protected void onStart(){
        super.onStart();
        if(!PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(Constants.IS_FIRST_TIME,true)){
            startActivity(new Intent(this, MainActivity.class));
        }
    }
}

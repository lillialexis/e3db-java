package com.tozny.e3db.feedback;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tozny.e3db.Client;
import com.tozny.e3db.Config;
import com.tozny.e3db.Result;
import com.tozny.e3db.ResultHandler;

import java.io.FileOutputStream;
import java.util.UUID;

import static com.tozny.e3db.feedback.Properties.REQUEST_REGISTRATION;

public class RegistrationActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setContentView(R.layout.activity_registration);
    Client.register(Properties.ACCOUNT_TOKEN, "feedback " + UUID.randomUUID(), "https://dev.e3db.com", new ResultHandler<Config>() {
      @Override
      public void handle(Result<Config> result) {
        try {
          if (result.isError()) {
            throw new RuntimeException(result.asError().other());
          }

          try (FileOutputStream fileOutputStream = openFileOutput("credentials.json", MODE_PRIVATE)) {
            fileOutputStream.write(result.asValue().json().getBytes("UTF-8"));
            setResult(REQUEST_REGISTRATION);

            TextView message = findViewById(R.id.registration_message);
            message.setText("Success!");
            ProgressBar p = findViewById(R.id.registration_progressBar);
            p.setVisibility(View.INVISIBLE);

            finish();
          }
        }
        catch(Throwable e) {
          Log.e("feedback", "Error registering" + e);
          throw new RuntimeException(e);
        }
      }
    });
    super.onCreate(savedInstanceState);
  }
}

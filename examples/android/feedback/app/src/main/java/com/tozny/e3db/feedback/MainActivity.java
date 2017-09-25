package com.tozny.e3db.feedback;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.tozny.e3db.Client;
import com.tozny.e3db.ClientBuilder;
import com.tozny.e3db.Config;
import com.tozny.e3db.QueryParams;
import com.tozny.e3db.QueryParamsBuilder;
import com.tozny.e3db.QueryResponse;
import com.tozny.e3db.Record;
import com.tozny.e3db.RecordData;
import com.tozny.e3db.Result;
import com.tozny.e3db.ResultHandler;

import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.tozny.e3db.feedback.Properties.REQUEST_REGISTRATION;

public class MainActivity extends AppCompatActivity {

  private final AtomicBoolean registered = new AtomicBoolean(false);
  private final AtomicReference<Record> record = new AtomicReference<>(null);

  private Client client;

  private void createClient() {
    try (FileInputStream fileInputStream = openFileInput("credentials.json")) {
      client = new ClientBuilder().fromConfig(
        Config.fromJson(
          new String(IOUtils.toByteArray(fileInputStream), "UTF-8")
        )).build();
      registered.set(true);
    } catch (FileNotFoundException ex) {
      registered.set(false);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private void manageFeedback() {
    QueryParams params = new QueryParamsBuilder().setTypes(Properties.RECORD_TYPE).setIncludeData(true).build();
    client.query(params,
      new ResultHandler<QueryResponse>() {
        @Override
        public void handle(Result<QueryResponse> result) {
          try {
            if(result.isError())
              throw new RuntimeException(result.asError().other());

            Button deleteButton = findViewById(R.id.feedback_delete);
            Button submitButton = findViewById(R.id.feedback_submit);
            EditText feedbackName = (EditText) findViewById(R.id.feedback_name);
            EditText feedbackComments = (EditText) findViewById(R.id.feedback_comments);

            List<Record> records = result.asValue().records();
            if(records.size() > 0) {
              record.set(records.get(0));
              Record feedback = records.get(0);

              if (feedback.data().containsKey(Properties.NAME))
                feedbackName.setText(feedback.data().get(Properties.NAME));

              if (feedback.data().containsKey(Properties.COMMENTS))
                feedbackComments.setText(feedback.data().get(Properties.COMMENTS));

              deleteButton.setEnabled(true);
              deleteButton.setVisibility(View.VISIBLE);

              submitButton.setText("Update");
              submitButton.setEnabled(true);
            }
            else {
              record.set(null);
              feedbackName.setText(null);
              feedbackComments.setText(null);

              deleteButton.setVisibility(View.INVISIBLE);
              deleteButton.setEnabled(false);

              submitButton.setText("Submit");
              submitButton.setEnabled(true);
            }
          }
          catch(Throwable e) {
            throw new RuntimeException(e);
          }
        }
      });
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    createClient();
    setContentView(R.layout.activity_main);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch(requestCode) {
      case REQUEST_REGISTRATION:
        break;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  protected void onResume() {
    createClient();

    if(! registered.get()) {
      startActivityForResult(new Intent(this, RegistrationActivity.class), REQUEST_REGISTRATION);
    }
    else {
      final Button deleteButton = findViewById(R.id.feedback_delete);
      final Button submitButton = findViewById(R.id.feedback_submit);
      deleteButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          final Record r = record.get();
          if(r != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Are you sure you want to delete your feedback?");
            builder.setTitle("Delete Feedback");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                client.delete(r.meta().recordId(), r.meta().version(), new ResultHandler<Void>() {
                  @Override
                  public void handle(Result<Void> result) {
                    if(result.isError())
                      throw new RuntimeException(result.asError().other());

                    Toast.makeText(MainActivity.this, "Feedback Deleted", Toast.LENGTH_SHORT).show();
                    manageFeedback();
                  }
                });
              }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {

              }
            });

            builder.show();
          }
        }
      });

      submitButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          Map<String, String> feedback = new HashMap<>();
          feedback.put(Properties.COMMENTS, ((EditText) findViewById(R.id.feedback_comments)).getText().toString());
          feedback.put(Properties.NAME, ((EditText) findViewById(R.id.feedback_name)).getText().toString());

          if (record.get() != null) {
            Toast.makeText(MainActivity.this, "Updating Feedback", Toast.LENGTH_SHORT).show();
            client.update(record.get().meta(), new RecordData(feedback), null, new ResultHandler<Record>() {
              @Override
              public void handle(Result<Record> result) {
                if(result.isError())
                  throw new RuntimeException(result.asError().other());

                record.set(result.asValue());
                Toast.makeText(MainActivity.this, "Feedback Updated", Toast.LENGTH_SHORT).show();
                manageFeedback();
              }
            });
          } else {
            client.write(Properties.RECORD_TYPE, new RecordData(feedback), null, new ResultHandler<Record>() {
              @Override
              public void handle(Result<Record> result) {
                if(result.isError())
                  throw new RuntimeException(result.asError().other());

                record.set(result.asValue());
                Toast.makeText(MainActivity.this, "Feedback Submitted", Toast.LENGTH_SHORT).show();
                client.share(Properties.RECORD_TYPE, Properties.READER_ID, new ResultHandler<Void>() {
                  @Override
                  public void handle(Result<Void> result2) {
                  }
                });

                manageFeedback();
              }
            });
          }
        }
      });

      manageFeedback();
    }
    super.onStart();
  }

}

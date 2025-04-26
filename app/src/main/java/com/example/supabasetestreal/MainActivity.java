package com.example.supabasetestreal;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import android.Manifest;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
public class MainActivity extends AppCompatActivity {
    // Declare the launcher at the top of your Activity/Fragment:
    // Declare the launcher at the top of your Activity/Fragment:
    RequestQueue requestQueue;
    String secretKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJxZ2Jzb21obGRwZnNiZ2ZrZ3NiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Mzk4MTQzNTcsImV4cCI6MjA1NTM5MDM1N30.GwQuIoF3Jh5epC9CiP8eTLcVGWEmutgBzt6ouFmfd7I";
    ListView myList;
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // FCM SDK (and your app) can post notifications.
                } else {
                    // TODO: Inform user that that your app will not show notifications.
                }
            });

    private void askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
    public String event(String g) {
        switch(g) {
            case "moti":
                return "Motion detected.";
            case "touch":
                return "Door knob touched.";
            case "fail":
                return "Failed attempt detected.";
            default:
                return "Door opened by " + g + ".";
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String timeDist(Duration d) {
        if(d.getSeconds()<60) {
            return d.getSeconds() + " seconds ago.";
        } else if(d.toMinutes()<60) {
            return d.toMinutes() + " minutes ago.";
        } else if(d.toHours()<60) {
            return d.toHours() + " hours ago.";
        } else  {
            return d.toDays() + " days ago.";
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public String eventText(JsonElement a) {

        CharSequence timestampString;
        OffsetDateTime timestamp = OffsetDateTime.parse(a.getAsJsonObject().get("created_at").getAsString());
        Duration d = Duration.between( timestamp , OffsetDateTime.now() ) ;
        System.out.println(d.getSeconds());

        return event(a.getAsJsonObject().get("eventID").getAsString()) + " - " + timeDist(d);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         requestQueue = Volley.newRequestQueue(getApplicationContext());
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        askNotificationPermission();
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {

                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }
                        Log.w(TAG, "Fetching !!!!!!!!!!!!", task.getException());

                        // Get new FCM registration token
                        String token = task.getResult();

                        // Log and toast
                        System.out.println(token);
                    }
                });
        Handler handler = new Handler();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());

        Runnable r=new Runnable() {
            public void run() {
                adapter.clear();
                String url = "https://bqgbsomhldpfsbgfkgsb.supabase.co/rest/v1/log?select=created_at,eventID&order=created_at.desc&limit=20";
                StringRequest postRequest = new StringRequest(Request.Method.GET, url,
                        new Response.Listener<String>()
                        {
                            @Override
                            public void onResponse(String response) {
                                // response
                                Log.d("Response", response);
                                JsonArray data = (JsonArray) JsonParser.parseString(response);
                                for(JsonElement g : data) {
                                    adapter.add(eventText(g));
                                }
                            }
                        },
                        new Response.ErrorListener()
                        {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                // TODO Auto-generated method stub
                                Log.d("ERROR","error => "+error.toString());
                            }
                        }
                ) {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String>  params = new HashMap<String, String>();
                        params.put("apikey", secretKey);

                        return params;
                    }
                };
                requestQueue.add(postRequest);


                myList = findViewById(R.id.listView);
                myList.setAdapter(adapter);
                handler.postDelayed(this, 10000);
            }
        };
        handler.postDelayed(r, 0);

    }

}
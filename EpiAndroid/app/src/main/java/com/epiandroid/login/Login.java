package com.epiandroid.login;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.transition.Explode;
import android.transition.Slide;
import android.view.Display;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.epiandroid.ErrorResponse;
import com.epiandroid.R;
import com.epiandroid.Token;
import com.epiandroid.mainLayout.MainActivity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import java.io.IOException;

public class Login extends AppCompatActivity {
    private String login;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        final SharedPreferences settings = getSharedPreferences("preferences", 0);
        final SharedPreferences.Editor editor = settings.edit();

        login = settings.getString("login", null);
        password = settings.getString("password", null);
        final CheckBox checkBox = (CheckBox) findViewById(R.id.remember);

        if (login != null && password != null) {
            checkBox.setChecked(true);
            ((EditText) findViewById(R.id.login)).setText(login);
            ((EditText) findViewById(R.id.password)).setText(password);
        }
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int height = size.y;
        ((ImageView) findViewById(R.id.logo)).setMaxHeight(height / 2);

        connexion(editor, checkBox);
        moveToTop();
        setupWindowAnimations();
    }

    private void setupWindowAnimations() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Explode explode = new Explode();
            explode.setDuration(1000);
            getWindow().setExitTransition(explode);
            getWindow().setReturnTransition(new Slide());
        }
    }

    private void moveToTop() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ImageView imageView = (ImageView) findViewById(R.id.logo);
                Animation animTranslate = AnimationUtils.loadAnimation(Login.this, R.anim.logo_in);
                animTranslate.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        findViewById(R.id.form).setVisibility(View.VISIBLE);
                        Animation animFade = AnimationUtils.loadAnimation(Login.this, R.anim.form_in);
                        findViewById(R.id.form).setAnimation(animFade);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                imageView.startAnimation(animTranslate);
            }
        }, 800);
    }

    private void connexion(final SharedPreferences.Editor editor, final CheckBox checkBox) {
        final Button buttonLogin = (Button) findViewById(R.id.buttonLogin);
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.form).setVisibility(View.GONE);
                findViewById(R.id.progress).setVisibility(View.VISIBLE);
                findViewById(R.id.error).setVisibility(View.GONE);
                login = ((EditText) findViewById(R.id.login)).getText().toString();
                password = ((EditText) findViewById(R.id.password)).getText().toString();

                Ion.with(getApplicationContext())
                        .load("http://epitech-api.herokuapp.com/login")
                        .setBodyParameter("login", login)
                        .setBodyParameter("password", password)
                        .asJsonObject().withResponse().setCallback(new FutureCallback<Response<JsonObject>>() {
                    @Override
                    public void onCompleted(java.lang.Exception e, Response<JsonObject> result) {
                        findViewById(R.id.progress).setVisibility(View.GONE);
                        if (result == null) {
                            ((TextView) findViewById(R.id.error)).setText(getResources().getString(R.string.noconnexion));
                            findViewById(R.id.error).setVisibility(View.VISIBLE);
                        } else if (result.getHeaders().code() == 200)
                            success(editor, checkBox, result.getResult());
                        else
                            error(result.getResult());
                        findViewById(R.id.form).setVisibility(View.VISIBLE);
                    }
                });

            }
        });
    }

    private void success(SharedPreferences.Editor editor, CheckBox checkBox, JsonObject result) {
        try {
            if (checkBox.isChecked()) {
                editor.putString("login", login);
                editor.putString("password", password);
            } else {
                editor.remove("login");
                editor.remove("password");
            }
            ObjectMapper objectMapper = new ObjectMapper();

            Token token = objectMapper.readValue(String.valueOf(result), Token.class);
            editor.putString("token", token.getToken());
            editor.commit();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
            } else
                startActivity(intent);
            // finish();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void error(JsonObject result) {
        try {
            findViewById(R.id.form).setVisibility(View.VISIBLE);
            ObjectMapper objectMapper = new ObjectMapper();
            ErrorResponse errorResponse = objectMapper.readValue(String.valueOf(result), ErrorResponse.class);
            ((TextView) findViewById(R.id.error)).setText(errorResponse.getError().getMessage());
            findViewById(R.id.error).setVisibility(View.VISIBLE);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}

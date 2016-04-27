package com.studio.artaban.anaglyph3d.process.configure;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewStub;

import com.studio.artaban.anaglyph3d.R;

public class ContrastActivity extends AppCompatActivity {

    //
    public void onValidateContrast(View sender) {







    }

    //////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contrast);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (toolbar != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                toolbar.setBackgroundColor(Color.BLACK);
                getWindow().setNavigationBarColor(Color.BLACK);
                getWindow().setStatusBarColor(Color.BLACK);
            }
            else // Default status bar color (API < 21)
                toolbar.setBackgroundColor(Color.argb(255, 30, 30, 30));
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);






        final ViewStub stub = (ViewStub) findViewById(R.id.layout_container);
        stub.setLayoutResource(R.layout.contrast_landscape);
        stub.inflate();





    }
}

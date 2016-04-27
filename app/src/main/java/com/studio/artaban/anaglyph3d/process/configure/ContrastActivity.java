package com.studio.artaban.anaglyph3d.process.configure;

import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.helpers.Logs;

public class ContrastActivity extends AppCompatActivity {

    private ImageView mContrastImage;

    private int getActionBarHeight() { // Return height of the action bar (in pixel)

        TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true))
            return TypedValue.complexToDimensionPixelSize(typedValue.data,
                    getResources().getDisplayMetrics());

        Logs.add(Logs.Type.W, "'android.R.attr.actionBarSize' attribute not found");
        return 0;
    }

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

        final ActionBar appBar = getSupportActionBar();
        if (appBar != null)
            appBar.setDisplayHomeAsUpEnabled(true);









        final ViewStub stub = (ViewStub) findViewById(R.id.layout_container);
        stub.setLayoutResource(R.layout.contrast_landscape);
        final View root = stub.inflate();


        ///////////////// Landscape


        final Point screenSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(screenSize);
        int screenWidth = screenSize.x;
        int screenHeight = screenSize.y - getActionBarHeight();



        // Get control panel height at the screen bottom (in pixel)

        final ImageView icon = (ImageView)root.findViewById(R.id.brightness_icon);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)icon.getLayoutParams();
        screenHeight -= params.height;




        mContrastImage = (ImageView)root.findViewById(R.id.image_contrast);
        params = (LinearLayout.LayoutParams)mContrastImage.getLayoutParams();
        params.width = screenWidth >> 1;
        params.height = (int)(params.width * 128 / (float)75);
        if (params.height < screenHeight) {

            params.height = screenHeight;
            params.width = (int)(75 * screenHeight / (float)128);

            // Shift both images in order to move them in the middle of the screen (horizontally)
            // -> This is needed coz the 'LinearLayout' allows child images to overstep its bounds
            //    vertically but not horizontally. If not shift the other image will be partially
            //    visible.

            params.setMargins((screenWidth - (params.width << 1)) / 2, 0, 0, 0);
            // ...note the left margin above has a negative value to shift images on the left
        }
        mContrastImage.setLayoutParams(params);

        final ImageView compareImage = (ImageView)root.findViewById(R.id.image_compare);
        params = (LinearLayout.LayoutParams)compareImage.getLayoutParams();
        params.width = screenWidth >> 1;
        params.height = (int)(params.width * 128 / (float)75);
        if (params.height < screenHeight) {

            params.height = screenHeight;
            params.width = (int)(75 * screenHeight / (float)128);
        }
        compareImage.setLayoutParams(params);






        // Load images






    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);




    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {






        super.onSaveInstanceState(outState);
    }
}

package cz.vutbr.fit.mogger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import cz.vutbr.fit.mogger.R;
import cz.vutbr.fit.mogger.gesture.GestureManager;

import java.io.File;

public class SettingsDetailActivity extends Activity {
    FileDialog fileDialog = null;
    File mPath = null;

    EditText name = null;
    SeekBar threshold = null;
    ImageButton openFile = null;
    TextView fileName = null;
    ImageButton addGesture = null;
    TextView gestureOk = null;
    ImageButton save = null;
    ImageButton delete = null;

    // plna cesta
    String fullPath;

    // pozice gest v seznamu
    int position = 0;
    Gesture g = null;


    // jedna se o prvni pridani?
    boolean isFirst = false;

    // min max pro threshold
    final int MIN = 30;
    final int MAX = 200;

    // flag na detekci umeleho ulozeni
    boolean manualSave = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settingsdetailactivity);

        mPath = new File(Environment.getExternalStorageDirectory() + "//DIR//");
        fileDialog = new FileDialog(this, mPath);
        fileDialog.setFileEndsWith(".mp3");
        fileDialog.setFileEndsWith2(".wav");
        fileDialog.addFileListener(new cz.vutbr.fit.mogger.FileDialog.FileSelectedListener() {
            public void fileSelected(File file) {

                fullPath = file.toString();
                fileName.setText(getFileNameOnly(fullPath));
                //Log.d(getClass().getName(), "selected file " + file.toString());
            }
        });

        name = (EditText) findViewById(R.id.editText);
        threshold = (SeekBar) findViewById(R.id.seekBar);
        openFile = (ImageButton) findViewById(R.id.imageButton);
        openFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //fileDialog
                fileDialog.showDialog();
            }
        });
        fileName = (TextView) findViewById(R.id.textView5);
        fileName.setText("Empty");
        addGesture = (ImageButton) findViewById(R.id.imageButton2);
        gestureOk = (TextView) findViewById(R.id.textView7);
        gestureOk.setText("Empty");
        save = (ImageButton) findViewById(R.id.imageButton5);
        delete = (ImageButton) findViewById(R.id.imageButton4);


        // najdi gesto z pozice v poli gest
        position = (int) getIntent().getExtras().getInt("gesture");


        // posunuti intervalu do <min,max>
        threshold.setMax(MAX-MIN);

        if (position >= 0) {
            g = GestureManager.createInstance(getApplicationContext()).getGestures().get(position);
            if (g != null) {
                // vypis do GUI
                name.setText(g.name);
                String name = getFileNameOnly(g.fileSound);
                if(name == "")
                {
                    fileName.setText("Empty");
                }
                else {
                    fileName.setText(name);
                    fullPath = g.fileSound;
                }
                if(g.size() > 0)
                {
                    gestureOk.setText("OK");
                }


                threshold.setProgress(g.getThreshold()-MIN);
            }//if
        }//if

        // nastaveni labelu zobrazujici hodnotu threshold
        final TextView seekBarValue = (TextView)findViewById(R.id.edtSeekBar);
        if (g != null) seekBarValue.setText(String.valueOf(g.getThreshold()));
        threshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                seekBarValue.setText(String.valueOf(progress + MIN));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // nelze mazat, pridavame-li nove gesto
        if (g == null) delete.setVisibility(View.INVISIBLE);

        // pridani gesta
        addGesture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // pokud je gesto nove - index je mimo
                // udelame prvni udelame umely save
                if (position == -1) {
                    manualSave = true; // jelikoz to neumime predat v evente...
                    save.performClick();
                    manualSave = false;
                    // pozice je "ta posledni"
                    GestureManager manager = GestureManager.createInstance(getApplicationContext());
                    position = manager.getGestures().size() - 1;

                    //prvni nahravani gesta
                    isFirst = true;
                }

                Log.d("SettingsDetailActivity", "Position: " + position);

                // zobrazeni detailu polozky
                Intent myIntent = new Intent(SettingsDetailActivity.this, RecordActivity.class);
                // data do aktivity
                myIntent.putExtra("gesture", position);
                startActivity(myIntent);
            }
        });

        // ulozeni gesta
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GestureManager manager = GestureManager.createInstance(getApplicationContext());

                if (g == null) {
                    g = new Gesture();
                }
                g.name = name.getText().toString();
                g.fileSound = fullPath;

                Log.d("SettingsDetailActivity", "Calc threshold: " + g.getThreshold());

                g.setThreshold(threshold.getProgress() + MIN);

                manager.saveGesture(g);

                // zobrazeni textu uziv.
                Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_LONG).show();

                if (!manualSave) {
                    // ukonceni aktivity
                    finish();
                }
            }
        });


        // mazani gesta
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GestureManager manager = GestureManager.createInstance(getApplicationContext());

                if (g != null) {
                    manager.removeGesture(g);

                    // zobrazeni textu uziv.
                    Toast.makeText(getApplicationContext(), "Deleted", Toast.LENGTH_LONG).show();

                    // ukonceni aktivity
                    finish();
                }//if null
            }

        });

    }

    @Override
    public void onResume() {
        super.onResume();
        if(g != null && g.size() > 0) {
            gestureOk.setText(Integer.toString(g.size()));

            // prvni nahravani gesta?
            if (isFirst)
            {
                int v = g.calculateThreshold();
                Log.d("SettingsDetailActivity", "calculateThreshold(): " + v);
                // dosadime spocitanou hodnotu
                g.setThreshold(v);
                threshold.setProgress(g.getThreshold()-MIN);
                isFirst = false;
            }
        }
    }

    /**
     * Vraci nazev souboru z plne cesty
     *
     * @param fullPath Cesta cesta souboru
     */
    private String getFileNameOnly(String fullPath) {

        String fileOnlyName = "";
        if (fullPath != null) {
            int index = fullPath.lastIndexOf('/');

            if (index > 0 && index < fullPath.length()) {
                fileOnlyName = fullPath.substring(index + 1, fullPath.length());
                fileName.setText(fileOnlyName);
            }
        }//if
        return fileOnlyName;
    }
}
package edu.estgp.njoy;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import edu.estgp.njoy.njoy.R;

public class SettingsActivity extends Activity {

    private static final int JOYSTICK_MODE_SCAN_NEXT = 0;
    private static final int TIMEOUT_MOVE = 1000;

    private static final int BUTTON_A_MODE = 0;
    private static final int BUTTON_B_MODE = 1;
    private static final int TIMEOUT_BUTTONS = 3000;

    private Spinner spinnerJoystick;
    private SeekBar seekBarMove;

    private Spinner spinnerButtonModeA;
    private Spinner spinnerButtonModeB;
    private SeekBar seekBarButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setTitle(R.string.title_activity_settings);

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("njoy", Context.MODE_PRIVATE);
        int joystickMode = sharedPref.getInt("joystick_mode", JOYSTICK_MODE_SCAN_NEXT);
        int timeoutMove = sharedPref.getInt("timeout_move", TIMEOUT_MOVE);

        int buttonA = sharedPref.getInt("button_A_mode", BUTTON_A_MODE);
        int buttonB = sharedPref.getInt("button_B_mode", BUTTON_B_MODE);
        int timeoutButtons = sharedPref.getInt("timeout_buttons", TIMEOUT_BUTTONS);

        final int start = 100;
        final int step = 100;

        final TextView textViewTimeoutMove = (TextView)findViewById(R.id.textViewTimeoutMove);
        textViewTimeoutMove.setTag(textViewTimeoutMove.getText());
        seekBarMove = (SeekBar)findViewById(R.id.seekBarMove);
        seekBarMove.setProgress(timeoutMove);
        seekBarMove.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                int discrete = 100 * (progress / 100);
                textViewTimeoutMove.setText(textViewTimeoutMove.getTag().toString() + " (" + discrete + " ms)");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView textViewTimeoutButtons = (TextView)findViewById(R.id.textViewTimeoutButtons);
        textViewTimeoutButtons.setTag(textViewTimeoutButtons.getText());
        seekBarButtons = (SeekBar)findViewById(R.id.seekBarButtons);
        seekBarButtons.setProgress(timeoutButtons);

        seekBarButtons.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int discrete = 100* (progress / 100);
                textViewTimeoutButtons.setText(textViewTimeoutButtons.getTag().toString() + " (" + discrete + " ms)");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        textViewTimeoutMove.setText(textViewTimeoutMove.getTag().toString() + " (" + timeoutMove + " ms)");
        textViewTimeoutButtons.setText(textViewTimeoutButtons.getTag().toString() + " (" + timeoutButtons + " ms)");

        spinnerJoystick = (Spinner) findViewById(R.id.spinnerJoystick);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.joystick_modes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerJoystick.setAdapter(adapter);
        spinnerJoystick.setSelection(joystickMode);

        spinnerButtonModeA = (Spinner) findViewById(R.id.spinnerButtonA);
        adapter = ArrayAdapter.createFromResource(this,
                R.array.button_mode, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerButtonModeA.setAdapter(adapter);
        spinnerButtonModeA.setSelection(buttonA);

        spinnerButtonModeB = (Spinner) findViewById(R.id.spinnerButtonB);
        adapter = ArrayAdapter.createFromResource(this,
                R.array.button_mode, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerButtonModeB.setAdapter(adapter);
        spinnerButtonModeB.setSelection(buttonB);
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("njoy", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putInt("joystick_mode", spinnerJoystick.getSelectedItemPosition());
        editor.putInt("timeout_move", 100 * (seekBarMove.getProgress() / 100));

        editor.putInt("button_A_mode", spinnerButtonModeA.getSelectedItemPosition());
        editor.putInt("button_B_mode", spinnerButtonModeB.getSelectedItemPosition());
        editor.putInt("timeout_buttons", 100 * (seekBarButtons.getProgress() / 100));

        editor.commit();
    }
}

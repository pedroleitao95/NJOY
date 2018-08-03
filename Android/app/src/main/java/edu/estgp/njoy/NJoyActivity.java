package edu.estgp.njoy;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.estgp.app.Option;
import edu.estgp.app.Screen;
import edu.estgp.ble.BluetoothConnectionState;
import edu.estgp.ble.BluetoothLeScanActivity;
import edu.estgp.ble.BlunoActivity;
import edu.estgp.contact.NJoyContact;
import edu.estgp.njoy.njoy.R;
import edu.estgp.pilight.DeviceStatusInterface;
import edu.estgp.pilight.Pilight;
import edu.estgp.sms.NJoySMS;
import edu.estgp.sms.SMS;

public class NJoyActivity extends BlunoActivity implements DeviceStatusInterface {
    private ListView listView;
    private GridView gridView;
    private GridAdapter gridViewAdapter;
    private TextView textViewTitle;
    private ImageButton imageButtonState;
    private TextView textViewState;

    private static int currentScreen = 0;
    private static Stack<Integer> stackOfScreens = new Stack<Integer>();

    private static final int JOYSTICK_MODE_SCAN_NEXT = 0;
    private static final int JOYSTICK_MODE_SCAN_PREVIOUS = 1;
    private static final int JOYSTICK_MODE_LEFT_RIGHT = 2;
    private static final int JOYSTICK_MODE_FULL_DIRECTIONS = 3;

    private static final int TIMEOUT_MOVE = 2000;

    private static final int BUTTON_A_MODE = 0;
    private static final int BUTTON_B_MODE = 1;
    private static final int TIMEOUT_BUTTONS = 3000;

    private int joystickMode = JOYSTICK_MODE_LEFT_RIGHT;
    private int timeoutMove = TIMEOUT_MOVE;

    int buttonA = BUTTON_A_MODE;
    int buttonB = BUTTON_B_MODE;
    private int timeoutButtons = TIMEOUT_BUTTONS;


    private boolean connecting = true;

    private Handler mHandler = new Handler();

    private String dataBuffer = "";
    private Pattern pattern = Pattern.compile("(\\d{6})\\r\\n");
    private long previousJoystickMovement = 0;
    private long previousButtonClick = 0;


    // Screens...
    private Screen[] screens = new Screen[] {
        new Screen("Início", new Option[]{
            new Option(R.drawable.filme2, "Animação", "screen: 1"),
            new Option(R.drawable.casa, "Casa", "screen: 2"),
            new Option(R.drawable.jogos, "Jogos", "screen: 3"),
            new Option(R.drawable.escola, "Escola", "screen: 4"),
            new Option(R.drawable.escrever, "Mensagens", "screen: 8"),
            new Option(R.drawable.escrever, "NJOY Store", "apps"),
        }),

        new Screen("Animação", new Option[]{
            new Option(R.drawable.filme2, "Documentários", "screen: 7"),
            new Option(R.drawable.musica, "Canções", "screen: 6"),
            new Option(R.drawable.filme, "Filmes", ""),
        }),

        new Screen("Casa", new Option[]{
            new Option(R.drawable.tv, "televisão", "domotic: toogle tv"),
            new Option(R.drawable.luz, "luz", "domotic: toogle light"),
            new Option(R.drawable.radio, "rádio", "domotic: toogle hvac"),
            /*new Option(R.drawable.ar_condicionado, "Ar condicionado", "domotic: toogle hvac"), */
        }),

        new Screen("Jogos", new Option[]{
            new Option(R.drawable.boccia, "Bocia", ""),
            new Option(R.drawable.jogo_outro, "Outros", ""),
        }),

        new Screen("Escola", new Option[]{
            new Option(R.drawable.ler, "Leitura", ""),
            new Option(R.drawable.escrever, "Escrita", ""),
            new Option(R.drawable.matematica, "Matemática", ""),
            new Option(R.drawable.animais, "Animais", "screen: 5"),
        }),

        new Screen("Animais", new Option[]{
            new Option(R.drawable.gnu, "Gnu", "talk: gnu"),
            new Option(R.drawable.pomba, "Pomba", "talk: pomba"),
            new Option(R.drawable.pato, "Pato", "talk: pato"),
            new Option(R.drawable.coiote, "Lobo", "talk: lobo"),
            new Option(R.drawable.pintassilgo, "Pintassilgo", "talk: pintassilgo"),
        }),

        new Screen("Canções", new Option[]{
            new Option(R.drawable.agua, "Água, a nossa amiga", "youtube: XiQudQyl78M"),
            new Option(R.drawable.primavera, "Canção da Primavera", "youtube: RVrbwDmuwJk"),
        }),

        new Screen("Documentários", new Option[]{
            new Option(R.drawable.bbc, "Que mundo maravilhoso", "youtube: B8WHKRzkCOY"),
            new Option(R.drawable.home, "Planeta terra", "youtube: Wa546EesVPE"),
            new Option(R.drawable.serengeti, "Vida selvagem", "youtube: 5bHqG4ikRB8"),
        }),

        new Screen("Mensagens escritas", new Option[]{
            new Option(R.drawable.read_sms, "Mensagens novas", "sms: read"),
            new Option(R.drawable.mae, "Olá mãe, quando chegas?", "sms: 960187060: Olá mãe, quando chegas?"),
            new Option(R.drawable.pai, "SOS", "sms: 960187060: Preciso de ajuda"),
        }),
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("njoy", Context.MODE_PRIVATE);
        joystickMode = sharedPref.getInt("joystick_mode", JOYSTICK_MODE_SCAN_NEXT);
        timeoutMove = sharedPref.getInt("timeout_move", TIMEOUT_MOVE);
        buttonA = sharedPref.getInt("button_A_mode", BUTTON_A_MODE);
        buttonB = sharedPref.getInt("button_B_mode", BUTTON_B_MODE);
        timeoutButtons = sharedPref.getInt("timeout_buttons", TIMEOUT_BUTTONS);

        View rootView = findViewById(android.R.id.content);
        rootView.setKeepScreenOn(true);

        ImageView imageViewLogo = (ImageView)findViewById(R.id.imageViewLogo);
        imageViewLogo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent(NJoyActivity.this, SettingsActivity.class);
                startActivity(intent);
                return false;
            }
        });

        textViewTitle = (TextView) findViewById(R.id.textViewTitle);
        textViewState = (TextView) findViewById(R.id.textViewState);
        imageButtonState = (ImageButton) findViewById(R.id.imageButtonState);
        imageButtonState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NJoyActivity.this, BluetoothLeScanActivity.class);
                startActivity(intent);
            }
        });

        gridView = (GridView)findViewById(R.id.gridview);
        setScreen(currentScreen);

        listView = (ListView)findViewById(R.id.listviewSMS);

        // connect to pilight
        Pilight.connect(this);
    }

    @Override
    public void onBackPressed() {
        if (stackOfScreens.isEmpty())
            ; //finish();
        else {
            if (listView.getVisibility() == View.VISIBLE) {
                listView.setVisibility(View.GONE);
                gridView.setVisibility(View.VISIBLE);
                Screen screen = screens[currentScreen];
                textViewTitle.setText(screen.getTitle());
            }
            else {
                int idx = stackOfScreens.pop();

                Screen screen = screens[idx];
                textViewTitle.setText(screen.getTitle());
                gridViewAdapter = new GridAdapter(this, screen);
                gridView.setAdapter(gridViewAdapter);
                gridViewAdapter.setSelection(0);
                TTS.speak(this, screen.getTitle());

                currentScreen = idx;
            }
        }
    }

    /* pilight domotic events */
    @Override
    public void onInitialDeviceStatus(final String device, final boolean state) {
        Log.d("NJOY", "onInitialDeviceStatus " + device + ", " + state);
    }

    @Override
    public void onDeviceStatusChanged(final String device, final boolean state) {
        Log.d("NJOY", "onDeviceStatusChanged " + device + ", " + state);

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                String tts = "";
                switch (device) {
                    case "tv":
                        tts = "Televisão " + (state ? "ligada" : "desligada");
                        break;
                    case "hvac":
                        tts = "Rádio " + (state ? "ligado" : "desligado");
                        break;
                    case "light":
                        tts = "Luz " + (state ? "ligada" : "desligada");
                        break;
                    case "xxx":
                        tts = "Ar condicionado " + (state ? "ligado" : "desligado");
                        break;
                }

                TTS.speak(NJoyActivity.this, tts);

                setDeviceText(gridViewAdapter.getViewSelected(), state);
            }
        });
    }

    private void setDeviceText(View view, Boolean state) {
        if (view != null) {
            TextView textView = (TextView)view.findViewById(R.id.textViewText);

            if (textView != null) {
                String text = textView.getText().toString();
                text = text.replace("Desligar\n", "");
                text = text.replace("Ligar\n", "");

                textView.setText((state ? "Desligar\n" : "Ligar\n") + text);
            }
        }
    }


    // joystick events
    @Override
    public void onBluetoothPermissionsResult(boolean allGranted) {
        if (!allGranted) textViewState.setText("Ligue o GPS");
    }

    @Override
    public void onConectionStateChange(BluetoothConnectionState connectionState) {
        switch (connectionState) {
            case CONNECTED:
                Log.d("NJOY", "Ligado ao joystick NJOY");
                imageButtonState.setImageResource(R.drawable.leddarkblue);
                textViewState.setText("Ligado");

                connecting = false;
                break;
            case CONNECTING:
                Log.d("NJOY", "A ligar ao joystick NJOY...");
                imageButtonState.setImageResource(R.drawable.ledlightblue);
                textViewState.setText("A ligar ao joystick...");

                connecting = true;
                break;
            case SCAN:
                Log.d("NJOY", "Procurar joystick NJOY");
                imageButtonState.setImageResource(R.drawable.ledlightorange);
                textViewState.setText("Procurar joystick");

                connecting = true;
                break;
            case SCANNING:
                Log.d("NJOY", "A procurar o joystick NJOY...");
                imageButtonState.setImageResource(R.drawable.ledorange);
                textViewState.setText("A procurar joystick...");

                connecting = true;
                break;
            case DISCONNECTING:
                Log.d("NJOY", "A desligar do joystick NJOY");
                imageButtonState.setImageResource(R.drawable.ledgray);
                textViewState.setText("Desligado");
                break;
            default:
                break;
        }
    }

    @Override
    public void onSerialReceived(String theString) {
        Log.d("NJOY", "onSerialReceived: " + theString);

        dataBuffer += theString;
        Matcher matcher = pattern.matcher(dataBuffer);

        while (matcher.find()) {
            String found = matcher.group(1);

            Log.d("NJOY", "onSerialReceived process: " + found);
            byte data[] = found.getBytes();

            boolean joystick = data[0] == '1' || data[1] == '1' || data[2] == '1' || data[3] == '1';
            boolean bothButtons = data[4] == '1' && data[5] == '1';

            if (!TTS.isSpeaking()) {
                if (joystick && (System.currentTimeMillis() - previousJoystickMovement) > timeoutMove) {

                    if (joystickMode == JOYSTICK_MODE_SCAN_NEXT) {
                        next();
                    } else if (joystickMode == JOYSTICK_MODE_SCAN_PREVIOUS) {
                        previous();
                    } else if (joystickMode == JOYSTICK_MODE_LEFT_RIGHT) {
                        if (data[0] == '1' || data[1] == '1') {
                            next();
                        } else {
                            previous();
                        }
                    } else { // if (joystickMode == JOYSTICK_MODE_FULL_DIRECTIONS)
                        next();
                    }
                }
                else if (!bothButtons && (System.currentTimeMillis() - previousButtonClick) > timeoutButtons) {
                    if (data[5] == '1') {   // button A
                        Log.d("NJOY", "Button A");
                        if (buttonA == 0 && listView.getVisibility() == View.GONE)
                            gridViewAdapter.executeSelectedOption();
                        else
                            onBackPressed();
                    }
                    if (data[4] == '1') {   // button B
                        Log.d("NJOY", "Button B");
                        if (buttonB == 0 && listView.getVisibility() == View.GONE)
                            gridViewAdapter.executeSelectedOption();
                        else
                            onBackPressed();
                    }

                    previousButtonClick = System.currentTimeMillis();
                }
            }

            if (dataBuffer.length() > 8) {
                dataBuffer = dataBuffer.substring(8, dataBuffer.length());
            }
            else
                dataBuffer = "";
        }
    }

    public void setScreen(int idx) {
        Screen screen = screens[idx];
        textViewTitle.setText(screen.getTitle());
        gridViewAdapter = new GridAdapter(this, screen);
        gridView.setAdapter(gridViewAdapter);
        gridViewAdapter.setSelection(0);

        if (idx != 0 && idx != currentScreen) stackOfScreens.push(currentScreen);
        currentScreen = idx;

        TTS.speak(this, screen.getTitle());
    }

    private ArrayList<String> valuesSMS = new ArrayList<String>();
    public void readSms() {
        List<SMS> list = NJoySMS.getUnreadMessages(this);

        valuesSMS.clear();
        for (SMS sms: list) {
            valuesSMS.add(String.format(getString(R.string.sms_received),
                    NJoyContact.findContactByPhone(this, sms.getAddress()), sms.getMsg()));
        }

        if (valuesSMS.size() == 0) {
            TTS.speak(this, getString(R.string.no_sms));
            return;
        }
        gridView.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);

        textViewTitle.setText(R.string.sms_new);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_activated_1, android.R.id.text1, valuesSMS);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TTS.speak(NJoyActivity.this, valuesSMS.get(listView.getCheckedItemPosition()));
            }
        });

        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setItemChecked(-1, true);
    }

    private void next() {
        if (listView.getVisibility() == View.VISIBLE) {
            int size = listView.getCount();
            int position = listView.getCheckedItemPosition() + 1;
            if (position >= size) position = 0;

            listView.setItemChecked(position, true);
            TTS.speak(this, valuesSMS.get(position));

            // set message read
            NJoySMS.markMessageReadByPosition(this, position);
        }
        else {
            int size = gridViewAdapter.getCount();
            int position = gridViewAdapter.getSelection() + 1;
            if (position >= size) position = 0;

            gridViewAdapter.setSelection(position);
        }

        previousJoystickMovement = System.currentTimeMillis();
    }

    private void previous() {
        if (listView.getVisibility() == View.VISIBLE) {
            int size = listView.getCount();
            int position = listView.getCheckedItemPosition() - 1;
            if (position < 0) position = size - 1;

            listView.setItemChecked(position, true);
            TTS.speak(this, valuesSMS.get(position));

            // set message read
            NJoySMS.markMessageReadByPosition(this, position);
        }
        else {
            int size = gridViewAdapter.getCount();
            int position = gridViewAdapter.getSelection() - 1;
            if (position < 0) position = size - 1;

            gridViewAdapter.setSelection(position);
        }
        previousJoystickMovement = System.currentTimeMillis();
    }
}

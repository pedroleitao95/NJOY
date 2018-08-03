package edu.estgp.njoy;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import edu.estgp.app.Option;
import edu.estgp.app.Screen;
import edu.estgp.njoy.njoy.R;
import edu.estgp.pilight.Pilight;
import edu.estgp.sms.NJoySMS;
import edu.estgp.sms.SMS;

/**
 * Created by vrealinho on 08/03/18.
 */

public class GridAdapter extends BaseAdapter {
    private NJoyActivity mainActivity;
    private Screen screen;

    private View viewSelected = null;
    private int selection = 0;
    private ImageButton selectedButton = null;


    public GridAdapter(NJoyActivity mainActivity, Screen screen) {
        this.mainActivity = mainActivity;
        this.screen = screen;
        for (Option option : this.screen.getOptions()) {
            option.setView(null);
            if (option.getAction().startsWith("sms: read")) {
                List<SMS> listSMS = NJoySMS.getUnreadMessages(mainActivity);
                if (listSMS.size() == 0) {
                    option.setText(mainActivity.getString(R.string.no_sms));
                    option.setAction("");
                }
                else {
                    option.setText(String.format(mainActivity.getString(R.string.sms_number), listSMS.size()));
                }
            }
        }
    }

    public int getCount() {
        return screen.getOptions().length;
    }

    public Object getItem(int position) {
        return screen.getOptions()[position];
    }

    public long getItemId(int position) {
        return position;
    }

    public Option getOption(final int position) {
        return screen.getOptions()[position];
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        final Option option = screen.getOptions()[position];
        ViewHolder viewHolder = null;

        // view holder pattern
        if (convertView == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(mainActivity);
            convertView = layoutInflater.inflate(R.layout.grid_element, null);

            final ImageButton imageButton = (ImageButton) convertView.findViewById(R.id.imageButton);
            final TextView textView = (TextView) convertView.findViewById(R.id.textViewText);

            viewHolder = new ViewHolder(textView, imageButton);
            convertView.setTag(viewHolder);

            if (option.getView() == null) {
                option.setView(convertView);
                option.setImageButton(viewHolder.imageButton);
                option.setTextView(viewHolder.textView);
            }
        }
        else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        if (position == selection) {
            selectedButton = option.getImageButton();
            selectedButton.setSelected(true);
        }

        viewHolder.imageButton.setImageResource(screen.getOptions()[position].getImage());
        viewHolder.imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selection = position;
                setSelection(selection);
                executeOption(option);

                return;
            }
        });

        if (option.getAction().startsWith("domotic:")) {
            String[] s = option.getAction().split(" ");
            String device = s[2].trim();

            Boolean state = Pilight.devices.get(device);
            if (state != null)
                viewHolder.textView.setText((state ? "Desligar\n" : "Ligar\n") + option.getText());
            else
                viewHolder.textView.setText(option.getText());
        }
        else
            viewHolder.textView.setText(option.getText());

        return convertView;
    }

    public View getViewSelected() {
        return viewSelected;
    }

    public int getSelection() {
        return selection;
    }

    public void setSelection(int position) {

        if (position >= getCount())
            position = 0;
        else if (position < 0)
            position = getCount() - 1;

        if (selectedButton != null) {
            selectedButton.setSelected(false);
        }

        Option option = getOption(position);

        selectedButton = option.getImageButton();
        selection = position;

        if (selectedButton != null) {
            selectedButton.setSelected(true);
        }

        TTS.speak(mainActivity, option.getText());
    }

    private void executeOption(Option option) {
        if (option.getAction().startsWith("talk:")) {
            String[] s = option.getAction().split(":");
            String talk = s[1].trim();
            TTS.speak(mainActivity, talk);
        }
        else if (option.getAction().startsWith("screen: ")) {
            String[] s = option.getAction().split(":");
            int screen = Integer.parseInt(s[1].trim());

            mainActivity.setScreen(screen);
        }
        else if (option.getAction().startsWith("domotic: ")) {
            String[] s = option.getAction().split(" ");
            String device = s[2].trim();

            Pilight.toogleDevice(device);
        }
        else if (option.getAction().startsWith("youtube:")) {
            String[] s = option.getAction().split(":");
            String id = s[1].trim();

            // INÍCIO: para a FUTURÁLIA
            if (id.equals("XiQudQyl78M")) {
                Intent intent = new Intent(mainActivity, VideoActivity.class);
                intent.putExtra("VIDEO", R.raw.agua);
                mainActivity.startActivity(intent);
            }
            else if (id.equals("RVrbwDmuwJk")) {
                Intent intent = new Intent(mainActivity, VideoActivity.class);
                intent.putExtra("VIDEO", R.raw.primavera);
                mainActivity.startActivity(intent);
            }
            else if (id.equals("B8WHKRzkCOY")) {
                Intent intent = new Intent(mainActivity, VideoActivity.class);
                intent.putExtra("VIDEO", R.raw.bbc);
                mainActivity.startActivity(intent);
            }
            // FIM: para a FUTURÁLIA
            else {
                Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id));
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + id));
                try {
                    mainActivity.startActivity(appIntent);
                } catch (ActivityNotFoundException ex) {
                    mainActivity.startActivity(webIntent);
                }
            }
        }
        else if (option.getAction().startsWith("sms:")) {
            String[] s = option.getAction().split(":");
            String dest = s[1].trim();

            if (dest.equals("read")) {
                mainActivity.readSms();
            }
            else {
                NJoySMS.sendSMS(mainActivity, dest, s[2].trim());
            }
        }
        else {
            TTS.speak(mainActivity, "Opção " + option.getText() + " não configurada");
        }
    }

    private void executeOption(int position) {
        if (position >= 0 && position < getCount())
            executeOption(screen.getOptions()[position]);
    }

    public void executeSelectedOption() {
        executeOption(selection);
    }

    private class ViewHolder {
        private final TextView textView;
        private final ImageButton imageButton;

        public ViewHolder(TextView textView, ImageButton imageButton) {
            this.textView = textView;
            this.imageButton = imageButton;
        }
    }
}
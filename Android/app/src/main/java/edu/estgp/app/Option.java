package edu.estgp.app;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * Created by vrealinho on 08/03/18.
 */

public class Option {
    private int image;
    private String text;
    private String action;
    private View view = null;
    private ImageButton imageButton = null;
    private TextView textView = null;

    public Option(int image, String text, String action) {
        this.image = image;
        this.text = text;
        this.action = action;
    }

    public int getImage() {
        return image;
    }

    public void setImage(int image) {
        this.image = image;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;
    }

    public ImageButton getImageButton() {
        return imageButton;
    }

    public void setImageButton(ImageButton imageButton) {
        this.imageButton = imageButton;
    }

    public TextView getTextView() {
        return textView;
    }

    public void setTextView(TextView textView) {
        this.textView = textView;
    }
}

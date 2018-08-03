package edu.estgp.app;

/**
 * Created by vrealinho on 08/03/18.
 */

public class Screen {
    private String title;
    private Option[] options;

    public Screen(String title, Option[] options) {
        this.title = title;
        this.options = options;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Option[] getOptions() {
        return options;
    }

    public void setOptions(Option[] options) {
        this.options = options;
    }
}

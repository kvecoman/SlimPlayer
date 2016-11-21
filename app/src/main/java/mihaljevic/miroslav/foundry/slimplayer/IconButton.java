package mihaljevic.miroslav.foundry.slimplayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.Button;

/**
 * Created by Miroslav on 21.11.2016..
 */
public class IconButton extends Button {

    public IconButton(Context context) {
        super(context);
        updateTypeface();
    }

    public IconButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        updateTypeface();
    }

    public IconButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        updateTypeface();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public IconButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        updateTypeface();
    }

    //TODO - continue here - layout not working, create test activity with notification layout and icons to see if it works

    public void updateTypeface()
    {
        Typeface typeface = Typeface.createFromAsset(getContext().getAssets(),"icons");
        setTypeface(typeface);
    }
}

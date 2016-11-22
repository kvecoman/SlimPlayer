package mihaljevic.miroslav.foundry.slimplayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.Button;

/**
 * Button that displays font icon
 *
 * @author Miroslav Mihaljević
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


    public void updateTypeface()
    {
        Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/icons.ttf");
        setTypeface(typeface);
    }
}

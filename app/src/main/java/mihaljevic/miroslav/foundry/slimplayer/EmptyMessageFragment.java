package mihaljevic.miroslav.foundry.slimplayer;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A fragment used to display an empty message
 *
 * @author Miroslav Mihaljević
 */
public class EmptyMessageFragment extends Fragment
{

    public static final String MESSAGE_KEY = "message_key";

    private View mView;


    public EmptyMessageFragment()
    {
        // Required empty public constructor
    }


    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState )
    {
        mView = inflater.inflate( R.layout.empty_page, container, false );
        return mView;
    }

    @Override
    public void onActivityCreated( @Nullable Bundle savedInstanceState )
    {
        super.onActivityCreated( savedInstanceState );

        Bundle      args;
        TextView    textView;

        args = getArguments();


        textView = ( ( TextView ) mView.findViewById( R.id.empty_text ) );

        //Set empty message if one was provided
        if ( args != null && args.containsKey( MESSAGE_KEY ) )
        {
            textView.setText( args.getString( MESSAGE_KEY ) );
        }

        //Check if host activity is implementing click listener for text view and if it does, hook to it
        if ( getContext() instanceof TextView.OnClickListener )
        {
            textView.setOnClickListener( ( ( View.OnClickListener ) getContext() ) );
        }

    }
}

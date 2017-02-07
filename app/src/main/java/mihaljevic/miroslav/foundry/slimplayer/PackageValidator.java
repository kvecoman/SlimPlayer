package mihaljevic.miroslav.foundry.slimplayer;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.os.Process;
import android.util.Base64;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by miroslav on 05.02.17..
 *
 * Inspired by package validator from MediaBrowserService sample
 */



public class PackageValidator
{
    protected final String TAG = getClass().getSimpleName();

    private final Map<String, PackageId> mCertificates;

    private static PackageValidator sInstance;

    public static PackageValidator getInstance()
    {
        if (sInstance == null)
            sInstance = new PackageValidator();

        return sInstance;
    }

    private PackageValidator()
    {
        mCertificates = new HashMap<>(  );

        readCertificates();

    }


    private void readCertificates()
    {
        XmlResourceParser xmlParser;
        SlimPlayerApplication app;
        int state;
        String name;
        String packageName;
        String certificate;
        PackageId packageId;

        app = SlimPlayerApplication.getInstance();
        xmlParser = app.getResources().getXml( R.xml.allowed_media_browser_callers );

        try
        {
            state = xmlParser.next();

            while (state != XmlResourceParser.END_DOCUMENT)
            {
                if (state == XmlResourceParser.START_TAG  && xmlParser.getName().equals( "signing_certificate" ))
                {
                    name =          xmlParser.getAttributeValue(null, "name");
                    packageName =   xmlParser.getAttributeValue( null, "package" );
                    certificate =   xmlParser.nextText().replaceAll("\\s|\\n", "");

                    packageId = new PackageId( name, packageName );

                    mCertificates.put( certificate, packageId );
                }

                state = xmlParser.next();
            }
        }
        catch (XmlPullParserException e)
        {
            e.printStackTrace();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
    }



    public boolean validate(String callingPackage, int callingUID)
    {

        PackageId storedPackageId;
        SlimPlayerApplication app;
        PackageManager packageManager;
        PackageInfo packageInfo;
        String callerCertificate;


        app = SlimPlayerApplication.getInstance();
        packageManager = app.getPackageManager();
        packageInfo = null;

        if ( Process.SYSTEM_UID == callingUID || Process.myUid() == callingUID)
            return true;

        try
        {
            packageInfo = packageManager.getPackageInfo( callingPackage, PackageManager.GET_SIGNATURES );
        }
        catch (PackageManager.NameNotFoundException e)
        {
            Log.e( TAG, "Package manager could not get package info for calling package" );
            e.printStackTrace();
        }


        if (packageInfo == null)
            return false;

        if (packageInfo.signatures.length != 1)
        {
            Log.w( TAG, "Caller package has incorrect number of signatures" );
            return false;
        }

        callerCertificate = Base64.encodeToString(packageInfo.signatures[0].toByteArray(), Base64.NO_WRAP);


        storedPackageId = mCertificates.get( callerCertificate );

        if (storedPackageId == null)
            return false;

        if (storedPackageId.packageName.equals( callingPackage ))
            return true;

        return false;
    }





    class PackageId
    {
        String name;
        String packageName;

        public PackageId( String name, String packageName )
        {
            this.name = name;
            this.packageName = packageName;
        }


    }
}

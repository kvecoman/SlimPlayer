package mihaljevic.miroslav.foundry.slimplayer;

/**
 * Created by Miroslav on 18.1.2017..
 */

public class DoubleKey {
    private static String nullStr = "-1";

    String mStr1;
    String mStr2;

    public DoubleKey(String str1, String str2)
    {
        mStr1 = str1 == null ? nullStr : str1;
        mStr2 = str2 == null ? nullStr : str2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DoubleKey doubleKey = (DoubleKey) o;

        if (!mStr1.equals(doubleKey.mStr1)) return false;
        return mStr2.equals(doubleKey.mStr2);

    }

    @Override
    public int hashCode() {

        //We use "-1" hash if string is null

        int result = mStr1.hashCode();
        result = 31 * result + mStr2.hashCode();
        return result;
    }
}

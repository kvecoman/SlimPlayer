package mihaljevic.miroslav.foundry.slimplayer;

import android.util.Log;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;

/**
 * Created by miroslav on 17.02.17..
 *
 * Container that has weak entries that get GC-ed
 *
 * @author Miroslav Mihaljević
 */


//TODO - use some other hash algorithm
public class WeakMap<K,V>
{
    private final String TAG = getClass().getSimpleName();

    public static final int DEFAULT_CAPACITY = 16;

    public static final float DEFAULT_LOAD_FACTOR = 0.75f;

    public static final int MAXIMUM_CAPACITY = 1 << 30;

    //Always power of two
    private int mCapacity;

    private float mLoadFactor;

    private int mTreshold;

    private int mSize = 0;

    private WeakEntry<K,V>[] mTable;

    private ReferenceQueue<V> mQueue;



    public WeakMap ()
    {
        this (DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR );
    }

    public WeakMap( int capacity )
    {
        this(capacity, DEFAULT_LOAD_FACTOR );
    }

    public WeakMap ( int capacity, float loadFactor )
    {
        if (capacity <= 0)
            capacity = DEFAULT_CAPACITY;

        if (capacity > MAXIMUM_CAPACITY)
            capacity = MAXIMUM_CAPACITY;

        if (loadFactor <= 0)
            loadFactor = DEFAULT_LOAD_FACTOR;

        mCapacity = 1;

        while (mCapacity < capacity)
            mCapacity <<= 1;

        mLoadFactor = loadFactor;

        mTreshold = (int)(mLoadFactor * mCapacity);

        mTable = newTable( mCapacity );

        mQueue = new ReferenceQueue<>();

    }


    private WeakEntry<K,V>[] newTable( int capacity )
    {
        if (capacity > MAXIMUM_CAPACITY)
            return (WeakEntry<K,V>[]) (new WeakEntry[MAXIMUM_CAPACITY]);

        return (WeakEntry<K,V>[]) (new WeakEntry[capacity]);
    }

    private void transfer( WeakEntry<K,V>[] oldTable, WeakEntry<K,V>[] newTable )
    {
        if (newTable.length < oldTable.length)
            throw new IllegalArgumentException( "New table is smaller than the old table" );


        int             newLength;
        WeakEntry<K,V>  entry;
        WeakEntry<K,V>  nextEntry;


        newLength = newTable.length;

        for (int i = 0; i < oldTable.length;i++)
        {
            entry = oldTable[i];

            while (entry != null)
            {
                nextEntry = entry.next;

                //newEntry = recalculateEntry( entry, newLength );
                entry.next  = null;
                entry.index = indexFor(entry.hash, newLength);

                putInTable( newTable,  entry);

                entry = nextEntry;

            }


        }

    }



    //Returns whether we have added a new entry
    private boolean putInTable( WeakEntry<K,V>[] table, WeakEntry<K,V> newEntry )
    {

        if ( newEntry == null )
            return false;


        WeakEntry<K,V> oldEntry;
        WeakEntry<K,V> nextEntry;


        newEntry.next = null;


        oldEntry = table[newEntry.index];

        if ( oldEntry == null )
        {
            //If the place in table is freejust put it here and be done
            table[newEntry.index] = newEntry;
        }
        else if ( oldEntry.key.equals( newEntry.key ) )
        {
            //Case if we have matching key right here in first place
            table[newEntry.index]   = newEntry;
            newEntry.next           = oldEntry.next;

            return false;
        }
        else
        {
            //Case where we have collision, we need to put our entry at end of linked list

            //Stop either when we are at end of chain or when we have matching keys
            while ( oldEntry.next != null && !oldEntry.next.key.equals( newEntry.key ) )
            {
                oldEntry = oldEntry.next;
            }

            nextEntry = oldEntry.next;

            //We have stopped for some reason and we just add entry at the end of chain
            oldEntry.next = newEntry;

            if (nextEntry != null && nextEntry.key.equals( newEntry.index ) )
            {
                //Case where we are replacing value because we have matching keys
                newEntry.next = nextEntry.next;
                return false;
            }

        }

        return true;
    }

    private synchronized void resize( int capacity )
    {
        WeakEntry<K,V>[] oldTable;
        WeakEntry<K,V>[] newTable;

        //Clear stale entries
        clearStaleEntries();

        if (capacity > MAXIMUM_CAPACITY)
        {
            mCapacity = MAXIMUM_CAPACITY;
            mTreshold = Integer.MAX_VALUE;
        }
        else
        {
            mCapacity = capacity;
            mTreshold = (int) (mLoadFactor * mCapacity);
        }

        oldTable = mTable;
        newTable = newTable( mCapacity );

        //Transfer entries and recalculate indexes into new table
        transfer( oldTable, newTable );

        //Set new table as our active table
        mTable = newTable;

    }

    public synchronized void put(K key, V value)
    {
        int hash;
        int index;
        boolean added;
        WeakEntry<K,V> newEntry;




        clearStaleEntries();

        hash    = key.hashCode() ;
        index   = indexFor(hash, mTable.length);

        newEntry = new WeakEntry<>( key, hash, value, mQueue, index );

        added = putInTable( mTable, newEntry );

        //If the entry is not replaced then increase mSize, otherwise not
        mSize += added ? 1 : 0;



        //If we have reached threshold we need to resize the table
        if (mSize >= mTreshold && mTreshold != Integer.MAX_VALUE)
            resize( mCapacity * 2 );

    }

    public synchronized V get(K key)
    {
        int             hash;
        int             index;
        WeakEntry<K,V>  entry;
        V               value;

        clearStaleEntries();

        hash    = key.hashCode();
        index   = indexFor( hash, mTable.length );
        entry   = mTable[index];

        //If place in table is empty just return null
        if ( entry == null )
            return null;

        //Iterate until we are at the end or until we find a matching key
        while ( entry.next != null && !entry.key.equals( key ) )
        {
            entry = entry.next;
        }

        //If the keys match, return that value
        if ( entry.key.equals( key ) )
        {
            value = entry.get();

            return value == null ? null : value;
        }

        Log.w(TAG, "Entry was found at calculated index, but we couldn't match the keys");

        //Something went wrong
        return null;
    }

    private synchronized void clearStaleEntries()
    {

        int             index;
        WeakEntry<K,V>  tableEntry;
        WeakEntry<K,V>  weakEntry;
        int             deletedCount;

        deletedCount = 0;

        //Iterate through all recently cleared references
        while ( ( weakEntry = (( WeakEntry<K,V> ) ( mQueue.poll() )) ) != null )
        {

            index = weakEntry.index;

            tableEntry = mTable[index];

            if ( tableEntry == null )
            {
                continue;
            }

            //Try to find entry that has value in its reference
            while ( tableEntry.next != null && tableEntry.get() == null )
            {
                tableEntry = tableEntry.next;
                deletedCount++;
            }

            if ( tableEntry.get() == null )
            {
                //If we didn't find entry that has value, just set it to null and we are done
                mTable[index] = null;
                deletedCount++;
            }
            else
            {
                //Since we have entry that has value, set it as first in our linked sub-list
                mTable[index] = tableEntry;

                //Iterate through other linked nodes trying to find entries without values
                while ( tableEntry != null && tableEntry.next != null )
                {
                    if ( tableEntry.next.get() == null )
                    {
                        //We found empty entry, now bridge over to the one after it (if it exist, if not it is null, and end of this)
                        tableEntry.next = tableEntry.next.next;
                        deletedCount++;
                    }

                    tableEntry = tableEntry.next;
                }
            }

        }


        mSize -= deletedCount;
    }

    public int size()
    {
        return mSize;
    }

    public void clear()
    {
        //Clear the table and set size to null
        Arrays.fill( mTable, null );
        mSize = 0;

        //Clear out all GC-ed objects
        while (mQueue.poll() != null);
    }




    private int indexFor(int hash, int length)
    {
        return hash & (length - 1);
    }




    private class WeakEntry<H,J> extends WeakReference<J>
    {
        H   key;
        int hash;
        int index;

        WeakEntry<H,J> next;

        WeakEntry( H key, int hash, J value, ReferenceQueue<J> queue, int index )
        {
            super(value, queue);

            this.key    = key;
            this.hash   = hash;
            this.index  = index;
        }

    }


}

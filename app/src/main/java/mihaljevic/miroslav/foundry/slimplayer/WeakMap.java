package mihaljevic.miroslav.foundry.slimplayer;

import android.util.Log;
import android.util.SparseIntArray;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;

/**
 * Created by miroslav on 17.02.17..
 */

//TODO - set upper limitation
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

    //private SparseIntArray mValueIndexMap;

    public WeakMap ()
    {
        this (DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR );
    }

    public WeakMap( int capacity )
    {
        this(capacity, DEFAULT_LOAD_FACTOR );
    }

    public WeakMap (int capacity, float loadFactor)
    {
        if (capacity <= 0)
            capacity = DEFAULT_CAPACITY;

        if (loadFactor <= 0)
            loadFactor = DEFAULT_LOAD_FACTOR;

        mCapacity = 1;
        while (mCapacity < capacity)
            mCapacity <<= 1;

        mLoadFactor = loadFactor;

        mTreshold = (int)(mLoadFactor * mCapacity);

        mTable = newTable( mCapacity );

        mQueue = new ReferenceQueue<>();

        //mValueIndexMap = new SparseIntArray( mCapacity );
    }


    private WeakEntry<K,V>[] newTable( int capacity)
    {
        return (WeakEntry<K,V>[]) (new WeakEntry[capacity]);
    }

    private void transfer( WeakEntry<K,V>[] oldTable, WeakEntry<K,V>[] newTable)
    {
        if (newTable.length < oldTable.length)
            throw new IllegalArgumentException( "New table is smaller than the old table" );

        int oldIndex;
        int newIndex;
        int newLength;
        WeakEntry<K,V> entry;
        WeakEntry<K,V> nextEntry;
        WeakEntry<K,V> newEntry;



        //We iterate only through used indexes
        /*for (int i = 0;i < mValueIndexMap.size();i++)
        {
            //Acquire entry from old table using old index
            oldIndex = mValueIndexMap.valueAt( i );
            entry = oldTable[oldIndex];
            //TODO - continue here- somehow oldIndex now became bigger than array (because we are modifying valueIndex map while iterating it)

            //Recalculate index using new size
            newIndex = indexFor( entry.hash, newTable.length );

            mValueIndexMap.put( entry.hashCode(), newIndex );

            //Use new index to put entry in new table
            newTable[newIndex] = entry;
        }*/

        newLength = newTable.length;
        //mValueIndexMap.clear();
        //TODO - optimize
        for (int i = 0; i < oldTable.length;i++)
        {
            entry = oldTable[i];


            while (entry != null)
            {
                nextEntry = entry.next;

                newEntry = recalculateEntry( entry, newLength );
                putInTable( newTable,  newEntry);

                entry = nextEntry;

            }


        }

    }

    WeakEntry<K,V> recalculateEntry(WeakEntry<K,V> oldEntry, int tableLength)
    {
        //IndexReference<V> indexReference;
        WeakEntry<K,V> entry;
        V value;
        K key;
        int hash;
        int index;

        //indexReference = oldEntry.ref.get();


        if (oldEntry == null || oldEntry.get() == null)
            return null;

        value = (V)oldEntry.get();
        key = oldEntry.key;
        hash = oldEntry.hash;
        index = indexFor( hash, tableLength );


        entry = new WeakEntry<>( key, hash, value, mQueue, index );

        return entry;
    }

    private void putInTable(WeakEntry<K,V>[] table, WeakEntry<K,V> newEntry)
    {
        if (newEntry == null)
            return;

        int hash;
        int index;
        K key;
        WeakEntry<K,V> oldEntry;
        WeakEntry<K,V> nextEntry;

        key = newEntry.key;
        hash = newEntry.hash;
        index = indexFor( hash, table.length );

        newEntry.next = null;



        oldEntry = table[index];

        if (oldEntry == null)
        {
            //If the place in table is free then we just put entry and we are done with it
            table[index] = newEntry;
        }
        else
        {
            //Case where we have collision, we need to put our entry at end of linked list

            //Stop either when we are at end of chain or when we have matching keys
            while ( oldEntry.next != null && !oldEntry.next.key.equals( key ))
            {
                oldEntry = oldEntry.next;
            }

            nextEntry = oldEntry.next;

            //We have stopped for some reason and we just add entry at the end of chain
            oldEntry.next = newEntry;


            if (nextEntry != null && nextEntry.key.equals( key ))
            {
                //Case where we are replacing value because we have matching keys
                newEntry.next = nextEntry.next;
            }
        }


    }

    private void resize(int capacity)
    {
        WeakEntry<K,V>[] oldTable;
        WeakEntry<K,V>[] newTable;

        //Clear stale entries
        clearStaleEntries();

        oldTable = mTable;
        newTable = newTable( capacity );

        //Transfer entries and recalculate indexes into new table
        transfer( oldTable, newTable );

        //Set new table as our active table
        mTable = newTable;

        mCapacity = mTable.length;

        mTreshold = (int) (mLoadFactor * mCapacity);

    }

    public void put(K key, V value)
    {
        int hash;
        int index;
        WeakEntry<K,V> newEntry;
        WeakEntry<K,V> oldEntry;
        WeakEntry<K,V> nextEntry;

        clearStaleEntries();

        hash = key.hashCode() ;
        index = indexFor(hash, mTable.length);

        newEntry = new WeakEntry<>( key, hash, value, mQueue, index );

        /*oldEntry = mTable[index];

        if (oldEntry == null)
        {
            //If the place in table is free then we just put entry and we are done with it
            mTable[index] = newEntry;
        }
        else
        {
            //Case where we have collision, we need to put our entry at end of linked list

            //Stop either when we are at end of chain or when we have matching keys
            while ( oldEntry.next != null && !oldEntry.next.key.equals( key ))
            {
                oldEntry = oldEntry.next;
            }

            nextEntry = oldEntry.next;

            //We have stopped for some reason and we just add entry at the end of chain
            oldEntry.next = newEntry;


            if (nextEntry != null && nextEntry.key.equals( key ))
            {
                //Case where we are replacing value because we have matching keys
                newEntry.next = nextEntry.next;
            }
        }*/

        putInTable( mTable, newEntry );

        //Save at which position is our value (we use that when clearing GC-ed values)
       // mValueIndexMap.put( value.hashCode(), index );
        mSize++;

        //If we have reached threshold we need to resize the table
        if (mSize >= mTreshold)
            resize( mCapacity * 2 );

    }

    public V get(K key)
    {
        int hash;
        int index;
        WeakEntry<K,V> entry;
        //IndexReference<V> indexReference;
        V value;

        clearStaleEntries();

        hash = key.hashCode();
        index = indexFor( hash, mTable.length );
        entry = mTable[index];

        //If place in table is empty just return null
        if (entry == null)
            return null;

        //Iterate until we are at the end or until we find a matching key
        while (entry.next != null && !entry.key.equals( key ) )
        {
            entry = entry.next;
        }

        //If the keys match, return that value
        if (entry.key.equals( key ))
        {
            value = (V)entry.get();

            return value == null ? null : value;
        }

        Log.w(TAG, "Entry was found at calculated index, but we couldn't match the keys");

        //Something went wrong
        return null;
    }

    private void clearStaleEntries()
    {
        V value;
        int index;
        WeakEntry<K,V> entry;
        //IndexReference<V> indexReference;
        WeakReference<V> weakReference;
        WeakEntry<K,V> weakEntry;
        int deletedCount;

        //Iterate through all recently cleared references
        while ( ( weakEntry = (( WeakEntry<K,V> ) ( mQueue.poll() )) ) != null )
        {
            deletedCount = 0;

            //Find index of our entry using ValueIndex map (actually SparseIntArray)
            index = weakEntry.index;
            value = (V)weakEntry.get();

            entry = mTable[index];

            if (entry == null)
            {
                Log.w(TAG, "Entry retrieved from value in ValueIndex map is null");
                continue;
            }

            //Try to find entry that has value in its reference
            while (entry.next != null && value == null)
            {
                entry = entry.next;
                deletedCount++;
            }

            if (value == null)
            {
                //If we didn't find entry that has value, just set it to null and we are done
                mTable[index] = null;
            }
            else
            {
                //Since we have entry that has value, set it as first in our linked sub-list
                mTable[index] = entry;

                //Iterate through other linked nodes trying to find entries without values
                while (entry != null && entry.next != null)
                {
                    if (entry.next.get() == null)
                    {
                        //We found empty entry, now bridge over to the one after it (if it exist, if not it is null, and end of this)
                        entry.next = entry.next.next;
                        deletedCount++;
                    }

                    entry = entry.next;
                }
            }

            //Remove this index from ValueIndexMap since we are done with it
            if (deletedCount > 0)
            {
                Log.d( TAG, "Deleted " + deletedCount + " on index " + index );
            }

            mSize -= deletedCount;

            if (mSize < 0)
                Log.e(TAG,"After clearing stale entries, mSize became less than 0");

        }
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


    //We don't use this
    private int hashJenkinsVariant( int poorHash)
    {
        poorHash += (poorHash <<  15) ^ 0xffffcd7d;
        poorHash ^= (poorHash >>> 10);
        poorHash += (poorHash <<   3);
        poorHash ^= (poorHash >>>  6);
        poorHash += (poorHash <<   2) + (poorHash << 14);

        return poorHash ^ (poorHash >>> 16);
    }




    private int indexFor(int hash, int length)
    {
        return hash & (length - 1);
    }



    /*private class Entry<U,I>
    {
        U key;
        int hash;
        WeakReference<IndexReference<I>> ref;

        Entry<U,I> next;

        Entry( U key, int hash, I value, ReferenceQueue<IndexReference<I>> queue, int index )
        {
            IndexReference<I> indexReference;

            this.key = key;
            this.hash = hash;


            indexReference = new IndexReference<>( index, value );

            ref = new WeakReference<IndexReference<I>>( indexReference, queue );
        }
    }*/

    private class WeakEntry<H,J> extends WeakReference<J>
    {
        H key;
        int hash;
        int index;

        WeakEntry<H,J> next;

        WeakEntry( H key, int hash, J value, ReferenceQueue<J> queue, int index )
        {
            super(value, queue);
            this.key = key;
            this.hash = hash;
            this.index = index;
        }

    }

    //Class that hold reference to object and index of it in table
    /*private class IndexReference<Z>
    {
        int index;
        Z value;

        public IndexReference( int index, Z value )
        {
            this.index = index;
            this.value = value;
        }
    }*/
}

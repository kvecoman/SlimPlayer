package mihaljevic.miroslav.foundry.slimplayer;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by miroslav on 07.02.17..
 *
 * Lean and fast LRU cache, thread safe
 *
 * @author Miroslav Mihaljević
 */

public class LRUCache<K, V>
{
    private final String TAG = getClass().getSimpleName();

    protected int mCapacity;
    protected int mSize = 0;
    private Node<K, V> mFirst;
    private Node<K, V> mLast;

    //private Set<WeakReference<Node<K,V>>> dbgList;

    private class Node<T, U>
    {
        Node<T, U> previous;
        Node<T, U> next;
        T key;
        U value;

        public Node( T key, U value, Node<T, U> previous, Node<T, U> next )
        {
            this.previous = previous;
            this.next = next;
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString()
        {
            return key.toString() + ": " + value.toString();
        }
    }

    public LRUCache( int capacity )
    {

        mCapacity = capacity;
        //dbgList = new HashSet<>(  );
    }

    public synchronized void put( K key, V value )
    {
        Log.v(TAG, "put() - Key = " + key.toString());

        Node<K, V> node;

        node = getNode( key );

        //If node exists already, then just front it
        if ( node != null )
        {
            frontNode( node );
            return;
        }

        node = new Node<>( key, value, null, null );

        //If this is the first node we are adding
        if ( mFirst == null )
        {
            Log.d(TAG,"mFirst == null");
            mFirst = node;
            mSize++;

        }
        //This happens when this is the second node we are adding
        else if (mLast == null)
        {
            Log.d(TAG,"mLast == null");
            mLast = node;
            mLast.next = mFirst;
            mFirst.previous = mLast;
            mSize++;
        }
        //This is case when we have first two nodes bu we are not full yet
        else if ( mSize < mCapacity )
        {
            Log.d(TAG,"mSize < mCapacity");
            node.previous = mFirst;
            mFirst.next = node;
            mFirst = node;
            mSize++;

        }
        //This is the case when we are full
        else if ( mSize == mCapacity )
        {
            Log.d(TAG,"mSize == mCapacity");
            node.previous = mFirst;
            mFirst.next = node;
            mFirst = node;
            mLast.previous = null;
            mLast = mLast.next;

            //Clean up removed node to prevent memory leaks
            mLast.previous.previous = null;
            mLast.previous.next = null;
            mLast.previous = null;
        }


        //dbgList.add( new WeakReference<>( node ) );

        //DEBUG PURPOSES - put()
        /*listAllNodes();
        mFirst = mFirst;*/
    }


    public synchronized V get( K key )
    {
        Log.v(TAG, "get() - Key = " + key.toString());

        Node<K, V> node;

        node = getNode( key );

        if ( node == null )
            return null;

        frontNode( node );

        //DEBUG PURPOSES - get()
        /*listAllNodes();
        mFirst = mFirst;*/

        return mFirst.value;

    }

    private synchronized void frontNode( Node<K, V> node )
    {
        Log.v(TAG, "front() - Key = " + node.key.toString());

        if ( node == mLast )
        {
            Log.d(TAG,"node == mLast");
            mFirst.next = mLast;
            mLast.previous = mFirst;
            mFirst = mLast;
            mLast = mLast.next;

            mLast.previous = null;
            mFirst.next = null;
        }
        else if ( node != mFirst )
        {
            Log.d(TAG,"node != mFirst");
            //Case when node isn't first nor last

            //Cut out node out of middle
            node.previous.next = node.next;
            node.next.previous = node.previous;

            mFirst.next = node;
            node.previous = mFirst;
            mFirst = node;
            mFirst.next = null;
        }
    }

    private synchronized Node<K, V> getNode( K key )
    {
        Node<K, V> node;

        node = mFirst;

        while ( node != null )
        {
            if ( node.key.equals( key ) )
                return node;

            node = node.previous;
        }

        return null;
    }

    public synchronized void remove( K key )
    {
        Log.v(TAG, "remove() - Key = " + key.toString());

        Node<K, V> node;

        node = getNode( key );

        if ( node == null )
            return;

        if ( node == mFirst )
        {
            Log.d(TAG, "node == mFirst");
            mFirst = node.previous;
            mFirst.next = null;
        }
        else if ( node == mLast )
        {
            Log.d(TAG, "node == mLast");
            mLast = mLast.next;
            mLast.previous = null;
        }
        else
        {
            Log.d(TAG, "else");
            node.previous.next = node.next;
            node.next.previous = node.previous;
        }

        //Node might still live on and cause leaks so we solve that here
        node.previous = null;
        node.next = null;

        //DEBUG PURPOSES - remove()
        /*listAllNodes();
        mFirst = mFirst;*/

        mSize--;

    }

    public synchronized void removeLastNode()
    {
        Log.v(TAG, "removeLast()");

        Node<K,V> nextNode;

        if (mFirst == null)
        {
            return;
        }
        else if (mLast == null)
        {
            //We have just the first node
            mFirst.previous = null;
            mFirst.next = null;
            mFirst = null;
        }
        else if (mFirst.previous == mLast)
        {
            //This means we have just 2 nodes
            mLast.previous = null;
            mLast.next = null;
            mLast = null;

            mFirst.previous = null;
        }
        else
        {
            //We have 3 or more nodes
            nextNode = mLast.next;
            nextNode.previous = null;

            mLast.previous = null;
            mLast.next = null;

            mLast = nextNode;
        }

        mSize--;
    }

    public synchronized void removeAll()
    {
        Log.v(TAG, "removeAll()");

        mSize = 0;

        Node<K,V> node;
        Node<K,V> previousNode;

        node = mFirst;

        while (node != null)
        {
            previousNode = node.previous;

            node.next = null;
            node.previous = null;

            node = previousNode;
        }

        mFirst = null;
        mLast = null;

    }

    //DEBUG METHOD - only used to verify correct behaviour of cache
   /*private List<Node<K,V>> listAllNodes()
    {
        List<Node<K,V>> list;
        Node<K,V> node;

        list = new ArrayList<>(  );

        node = mFirst;

        while (node != null)
        {
            list.add( node );

            node = node.previous;
        }

        //Set and return list also
        //dbgList = list;

        return  list;
    }*/

    private ArrayList<String> listNodesStr()
    {
        ArrayList<String> list;
        Node<K,V> node;

        list = new ArrayList<>( mSize );
        node = mFirst;

        while (node != null)
        {
            list.add( new String( node.key.toString() ));

            node = node.previous;
        }


        return list;
    }

    /*public int countNulls()
    {
        int count;

        count = 0;

        for (WeakReference<Node<K,V>> ref : dbgList)
        {
            if (ref.get() == null)
                count++;
        }

        return count;
    }*/

}

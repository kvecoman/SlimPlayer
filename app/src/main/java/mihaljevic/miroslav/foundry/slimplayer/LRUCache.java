package mihaljevic.miroslav.foundry.slimplayer;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

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

    private int mCapacity;
    private int mSize = 0;
    //private HashMap<K, Node<K,V>> mMap;
    private Node<K,V> mFirst;
    private Node<K,V> mLast;

    class Node<T, U>
    {
        Node<T,U> previous;
        Node<T,U> next;
        T key;
        U value;

        public Node(T key, U value, Node<T, U> previous, Node<T, U> next  )
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

    public LRUCache(int capacity)
    {
        mCapacity = capacity;

        //mMap = new HashMap<>( capacity, 1.1f );

    }

    public synchronized void put(K key, V value)
    {
        Node<K,V> node;

        node = getNode( key );

        if (node != null)
        {
            frontNode( node );
            return;
        }

        node = new Node<>( key, value, null, null );

        if (mFirst == null)
        {
            mFirst = node;
            mLast = node;
            mSize++;

            //mFirst.previous = mLast;
            //mLast.next = mFirst;

            //mMap.put( key, node );
        }
        else if (mSize < mCapacity)
        {
            node.previous = mFirst;
            mFirst.next = node;
            mFirst = node;
            mSize++;

            //mMap.put( key, node );
        }
        else if (mSize == mCapacity)
        {
            node.previous = mFirst;
            mFirst.next = node;
            mFirst = node;
            mLast.previous = null;
            mLast = mLast.next;
            mLast.previous = null;
        }
    }


    public synchronized V get(K key)
    {
        Node<K,V> node;

        node = getNode( key );

        if (node == null)
            return null;

        frontNode( node );

        mFirst = mFirst; //DEBUG PURPOSES

        return mFirst.value;

    }

    private synchronized void frontNode(Node<K,V> node)
    {
        if (node == mFirst)
        {
            return;
        }
        else if (node == mLast)
        {
            mFirst.next = mLast;
            mFirst = mLast;
            mLast = mLast.next;
            mLast.previous = null;
            mFirst.next = null;
        }
        else
        {
            //Cut out node out of middle
            node.previous.next = node.next;
            node.next.previous = node.previous;

            mFirst.next = node;
            node.previous = mFirst;
            mFirst = node;
            mFirst.next = null;
        }
    }

    private synchronized Node<K,V> getNode(K key)
    {
        Node<K,V> node;

        node = mFirst;

        while (node != null)
        {
            if (node.key.equals( key ))
                return node;

            node = node.previous;
        }

        return node;
    }

    public synchronized void remove(K key)
    {
        Node<K,V> node;

        node = getNode( key );

        if (node == null)
            return;

        if (node == mFirst)
        {
            mFirst = node.previous;
            mFirst.next = null;
        }
        else if (node == mLast)
        {
            mLast.next.previous = null;
            mLast = null;
        }
        else
        {
            node.previous.next = node.next;
            node.next.previous = node.previous;
        }

        mSize--;

    }

    //DEBUG METHOD - only used to verify correct behaviour of cache
    private List<Node<K,V>> listAllNodes()
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

        return  list;
    }

}

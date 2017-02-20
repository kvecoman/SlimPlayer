package mihaljevic.miroslav.foundry.slimplayer;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Created by miroslav on 16.02.17..
 *
 * This is actually some sort of linked list
 *
 * @author Miroslav Mihaljević
 */

public class CrazyLinkedList<V>
{
    private final String TAG = getClass().getSimpleName();

    private int mSize = 0;
    private Node<V> mFirst;
    private Node<V> mLast;
    private ReferenceQueue<Object> mQueue = new ReferenceQueue<>();

    private class Node<U>
    {
        String key;
        WeakReference<U> ref;

        Node<U> next;
        Node<U> previous;

        public Node( String key, U value, Node<U> next, Node<U> previous )
        {
            this.key = key;
            this.ref = new WeakReference<U>( value, CrazyLinkedList.this.mQueue );
            this.next = next;
            this.previous = previous;
        }
    }

    public CrazyLinkedList()
    {
    }

    public void put(String key, V value)
    {
        if (key == null || value == null)
            return;

        Node<V> oldNode;
        Node<V> nodePrevious;
        Node<V> nodeNext;
        Node<V> newNode;

        oldNode = getNode(key);
        newNode = new Node<>(key, value,null,null);

        //If the node already exist
        if (oldNode != null)
        {
            //Replace old node with new one
            nodePrevious = oldNode.previous;
            nodeNext = oldNode.next;



            if (nodePrevious != null)
            {
                nodePrevious.next = oldNode;
                newNode.previous = nodePrevious;
            }

            if (nodeNext != null)
            {
                nodeNext.previous = oldNode;
                newNode.next = nodeNext;
            }

            oldNode.next = null;
            oldNode.previous = null;

            return;

        }

        //If we are adding new node
        if (mFirst == null)
        {
            mFirst = newNode;
            mSize++;
        }
        else if( mLast == null )
        {
            newNode.next = mFirst;
            newNode.previous = null;

            mFirst.previous = newNode;

            mLast = newNode;

            mSize++;
        }
        else
        {
            newNode.next = mLast;
            newNode.previous = null;

            mLast = newNode;

            mSize++;
        }

    }

    public V get(String key)
    {
        Node<V> node;

        node = getNode( key );

        if (node == null)
            return null;

        return node.ref.get();
    }

    private Node<V> getNode(String key)
    {


        if (key == null)
            return null;

        Node<V> node;

        node = mFirst;

        while (node != null)
        {
            if (node.key.equals( key ))
                return node;

            node = node.previous;
        }

        return null;
    }


    private void removeGarbage()
    {
        Node<V> node;

        node = mFirst;

        //TODO - continue here, see about clearing garbage collected entries (maybe change this whole thing to use logic from hash tables??)

        mQueue.poll();
    }







}

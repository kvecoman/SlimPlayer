//
// Created by miroslav on 14.04.17..
//

#ifndef SLIMPLAYER_SYNCHRONIZEDLINKEDLIST_H
#define SLIMPLAYER_SYNCHRONIZEDLINKEDLIST_H

#include <mutex>

template <class U>
class Node
{
public:
    Node * previous;
    Node * next;
    U * value;

    Node( U * value )
    {
        this->value = value;
    }
};




template <class T>
class SynchronizedLinkedList
{
private:
    Node<T> * mFirst;
    Node<T> * mLast;

    Node<T> * mCurrent;

    std::mutex lock;

public:

    SynchronizedLinkedList()
    {
        mFirst      = nullptr;
        mLast       = nullptr;
        mCurrent    = nullptr;
    };

    void push( T * valuePtr )
    {
        __android_log_print( ANDROID_LOG_VERBOSE, "SynchronizedLinkedList", "push()" );

        lock.lock();

        Node<T> * newNode;

        newNode = new Node<T>( valuePtr );

        if ( mFirst == nullptr )
        {
            newNode->previous  = nullptr;
            newNode->next      = nullptr;

            mFirst = newNode;
        }
        else if( mLast == nullptr )
        {
            mFirst->next = newNode;

            newNode->previous   = mFirst;
            newNode->next       = nullptr;

            mLast = newNode;
        }
        else
        {
            newNode->previous   = mLast;
            newNode->next       = nullptr;

            mLast = newNode;
        }

        lock.unlock();
    }

    void begin()
    {
        __android_log_print( ANDROID_LOG_VERBOSE, "SynchronizedLinkedList", "begin()" );

        lock.lock();

        mCurrent = mFirst;

        lock.unlock();
    }

    T * currentItem()
    {
        __android_log_print( ANDROID_LOG_VERBOSE, "SynchronizedLinkedList", "currentItem()" );

        lock.lock();

        T * valuePtr;

        if ( mCurrent == nullptr )
        {
            lock.unlock();
            return nullptr;
        }

        valuePtr = mCurrent->value;

        lock.unlock();

        return  valuePtr;


    }

    void next()
    {

        __android_log_print( ANDROID_LOG_VERBOSE, "SynchronizedLinkedList", "next()" );

        lock.lock();
        mCurrent = mCurrent->next;
        lock.unlock();
    }

    bool empty()
    {
        bool empty;

        lock.lock();

        empty = ( mFirst == nullptr );

        lock.unlock();

        return empty;
    }



    void clear()
    {
        lock.lock();

        mFirst      = nullptr;
        mLast       = nullptr;
        mCurrent    = nullptr;

        lock.unlock();
    }


    T * removeCurrent()
    {
        __android_log_print( ANDROID_LOG_VERBOSE, "SynchronizedLinkedList", "removeCurrent()" );

        lock.lock();

        Node<T> * removedNode;
        Node<T> * nodeBefore;
        Node<T> * nodeAfter;

        removedNode = mCurrent;

        if ( removedNode == nullptr )
        {
            lock.unlock();
            return nullptr;
        }


        if ( removedNode == mFirst )
        {

            mFirst      = mFirst->next;
            mCurrent    = mFirst;

            removedNode->next       = nullptr;
            removedNode->previous   = nullptr;

            //If the list did not became empty, the properly clean up the first element
            if ( mFirst != nullptr )
            {
                mFirst->previous = nullptr;

                if ( mFirst == mLast )
                {
                    mFirst->next    = nullptr;
                    mLast           = nullptr;
                }
            }

        }
        else if ( removedNode == mLast )
        {

            mLast       = mLast->previous;
            mCurrent    = mLast;

            mLast->next = nullptr;

            if ( mLast == mFirst )
            {
                mLast = nullptr;
                mFirst->previous = nullptr;
            }

            removedNode->next       = nullptr;
            removedNode->previous   = nullptr;
        }
        else
        {
            nodeBefore = removedNode->previous;
            nodeAfter = removedNode->next;

            removedNode->next       = nullptr;
            removedNode->previous   = nullptr;

            nodeBefore->next    = nodeAfter;
            nodeAfter->previous = nodeBefore;

            mCurrent = nodeAfter;


        }

        lock.unlock();

        return removedNode->value;
    }


};


#endif //SLIMPLAYER_SYNCHRONIZEDLINKEDLIST_H

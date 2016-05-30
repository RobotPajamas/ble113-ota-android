package com.robotpajamas.android.ble113_ota.utils;

import java.io.File;
import java.util.Arrays;

public class DataChunker {

    // Hard-coding to use 16 byte chunks. Max is 20 bytes, but this lets us avoid handling remainders
    private final int CHUNK_LENGTH = 16;

    private final int mTotalChunks;
    private byte[] mFileData = null;
    private int mCurrentChunk;
    private int mLength = 0;
    private int mRemainingLength = 0;

    public DataChunker(File file) {

        mFileData = data;
        mLength = mFileData.length;

        // Pointers that will continually need updating
        mCurrentChunk = 0;
        mRemainingLength = mLength;

        // Calculate how many chunks will be needed (OTA files are always multiples of 16 bytes)
        mTotalChunks = mLength / CHUNK_LENGTH;
    }

    public byte[] next() {
        int rangeStart = mCurrentChunk * CHUNK_LENGTH;
        if (rangeStart + CHUNK_LENGTH > mLength) {
            return null;
        }

        // Update trackers
        ++mCurrentChunk;
        mRemainingLength -= CHUNK_LENGTH;

        return Arrays.copyOfRange(mFileData, rangeStart, rangeStart + CHUNK_LENGTH);
    }

    public boolean hasNext() {
        return mRemainingLength > 0;
    }

    public void reset() {
        mCurrentChunk = 0;
        mRemainingLength = mLength;
    }

    public int getTotalChunks() {
        return mTotalChunks;
    }

    public int getCurrentChunk() {
        return mCurrentChunk;
    }
}

package com.example.leeronziv.alohaworld_chatvideo;

public class Conversation
{
    public boolean seen;
    public long time_stamp;

    public Conversation(boolean seen, long timestamp)
    {
        this.seen = seen;
        this.time_stamp = timestamp;
    }

    public Conversation() {}

    public boolean isSeen() { return seen; }

    public void setSeen(boolean seen) { this.seen = seen; }

    public long getTimestamp() { return time_stamp; }

    public void setTimestamp(long timestamp) { this.time_stamp = timestamp; }
}

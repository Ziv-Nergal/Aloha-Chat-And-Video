package com.example.leeronziv.alohaworld_chatvideo;

public class Friend
{
    public String name;
    public String status;
    public String thumb_image;

    public Friend() {}

    public Friend(String name, String date, String thumb_image)
    {
        this.name = name;
        this.status = date;
        this.thumb_image = thumb_image;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getThumb_image()
    {
        return thumb_image;
    }

    public void setThumb_image(String thumb_image)
    {
        this.thumb_image = thumb_image;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}

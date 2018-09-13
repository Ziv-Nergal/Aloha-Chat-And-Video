package com.example.leeronziv.alohaworld_chatvideo;

public class User
{
    public String name;
    public String image;
    public String status;
    public String thumb_image;

    public User(String name, String image, String status, String thumb_image)
    {
        this.name = name;
        this.image = image;
        this.status = status;
        this.thumb_image = thumb_image;
    }

    public User()
    {
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getImage()
    {
        return image;
    }

    public void setImage(String image)
    {
        this.image = image;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getThumb_image()
    {
        return thumb_image;
    }

    public void setthumb_image(String thumb_image)
    {
        this.thumb_image = thumb_image;
    }
}

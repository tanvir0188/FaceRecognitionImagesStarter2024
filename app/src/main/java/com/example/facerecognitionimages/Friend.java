package com.example.facerecognitionimages;

public class Friend {
    private String name, photo;
    private float[] embedding;

    public Friend(String name, String photo, float[] embedding) {
        this.name = name;
        this.photo = photo;
        this.embedding = embedding;
    }

    public String getName() {
        return name;
    }

    public String getPhoto() {
        return photo;
    }

    public float[] getEmbedding() {
        return embedding;
    }
}

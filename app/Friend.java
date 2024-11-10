public class Friend {
    private String name;
    private String photo;
    private float embedding[];

    public String getName() {
        return name;
    }

    public String getPhoto() {
        return photo;
    }

    public float[] getEmbedding() {
        return embedding;
    }


    public Friend(String name, String photo, float[] embedding) {
        this.name = name;
        this.photo = photo;
        this.embedding = embedding;
    }
}

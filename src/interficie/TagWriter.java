package interficie;

import domini.Tag;

public interface TagWriter extends TagReader {
    Tag addTag(String nom);
    void deleteTag(Tag tag);
    void renameTag(int id, String newNom);
    void addLlibreToTag(long isbn, int tagId);
    void removeLlibreFromTag(long isbn, int tagId);
}

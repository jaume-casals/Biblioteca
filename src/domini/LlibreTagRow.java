package domini;

/** Membership row joining a book (isbn) to a tag (tagId). */
public record LlibreTagRow(long isbn, int tagId) {}

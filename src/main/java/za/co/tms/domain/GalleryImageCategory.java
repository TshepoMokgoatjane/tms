package za.co.tms.domain;

public enum GalleryImageCategory {

    PROPERTY,
    ROOM,
    EXTERIOR,
    AMENITY,
    // Photos that belong to a specific advertised unit (tied to a Room via roomId).
    UNIT
}

package za.co.tms.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Gallery entity representing images of the property, the room, the interior, and exterior")
public class GalleryImage extends AuditModel implements Serializable {

 @Serial
 private static final long serialVersionUID = 1L;

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 @Column(updatable = false, nullable = false)
 private Long id;

 @Schema(description = "Image title/caption", example = "Living room view")
 private String title;

 @Schema(description = "Image description", example = "Spacious living room with natural light")
 @Column(columnDefinition = "TEXT")
 private String description;

 @Schema(description = "Original uploaded filename", example = "living-room.jpg")
 private String originalFilename;

 @Schema(description = "MIME type", example = "image/jpeg")
 private String contentType;

 @Schema(description = "File size in bytes", example = "245000")
 private Long fileSize;

 @Lob
 @JsonIgnore
 @Column(name = "image_data", columnDefinition = "LONGBLOB")
 @Schema(hidden = true)
 private byte[] imageData;

 @Enumerated(EnumType.STRING)
 @Column(nullable = false)
 @Schema(description = "Image category", example = "ROOM")
 private GalleryImageCategory category;

 @Enumerated(EnumType.STRING)
 @Column(nullable = false)
 @Schema(description = "Image visibility status", example = "ACTIVE")
 private GalleryImageStatus status;

 @Schema(description = "Display order for sorting", example = "1")
 private int displayOrder;

 @Schema(description = "Owning room id when this image is a specific unit's photo (category UNIT). Null for general gallery images.", example = "5")
 private Long roomId;

 /**
  * Metadata-only constructor used by list/projection queries so the heavy {@code imageData}
  * blob is NEVER loaded for list views (prevents loading every image's bytes into heap, which
  * caused OutOfMemoryError). {@code imageData} is deliberately left null; it is only populated
  * when a single image is fetched by id for serving.
  */
 public GalleryImage(Long id, String title, String description, String originalFilename,
                     String contentType, Long fileSize, GalleryImageCategory category,
                     GalleryImageStatus status, int displayOrder, Long roomId) {
     this.id = id;
     this.title = title;
     this.description = description;
     this.originalFilename = originalFilename;
     this.contentType = contentType;
     this.fileSize = fileSize;
     this.category = category;
     this.status = status;
     this.displayOrder = displayOrder;
     this.roomId = roomId;
 }
}

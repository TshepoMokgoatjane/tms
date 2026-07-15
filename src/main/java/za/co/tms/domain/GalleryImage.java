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
}
